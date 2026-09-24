package com.discipolat.modules.tenants.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

@Component
public class TenantPlanPolicy {

    private static final Map<String, String> CANONICAL_KEYS = Map.of(
            "FREE", "DISCOVERY",
            "DISCOVERY", "DISCOVERY",
            "STARTER", "STARTUP",
            "STARTUP", "STARTUP",
            "PRO", "GROWTH",
            "GROWTH", "GROWTH",
            "ENTERPRISE", "NETWORK",
            "NETWORK", "NETWORK"
    );

    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanRepository planRepository;
    private final ObjectMapper objectMapper;

    public TenantPlanPolicy(TenantSubscriptionRepository subscriptionRepository,
                            SaasPlanRepository planRepository,
                            ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.objectMapper = objectMapper;
    }

    public ResolvedPlan resolve(Tenant tenant) {
        TenantSubscription subscription = subscriptionRepository.findCurrentByTenantId(tenant.getId()).orElse(null);
        String persistedKey = subscription != null ? subscription.getPlanKey() : tenant.getPlan();
        if (persistedKey == null || persistedKey.isBlank()) {
            // 6e composant du record = TenantSubscription : `null` (et non `false`),
            // sinon le plan résolu ne tient pas compte de l'abonnement courant.
            return new ResolvedPlan(null, null, null, Map.of(), false, null);
        }
        SaasPlan plan = planRepository.findById(persistedKey).orElse(null);
        String canonicalKey = normalizePlanKey(persistedKey);
        boolean catalogActive = plan != null && Boolean.TRUE.equals(plan.getIsActive())
                && !isDisabledPlanStatus(plan.getStatus());
        boolean subscriptionActive = subscription == null || isEnabledSubscription(subscription.getStatus());
        Map<String, Object> limits = plan == null ? Map.of() : readObject(plan.getLimitsJson());
        return new ResolvedPlan(persistedKey, canonicalKey, plan, limits,
                catalogActive && subscriptionActive, subscription);
    }

    public String normalizePlanKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        return CANONICAL_KEYS.getOrDefault(normalized, normalized);
    }

    public OptionalLong limit(ResolvedPlan resolvedPlan, Limit limit) {
        if (resolvedPlan == null || resolvedPlan.plan() == null) {
            return OptionalLong.empty();
        }
        Long value = switch (limit) {
            case USERS -> first(resolvedPlan.plan().getSeatsLimit(), resolvedPlan.limits(), "max_users", "members");
            case STORAGE_MB -> first(resolvedPlan.plan().getStorageLimitMb(), resolvedPlan.limits(), "max_storage_mb", "storage_mb");
            case AI_CREDITS -> first(resolvedPlan.plan().getAiCreditsLimit(), resolvedPlan.limits(), "max_ai_requests_month", "ai_credits");
            case COURSES -> first(null, resolvedPlan.limits(), "max_courses");
            case MESSAGES -> first(null, resolvedPlan.limits(), "max_messages_month");
        };
        return value == null || value < 0 ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public Optional<Boolean> feature(ResolvedPlan resolvedPlan, String... keys) {
        if (resolvedPlan == null || resolvedPlan.plan() == null) {
            return Optional.empty();
        }
        Map<String, Object> features = readObject(resolvedPlan.plan().getFeaturesJson());
        for (String key : keys) {
            Object value = features.get(key);
            if (value instanceof Boolean booleanValue) {
                return Optional.of(booleanValue);
            }
        }
        return Optional.empty();
    }

    public boolean isDisabledPlanStatus(String status) {
        return status != null && (status.equalsIgnoreCase("INACTIVE")
                || status.equalsIgnoreCase("DEPRECATED"));
    }

    public boolean isEnabledSubscription(SubscriptionStatus status) {
        return status == SubscriptionStatus.ACTIVE
                || status == SubscriptionStatus.TRIAL
                || status == SubscriptionStatus.PAST_DUE;
    }

    private Long first(Integer dedicated, Map<String, Object> limits, String... keys) {
        if (dedicated != null) {
            return dedicated.longValue();
        }
        for (String key : keys) {
            Object value = limits.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private Map<String, Object> readObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    public enum Limit {
        USERS,
        STORAGE_MB,
        AI_CREDITS,
        COURSES,
        MESSAGES
    }

    public record ResolvedPlan(
            String persistedKey,
            String canonicalKey,
            SaasPlan plan,
            Map<String, Object> limits,
            boolean enforcementEnabled,
            TenantSubscription subscription
    ) {
        public boolean hasCatalogPlan() {
            return plan != null;
        }
    }
}
