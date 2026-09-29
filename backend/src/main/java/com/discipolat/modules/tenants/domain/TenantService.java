package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.domain.DictionaryService;
import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.api.UpdateTenantRequest;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion des tenants (églises) de la plateforme SaaS.
 *
 * <p>La table {@code tenants} est l'exception au modèle tenant-scopé : elle est
 * GLOBALE (un tenant ne peut pas être filtré par tenant_id — elle le définit).
 * Seul un ADMIN (super-utilisateur) peut lister/créer/modifier des tenants.
 * Toute mutation est tracée dans le journal d'audit.
 *
 * <p>La création d'un tenant est l'amorce d'une nouvelle église : l'utilisateur
 * qui l'onboardera sera rattaché via un JWT portant le {@code tenantId} de ce
 * nouveau tenant (flux multi-tenant V70).
 *
 * <p><b>Audit (constat M1).</b> Toute mutation — création, mise à jour,
 * changement de plan, suspension, réactivation, fin d'onboarding — écrit
 * exactement un événement dans le journal d'audit chaîné par hachage, porteur de
 * l'acteur courant. Les lectures ({@link #list()}, {@link #get(UUID)}) n'en
 * écrivent aucun.
 */
@Service
@Transactional
public class TenantService {

    private static final String DEFAULT_PLAN = "free";

    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final TenantFeatureService featureService;
    private final TenantPlanPolicy planPolicy;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanService saasPlanService;
    private final ApplicationEventPublisher eventPublisher;
    private final DictionaryService dictionaryService;

    public TenantService(TenantRepository tenantRepository, AuditService auditService,
                         EntityPropagationPublisher propagationPublisher,
                         TenantFeatureService featureService,
                         TenantPlanPolicy planPolicy,
                         TenantSubscriptionRepository subscriptionRepository,
                         SaasPlanService saasPlanService,
                         ApplicationEventPublisher eventPublisher,
                         DictionaryService dictionaryService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.featureService = featureService;
        this.planPolicy = planPolicy;
        this.subscriptionRepository = subscriptionRepository;
        this.saasPlanService = saasPlanService;
        this.eventPublisher = eventPublisher;
        this.dictionaryService = dictionaryService;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(Tenant::getCreatedAt))
                .map(TenantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        return TenantResponse.from(getEntity(id));
    }

