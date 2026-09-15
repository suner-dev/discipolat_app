package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class TenantSettingsController {

    private final TenantSettingsService settingsService;

    public TenantSettingsController(TenantSettingsService settingsService) {
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
    public ResponseEntity<TenantSettingsResponse> getSettings() {
        UUID tenantId = getCurrentTenantId();
        TenantSettings settings = settingsService.getSettings(tenantId);
        return ResponseEntity.ok(toResponse(settings));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<TenantSettingsResponse> updateSettings(@Valid @RequestBody TenantSettingsRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        TenantSettings saved = settingsService.updateSettings(tenantId, request, currentUserId);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping(value = "/branding/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadBrandingAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assetType") String assetType) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        String url = settingsService.uploadBrandingAsset(tenantId, file, assetType, currentUserId);
        return ResponseEntity.ok(Map.of("url", url, "assetType", assetType));
    }

    @GetMapping("/branding/css")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getBrandingCss() {
        UUID tenantId = getCurrentTenantId();
        String css = settingsService.generateBrandingCss(tenantId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(css);
    }

    @GetMapping("/public-branding")
    public ResponseEntity<Map<String, Object>> getPublicBranding() {
        UUID tenantId = getCurrentTenantId();
        Map<String, Object> branding = settingsService.getPublicBranding(tenantId);
        return ResponseEntity.ok(branding);
    }

    private TenantSettingsResponse toResponse(TenantSettings settings) {
        return new TenantSettingsResponse(
                settings.getId(),
                settings.getTenant() != null ? settings.getTenant().getId() : null,
                settings.getBusinessName(),
                settings.getSlogan(),
                settings.getLegalName(),
                settings.getDescription(),
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
                settings.getLocale(),
                settings.getSupportedLocales(),
                settings.getTimezone(),
                settings.getCountry(),
                settings.getCity(),
                settings.getCurrency(),
                settings.getDateFormat(),
                settings.getTimeFormat(),
                settings.getDateTimeFormat(),
                settings.getPhoneCountryCode(),
                settings.getWeekStartDay(),
                settings.getEmail(),
                settings.getPhone(),
                settings.getWebsite(),
                settings.getAddress(),
                settings.getOpeningHours(),
                settings.getWorkingDays(),
                settings.getInvitationEmailSubject(),
                settings.getInvitationEmailBody(),
                settings.getWelcomeEmailSubject(),
                settings.getWelcomeEmailBody(),
                settings.getFooterText(),
                settings.getFooterLinks(),
                settings.getLowBandEnabled(),
                settings.getPublicDirectoryEnabled(),
                settings.getLegacyMigrationEnabled(),
                settings.getOfflineMode(),
                settings.getAnalyticsEnabled(),
                settings.getAiFeaturesEnabled(),
                settings.getChatEnabled(),
                settings.getAcademyEnabled(),
                settings.getMarketplaceEnabled(),
                settings.getApiAccessEnabled(),
                settings.getCustomDomainEnabled(),
                settings.getSsoEnabled(),
                settings.getTwoFactorRequired(),
                settings.getPasswordPolicyEnabled(),
                settings.getSessionTimeoutMinutes(),
                settings.getMaxFailedLoginAttempts(),
                settings.getLockoutDurationMinutes(),
                settings.getUiConfig(),
                settings.getNotificationRules(),
                settings.getIntegrationConfig(),
                settings.getCustomCss(),
                settings.getCustomHeadHtml(),
                settings.getCreatedAt(),
                settings.getUpdatedAt(),
                settings.getVersion()
        );
    }

    public record TenantSettingsResponse(
            UUID id, UUID tenantId,
            String businessName, String slogan, String legalName, String description,
            String logoUrl, String logoDarkUrl, String coverUrl, String faviconUrl,
            String primaryColor, String secondaryColor, String accentColor,
            String surfaceColor, String backgroundColor,
            String textPrimaryColor, String textSecondaryColor,
            String successColor, String warningColor, String errorColor, String infoColor,
            String primaryFont, String secondaryFont, String headingFont, String monoFont,
            String locale, List<String> supportedLocales, String timezone, String country,
            String city, String currency, String dateFormat, String timeFormat,
            String dateTimeFormat, String phoneCountryCode, Integer weekStartDay,
            String email, String phone, String website, String address,
            Map<String, Object> openingHours, List<String> workingDays,
            String invitationEmailSubject, String invitationEmailBody,
            String welcomeEmailSubject, String welcomeEmailBody,
            String footerText, List<Map<String, String>> footerLinks,
            Boolean lowBandEnabled, Boolean publicDirectoryEnabled, Boolean legacyMigrationEnabled,
            String offlineMode, Boolean analyticsEnabled, Boolean aiFeaturesEnabled,
            Boolean chatEnabled, Boolean academyEnabled, Boolean marketplaceEnabled,
            Boolean apiAccessEnabled, Boolean customDomainEnabled, Boolean ssoEnabled,
            Boolean twoFactorRequired, Boolean passwordPolicyEnabled,
            Integer sessionTimeoutMinutes, Integer maxFailedLoginAttempts, Integer lockoutDurationMinutes,
            Map<String, Object> uiConfig, Map<String, Object> notificationRules,
            Map<String, Object> integrationConfig, String customCss, String customHeadHtml,
            java.time.Instant createdAt, java.time.Instant updatedAt, Integer version
    ) {}

    public record TenantSettingsRequest(
            // Identité
            String businessName, String slogan, String legalName, String description,
            // Branding visuel
            String logoUrl, String logoDarkUrl, String coverUrl, String faviconUrl,
            // Couleurs
            String primaryColor, String secondaryColor, String accentColor,
            String surfaceColor, String backgroundColor,
            String textPrimaryColor, String textSecondaryColor,
            String successColor, String warningColor, String errorColor, String infoColor,
            // Polices
            String primaryFont, String secondaryFont, String headingFont, String monoFont,
            // Localisation
            String locale, List<String> supportedLocales, String timezone, String country,
            String city, String currency, String dateFormat, String timeFormat,
            String dateTimeFormat, String phoneCountryCode, Integer weekStartDay,
            // Contact
            String email, String phone, String website, String address,
            Map<String, Object> openingHours, List<String> workingDays,
            // Textes communication
            String invitationEmailSubject, String invitationEmailBody,
            String welcomeEmailSubject, String welcomeEmailBody,
            String footerText, List<Map<String, String>> footerLinks,
            // Feature flags
            Boolean lowBandEnabled, Boolean publicDirectoryEnabled, Boolean legacyMigrationEnabled,
            String offlineMode, Boolean analyticsEnabled, Boolean aiFeaturesEnabled,
            Boolean chatEnabled, Boolean academyEnabled, Boolean marketplaceEnabled,
            Boolean apiAccessEnabled, Boolean customDomainEnabled, Boolean ssoEnabled,
            Boolean twoFactorRequired, Boolean passwordPolicyEnabled,
            Integer sessionTimeoutMinutes, Integer maxFailedLoginAttempts, Integer lockoutDurationMinutes,
            // Config avancée
            Map<String, Object> uiConfig, Map<String, Object> notificationRules,
            Map<String, Object> integrationConfig, String customCss, String customHeadHtml,
            // Métadonnées
            List<String> updatedFields
    ) {}
}