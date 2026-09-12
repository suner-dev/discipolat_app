package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service for enforcing feature/module access control.
 * Backend must refuse access even if frontend hides the feature.
 */
@Service
@Transactional
public class FeatureAccessService {

    private final TenantRepository tenantRepository;
    private final QuotaService quotaService;

    public FeatureAccessService(TenantRepository tenantRepository, QuotaService quotaService) {
        this.tenantRepository = tenantRepository;
        this.quotaService = quotaService;
    }

    /**
     * Check if a feature is enabled for the tenant.
     * Throws exception if disabled.
     */
    public void requireFeature(UUID tenantId, String featureKey) {
        if (!isFeatureEnabled(tenantId, featureKey)) {
            throw new BusinessRuleException(
                    "Fonctionnalité '" + featureKey + "' non activée pour ce tenant. " +
                            "Veuillez l'activer dans les paramètres ou contacter l'administrateur.",
                    "FEATURE_DISABLED_" + featureKey.toUpperCase());
        }
    }

    /**
     * Check if feature is enabled (boolean return).
     */
    public boolean isFeatureEnabled(UUID tenantId, String featureKey) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> features = parseJson(tenant.getFeaturesJson());
        return Boolean.TRUE.equals(features.get(featureKey));
    }

    /**
     * Require feature AND quota for operations that need both.
     * Example: Creating a course needs ACADEMY feature + course quota.
     */
    public void requireFeatureAndQuota(UUID tenantId, String featureKey, QuotaCheck quotaCheck) {
        requireFeature(tenantId, featureKey);
        quotaCheck.check(tenantId);
    }

    /**
     * Get all feature flags for a tenant.
     */
    @Transactional(readOnly = true)
    public Map<String, Boolean> getAllFeatures(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> features = parseJson(tenant.getFeaturesJson());
        Map<String, Boolean> result = new java.util.HashMap<>();

        // Known feature keys with defaults
        String[] knownFeatures = {
                "discipleship", "academy", "ai_copilot", "finance", "marketplace",
                "api", "whatsapp", "analytics", "mobile_money", "support_priority"
        };

        for (String key : knownFeatures) {
            result.put(key, Boolean.TRUE.equals(parseJson(tenant.getFeaturesJson()).get(key)));
        }

        return result;
    }

    /**
     * Enable/disable a feature for a tenant (admin only).
     */
    public void setFeature(UUID tenantId, String featureKey, boolean enabled, UUID adminId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant not found", "TENANT_NOT_FOUND"));

        Map<String, Object> features = parseJson(tenant.getFeaturesJson());
        features.put(featureKey, enabled);

        tenant.setFeaturesJson(toJson(features));
        // tenantRepository.save(tenant); // Caller should save

        // Audit log would go here
    }

    /**
     * Check if tenant can access a module (alias for feature).
     */
    public void requireModule(UUID tenantId, String moduleKey) {
        requireFeature(tenantId, moduleKey);
    }

    /**
     * Functional interface for quota checks.
     */
    @FunctionalInterface
    public interface QuotaCheck {
        void check(UUID tenantId);
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