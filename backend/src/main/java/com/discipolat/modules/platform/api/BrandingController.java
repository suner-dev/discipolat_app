package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Branding Controller (Section 28 du prompt)
 * Chaque tenant peut personnaliser son environnement
 */
@RestController
@RequestMapping("/api/v1/admin/branding")
public class BrandingController {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public BrandingController(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant");
        return tenantId;
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BrandingResponse> getBranding() {
        UUID tenantId = getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        return ResponseEntity.ok(new BrandingResponse(
                tenant.getId(),
                tenant.getBrandingJson() != null ? parseBranding(tenant.getBrandingJson()) : getDefaultBranding()
        ));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<BrandingResponse> updateBranding(@RequestBody BrandingUpdateRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        String brandingJson = toBrandingJson(request);
        tenant.setBrandingJson(brandingJson);
        tenantRepository.save(tenant);

        auditService.log(currentUserId, tenantId, "BRANDING_UPDATED", "TENANT",
                tenantId, "SUCCESS", Map.of(), null, null, null);

        return ResponseEntity.ok(new BrandingResponse(tenantId, parseBranding(brandingJson)));
    }

    private Map<String, Object> parseBranding(String json) {
        if (json == null || json.isBlank()) {
            return getDefaultBranding();
        }
        try {
            return com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return getDefaultBranding();
        }
    }

    private String toBrandingJson(BrandingUpdateRequest request) {
        Map<String, Object> branding = new java.util.HashMap<>();
        if (request.primaryColor() != null) branding.put("primaryColor", request.primaryColor());
        if (request.secondaryColor() != null) branding.put("secondaryColor", request.secondaryColor());
        if (request.accentColor() != null) branding.put("accentColor", request.accentColor());
        if (request.logoUrl() != null) branding.put("logoUrl", request.logoUrl());
        if (request.faviconUrl() != null) branding.put("faviconUrl", request.faviconUrl());
        if (request.logoDarkUrl() != null) branding.put("logoDarkUrl", request.logoDarkUrl());
        if (request.primaryFont() != null) branding.put("primaryFont", request.primaryFont());
        if (request.secondaryFont() != null) branding.put("secondaryFont", request.secondaryFont());
        branding.put("churchName", request.churchName() != null ? request.churchName() : "");
        branding.put("tagline", request.tagline() != null ? request.tagline() : "");
        branding.put("address", request.address() != null ? request.address() : "");
        branding.put("phone", request.phone() != null ? request.phone() : "");
        branding.put("email", request.email() != null ? request.email() : "");
        branding.put("website", request.website() != null ? request.website() : "");
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(branding);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> getDefaultBranding() {
        return Map.of(
                "primaryColor", "#6366F1",
                "secondaryColor", "#8B5CF6",
                "accentColor", "#EC4899",
                "logoUrl", "",
                "faviconUrl", "",
                "logoDarkUrl", "",
                "primaryFont", "Inter",
                "secondaryFont", "Inter",
                "churchName", "",
                "tagline", "",
                "address", "",
                "phone", "",
                "email", "",
                "website", ""
        );
    }

    public record BrandingResponse(UUID tenantId, Map<String, Object> branding) {}
    public record BrandingUpdateRequest(
            String primaryColor, String secondaryColor, String accentColor,
            String logoUrl, String faviconUrl, String logoDarkUrl,
            String primaryFont, String secondaryFont,
            String churchName, String tagline, String address,
            String phone, String email, String website
    ) {}
}
