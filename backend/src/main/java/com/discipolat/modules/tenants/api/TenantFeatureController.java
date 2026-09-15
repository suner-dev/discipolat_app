package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantFeature;
import com.discipolat.modules.tenants.domain.TenantFeatureService;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API de gestion des modules par tenant (G1.3 - §29)
 * GET/PUT/DELETE /api/admin/tenant-features
 */
@RestController
@RequestMapping("/api/v1/admin/tenant-features")
public class TenantFeatureController {

    private final TenantFeatureService featureService;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantFeatureController(TenantFeatureService featureService,
                                   TenantRepository tenantRepository,
                                   AuditService auditService) {
        this.featureService = featureService;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
        return tenantId;
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN')")
    public ResponseEntity<List<TenantFeatureResponse>> getFeatures() {
        UUID tenantId = getCurrentTenantId();
        List<TenantFeature> features = featureService.getFeaturesByTenant(tenantId);
        return ResponseEntity.ok(features.stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/enabled")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN')")
    public ResponseEntity<List<TenantFeatureResponse>> getEnabledFeatures() {
        UUID tenantId = getCurrentTenantId();
        List<TenantFeature> features = featureService.getEnabledFeatures(tenantId);
        return ResponseEntity.ok(features.stream()
                .map(this::toResponse)
                .toList());
    }

    @PutMapping("/{moduleCode}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<TenantFeatureResponse> updateFeature(
            @PathVariable String moduleCode,
            @RequestBody TenantFeatureUpdateRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        TenantFeature feature = featureService.enableFeature(
                tenantId, moduleCode, request.configuration());

        auditService.logSimple(currentUserId, tenantId, "TENANT_FEATURE_UPDATED",
                "TENANT_FEATURE", feature.getId(), "SUCCESS",
                Map.of("moduleCode", moduleCode, "enabled", request.enabled()));

        return ResponseEntity.ok(toResponse(feature));
    }

    @DeleteMapping("/{moduleCode}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Void> disableFeature(@PathVariable String moduleCode) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        featureService.disableFeature(tenantId, moduleCode);

        auditService.logSimple(currentUserId, tenantId, "TENANT_FEATURE_DISABLED",
                "TENANT_FEATURE", null, "SUCCESS",
                Map.of("moduleCode", moduleCode));

        return ResponseEntity.noContent().build();
    }

    // ==================== DTOs ====================

    public record TenantFeatureResponse(
            UUID id,
            UUID tenantId,
            String moduleCode,
            boolean enabled,
            Map<String, Object> configuration,
            Map<String, Object> limits,
            UUID createdAt,
            UUID updatedAt
    ) {}

    public record TenantFeatureUpdateRequest(
            boolean enabled,
            Map<String, Object> configuration
    ) {}

    private TenantFeatureResponse toResponse(TenantFeature feature) {
        return new TenantFeatureResponse(
                feature.getId(),
                feature.getTenant() != null ? feature.getTenant().getId() : null,
                feature.getModuleCode(),
                feature.getEnabled(),
                feature.getConfigurationJson(),
                feature.getLimitsJson(),
                feature.getCreatedAt() != null ? feature.getCreatedAt().toString().hashCode() : null,
                feature.getUpdatedAt() != null ? feature.getUpdatedAt().toString().hashCode() : null
        );
    }
}
