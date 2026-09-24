package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.ai.domain.AiUsageRepository;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.messages.domain.ConversationMessageRepository;
import com.discipolat.modules.trainings.domain.CourseRepository;
import com.discipolat.modules.users.domain.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

@Service
@Transactional
public class QuotaService {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final FileEntityRepository fileRepository;
    private final CourseRepository courseRepository;
    private final ConversationMessageRepository messageRepository;
    private final AiUsageRepository aiUsageRepository;
    private final TenantPlanPolicy planPolicy;
    private final TenantUsageSnapshotService usageSnapshotService;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public QuotaService(TenantRepository tenantRepository,
                        UserRepository userRepository,
                        OrganizationNodeRepository orgNodeRepository,
                        FileEntityRepository fileRepository,
                        CourseRepository courseRepository,
                        ConversationMessageRepository messageRepository,
                        AiUsageRepository aiUsageRepository,
                        TenantPlanPolicy planPolicy,
                        TenantUsageSnapshotService usageSnapshotService,
                        ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.fileRepository = fileRepository;
        this.courseRepository = courseRepository;
        this.messageRepository = messageRepository;
        this.aiUsageRepository = aiUsageRepository;
        this.planPolicy = planPolicy;
        this.usageSnapshotService = usageSnapshotService;
        this.objectMapper = objectMapper;
    }

    public void checkCanCreateUser(UUID tenantId) {
        LockedPlan locked = lockPlan(tenantId);
        OptionalLong limit = planPolicy.limit(locked.plan(), TenantPlanPolicy.Limit.USERS);
        if (!locked.enforced() || limit.isEmpty()) {
            throw invalidConfiguration();
        }
        long currentUsers = userRepository.countByTenantIdAndDeletedFalse(tenantId);
        if (currentUsers >= limit.getAsLong()) {
            throw exceeded("users", currentUsers, limit.getAsLong());
        }
    }

