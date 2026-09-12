package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
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

    public SubscriptionService(TenantRepository tenantRepository,
                               TenantSubscriptionRepository subscriptionRepository,
                               SaasPlanRepository planRepository,
                               FeatureAccessService featureAccessService,
                               QuotaService quotaService) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.featureAccessService = featureAccessService;
        this.quotaService = quotaService;
    }

    /**
     * Subscribe a tenant to a plan.
     */
    public TenantSubscription subscribe(UUID tenantId, String planKey, String billingCycle, UUID adminId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        SaasPlan plan = planRepository.findById(planKey)
                .orElseThrow(() -> new BusinessRuleException("Plan not found: " + planKey, "PLAN_NOT_FOUND"));

        if (!plan.getIsActive()) {
            throw new BusinessRuleException("Plan is not active: " + planKey, "PLAN_INACTIVE");
        }

        // Cancel existing subscription if any
        Optional<TenantSubscription> existing = subscriptionRepository.findByTenantId(tenantId);
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

        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId)
                .planKey(planKey)
                .status(SubscriptionStatus.TRIAL)
                .billingCycle(billingCycle)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(false)
                .trialEndsAt(LocalDateTime.now().plusDays(14).atZone(java.time.ZoneId.systemDefault()).toInstant())
                .quotasJson(plan.getLimitsJson())
                .build();

        subscription = subscriptionRepository.save(subscription);

        // Update tenant plan and activate features
        tenant.setPlan(planKey);
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
     * Cancel subscription at period end.
     */
    public void cancelAtPeriodEnd(UUID tenantId, UUID adminId) {
        TenantSubscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleException("No active subscription", "NO_SUBSCRIPTION"));

        sub.setCancelAtPeriodEnd(true);
        sub.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(sub);
    }

    /**
     * Cancel subscription immediately.
     */
    public void cancelImmediately(UUID tenantId, UUID adminId) {
        TenantSubscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRuleException("No active subscription", "NO_SUBSCRIPTION"));

        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setCanceledAt(Instant.now());
        sub.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(sub);

        // Downgrade to free plan
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        tenant.setPlan("FREE");
        tenant.setFeaturesJson("{}");
        // tenantRepository.save(tenant);
    }

    /**
     * Reactivate a canceled subscription.
     */
    public void reactivate(UUID tenantId, UUID adminId) {
        TenantSubscription sub = subscriptionRepository.findByTenantId(tenantId)
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

        SaasPlan newPlan = planRepository.findById(newPlanKey)
                .orElseThrow(() -> new BusinessRuleException("Plan not found: " + newPlanKey, "PLAN_NOT_FOUND"));

        TenantSubscription sub = subscriptionRepository.findByTenantId(tenantId)
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
                .planKey(newPlanKey)
                .status(SubscriptionStatus.PENDING_CHANGE)
                .billingCycle(sub.getBillingCycle())
                .currentPeriodStart(newPeriodStart)
                .currentPeriodEnd(newPeriodEnd)
                .quotasJson(newPlan.getLimitsJson())
                .build();

        newSub = subscriptionRepository.save(newSub);

        // Update tenant plan immediately for feature access
        tenant.setPlan(newPlanKey);
        Map<String, Object> features = parseJson(newPlan.getFeaturesJson());
        tenant.setFeaturesJson(toJson(features));
        // tenantRepository.save(tenant);

        return newSub;
    }

    /**
     * Get current subscription for tenant.
     */
    @Transactional(readOnly = true)
    public Optional<TenantSubscription> getCurrentSubscription(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId);
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
        Optional<SaasPlan> plan = planRepository.findById(s.getPlanKey());

        return Map.of(
                "hasSubscription", true,
                "subscription", Map.of(
                        "id", s.getId().toString(),
                        "planKey", s.getPlanKey(),
                        "status", s.getStatus().name(),
                        "billingCycle", s.getBillingCycle(),
                        "currentPeriodStart", s.getCurrentPeriodStart(),
                        "currentPeriodEnd", s.getCurrentPeriodEnd(),
                        "cancelAtPeriodEnd", s.getCancelAtPeriodEnd(),
                        "canceledAt", s.getCanceledAt(),
                        "trialEndsAt", s.getTrialEndsAt(),
                        "plan", plan.map(p -> Map.of(
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
        // Check current usage against new limits
        Map<String, Object> usage = quotaService.getQuotaUsage(tenantId);

        for (Map.Entry<String, Object> entry : newLimits.entrySet()) {
            String key = entry.getKey();
            long newLimit = ((Number) entry.getValue()).longValue();

            if (usage.containsKey(key)) {
                Map<String, Object> usageInfo = (Map<String, Object>) usage.get(key);
                long used = ((Number) usageInfo.get("used")).longValue();
                if (used > newLimit) {
                    throw new BusinessRuleException(
                            "Impossible de rétrograder: usage actuel (" + used + ") dépasse la nouvelle limite (" + newLimit + ") pour " + key,
                            "QUOTA_EXCEEDS_DOWNGRADE_LIMIT");
                }
            }
        }
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