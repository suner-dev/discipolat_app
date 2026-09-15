package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/branding")
public class BrandingController {

    private final TenantSettingsService settingsService;

    public BrandingController(TenantSettingsService settingsService) {
        this.settingsService = settingsService;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BrandingResponse> getBranding() {
        UUID tenantId = getCurrentTenantId();
        TenantSettingsService.BrandingRequest emptyRequest = new TenantSettingsService.BrandingRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        // On récupère les settings complets pour le branding
        var settings = settingsService.getSettings(tenantId);
        return ResponseEntity.ok(toBrandingResponse(settings));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<BrandingResponse> updateBranding(@Valid @RequestBody BrandingRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        // Convertir en TenantSettingsRequest pour réutiliser la logique
        TenantSettingsService.TenantSettingsRequest settingsRequest = new TenantSettingsService.TenantSettingsRequest(
                null, null, null, null,
                request.logoUrl(), request.logoDarkUrl(), request.coverUrl(), request.faviconUrl(),
                request.primaryColor(), request.secondaryColor(), request.accentColor(),
                request.surfaceColor(), request.backgroundColor(),
                request.textPrimaryColor(), request.textSecondaryColor(),
                request.successColor(), request.warningColor(), request.errorColor(), request.infoColor(),
                request.primaryFont(), request.secondaryFont(), request.headingFont(), request.monoFont(),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                request.customCss(), request.customHeadHtml(),
                request.updatedFields()
        );

        TenantSettingsService.BrandingRequest brandingRequest = new TenantSettingsService.BrandingRequest(
                request.logoUrl(), request.logoDarkUrl(), request.coverUrl(), request.faviconUrl(),
                request.primaryColor(), request.secondaryColor(), request.accentColor(),
                request.surfaceColor(), request.backgroundColor(),
                request.textPrimaryColor(), request.textSecondaryColor(),
                request.successColor(), request.warningColor(), request.errorColor(), request.infoColor(),
                request.primaryFont(), request.secondaryFont(), request.headingFont(), request.monoFont(),
                request.customCss(), request.customHeadHtml(),
                request.updatedFields()
        );

        TenantSettingsService.BrandingRequest empty = new TenantSettingsService.BrandingRequest(
                request.logoUrl(), request.logoDarkUrl(), request.coverUrl(), request.faviconUrl(),
                request.primaryColor(), request.secondaryColor(), request.accentColor(),
                request.surfaceColor(), request.backgroundColor(),
                request.textPrimaryColor(), request.textSecondaryColor(),
                request.successColor(), request.warningColor(), request.errorColor(), request.infoColor(),
                request.primaryFont(), request.secondaryFont(), request.headingFont(), request.monoFont(),
                request.customCss(), request.customHeadHtml(),
                request.updatedFields()
        );

        var saved = settingsService.updateBranding(tenantId, brandingRequest, currentUserId);
        return ResponseEntity.ok(toBrandingResponse(saved));
    }

    @PostMapping(value = "/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadBrandingAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assetType") String assetType) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        String url = settingsService.uploadBrandingAsset(tenantId, file, assetType, currentUserId);
        return ResponseEntity.ok(Map.of("url", url, "assetType", assetType));
    }

    @GetMapping("/css")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getBrandingCss() {
        UUID tenantId = getCurrentTenantId();
        String css = settingsService.generateBrandingCss(tenantId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(css);
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicBranding() {
        UUID tenantId = getCurrentTenantId();
        Map<String, Object> branding = settingsService.getPublicBranding(tenantId);
        return ResponseEntity.ok(branding);
    }

    private BrandingResponse toBrandingResponse(com.discipolat.modules.tenants.domain.TenantSettings settings) {
        return new BrandingResponse(
                settings.getId(),
                settings.getTenant() != null ? settings.getTenant().getId() : null,
                settings.getBusinessName(),
                settings.getSlogan(),
                settings.getLogoUrl(),
                settings.getLogoDarkUrl(),
                settings.getCoverUrl(),
                settings.getFaviconUrl(),
                settings.getPrimaryColor(),
                settings.getSecondaryColor(),
                settings.getAccentColor(),
                settings.getSurfaceColor(),
                settings.getBackgroundColor(),
                settings.getTextPrimaryColor(),
                settings.getTextSecondaryColor(),
                settings.getSuccessColor(),
                settings.getWarningColor(),
                settings.getErrorColor(),
                settings.getInfoColor(),
                settings.getPrimaryFont(),
                settings.getSecondaryFont(),
                settings.getHeadingFont(),
                settings.getMonoFont(),
                settings.getCustomCss(),
                settings.getCustomHeadHtml(),
                settings.getUpdatedAt()
        );
    }

    public record BrandingResponse(
            UUID id, UUID tenantId,
            String businessName, String slogan,
            String logoUrl, String logoDarkUrl, String coverUrl, String faviconUrl,
            String primaryColor, String secondaryColor, String accentColor,
            String surfaceColor, String backgroundColor,
            String textPrimaryColor, String textSecondaryColor,
            String successColor, String warningColor, String errorColor, String infoColor,
            String primaryFont, String secondaryFont, String headingFont, String monoFont,
            String customCss, String customHeadHtml,
            java.time.Instant updatedAt
    ) {}

    public record BrandingRequest(
            String logoUrl, String logoDarkUrl, String coverUrl, String faviconUrl,
            String primaryColor, String secondaryColor, String accentColor,
            String surfaceColor, String backgroundColor,
            String textPrimaryColor, String textSecondaryColor,
            String successColor, String warningColor, String errorColor, String infoColor,
            String primaryFont, String secondaryFont, String headingFont, String monoFont,
            String customCss, String customHeadHtml,
            java.util.List<String> updatedFields
    ) {}
}