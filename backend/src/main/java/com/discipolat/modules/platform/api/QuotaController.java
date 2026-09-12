package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.FeatureAccessService;
import com.discipolat.modules.tenants.domain.QuotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/quotas")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
public class QuotaController {

    private final QuotaService quotaService;
    private final FeatureAccessService featureAccessService;

    public QuotaController(QuotaService quotaService, FeatureAccessService featureAccessService) {
        this.quotaService = quotaService;
        this.featureAccessService = featureAccessService;
    }

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getQuotaUsage() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(quotaService.getQuotaUsage(tenantId));
    }

    @GetMapping("/check/{resource}")
    public ResponseEntity<Map<String, Object>> checkQuota(@PathVariable String resource) {
        UUID tenantId = TenantContext.requireTenantId();
        
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            switch (resource.toLowerCase()) {
                case "user" -> quotaService.checkCanCreateUser(tenantId);
                case "church" -> quotaService.checkCanCreateChurch(tenantId, com.discipolat.modules.tenants.domain.OrganizationNodeType.ROOT_CHURCH);
                case "department" -> quotaService.checkCanCreateDepartment(tenantId);
                case "course" -> quotaService.checkCanCreateCourse(tenantId);
                case "ai" -> quotaService.checkCanMakeAiRequest(tenantId);
                default -> result.put("error", "Unknown resource: " + resource);
            }
            result.put("allowed", true);
        } catch (com.discipolat.common.domain.BusinessRuleException e) {
            result.put("allowed", false);
            result.put("error", e.getMessage());
            result.put("code", e.getCode());
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/features")
    public ResponseEntity<Map<String, Boolean>> getAllFeatures() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(featureAccessService.getAllFeatures(tenantId));
    }

    @PostMapping("/features/{featureKey}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleFeature(
            @PathVariable String featureKey,
            @RequestBody Map<String, Boolean> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        boolean enabled = request.get("enabled");

        featureAccessService.setFeature(tenantId, featureKey, enabled, currentUserId);
        
        return ResponseEntity.ok(Map.of(
                "feature", featureKey,
                "enabled", enabled,
                "message", enabled ? "Fonctionnalité activée" : "Fonctionnalité désactivée"
        ));
    }
}