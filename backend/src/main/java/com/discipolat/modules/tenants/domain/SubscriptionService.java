package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.Payloads;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing tenant subscriptions, billing, and Stripe integration.
 */
@Service
@Transactional
public class SubscriptionService {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanRepository planRepository;
    private final FeatureAccessService featureAccessService;
    private final QuotaService quotaService;
    private final PricingCatalogService catalogService;
    private final ObjectMapper objectMapper;

    public SubscriptionService(TenantRepository tenantRepository,
                               TenantSubscriptionRepository subscriptionRepository,
                               SaasPlanRepository planRepository,
                               FeatureAccessService featureAccessService,
                               QuotaService quotaService,
                               PricingCatalogService catalogService,
                               ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.featureAccessService = featureAccessService;
        this.quotaService = quotaService;
        this.catalogService = catalogService;
        this.objectMapper = objectMapper;
    }

    /**
     * Subscribe a tenant to a plan.
     */
    public TenantSubscription subscribe(UUID tenantId, String planKey, String billingCycle, UUID adminId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        String canonicalKey = TenantPlanPolicy.canonicalizePlanKey(planKey);
        if (canonicalKey == null) {
            throw new BusinessRuleException("Plan not found: " + planKey, "PLAN_NOT_FOUND");
        }
        SaasPlan plan = planRepository.findByKeyIgnoreCaseAndIsActiveTrue(canonicalKey)
                .orElseThrow(() -> new BusinessRuleException("Plan not found: " + planKey, "PLAN_NOT_FOUND"));

        if (!plan.getIsActive()) {
            throw new BusinessRuleException("Plan is not active: " + planKey, "PLAN_INACTIVE");
        }

        // Cancel existing subscription if any
        Optional<TenantSubscription> existing = subscriptionRepository.findCurrentByTenantId(tenantId);
        if (existing.isPresent()) {
            TenantSubscription sub = existing.get();
sub.setStatus(SubscriptionStatus.CANCELED);
            sub.setCanceledAt(Instant.now());
            sub.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(sub);
        }

        // Create new subscription
        Instant now = Instant.now();
        Instant periodEnd = "yearly".equalsIgnoreCase(billingCycle) ?
                LocalDateTime.now().plusYears(1).atZone(java.time.ZoneId.systemDefault()).toInstant() :
                LocalDateTime.now().plusMonths(1).atZone(java.time.ZoneId.systemDefault()).toInstant();

        PricingCatalogService.ResolvedPrice price = resolvePriceForSnapshot(plan, billingCycle);

        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId)
                .planKey(plan.getKey())
                .status(SubscriptionStatus.TRIAL)
                .billingCycle(billingCycle)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(false)
                .trialEndsAt(LocalDateTime.now().plusDays(14).atZone(java.time.ZoneId.systemDefault()).toInstant())
                .quotasJson(plan.getLimitsJson())
                // Figeage du prix (contrat §2.4). Une hausse de tarif décidée plus
                // tard ne doit pas modifier la facture de cet abonnement en cours :
                // le client a souscrit à un prix, on le lui tient.
                .priceCurrency(price == null ? null : price.currency())
                .priceMinor(price == null ? null : price.amountMinor())
                .priceCycle(price == null ? null : price.cycle().name())
                .priceSource("ADMIN")
                .priceSnapshotJson(buildPriceSnapshot(plan, price))
                .build();

        subscription = subscriptionRepository.save(subscription);

        // Update tenant plan and activate features
        tenant.setPlan(plan.getKey());
        Map<String, Object> features = parseJson(plan.getFeaturesJson());
        tenant.setFeaturesJson(toJson(features));
        tenantRepository.save(tenant);

