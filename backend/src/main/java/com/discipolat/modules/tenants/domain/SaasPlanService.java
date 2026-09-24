package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service pour la gestion des plans SaaS
 */
@Service
@Transactional
public class SaasPlanService {

    public static final List<String> PUBLIC_PLAN_KEYS = List.of("DISCOVERY", "STARTUP", "GROWTH", "NETWORK");

    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;

    public SaasPlanService(SaasPlanRepository planRepository,
                           TenantSubscriptionRepository subscriptionRepository,
                           TenantRepository tenantRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<SaasPlan> getActivePlans() {
        return planRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<SaasPlan> getPublicPlans() {
        return planRepository.findByIsActiveTrueAndIsPublicTrueOrderBySortOrderAsc().stream()
                .filter(plan -> PUBLIC_PLAN_KEYS.contains(normalizePublicKey(plan.getKey())))
                .filter(plan -> plan.getStatus() == null
                        || !(plan.getStatus().equalsIgnoreCase("INACTIVE")
                        || plan.getStatus().equalsIgnoreCase("DEPRECATED")))
                .sorted(Comparator.comparingInt(plan -> PUBLIC_PLAN_KEYS.indexOf(normalizePublicKey(plan.getKey()))))
                .toList();
    }

    private String normalizePublicKey(String key) {
        return key == null ? "" : key.trim().toUpperCase(java.util.Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Optional<SaasPlan> getPlan(String key) {
        String canonicalKey = TenantPlanPolicy.canonicalizePlanKey(key);
        return canonicalKey == null
                ? Optional.empty()
                : planRepository.findByKeyIgnoreCaseAndIsActiveTrue(canonicalKey);
    }

    @Transactional(readOnly = true)
    public Optional<SaasPlan> getPlanByKey(String key) {
        String canonicalKey = TenantPlanPolicy.canonicalizePlanKey(key);
        return canonicalKey == null
                ? Optional.empty()
                : planRepository.findByKeyIgnoreCase(canonicalKey);
    }

    /**
     * Récupère l'abonnement actif d'un tenant
     */
    @Transactional(readOnly = true)
    public Optional<TenantSubscription> getTenantSubscription(UUID tenantId) {
        return subscriptionRepository.findCurrentByTenantId(tenantId);
    }

    /**
     * Récupère le plan actuel d'un tenant (avec fallback sur FREE)
     */
    @Transactional(readOnly = true)
    public SaasPlan getCurrentPlan(UUID tenantId) {
        return subscriptionRepository.findCurrentByTenantId(tenantId)
                .flatMap(sub -> getPlanByKey(sub.getPlanKey()))
                .orElseGet(() -> tenantRepository.findById(tenantId)
                        .flatMap(tenant -> getPlanByKey(tenant.getPlan()))
                        .orElse(null));
    }

    /**
     * Vérifie si un tenant a accès à une fonctionnalité
     */
    @Transactional(readOnly = true)
    public boolean hasFeature(UUID tenantId, String featureKey) {
        SaasPlan plan = getCurrentPlan(tenantId);
        if (plan == null) return false;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode features = mapper.readTree(plan.getFeaturesJson());
            return features.has(featureKey) && features.get(featureKey).asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Vérifie un quota pour un tenant
     */
    @Transactional(readOnly = true)
    public QuotaCheck checkQuota(UUID tenantId, String quotaKey, long currentUsage) {
        SaasPlan plan = getCurrentPlan(tenantId);
        if (plan == null) {
            return new QuotaCheck(false, 0, currentUsage, "Plan not found");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode limits = mapper.readTree(plan.getLimitsJson());
            if (!limits.has(quotaKey)) {
                return new QuotaCheck(true, Long.MAX_VALUE, currentUsage, "Unlimited");
            }

            long limit = limits.get(quotaKey).asLong();
            boolean allowed = currentUsage < limit;
            return new QuotaCheck(allowed, limit, currentUsage, allowed ? "OK" : "QUOTA_EXCEEDED");
        } catch (Exception e) {
            return new QuotaCheck(false, 0, currentUsage, "Error parsing limits");
        }
    }

    public record QuotaCheck(boolean allowed, long limit, long currentUsage, String message) {}

    /**
     * Abonne un tenant à un plan
     */
    public TenantSubscription subscribe(UUID tenantId, String planKey, String billingCycle, UUID creatorId) {
        String canonicalKey = TenantPlanPolicy.canonicalizePlanKey(planKey);
        if (canonicalKey == null) {
            throw new EntityNotFoundException("SaasPlan", "key", planKey);
        }
        SaasPlan plan = planRepository.findByKeyIgnoreCaseAndIsActiveTrue(canonicalKey)
                .orElseThrow(() -> new EntityNotFoundException("SaasPlan", "key", planKey));

        Tenant tenant = tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
        Optional<TenantSubscription> existing = subscriptionRepository.findCurrentByTenantId(tenantId);
        TenantSubscription subscription;
        java.time.Instant periodStart = java.time.Instant.now();
        java.time.Instant periodEnd = calculatePeriodEnd(periodStart, billingCycle);

        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.setPlanKey(plan.getKey());
            subscription.setBillingCycle(billingCycle);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodStart(periodStart);
            subscription.setCurrentPeriodEnd(periodEnd);
            subscription.setCancelAtPeriodEnd(false);
            subscription.setCanceledAt(null);
            subscription.setQuotasJson(plan.getLimitsJson());
        } else {
            subscription = TenantSubscription.builder()
                    .tenantId(tenantId)
                    .planKey(plan.getKey())
                    .billingCycle(billingCycle)
                    .status(SubscriptionStatus.ACTIVE)
                    .currentPeriodStart(periodStart)
                    .currentPeriodEnd(periodEnd)
                    .quotasJson(plan.getLimitsJson())
                    .build();
        }

        tenant.setPlan(plan.getKey());
        tenant.setFeaturesJson(plan.getFeaturesJson());
        tenantRepository.save(tenant);
        return subscriptionRepository.save(subscription);
    }

    /**
     * Annule l'abonnement à la fin de la période
     */
    public void cancelAtPeriodEnd(UUID tenantId) {
        subscriptionRepository.findCurrentByTenantId(tenantId).ifPresent(sub -> {
            sub.setCancelAtPeriodEnd(true);
            subscriptionRepository.save(sub);
        });
    }

    /**
     * Réactive un abonnement annulé
     */
    public void reactivate(UUID tenantId) {
        subscriptionRepository.findCurrentByTenantId(tenantId).ifPresent(sub -> {
            sub.setCancelAtPeriodEnd(false);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(sub);
        });
    }

    private java.time.Instant calculatePeriodEnd(java.time.Instant periodStart, String billingCycle) {
        if ("yearly".equalsIgnoreCase(billingCycle)) {
            return periodStart.plus(java.time.Duration.ofDays(365));
        }
        return periodStart.plus(java.time.Duration.ofDays(30));
    }

    @Transactional(readOnly = true)
    public List<SaasPlan> getAllPlans() {
        return planRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TenantSubscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public SaasPlan savePlan(SaasPlan plan) {
        return planRepository.save(plan);
    }
}