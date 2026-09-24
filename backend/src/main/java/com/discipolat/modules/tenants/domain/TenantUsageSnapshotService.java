package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.ai.domain.AiUsageRepository;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.messages.domain.ConversationMessageRepository;
import com.discipolat.modules.trainings.domain.CourseRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.OptionalLong;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TenantUsageSnapshotService {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final FileEntityRepository fileRepository;
    private final AiUsageRepository aiUsageRepository;
    private final CourseRepository courseRepository;
    private final ConversationMessageRepository messageRepository;
    private final TenantPlanPolicy planPolicy;
    private final Clock clock;

    @Autowired
    public TenantUsageSnapshotService(TenantRepository tenantRepository,
                                      UserRepository userRepository,
                                      FileEntityRepository fileRepository,
                                      AiUsageRepository aiUsageRepository,
                                      CourseRepository courseRepository,
                                      ConversationMessageRepository messageRepository,
                                      TenantPlanPolicy planPolicy) {
        this(tenantRepository, userRepository, fileRepository, aiUsageRepository,
                courseRepository, messageRepository, planPolicy, Clock.systemUTC());
    }

    TenantUsageSnapshotService(TenantRepository tenantRepository,
                               UserRepository userRepository,
                               FileEntityRepository fileRepository,
                               AiUsageRepository aiUsageRepository,
                               CourseRepository courseRepository,
                               ConversationMessageRepository messageRepository,
                               TenantPlanPolicy planPolicy,
                               Clock clock) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.aiUsageRepository = aiUsageRepository;
        this.courseRepository = courseRepository;
        this.messageRepository = messageRepository;
        this.planPolicy = planPolicy;
        this.clock = clock;
    }

    public TenantUsageSnapshot getSnapshot() {
        return getSnapshotForTenant(TenantContext.requireTenantId());
    }

    public TenantUsageSnapshot getSnapshotForTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));
        TenantPlanPolicy.ResolvedPlan resolvedPlan = planPolicy.resolve(tenant);
        Instant now = clock.instant();
        LocalDate month = LocalDate.now(clock);
        LocalDate nextMonth = month.plusMonths(1);
        LocalDateTime monthFrom = month.atStartOfDay();
        LocalDateTime monthTo = nextMonth.atStartOfDay();
        Instant monthStart = monthFrom.toInstant(ZoneOffset.UTC);
        Instant monthEnd = monthTo.toInstant(ZoneOffset.UTC);

        long users = userRepository.countByTenantIdAndDeletedFalse(tenantId);
        long storageBytes = fileRepository.sumSizeBytesByTenantIdAndDeletedFalse(tenantId);
        long aiCredits = aiUsageRepository.sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
                tenantId, month, nextMonth);
        long courses = courseRepository.countByTenantId(tenantId);
        long messages = messageRepository.countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
                tenantId, monthFrom, monthTo);

        TenantUsageSnapshot.SubscriptionInfo subscription = toSubscription(resolvedPlan);
        TenantUsageSnapshot.PlanInfo plan = toPlan(resolvedPlan);
        Long userLimit = limit(resolvedPlan, TenantPlanPolicy.Limit.USERS);
        Long storageLimit = storageLimit(resolvedPlan);
        Long aiLimit = limit(resolvedPlan, TenantPlanPolicy.Limit.AI_CREDITS);
        Long courseLimit = limit(resolvedPlan, TenantPlanPolicy.Limit.COURSES);
        Long messageLimit = limit(resolvedPlan, TenantPlanPolicy.Limit.MESSAGES);
        boolean enforced = resolvedPlan.enforcementEnabled();

        return new TenantUsageSnapshot(
                tenantId,
                now,
                resolvedPlan.subscription() == null ? null : resolvedPlan.subscription().getCurrentPeriodStart(),
                resolvedPlan.subscription() == null ? null : resolvedPlan.subscription().getCurrentPeriodEnd(),
                subscription,
                plan,
                metric(users, userLimit, TenantUsageSnapshot.MetricUnit.USERS,
                        TenantUsageSnapshot.MetricSource.PERSISTED_USERS,
                        TenantUsageSnapshot.MetricPeriod.CURRENT, null, null, enforced && userLimit != null),
                metric(storageBytes, storageLimit, TenantUsageSnapshot.MetricUnit.BYTES,
                        TenantUsageSnapshot.MetricSource.PERSISTED_FILES,
                        TenantUsageSnapshot.MetricPeriod.CURRENT, null, null, enforced && storageLimit != null),
                metric(aiCredits, aiLimit, TenantUsageSnapshot.MetricUnit.AI_CREDITS,
                        TenantUsageSnapshot.MetricSource.PERSISTED_AI_USAGE,
                        TenantUsageSnapshot.MetricPeriod.CALENDAR_MONTH, monthStart, monthEnd,
                        enforced && aiLimit != null),
                metric(courses, courseLimit, TenantUsageSnapshot.MetricUnit.COURSES,
                        TenantUsageSnapshot.MetricSource.PERSISTED_COURSES,
                        TenantUsageSnapshot.MetricPeriod.CURRENT, null, null, enforced && courseLimit != null),
                metric(messages, messageLimit, TenantUsageSnapshot.MetricUnit.MESSAGES,
                        TenantUsageSnapshot.MetricSource.PERSISTED_CONVERSATION_MESSAGES,
                        TenantUsageSnapshot.MetricPeriod.CALENDAR_MONTH, monthStart, monthEnd,
                        enforced && messageLimit != null));
    }

    public Page<TenantUsageOverview> getTenantOverviews(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(this::toOverview);
    }

    private TenantUsageOverview toOverview(Tenant tenant) {
        TenantUsageSnapshot snapshot = getSnapshotForTenant(tenant.getId());
        return new TenantUsageOverview(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus().name(),
                snapshot.plan() == null ? null : snapshot.plan().canonicalKey(),
                snapshot.plan() == null ? null : snapshot.plan().status(),
                snapshot.subscription() == null ? null : snapshot.subscription().status(),
                snapshot.users().used(),
                snapshot.storageBytes().used(),
                snapshot.aiCredits().used(),
                snapshot.courses().used(),
                snapshot.messages().used(),
                snapshot.plan() != null && snapshot.plan().enforcementEnabled());
    }

    private Long limit(TenantPlanPolicy.ResolvedPlan resolvedPlan, TenantPlanPolicy.Limit limit) {
        OptionalLong value = planPolicy.limit(resolvedPlan, limit);
        return value.isPresent() ? value.getAsLong() : null;
    }

    private Long storageLimit(TenantPlanPolicy.ResolvedPlan resolvedPlan) {
        OptionalLong megabytes = planPolicy.limit(resolvedPlan, TenantPlanPolicy.Limit.STORAGE_MB);
        if (megabytes.isEmpty()) {
            return null;
        }
        try {
            return Math.multiplyExact(megabytes.getAsLong(), BYTES_PER_MEGABYTE);
        } catch (ArithmeticException ignored) {
            return null;
        }
    }

    private TenantUsageSnapshot.Metric metric(long used,
                                               Long limit,
                                               TenantUsageSnapshot.MetricUnit unit,
                                               TenantUsageSnapshot.MetricSource source,
                                               TenantUsageSnapshot.MetricPeriod period,
                                               Instant periodStart,
                                               Instant periodEnd,
                                               boolean serverEnforced) {
        Double utilization = limit == null || limit == 0 ? null : round(used * 100.0 / limit);
        return new TenantUsageSnapshot.Metric(used, limit, utilization, unit, source,
                period, periodStart, periodEnd, serverEnforced);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private TenantUsageSnapshot.SubscriptionInfo toSubscription(TenantPlanPolicy.ResolvedPlan resolvedPlan) {
        TenantSubscription subscription = resolvedPlan.subscription();
        if (subscription == null) {
            return null;
        }
        return new TenantUsageSnapshot.SubscriptionInfo(
                subscription.getId(),
                subscription.getPlanKey(),
                subscription.getStatus() == null ? "UNKNOWN" : subscription.getStatus().name(),
                subscription.getBillingCycle(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCancelAtPeriodEnd());
    }

    private TenantUsageSnapshot.PlanInfo toPlan(TenantPlanPolicy.ResolvedPlan resolvedPlan) {
        if (resolvedPlan.persistedKey() == null) {
            return null;
        }
        SaasPlan plan = resolvedPlan.plan();
        String status = plan == null ? "MISSING" : plan.getStatus();
        if (plan != null && (status == null || status.isBlank())) {
            status = Boolean.TRUE.equals(plan.getIsActive()) ? "ACTIVE" : "INACTIVE";
        }
        return new TenantUsageSnapshot.PlanInfo(
                resolvedPlan.persistedKey(),
                resolvedPlan.canonicalKey(),
                plan == null ? null : plan.getName(),
                plan != null && Boolean.TRUE.equals(plan.getIsActive()),
                status,
                resolvedPlan.enforcementEnabled());
    }

    public record TenantUsageOverview(
            UUID tenantId,
            String tenantName,
            String tenantSlug,
            String tenantStatus,
            String canonicalPlanKey,
            String planStatus,
            String subscriptionStatus,
            long users,
            long storageBytes,
            long aiCredits,
            long courses,
            long messages,
            boolean enforcementEnabled
    ) {
    }
}
