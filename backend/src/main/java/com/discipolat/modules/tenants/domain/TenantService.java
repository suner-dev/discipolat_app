package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.api.UpdateTenantRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
 */
@Service
@Transactional
public class TenantService {

    private static final String DEFAULT_PLAN = "free";

    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public TenantService(TenantRepository tenantRepository, AuditService auditService,
                         EntityPropagationPublisher propagationPublisher) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
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
                .build();
        tenant = tenantRepository.save(tenant);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("TENANT", tenant.getId(),
                Map.of("name", tenant.getName(), "slug", tenant.getSlug()),
                "Tenant créé: " + tenant.getName());
        return TenantResponse.from(tenant);
    }

    public TenantResponse update(UUID id, UpdateTenantRequest request) {
        Tenant tenant = getEntity(id);
        if (request.name() != null && !request.name().isBlank()) {
            tenant.setName(request.name());
        }
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }
        if (request.plan() != null && !request.plan().isBlank()) {
            tenant.setPlan(request.plan());
        }
        tenant = tenantRepository.save(tenant);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishUpdated("TENANT", tenant.getId(),
                Map.of(), Map.of("name", tenant.getName(), "plan", tenant.getPlan()),
                "Tenant mis à jour: " + tenant.getName());
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
    }

    private Tenant getEntity(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", id));
    }
}