        // Activate features
        for (Map.Entry<String, Object> entry : features.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                featureAccessService.setFeature(tenant.getId(), entry.getKey(), true, null);
            }
        }

        return subscription;
    }

    /**
     * Prix à figer sur l'abonnement au moment de la souscription.
     *
     * <p>Aucun prix trouvé n'est pas une erreur ici : la souscription manuelle
     * par un administrateur est un acte de gestion, pas un encaissement Stripe. On
     * fige alors ce qu'on peut (null) et l'écran d'abonnement le dira
     * honnêtement plutôt que d'afficher un tarif au hasard.</p>
     */
    private PricingCatalogService.ResolvedPrice resolvePriceForSnapshot(SaasPlan plan, String billingCycle) {
        try {
            var cycle = BillingCycle.fromString(billingCycle);
            Optional<PricingCatalogService.ResolvedPrice> resolved =
                    catalogService.resolvePrice(plan.getKey(), null, cycle, null);
            return resolved.orElse(null);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    /**
     * Instantané explicite du plan figé avec l'abonnement.
     *
     * <p>Le but est opérationnel autant qu'historique : quand un client
     * contestera une facture dans deux ans, on doit pouvoir répondre « vous
     * étiez à 15 000 FCFA/mois quand vous avez souscrit », et non « c'est ce que
     * le catalogue dit aujourd'hui ».</p>
     */
    private String buildPriceSnapshot(SaasPlan plan, PricingCatalogService.ResolvedPrice price) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("frozenAt", Instant.now().toString());
        snapshot.put("planKey", plan.getKey());
        snapshot.put("planName", plan.getName());
        if (price != null) {
            snapshot.put("currency", price.currency());
            snapshot.put("amountMinor", price.amountMinor());
            snapshot.put("cycle", price.cycle().name());
            snapshot.put("channels", price.channels().stream().map(Enum::name).toList());
        } else {
            snapshot.put("note", "Aucun prix de catalogue applicable a la date de la souscription.");
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Cancel subscription at period end.
     */
    public void cancelAtPeriodEnd(UUID tenantId, UUID adminId) {
        TenantSubscription sub = subscriptionRepository.findCurrentByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleException("No active subscription", "NO_SUBSCRIPTION"));

        sub.setCancelAtPeriodEnd(true);
        sub.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(sub);
    }

    /**
     * Cancel subscription immediately.
     */
    public void cancelImmediately(UUID tenantId, UUID adminId) {
        TenantSubscription sub = subscriptionRepository.findCurrentByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleException("No active subscription", "NO_SUBSCRIPTION"));

        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setCanceledAt(Instant.now());
        sub.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(sub);

        // Downgrade to free plan
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        tenant.setPlan("FREE");
        tenant.setFeaturesJson("{}");
        tenantRepository.save(tenant);
    }

    /**
     * Reactivate a canceled subscription.
     */
    public void reactivate(UUID tenantId, UUID adminId) {
        TenantSubscription sub = subscriptionRepository.findCurrentByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleException("No subscription found", "NO_SUBSCRIPTION"));

        if (sub.getStatus() == SubscriptionStatus.CANCELED) {
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCancelAtPeriodEnd(false);
            sub.setCanceledAt(null);
            subscriptionRepository.save(sub);
        }
    }

    /**
     * Change plan (upgrade/downgrade).
     */
    public TenantSubscription changePlan(UUID tenantId, String newPlanKey, UUID adminId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        String canonicalKey = TenantPlanPolicy.canonicalizePlanKey(newPlanKey);
        if (canonicalKey == null) {
            throw new BusinessRuleException("Plan not found: " + newPlanKey, "PLAN_NOT_FOUND");
        }
        SaasPlan newPlan = planRepository.findByKeyIgnoreCaseAndIsActiveTrue(canonicalKey)
                .orElseThrow(() -> new BusinessRuleException("Plan not found: " + newPlanKey, "PLAN_NOT_FOUND"));

        TenantSubscription sub = subscriptionRepository.findCurrentByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleException("No active subscription", "NO_SUBSCRIPTION"));

        // Check quotas before downgrade
        if (newPlan.getLimitsJson() != null) {
            Map<String, Object> limits = parseJson(newPlan.getLimitsJson());
            validateQuotasForDowngrade(tenant.getId(), limits);
        }

        // Cancel current
        sub.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(sub);

        // Create new subscription starting next period
        Instant periodEnd = sub.getCurrentPeriodEnd();
        Instant newPeriodStart = periodEnd;
        Instant newPeriodEnd = LocalDateTime.now().plusMonths(1).atZone(java.time.ZoneId.systemDefault()).toInstant();

        TenantSubscription newSub = TenantSubscription.builder()
                .tenantId(tenantId)
                .planKey(newPlan.getKey())
                .status(SubscriptionStatus.PENDING_CHANGE)
                .billingCycle(sub.getBillingCycle())
                .currentPeriodStart(newPeriodStart)
                .currentPeriodEnd(newPeriodEnd)
                .quotasJson(newPlan.getLimitsJson())
                .build();

        newSub = subscriptionRepository.save(newSub);

        // Le tenant conserve son plan courant jusqu'à la prise d'effet programmée.
        return newSub;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void activateDuePlanChanges() {
        Instant now = Instant.now();
        for (TenantSubscription pending : subscriptionRepository
                .findByStatusAndCurrentPeriodStartLessThanEqual(SubscriptionStatus.PENDING_CHANGE, now)) {
            TenantSubscription current = subscriptionRepository.findCurrentByTenantId(pending.getTenantId())
                    .orElse(null);
            if (current != null && current.getId() != null && !current.getId().equals(pending.getId())
                    && current.getStatus() != SubscriptionStatus.PENDING_CHANGE) {
                current.setStatus(SubscriptionStatus.CANCELED);
                current.setCanceledAt(now);
                current.setCancelAtPeriodEnd(false);
                subscriptionRepository.save(current);
            }
            SaasPlan plan = planRepository.findByKeyIgnoreCase(pending.getPlanKey())
                    .orElseThrow(() -> new BusinessRuleException("Plan not found: " + pending.getPlanKey(), "PLAN_NOT_FOUND"));
            pending.setStatus(SubscriptionStatus.ACTIVE);
            pending.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(pending);
            Tenant tenant = tenantRepository.findById(pending.getTenantId())
                    .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));
            tenant.setPlan(plan.getKey());
            tenant.setFeaturesJson(plan.getFeaturesJson());
            tenantRepository.save(tenant);
        }
    }

    /**
     * Get current subscription for tenant.
     */
    @Transactional(readOnly = true)
    public Optional<TenantSubscription> getCurrentSubscription(UUID tenantId) {
        return subscriptionRepository.findCurrentByTenantId(tenantId);
    }

    /**
     * Get subscription details with plan info.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSubscriptionDetails(UUID tenantId) {
        Optional<TenantSubscription> sub = getCurrentSubscription(tenantId);
        if (sub.isEmpty()) {
            return Map.of("hasSubscription", false);
        }

        TenantSubscription s = sub.get();
        Optional<SaasPlan> plan = planRepository.findByKeyIgnoreCase(s.getPlanKey());

        return Payloads.of(
                "hasSubscription", true,
                "subscription", Payloads.of(
                        "id", s.getId().toString(),
                        "planKey", s.getPlanKey(),
                        "status", s.getStatus().name(),
                        "billingCycle", s.getBillingCycle(),
                        "currentPeriodStart", s.getCurrentPeriodStart(),
                        "currentPeriodEnd", s.getCurrentPeriodEnd(),
                        "cancelAtPeriodEnd", s.getCancelAtPeriodEnd(),
                        "canceledAt", s.getCanceledAt(),
                        "trialEndsAt", s.getTrialEndsAt(),
                        "plan", plan.map(p -> Payloads.of(
                                "name", p.getName(),
                                "priceMonthly", p.getPriceMonthly(),
                                "priceYearly", p.getPriceYearly(),
                                "features", parseJson(p.getFeaturesJson()),
                                "limits", parseJson(p.getLimitsJson())
                        )).orElse(null)
                ),
                "quotas", quotaService.getQuotaUsage(tenantId)
        );
    }

    /**
     * Handle Stripe webhook events (simplified).
     */
    public void handleStripeEvent(String eventType, Map<String, Object> payload) {
        switch (eventType) {
            case "invoice.payment_succeeded" -> handlePaymentSucceeded(payload);
            case "invoice.payment_failed" -> handlePaymentFailed(payload);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(payload);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(payload);
            default -> {}
        }
    }

    private void handlePaymentSucceeded(Map<String, Object> payload) {
        // Update subscription status to ACTIVE, extend period
    }

    private void handlePaymentFailed(Map<String, Object> payload) {
        // Set status to PAST_DUE, send notification
    }

    private void handleSubscriptionDeleted(Map<String, Object> payload) {
        // Cancel subscription in our system
    }

    private void handleSubscriptionUpdated(Map<String, Object> payload) {
        // Sync plan changes from Stripe
    }

    private void validateQuotasForDowngrade(UUID tenantId, Map<String, Object> newLimits) {
        Map<String, Object> usage = quotaService.getQuotaUsage(tenantId);
        validateDowngrade(usage, newLimits, "users", "used", "max_users", "members");
        validateDowngrade(usage, newLimits, "storage", "usedMb", "max_storage_mb", "storage_mb");
        validateDowngrade(usage, newLimits, "aiRequests", "used", "max_ai_requests_month", "ai_credits");
        validateDowngrade(usage, newLimits, "courses", "used", "max_courses");
        validateDowngrade(usage, newLimits, "messages", "used", "max_messages_month");
        validateDowngrade(usage, newLimits, "churches", "used", "max_churches");
        validateDowngrade(usage, newLimits, "departments", "used", "max_departments");
        validateDowngrade(usage, newLimits, "campuses", "used", "max_campuses");
        validateDowngrade(usage, newLimits, "groups", "used", "max_groups");
    }

    private void validateDowngrade(Map<String, Object> usage,
                                   Map<String, Object> limits,
                                   String metricKey,
                                   String usageKey,
                                   String... limitKeys) {
        Long limit = firstNumeric(limits, limitKeys);
        Object metricValue = usage.get(metricKey);
        if (limit == null || !(metricValue instanceof Map<?, ?> metric)) {
            return;
        }
        Object usedValue = metric.get(usageKey);
        if (usedValue instanceof Number used && used.doubleValue() > limit) {
            throw new BusinessRuleException(
                    "Impossible de rétrograder: usage actuel (" + used + ") dépasse la nouvelle limite (" + limit + ") pour " + metricKey,
                    "QUOTA_EXCEEDS_DOWNGRADE_LIMIT");
        }
    }

    private Long firstNumeric(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text) {
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}