    public TenantResponse create(CreateTenantRequest request) {
        String slug = request.slug().toLowerCase();
        if (tenantRepository.existsBySlug(slug)) {
            throw new BusinessRuleException(
                    "Un tenant avec ce slug existe déjà : " + slug, "SLUG_TAKEN");
        }
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .slug(slug)
                .status(TenantStatus.ACTIVE)
                .plan(request.plan() != null && !request.plan().isBlank()
                        ? request.plan() : DEFAULT_PLAN)
                .country(request.country())
                .currency(request.currency())
                .timezone(request.timezone())
                .locale(request.locale())
                .build();
        tenant = tenantRepository.save(tenant);
        auditService.logSimple("TENANT_CREATED", "TENANT", tenant.getId());
        if (tenant.getId() == null) {
            return TenantResponse.from(tenant);
        }
        ensureInitialSubscription(tenant);
        UUID previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenant.getId());
            seedDefaultModules(tenant.getId());
            // A3 (item 10) — chaque nouveau tenant reçoit SON jeu d'entrées de
            // dictionnaire : les termes (« âme », « disciple », « pasteur »…) sont
            // ensuite surchargeables par l'admin du tenant, sans code.
            dictionaryService.seedForTenant(tenant.getId());
            propagationPublisher.publishCreated("TENANT", tenant.getId(),
                    Map.of("name", tenant.getName(), "slug", tenant.getSlug()),
                    "Tenant créé: " + tenant.getName());
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
        return TenantResponse.from(tenant);
    }

    private void ensureInitialSubscription(Tenant tenant) {
        if (subscriptionRepository.findCurrentByTenantId(tenant.getId()).isPresent()) {
            return;
        }
        TenantPlanPolicy.ResolvedPlan resolved = planPolicy.resolve(tenant);
        if (!resolved.hasCatalogPlan() || !resolved.limitsValid() || !resolved.featuresValid()
                || !Boolean.TRUE.equals(resolved.plan().getIsActive())) {
            throw new BusinessRuleException("Plan catalogue indisponible pour le nouveau tenant", "PLAN_CONFIGURATION_INVALID");
        }
        Instant now = Instant.now();
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(tenant.getId())
                .planKey(resolved.plan().getKey())
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle("monthly")
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plus(30, ChronoUnit.DAYS))
                .cancelAtPeriodEnd(false)
                .quotasJson(resolved.plan().getLimitsJson())
                .build());
        tenant.setPlan(resolved.plan().getKey());
        tenant.setFeaturesJson(resolved.plan().getFeaturesJson());
        tenantRepository.save(tenant);
    }

    private void seedDefaultModules(UUID tenantId) {
        // Core modules that should always be enabled
        String[] coreModules = {"people", "events", "notifications", "dashboard", "org"};
        
        for (String moduleCode : coreModules) {
            if (!featureService.getFeature(tenantId, moduleCode).isPresent()) {
                featureService.enableFeature(tenantId, moduleCode, Map.of());
            }
        }
    }

    public TenantResponse update(UUID id, UpdateTenantRequest request) {
        Tenant tenant = getEntity(id);
        TenantStatus statusBefore = tenant.getStatus();
        String planBefore = tenant.getPlan();
        if (request.name() != null && !request.name().isBlank()) {
            tenant.setName(request.name());
        }
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }
        if (request.plan() != null && !request.plan().isBlank()) {
            String canonicalPlan = planPolicy.normalizePlanKey(request.plan());
            if (canonicalPlan == null || saasPlanService.getPlan(canonicalPlan).isEmpty()) {
                throw new BusinessRuleException("Le plan sélectionné n'existe pas ou n'est pas actif", "INVALID_PLAN");
            }
            String billingCycle = subscriptionRepository.findCurrentByTenantId(id)
                    .map(subscription -> subscription.getBillingCycle())
                    .orElse("monthly");
            saasPlanService.subscribe(id, canonicalPlan, billingCycle, null);
        }
        if (request.country() != null) {
            tenant.setCountry(request.country());
        }
        if (request.currency() != null) {
            tenant.setCurrency(request.currency());
        }
        if (request.timezone() != null) {
            tenant.setTimezone(request.timezone());
        }
        if (request.locale() != null) {
            tenant.setLocale(request.locale());
        }
        if (request.brandingJson() != null) {
            tenant.setBrandingJson(request.brandingJson());
        }
        if (request.featuresJson() != null) {
            tenant.setFeaturesJson(request.featuresJson());
        }
        if (request.settingsJson() != null) {
            tenant.setSettingsJson(request.settingsJson());
        }
        tenant = tenantRepository.save(tenant);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishUpdated("TENANT", tenant.getId(),
                Map.of(), Map.of("name", tenant.getName(), "plan", tenant.getPlan()),
                "Tenant mis à jour: " + tenant.getName());
        auditService.logSimple("TENANT_UPDATED", "TENANT", tenant.getId());
        if (planBefore != null && !planBefore.equals(tenant.getPlan())) {
            auditService.logSimple("TENANT_PLAN_CHANGED", "TENANT", tenant.getId());
        }
        publishStatusChangeIfNeeded(tenant, statusBefore);
        return TenantResponse.from(tenant);
    }

    public void deactivate(UUID id) {
        Tenant tenant = getEntity(id);
        String oldStatus = tenant.getStatus().name();
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("TENANT", tenant.getId(),
                oldStatus, TenantStatus.SUSPENDED.name(),
                "Tenant désactivé: " + tenant.getName());
        auditService.logSimple("TENANT_SUSPENDED", "TENANT", tenant.getId());
        publishStatusChangeIfNeeded(tenant, TenantStatus.valueOf(oldStatus));
    }

    public void reactivate(UUID id) {
        Tenant tenant = getEntity(id);
        String oldStatus = tenant.getStatus().name();
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
        propagationPublisher.publishStatusChanged("TENANT", tenant.getId(),
                oldStatus, TenantStatus.ACTIVE.name(),
                "Tenant réactivé: " + tenant.getName());
        auditService.logSimple("TENANT_REACTIVATED", "TENANT", tenant.getId());
        publishStatusChangeIfNeeded(tenant, TenantStatus.valueOf(oldStatus));
    }

    /**
     * Marque l'onboarding du tenant comme terminé (décision D2, migration V183).
     *
     * <p><b>Idempotent et non destructif</b> : si la date est déjà renseignée, elle
     * n'est <b>jamais</b> écrasée — la fin réelle de l'onboarding d'une église ne
     * bouge pas parce qu'un administrateur a rejoué une étape. L'acteur n'est
     * enregistré qu'à la première complétion.
     *
     * @return {@code true} si c'est cette appel qui a(finalisé) l'onboarding
     */
    public boolean markOnboardingCompleted(UUID actorId) {
        Tenant tenant = getEntityForOnboarding(TenantContext.getTenantId());
        if (tenant == null) {
            return false;
        }
        if (tenant.getOnboardingCompletedAt() != null) {
            // Déjà terminé : on conserve la date ET l'acteur d'origine.
            return false;
        }
        tenant.setOnboardingCompletedAt(Instant.now());
        tenant.setOnboardingCompletedBy(actorId);
        tenantRepository.save(tenant);
        auditService.logSimple("TENANT_ONBOARDING_COMPLETED", "TENANT", tenant.getId());
        return true;
    }

    private Tenant getEntityForOnboarding(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }
        return tenantRepository.findById(tenantId).orElse(null);
    }

    /**
     * Publie {@link TenantStatusChangedEvent} pour que {@code TenantStatusGuard}
     * invalide immédiatement son cache de statut (constat B1) : une suspension ou
     * une réactivation est effective sans attendre le TTL de 30 s.
     */
    private void publishStatusChangeIfNeeded(Tenant tenant, TenantStatus previousStatus) {
        if (previousStatus == null || previousStatus == tenant.getStatus()) {
            return;
        }
        eventPublisher.publishEvent(
                new TenantStatusChangedEvent(tenant.getId(), previousStatus, tenant.getStatus()));
    }

    public Optional<Tenant> findBySlug(String slug) {
        return tenantRepository.findBySlug(slug);
    }

    public boolean existsBySlug(String slug) {
        return tenantRepository.existsBySlug(slug);
    }

    private Tenant getEntity(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", id));
    }

    /**
     * Get tenant entity by ID (for internal services like rate limiting).
     */
    @Transactional(readOnly = true)
    public Tenant getTenant(UUID id) {
        return getEntity(id);
    }
}
