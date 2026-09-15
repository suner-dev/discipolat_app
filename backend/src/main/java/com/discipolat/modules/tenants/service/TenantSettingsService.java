package com.discipolat.modules.tenants.service;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import com.discipolat.modules.tenants.domain.TenantSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantSettingsService {

    private final TenantSettingsRepository settingsRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    public TenantSettings getSettings(UUID tenantId) {
        return settingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Paramètres tenant non trouvés pour: " + tenantId));
    }

    public TenantSettings getCurrentSettings() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
        return getSettings(tenantId);
    }

    public TenantSettings updateSettings(UUID tenantId, TenantSettingsRequest request, UUID userId) {
        TenantSettings settings = getSettings(tenantId);
        
        // Identité commerciale
        if (request.businessName() != null) settings.setBusinessName(request.businessName());
        if (request.slogan() != null) settings.setSlogan(request.slogan());
        if (request.legalName() != null) settings.setLegalName(request.legalName());
        if (request.description() != null) settings.setDescription(request.description());

        // Branding visuel
        if (request.logoUrl() != null) settings.setLogoUrl(request.logoUrl());
        if (request.logoDarkUrl() != null) settings.setLogoDarkUrl(request.logoDarkUrl());
        if (request.coverUrl() != null) settings.setCoverUrl(request.coverUrl());
        if (request.faviconUrl() != null) settings.setFaviconUrl(request.faviconUrl());

        // Couleurs
        if (request.primaryColor() != null) settings.setPrimaryColor(request.primaryColor());
        if (request.secondaryColor() != null) settings.setSecondaryColor(request.secondaryColor());
        if (request.accentColor() != null) settings.setAccentColor(request.accentColor());
        if (request.surfaceColor() != null) settings.setSurfaceColor(request.surfaceColor());
        if (request.backgroundColor() != null) settings.setBackgroundColor(request.backgroundColor());
        if (request.textPrimaryColor() != null) settings.setTextPrimaryColor(request.textPrimaryColor());
        if (request.textSecondaryColor() != null) settings.setTextSecondaryColor(request.textSecondaryColor());
        if (request.successColor() != null) settings.setSuccessColor(request.successColor());
        if (request.warningColor() != null) settings.setWarningColor(request.warningColor());
        if (request.errorColor() != null) settings.setErrorColor(request.errorColor());
        if (request.infoColor() != null) settings.setInfoColor(request.infoColor());

        // Polices
        if (request.primaryFont() != null) settings.setPrimaryFont(request.primaryFont());
        if (request.secondaryFont() != null) settings.setSecondaryFont(request.secondaryFont());
        if (request.headingFont() != null) settings.setHeadingFont(request.headingFont());
        if (request.monoFont() != null) settings.setMonoFont(request.monoFont());

        // Localisation
        if (request.locale() != null) settings.setLocale(request.locale());
        if (request.supportedLocales() != null) settings.setSupportedLocales(request.supportedLocales());
        if (request.timezone() != null) settings.setTimezone(request.timezone());
        if (request.country() != null) settings.setCountry(request.country());
        if (request.city() != null) settings.setCity(request.city());
        if (request.currency() != null) settings.setCurrency(request.currency());
        if (request.dateFormat() != null) settings.setDateFormat(request.dateFormat());
        if (request.timeFormat() != null) settings.setTimeFormat(request.timeFormat());
        if (request.dateTimeFormat() != null) settings.setDateTimeFormat(request.dateTimeFormat());
        if (request.phoneCountryCode() != null) settings.setPhoneCountryCode(request.phoneCountryCode());
        if (request.weekStartDay() != null) settings.setWeekStartDay(request.weekStartDay());

        // Contact
        if (request.email() != null) settings.setEmail(request.email());
        if (request.phone() != null) settings.setPhone(request.phone());
        if (request.website() != null) settings.setWebsite(request.website());
        if (request.address() != null) settings.setAddress(request.address());
        if (request.openingHours() != null) settings.setOpeningHours(request.openingHours());
        if (request.workingDays() != null) settings.setWorkingDays(request.workingDays());

        // Textes communication
        if (request.invitationEmailSubject() != null) settings.setInvitationEmailSubject(request.invitationEmailSubject());
        if (request.invitationEmailBody() != null) settings.setInvitationEmailBody(request.invitationEmailBody());
        if (request.welcomeEmailSubject() != null) settings.setWelcomeEmailSubject(request.welcomeEmailSubject());
        if (request.welcomeEmailBody() != null) settings.setWelcomeEmailBody(request.welcomeEmailBody());
        if (request.footerText() != null) settings.setFooterText(request.footerText());
        if (request.footerLinks() != null) settings.setFooterLinks(request.footerLinks());

        // Feature flags
        if (request.lowBandEnabled() != null) settings.setLowBandEnabled(request.lowBandEnabled());
        if (request.publicDirectoryEnabled() != null) settings.setPublicDirectoryEnabled(request.publicDirectoryEnabled());
        if (request.legacyMigrationEnabled() != null) settings.setLegacyMigrationEnabled(request.legacyMigrationEnabled());
        if (request.offlineMode() != null) settings.setOfflineMode(request.offlineMode());
        if (request.analyticsEnabled() != null) settings.setAnalyticsEnabled(request.analyticsEnabled());
        if (request.aiFeaturesEnabled() != null) settings.setAiFeaturesEnabled(request.aiFeaturesEnabled());
        if (request.chatEnabled() != null) settings.setChatEnabled(request.chatEnabled());
        if (request.academyEnabled() != null) settings.setAcademyEnabled(request.academyEnabled());
        if (request.marketplaceEnabled() != null) settings.setMarketplaceEnabled(request.marketplaceEnabled());
        if (request.apiAccessEnabled() != null) settings.setApiAccessEnabled(request.apiAccessEnabled());
        if (request.customDomainEnabled() != null) settings.setCustomDomainEnabled(request.customDomainEnabled());
        if (request.ssoEnabled() != null) settings.setSsoEnabled(request.ssoEnabled());
        if (request.twoFactorRequired() != null) settings.setTwoFactorRequired(request.twoFactorRequired());
        if (request.passwordPolicyEnabled() != null) settings.setPasswordPolicyEnabled(request.passwordPolicyEnabled());
        if (request.sessionTimeoutMinutes() != null) settings.setSessionTimeoutMinutes(request.sessionTimeoutMinutes());
        if (request.maxFailedLoginAttempts() != null) settings.setMaxFailedLoginAttempts(request.maxFailedLoginAttempts());
        if (request.lockoutDurationMinutes() != null) settings.setLockoutDurationMinutes(request.lockoutDurationMinutes());

        // Configuration avancée
        if (request.uiConfig() != null) settings.setUiConfig(request.uiConfig());
        if (request.notificationRules() != null) settings.setNotificationRules(request.notificationRules());
        if (request.integrationConfig() != null) settings.setIntegrationConfig(request.integrationConfig());
        if (request.customCss() != null) settings.setCustomCss(request.customCss());
        if (request.customHeadHtml() != null) settings.setCustomHeadHtml(request.customHeadHtml());

        settings.setUpdatedBy(userId);
        TenantSettings saved = settingsRepository.save(settings);

        // Audit
        auditService.log(userId, tenantId, "TENANT_SETTINGS_UPDATED", "TENANT_SETTINGS",
                tenantId, "SUCCESS", Map.of("updatedFields", request.updatedFields()), null, null, null);

        // Publier événement pour propagation temps réel
        publishSettingsChangedEvent(tenantId);

        return saved;
    }

    public TenantSettings updateBranding(UUID tenantId, BrandingRequest request, UUID userId) {
        TenantSettings settings = getSettings(tenantId);
        
        if (request.logoUrl() != null) settings.setLogoUrl(request.logoUrl());
        if (request.logoDarkUrl() != null) settings.setLogoDarkUrl(request.logoDarkUrl());
        if (request.coverUrl() != null) settings.setCoverUrl(request.coverUrl());
        if (request.faviconUrl() != null) settings.setFaviconUrl(request.faviconUrl());
        if (request.primaryColor() != null) settings.setPrimaryColor(request.primaryColor());
        if (request.secondaryColor() != null) settings.setSecondaryColor(request.secondaryColor());
        if (request.accentColor() != null) settings.setAccentColor(request.accentColor());
        if (request.surfaceColor() != null) settings.setSurfaceColor(request.surfaceColor());
        if (request.backgroundColor() != null) settings.setBackgroundColor(request.backgroundColor());
        if (request.textPrimaryColor() != null) settings.setTextPrimaryColor(request.textPrimaryColor());
        if (request.textSecondaryColor() != null) settings.setTextSecondaryColor(request.textSecondaryColor());
        if (request.successColor() != null) settings.setSuccessColor(request.successColor());
        if (request.warningColor() != null) settings.setWarningColor(request.warningColor());
        if (request.errorColor() != null) settings.setErrorColor(request.errorColor());
        if (request.infoColor() != null) settings.setInfoColor(request.infoColor());
        if (request.primaryFont() != null) settings.setPrimaryFont(request.primaryFont());
        if (request.secondaryFont() != null) settings.setSecondaryFont(request.secondaryFont());
        if (request.headingFont() != null) settings.setHeadingFont(request.headingFont());
        if (request.monoFont() != null) settings.setMonoFont(request.monoFont());
        if (request.customCss() != null) settings.setCustomCss(request.customCss());
        if (request.customHeadHtml() != null) settings.setCustomHeadHtml(request.customHeadHtml());

        settings.setUpdatedBy(userId);
        TenantSettings saved = settingsRepository.save(settings);

        // Audit
        auditService.log(userId, tenantId, "BRANDING_UPDATED", "TENANT_SETTINGS",
                tenantId, "SUCCESS", Map.of("updatedFields", request.updatedFields()), null, null, null);

        // Publier événement pour propagation temps réel
        publishSettingsChangedEvent(tenantId);

        return saved;
    }

    public String uploadBrandingAsset(UUID tenantId, MultipartFile file, String assetType, UUID userId) {
        // Validation type de fichier
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Seuls les fichiers images sont autorisés");
        }

        // Validation taille (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 5MB)");
        }

        // Upload isolé par tenant
        String path = "tenants/" + tenantId + "/branding/" + assetType + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String url = fileStorageService.upload(file, path);

        // Mettre à jour le setting correspondant
        TenantSettings settings = getSettings(tenantId);
        switch (assetType) {
            case "logo" -> settings.setLogoUrl(url);
            case "logo-dark" -> settings.setLogoDarkUrl(url);
            case "cover" -> settings.setCoverUrl(url);
            case "favicon" -> settings.setFaviconUrl(url);
            default -> throw new IllegalArgumentException("Type d'asset inconnu: " + assetType);
        }
        settings.setUpdatedBy(userId);
        settingsRepository.save(settings);

        // Audit
        auditService.log(userId, tenantId, "BRANDING_ASSET_UPLOADED", "TENANT_SETTINGS",
                tenantId, "SUCCESS", Map.of("assetType", assetType, "url", url), null, null, null);

        // Publier événement
        publishSettingsChangedEvent(tenantId);

        return url;
    }

    public String generateBrandingCss(UUID tenantId) {
        TenantSettings settings = getSettings(tenantId);
        return settings.generateBrandingCss();
    }

    public Map<String, Object> getPublicBranding(UUID tenantId) {
        TenantSettings settings = getSettings(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant non trouvé: " + tenantId));

        return Map.of(
                "tenantId", tenantId,
                "displayName", settings.getBusinessName() != null ? settings.getBusinessName() : tenant.getName(),
                "slogan", settings.getSlogan(),
                "logoUrl", settings.getLogoUrl(),
                "logoDarkUrl", settings.getLogoDarkUrl(),
                "coverUrl", settings.getCoverUrl(),
                "faviconUrl", settings.getFaviconUrl(),
                "primaryColor", settings.getPrimaryColor(),
                "secondaryColor", settings.getSecondaryColor(),
                "accentColor", settings.getAccentColor(),
                "surfaceColor", settings.getSurfaceColor(),
                "backgroundColor", settings.getBackgroundColor(),
                "textPrimaryColor", settings.getTextPrimaryColor(),
                "textSecondaryColor", settings.getTextSecondaryColor(),
                "successColor", settings.getSuccessColor(),
                "warningColor", settings.getWarningColor(),
                "errorColor", settings.getErrorColor(),
                "infoColor", settings.getInfoColor(),
                "primaryFont", settings.getPrimaryFont(),
                "secondaryFont", settings.getSecondaryFont(),
                "headingFont", settings.getHeadingFont(),
                "monoFont", settings.getMonoFont(),
                "customCss", settings.getCustomCss(),
                "customHeadHtml", settings.getCustomHeadHtml()
        );
    }

    public void initializeDefaultSettings(UUID tenantId, UUID userId) {
        if (settingsRepository.existsByTenantId(tenantId)) {
            return; // Already exists
        }

        TenantSettings settings = TenantSettings.builder()
                .tenant(tenantRepository.getReferenceById(tenantId))
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        settingsRepository.save(settings);
    }

    private void publishSettingsChangedEvent(UUID tenantId) {
        // Publier événement WebSocket pour propagation temps réel
        // Destination: /topic/tenant/{tenantId}/settings
        try {
            Map<String, Object> payload = Map.of(
                    "event", "tenant-settings:changed",
                    "tenantId", tenantId.toString(),
                    "timestamp", java.time.Instant.now().toString(),
                    "action", "REFRESH_CONFIG"
            );
            
            // Envoyer à tous les utilisateurs du tenant
            messagingTemplate.convertAndSend(
                    "/topic/tenant/" + tenantId + "/settings",
                    payload
            );
            
            // Aussi envoyer l'événement de branding spécifique
            messagingTemplate.convertAndSend(
                    "/topic/tenant/" + tenantId + "/branding",
                    Map.of(
                            "event", "tenant-branding:changed",
                            "tenantId", tenantId.toString(),
                            "timestamp", java.time.Instant.now().toString()
                    )
            );
        } catch (Exception e) {
            // Log mais ne pas faire échouer la transaction
            System.err.println("Erreur publication WebSocket tenant-settings:changed: " + e.getMessage());
        }
    }

    // Records pour DTOs
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

    public record BrandingRequest(
            String logoUrl, String logoDarkUrl, String coverUrl, String faviconUrl,
            String primaryColor, String secondaryColor, String accentColor,
            String surfaceColor, String backgroundColor,
            String textPrimaryColor, String textSecondaryColor,
            String successColor, String warningColor, String errorColor, String infoColor,
            String primaryFont, String secondaryFont, String headingFont, String monoFont,
            String customCss, String customHeadHtml,
            List<String> updatedFields
    ) {}
}