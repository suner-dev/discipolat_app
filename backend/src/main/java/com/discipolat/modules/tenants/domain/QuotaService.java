package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for enforcing tenant quotas based on their SaaS plan.
 * All entity creation that could exceed limits should call this service.
 */
@Service
@Transactional
public class QuotaService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanRepository planRepository;

    public QuotaService(TenantRepository tenantRepository,
                        UserRepository userRepository,
                        OrganizationNodeRepository orgNodeRepository,
                        TenantSubscriptionRepository subscriptionRepository,
                        SaasPlanRepository planRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    /**
     * Check if tenant can create a new user.
     * Throws BusinessRuleException if quota exceeded.
     */
    public void checkCanCreateUser(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> limits = getEffectiveLimits(tenant);
        if (limits == null || !limits.containsKey("max_users")) {
            return; // No limit configured
        }

        long currentUsers = userRepository.countByTenantId(tenantId);
        long maxUsers = ((Number) limits.get("max_users")).longValue();

        if (currentUsers >= maxUsers) {
            throw new BusinessRuleException(
                    "Limite d'utilisateurs atteinte: " + currentUsers + "/" + maxUsers +
                            ". Veuillez mettre à jour votre plan.",
                    "QUOTA_EXCEEDED_USERS");
        }
    }

    /**
     * Check if tenant can create a new church (ROOT_CHURCH or SUB_CHURCH).
     */
    public void checkCanCreateChurch(UUID tenantId, OrganizationNodeType type) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> limits = getEffectiveLimits(tenant);
        if (limits == null || !limits.containsKey("max_churches")) {
            return;
        }

        long currentChurches = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH)
                + orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH);
        long maxChurches = ((Number) limits.get("max_churches")).longValue();

        if (currentChurches >= maxChurches) {
            throw new BusinessRuleException(
                    "Limite d'églises atteinte: " + currentChurches + "/" + maxChurches +
                            ". Veuillez mettre à jour votre plan.",
                    "QUOTA_EXCEEDED_CHURCHES");
        }
    }

    /**
     * Check if tenant can create a new department.
     */
    public void checkCanCreateDepartment(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> limits = getEffectiveLimits(tenant);
        if (limits == null || !limits.containsKey("max_departments")) {
            return;
        }

        long currentDepartments = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);
        long maxDepartments = ((Number) limits.get("max_departments")).longValue();

        if (currentDepartments >= maxDepartments) {
            throw new BusinessRuleException(
                    "Limite de départements atteinte: " + currentDepartments + "/" + maxDepartments +
                            ". Veuillez mettre à jour votre plan.",
                    "QUOTA_EXCEEDED_DEPARTMENTS");
        }
    }

    /**
     * Check if tenant can create a new course.
     */
    public void checkCanCreateCourse(UUID tenantId) {
        // Would need CourseRepository - placeholder for now
        // Implement when CourseRepository is available
    }

    /**
     * Check if tenant can make an AI request.
     */
    public void checkCanMakeAiRequest(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> limits = getEffectiveLimits(tenant);
        if (limits == null || !limits.containsKey("max_ai_requests_month")) {
            return;
        }

        // TODO: Track current month AI requests from usage analytics
        // For now, just check if feature is enabled
        Map<String, Object> features = parseJson(tenant.getFeaturesJson());
        Boolean aiEnabled = (Boolean) features.getOrDefault("ai_copilot", false);
        if (!aiEnabled) {
            throw new BusinessRuleException("Fonctionnalité IA non activée pour ce tenant", "FEATURE_DISABLED_AI");
        }
    }

    /**
     * Check if tenant can send a message (monthly limit).
     */
    public void checkCanSendMessage(UUID tenantId) {
        // Would need message count tracking - placeholder
    }

    /**
     * Check if tenant has a specific feature enabled.
     */
    public boolean isFeatureEnabled(UUID tenantId, String featureKey) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> features = parseJson(tenant.getFeaturesJson());
        return Boolean.TRUE.equals(features.get(featureKey));
    }

    /**
     * Get all quota usage for a tenant.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuotaUsage(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> limits = getEffectiveLimits(tenant);
        Map<String, Object> usage = new java.util.HashMap<>();

        if (limits != null) {
            long users = userRepository.countByTenantId(tenantId);
            long churches = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH)
                    + orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH);
            long departments = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);
            long campuses = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS);
            long groups = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.GROUP);

            if (limits.containsKey("max_users")) {
                long max = ((Number) limits.get("max_users")).longValue();
                usage.put("users", Map.of("used", users, "limit", max, "percent", max > 0 ? (users * 100.0 / max) : 0));
            }
            if (limits.containsKey("max_churches")) {
                long max = ((Number) limits.get("max_churches")).longValue();
                usage.put("churches", Map.of("used", churches, "limit", max, "percent", max > 0 ? (churches * 100.0 / max) : 0));
            }
            if (limits.containsKey("max_departments")) {
                long max = ((Number) limits.get("max_departments")).longValue();
                usage.put("departments", Map.of("used", departments, "limit", max, "percent", max > 0 ? (departments * 100.0 / max) : 0));
            }
            if (limits.containsKey("max_campuses")) {
                long max = ((Number) limits.get("max_campuses")).longValue();
                usage.put("campuses", Map.of("used", campuses, "limit", max, "percent", max > 0 ? (campuses * 100.0 / max) : 0));
            }
            if (limits.containsKey("max_groups")) {
                long max = ((Number) limits.get("max_groups")).longValue();
                usage.put("groups", Map.of("used", groups, "limit", max, "percent", max > 0 ? (groups * 100.0 / max) : 0));
            }
            if (limits.containsKey("max_storage_mb")) {
                usage.put("storage", Map.of("usedMb", 0, "limitMb", limits.get("max_storage_mb"), "percent", 0));
            }
            if (limits.containsKey("max_ai_requests_month")) {
                usage.put("aiRequests", Map.of("used", 0, "limit", limits.get("max_ai_requests_month"), "percent", 0));
            }
            if (limits.containsKey("max_courses")) {
                usage.put("courses", Map.of("used", 0, "limit", limits.get("max_courses"), "percent", 0));
            }
            if (limits.containsKey("max_messages_month")) {
                usage.put("messages", Map.of("used", 0, "limit", limits.get("max_messages_month"), "percent", 0));
            }
        }

        return usage;
    }

    /**
     * Get effective limits for tenant (from subscription plan or tenant defaults).
     */
    private Map<String, Object> getEffectiveLimits(Tenant tenant) {
        // Try subscription plan first
        Optional<TenantSubscription> subscription = subscriptionRepository.findByTenantId(tenant.getId());
        if (subscription.isPresent()) {
            Optional<SaasPlan> plan = planRepository.findById(subscription.get().getPlanKey());
            if (plan.isPresent() && plan.get().getLimitsJson() != null) {
                return parseJson(plan.get().getLimitsJson());
            }
        }

        // Fallback to tenant's own limits_json (from V135)
        if (tenant.getFeaturesJson() != null) {
            Map<String, Object> features = parseJson(tenant.getFeaturesJson());
            if (features.containsKey("limits")) {
                return (Map<String, Object>) features.get("limits");
            }
        }

        // Default FREE plan limits
        return Map.of(
                "max_users", 50L,
                "max_churches", 1L,
                "max_departments", 10L,
                "max_campuses", 5L,
                "max_groups", 20L,
                "max_storage_mb", 100L,
                "max_ai_requests_month", 100L,
                "max_courses", 5L,
                "max_messages_month", 1000L
        );
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}