    public void checkCanCreateChurch(UUID tenantId, OrganizationNodeType type) {
        LockedPlan locked = lockPlan(tenantId);
        Long limit = organizationLimit(locked.plan(), "churches");
        if (!locked.enforced() || limit == null) {
            throw invalidConfiguration();
        }
        long current = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH)
                + orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH);
        if (current >= limit) {
            throw exceeded("churches", current, limit);
        }
    }

    public void checkCanCreateDepartment(UUID tenantId) {
        LockedPlan locked = lockPlan(tenantId);
        Long limit = organizationLimit(locked.plan(), "departments");
        if (!locked.enforced() || limit == null) {
            throw invalidConfiguration();
        }
        long current = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);
        if (current >= limit) {
            throw exceeded("departments", current, limit);
        }
    }

    public void checkCanCreateCourse(UUID tenantId) {
        LockedPlan locked = lockPlan(tenantId);
        OptionalLong limit = planPolicy.limit(locked.plan(), TenantPlanPolicy.Limit.COURSES);
        if (!locked.enforced() || limit.isEmpty()) {
            throw invalidConfiguration();
        }
        long current = courseRepository.countByTenantId(tenantId);
        if (current >= limit.getAsLong()) {
            throw exceeded("courses", current, limit.getAsLong());
        }
    }

    public void checkCanStoreFile(UUID tenantId, long additionalBytes) {
        if (additionalBytes < 0) {
            throw new BusinessRuleException("File size cannot be negative", "INVALID_FILE_SIZE");
        }
        LockedPlan locked = lockPlan(tenantId);
        OptionalLong megabytes = planPolicy.limit(locked.plan(), TenantPlanPolicy.Limit.STORAGE_MB);
        if (!locked.enforced() || megabytes.isEmpty()) {
            throw invalidConfiguration();
        }
        Long limit;
        try {
            limit = Math.multiplyExact(megabytes.getAsLong(), BYTES_PER_MEGABYTE);
        } catch (ArithmeticException ignored) {
            throw invalidConfiguration();
        }
        long current = fileRepository.sumSizeBytesByTenantIdAndDeletedFalse(tenantId);
        try {
            if (Math.addExact(current, additionalBytes) > limit) {
                throw new BusinessRuleException(
                        "Storage quota exceeded: " + current + "/" + limit + " bytes",
                        "QUOTA_EXCEEDED_STORAGE");
            }
        } catch (ArithmeticException ignored) {
            throw new BusinessRuleException(
                    "Storage quota exceeded: " + current + "/" + limit + " bytes",
                    "QUOTA_EXCEEDED_STORAGE");
        }
    }

    public void checkCanMakeAiRequest(UUID tenantId) {
        checkCanConsumeAiCredits(tenantId, 1);
    }

    public void checkCanConsumeAiCredits(UUID tenantId, int credits) {
        if (credits < 0) {
            throw new BusinessRuleException("AI credits cannot be negative", "INVALID_AI_CREDITS");
        }
        LockedPlan locked = lockPlan(tenantId);
        if (!locked.enforced()) {
            return;
        }
        Optional<Boolean> aiFeature = planPolicy.feature(locked.plan(), "ai", "ai_copilot");
        if (aiFeature.isPresent() && !aiFeature.get()) {
            throw new BusinessRuleException("AI feature is disabled for this tenant", "FEATURE_DISABLED_AI");
        }
        OptionalLong limit = planPolicy.limit(locked.plan(), TenantPlanPolicy.Limit.AI_CREDITS);
        if (limit.isEmpty()) {
            throw invalidConfiguration();
        }
        LocalDate month = LocalDate.now(clock);
        long used = aiUsageRepository.sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
                tenantId, month, month.plusMonths(1));
        if ((long) credits > limit.getAsLong() - Math.min(used, limit.getAsLong())) {
            throw exceeded("ai_credits", used, limit.getAsLong());
        }
    }

    public void checkCanSendMessage(UUID tenantId) {
        LockedPlan locked = lockPlan(tenantId);
        OptionalLong limit = planPolicy.limit(locked.plan(), TenantPlanPolicy.Limit.MESSAGES);
        if (!locked.enforced() || limit.isEmpty()) {
            throw invalidConfiguration();
        }
        LocalDateTime from = LocalDate.now(clock).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        long current = messageRepository.countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
                tenantId, from, to);
        if (current >= limit.getAsLong()) {
            throw exceeded("messages", current, limit.getAsLong());
        }
    }

    public boolean isFeatureEnabled(UUID tenantId, String featureKey) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));
        return Boolean.TRUE.equals(readObject(tenant.getFeaturesJson()).get(featureKey));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQuotaUsage(UUID tenantId) {
        TenantUsageSnapshot snapshot = usageSnapshotService.getSnapshotForTenant(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));
        TenantPlanPolicy.ResolvedPlan resolvedPlan = planPolicy.resolve(tenant);
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("snapshot", snapshot);
        addMetric(usage, "users", snapshot.users());
        addMetric(usage, "storage", snapshot.storageBytes(), BYTES_PER_MEGABYTE, "usedMb", "limitMb");
        addMetric(usage, "aiRequests", snapshot.aiCredits());
        addMetric(usage, "courses", snapshot.courses());
        addMetric(usage, "messages", snapshot.messages());
        addOrganizationMetric(usage, "churches", resolvedPlan,
                orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH)
                        + orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH));
        addOrganizationMetric(usage, "departments", resolvedPlan,
                orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT));
        addOrganizationMetric(usage, "campuses", resolvedPlan,
                orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS));
        addOrganizationMetric(usage, "groups", resolvedPlan,
                orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.GROUP));
        return usage;
    }

    private void addMetric(Map<String, Object> target, String key, TenantUsageSnapshot.Metric metric) {
        if (metric.limit() != null) {
            Map<String, Object> legacy = new LinkedHashMap<>();
            legacy.put("used", metric.used());
            legacy.put("limit", metric.limit());
            legacy.put("percent", metric.utilizationPercent());
            target.put(key, legacy);
        }
    }

    private void addMetric(Map<String, Object> target,
                           String key,
                           TenantUsageSnapshot.Metric metric,
                           long divisor,
                           String usedKey,
                           String limitKey) {
        if (metric.limit() != null) {
            Map<String, Object> legacy = new LinkedHashMap<>();
            legacy.put(usedKey, metric.used() / (double) divisor);
            legacy.put(limitKey, metric.limit() / (double) divisor);
            legacy.put("percent", metric.utilizationPercent());
            target.put(key, legacy);
        }
    }

    private void addOrganizationMetric(Map<String, Object> target,
                                       String key,
                                       TenantPlanPolicy.ResolvedPlan resolvedPlan,
                                       long used) {
        Long limit = organizationLimit(resolvedPlan, key);
        if (limit != null) {
            target.put(key, Map.of("used", used, "limit", limit,
                    "percent", limit == 0 ? 0.0 : Math.round(used * 10000.0 / limit) / 100.0));
        }
    }

    private LockedPlan lockPlan(UUID tenantId) {
        Tenant tenant = tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));
        TenantPlanPolicy.ResolvedPlan resolvedPlan = planPolicy.resolve(tenant);
        if (resolvedPlan == null || !resolvedPlan.hasCatalogPlan() || resolvedPlan.subscription() == null
                || resolvedPlan.subscription().getPlanKey() == null
                || resolvedPlan.subscription().getPlanKey().isBlank()
                || !planPolicy.isEnabledSubscription(resolvedPlan.subscription().getStatus())
                || !resolvedPlan.enforcementEnabled() || !resolvedPlan.limitsValid()
                || !resolvedPlan.featuresValid()) {
            throw invalidConfiguration();
        }
        return new LockedPlan(resolvedPlan, true);
    }

    private Long organizationLimit(TenantPlanPolicy.ResolvedPlan resolvedPlan, String resource) {
        String key = switch (resource) {
            case "churches" -> "max_churches";
            case "departments" -> "max_departments";
            case "campuses" -> "max_campuses";
            case "groups" -> "max_groups";
            default -> null;
        };
        Long limit = key == null ? null : rawLimit(resolvedPlan, key);
        return limit != null ? limit : rawLimit(resolvedPlan, "spaces");
    }

    private Long rawLimit(TenantPlanPolicy.ResolvedPlan resolvedPlan, String key) {
        if (resolvedPlan == null || !resolvedPlan.enforcementEnabled() || !resolvedPlan.limitsValid()) {
            return null;
        }
        Object value = resolvedPlan.limits().get(key);
        if (value instanceof Number number) {
            long limit = number.longValue();
            return limit < 0 ? null : limit;
        }
        if (value instanceof String text) {
            try {
                long limit = Long.parseLong(text.trim());
                return limit < 0 ? null : limit;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BusinessRuleException invalidConfiguration() {
        return new BusinessRuleException("Quota configuration is unavailable or invalid", "QUOTA_CONFIGURATION_INVALID");
    }

    private BusinessRuleException exceeded(String resource, long used, long limit) {
        return new BusinessRuleException(
                "Quota exceeded for " + resource + ": " + used + "/" + limit,
                "QUOTA_EXCEEDED_" + resource.toUpperCase(Locale.ROOT));
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

    private record LockedPlan(TenantPlanPolicy.ResolvedPlan plan, boolean enforced) {
    }
}
