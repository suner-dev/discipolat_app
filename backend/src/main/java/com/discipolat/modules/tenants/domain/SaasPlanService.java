package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;

    public SaasPlanService(SaasPlanRepository planRepository, TenantSubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<SaasPlan> getActivePlans() {
        return planRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public Optional<SaasPlan> getPlan(String key) {
        return planRepository.findByKeyAndIsActiveTrue(key);
    }

    @Transactional(readOnly = true)
    public Optional<SaasPlan> getPlanByKey(String key) {
        return planRepository.findById(key);
    }

    /**
     * Récupère l'abonnement actif d'un tenant
     */
    @Transactional(readOnly = true)
    public Optional<TenantSubscription> getTenantSubscription(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId);
    }

    /**
     * Récupère le plan actuel d'un tenant (avec fallback sur FREE)
     */
    @Transactional(readOnly = true)
    public SaasPlan getCurrentPlan(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .flatMap(sub -> planRepository.findById(sub.getPlanKey()))
                .orElseGet(() -> planRepository.findByKeyAndIsActiveTrue("FREE").orElse(null));
    }

    /**
     * Vérifie si un tenant a accès à une fonctionnalité
     */
    @Transactional(readOnly = true)
    public boolean hasFeature(UUID tenantId, String featureKey) {
        SaasPlan plan = getCurrentPlan(tenantId);
        if (plan == null) return false;

        try {
            var features = com.fasterxml.jackson.databind.ObjectMapper().readTree(plan.getFeaturesJson());
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
            var limits = com.fasterxml.jackson.databind.ObjectMapper().readTree(plan.getLimitsJson());
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
        SaasPlan plan = planRepository.findByKeyAndIsActiveTrue(planKey)
                .orElseThrow(() -> new EntityNotFoundException("SaasPlan", planKey));

        Optional<TenantSubscription> existing = subscriptionRepository.findByTenantId(tenantId);
        TenantSubscription subscription;

        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.setPlanKey(planKey);
            subscription.setBillingCycle(billingCycle);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodStart(java.time.Instant.now());
            subscription.setCurrentPeriodEnd(calculatePeriodEnd(billingCycle));
            subscription.setCancelAtPeriodEnd(false);
            subscription.setCanceledAt(null);
        } else {
            subscription = TenantSubscription.builder()
                    .tenantId(tenantId)
                    .planKey(planKey)
                    .billingCycle(billingCycle)
                    .status(SubscriptionStatus.ACTIVE)
                    .currentPeriodStart(java.time.Instant.now())
                    .currentPeriodEnd(calculatePeriodEnd(billingCycle))
                    .quotasJson(plan.getLimitsJson())
                    .build();
        }

        return subscriptionRepository.save(subscription);
    }

    /**
     * Annule l'abonnement à la fin de la période
     */
    public void cancelAtPeriodEnd(UUID tenantId) {
        subscriptionRepository.findByTenantId(tenantId).ifPresent(sub -> {
            sub.setCancelAtPeriodEnd(true);
            subscriptionRepository.save(sub);
        });
    }

    /**
     * Réactive un abonnement annulé
     */
    public void reactivate(UUID tenantId) {
        subscriptionRepository.findByTenantId(tenantId).ifPresent(sub -> {
            sub.setCancelAtPeriodEnd(false);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(sub);
        });
    }

    private java.time.Instant calculatePeriodEnd(String billingCycle) {
        if ("yearly".equals(billingCycle)) {
            return java.time.Instant.now().plus(java.time.Duration.ofDays(365));
        }
        return java.time.Instant.now().plus(java.time.Duration.ofDays(30));
    }

    @Transactional(readOnly = true)
    public List<SaasPlan> getAllPlans() {
        return planRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TenantSubscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }
}