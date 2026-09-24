package com.discipolat.modules.tenants.domain;

import java.time.Instant;
import java.util.UUID;

public record TenantUsageSnapshot(
        UUID tenantId,
        Instant generatedAt,
        Instant periodStart,
        Instant periodEnd,
        SubscriptionInfo subscription,
        PlanInfo plan,
        Metric users,
        Metric storageBytes,
        Metric aiCredits,
        Metric courses,
        Metric messages
) {
    public enum MetricSource {
        PERSISTED_USERS,
        PERSISTED_FILES,
        PERSISTED_AI_USAGE,
        PERSISTED_COURSES,
        PERSISTED_CONVERSATION_MESSAGES
    }

    public enum MetricPeriod {
        CURRENT,
        CALENDAR_MONTH
    }

    public enum MetricUnit {
        USERS,
        BYTES,
        AI_CREDITS,
        COURSES,
        MESSAGES
    }

    public record SubscriptionInfo(
            UUID id,
            String planKey,
            String status,
            String billingCycle,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            Boolean cancelAtPeriodEnd
    ) {
    }

    public record PlanInfo(
            String persistedKey,
            String canonicalKey,
            String name,
            Boolean active,
            String status,
            Boolean enforcementEnabled
    ) {
    }

    public record Metric(
            long used,
            Long limit,
            Double utilizationPercent,
            MetricUnit unit,
            MetricSource source,
            MetricPeriod period,
            Instant periodStart,
            Instant periodEnd,
            boolean serverEnforced
    ) {
    }
}
