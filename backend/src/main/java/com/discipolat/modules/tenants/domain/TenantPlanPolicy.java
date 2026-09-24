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
        String persistedKey = subscription != null && subscription.getPlanKey() != null
                ? subscription.getPlanKey() : tenant.getPlan();
        if (persistedKey == null || persistedKey.isBlank()) {
            return new ResolvedPlan(null, null, null, Map.of(), false, false, false, null);
        }
        String canonicalKey = normalizePlanKey(persistedKey);
        SaasPlan plan = canonicalKey == null ? null : planRepository.findById(canonicalKey)
                .orElseGet(() -> planRepository.findByKeyIgnoreCaseAndIsActiveTrue(canonicalKey).orElse(null));
        ParsedObject parsedLimits = plan == null ? ParsedObject.invalid() : parseObject(plan.getLimitsJson());
        ParsedObject parsedFeatures = plan == null ? ParsedObject.invalid() : parseObject(plan.getFeaturesJson());
        boolean catalogActive = plan != null && Boolean.TRUE.equals(plan.getIsActive())
                && !isDisabledPlanStatus(plan.getStatus());
        boolean limitsValid = parsedLimits.valid() && validLimits(plan, parsedLimits.value());
        boolean featuresValid = parsedFeatures.valid();
        boolean subscriptionPlanValid = subscription == null || hasText(subscription.getPlanKey());
        boolean subscriptionActive = subscription == null || isEnabledSubscription(subscription.getStatus());
        return new ResolvedPlan(persistedKey, canonicalKey, plan,
                parsedLimits.value(), catalogActive && subscriptionPlanValid && subscriptionActive
                        && limitsValid && featuresValid,
                limitsValid, featuresValid, subscription);
    }

    public static String canonicalizePlanKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        return CANONICAL_KEYS.getOrDefault(normalized, normalized);
    }

    public String normalizePlanKey(String key) {
        return canonicalizePlanKey(key);
    }

    public OptionalLong limit(ResolvedPlan resolvedPlan, Limit limit) {
        if (resolvedPlan == null || resolvedPlan.plan() == null || !resolvedPlan.limitsValid()) {
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
        if (resolvedPlan == null || resolvedPlan.plan() == null || !resolvedPlan.featuresValid()) {
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
            Long value = numericValue(limits.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean validLimits(SaasPlan plan, Map<String, Object> limits) {
        if (plan == null || plan.getSeatsLimit() != null && plan.getSeatsLimit() < 0
                || plan.getStorageLimitMb() != null && plan.getStorageLimitMb() < 0
                || plan.getAiCreditsLimit() != null && plan.getAiCreditsLimit() < 0) {
            return false;
        }
        for (Map.Entry<String, Object> entry : limits.entrySet()) {
            if (isLimitKey(entry.getKey())) {
                Long value = numericValue(entry.getValue());
                if (value == null || value < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isLimitKey(String key) {
        return switch (key) {
            case "max_users", "members", "max_storage_mb", "storage_mb", "max_ai_requests_month",
                    "ai_credits", "max_courses", "max_messages_month", "max_churches", "max_departments",
                    "max_campuses", "max_groups", "spaces", "events" -> true;
            default -> false;
        };
    }

    private Long numericValue(Object value) {
        if (value instanceof Number number) {
            double decimal = number.doubleValue();
            if (!Double.isFinite(decimal) || decimal != Math.rint(decimal)) {
                return null;
            }
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ParsedObject parseObject(String json) {
        if (json == null || json.isBlank()) {
            return ParsedObject.invalid();
        }
        try {
            Map<String, Object> value = objectMapper.readValue(json, new TypeReference<>() {
            });
            return value == null ? ParsedObject.invalid() : new ParsedObject(value, true);
        } catch (Exception ignored) {
            return ParsedObject.invalid();
        }
    }

    private Map<String, Object> readObject(String json) {
        ParsedObject parsed = parseObject(json);
        return parsed.valid() ? parsed.value() : Map.of();
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
            boolean limitsValid,
            boolean featuresValid,
            TenantSubscription subscription
    ) {
        public boolean hasCatalogPlan() {
            return plan != null;
        }
    }

    private record ParsedObject(Map<String, Object> value, boolean valid) {
        private static ParsedObject invalid() {
            return new ParsedObject(Map.of(), false);
        }
    }
}
