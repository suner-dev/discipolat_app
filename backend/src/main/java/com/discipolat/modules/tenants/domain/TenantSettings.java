package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    // Identité commerciale
    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "slogan", length = 500)
    private String slogan;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Branding visuel
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "logo_dark_url", length = 1000)
    private String logoDarkUrl;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "favicon_url", length = 1000)
    private String faviconUrl;

    // Couleurs
    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Column(name = "accent_color", length = 7)
    private String accentColor;

    @Column(name = "surface_color", length = 7)
    private String surfaceColor;

    @Column(name = "background_color", length = 7)
    private String backgroundColor;

    @Column(name = "text_primary_color", length = 7)
    private String textPrimaryColor;

    @Column(name = "text_secondary_color", length = 7)
    private String textSecondaryColor;

    @Column(name = "success_color", length = 7)
    private String successColor;

    @Column(name = "warning_color", length = 7)
    private String warningColor;

    @Column(name = "error_color", length = 7)
    private String errorColor;

    @Column(name = "info_color", length = 7)
    private String infoColor;

    // Polices
    @Column(name = "primary_font", length = 100)
    private String primaryFont;

    @Column(name = "secondary_font", length = 100)
    private String secondaryFont;

    @Column(name = "heading_font", length = 100)
    private String headingFont;

    @Column(name = "mono_font", length = 100)
    private String monoFont;

    // Localisation
    @Column(name = "locale", length = 10)
    private String locale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_locales", columnDefinition = "jsonb")
    private List<String> supportedLocales;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "date_format", length = 20)
    private String dateFormat;

    @Column(name = "time_format", length = 10)
    private String timeFormat;

    @Column(name = "datetime_format", length = 30)
    private String dateTimeFormat;

    @Column(name = "phone_country_code", length = 10)
    private String phoneCountryCode;

    @Column(name = "week_start_day")
    private Integer weekStartDay;

    // Contact
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private Map<String, Object> openingHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "working_days", columnDefinition = "jsonb")
    private List<String> workingDays;

    // Textes d'invitation / communication
    @Column(name = "invitation_email_subject", length = 255)
    private String invitationEmailSubject;

    @Column(name = "invitation_email_body", columnDefinition = "TEXT")
    private String invitationEmailBody;

    @Column(name = "welcome_email_subject", length = 255)
    private String welcomeEmailSubject;

    @Column(name = "welcome_email_body", columnDefinition = "TEXT")
    private String welcomeEmailBody;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "footer_links", columnDefinition = "jsonb")
    private List<Map<String, String>> footerLinks;

    // Feature flags / Toggles
    @Column(name = "low_band_enabled")
    private Boolean lowBandEnabled;

    @Column(name = "public_directory_enabled")
    private Boolean publicDirectoryEnabled;

    @Column(name = "legacy_migration_enabled")
    private Boolean legacyMigrationEnabled;

    @Column(name = "offline_mode", length = 20)
    private String offlineMode;

    @Column(name = "analytics_enabled")
    private Boolean analyticsEnabled;

    @Column(name = "ai_features_enabled")
    private Boolean aiFeaturesEnabled;

    @Column(name = "chat_enabled")
    private Boolean chatEnabled;

    @Column(name = "academy_enabled")
    private Boolean academyEnabled;

    @Column(name = "marketplace_enabled")
    private Boolean marketplaceEnabled;

    @Column(name = "api_access_enabled")
    private Boolean apiAccessEnabled;

    @Column(name = "custom_domain_enabled")
    private Boolean customDomainEnabled;

    @Column(name = "sso_enabled")
    private Boolean ssoEnabled;

    @Column(name = "two_factor_required")
    private Boolean twoFactorRequired;

    @Column(name = "password_policy_enabled")
    private Boolean passwordPolicyEnabled;

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes;

    @Column(name = "max_failed_login_attempts")
    private Integer maxFailedLoginAttempts;

    @Column(name = "lockout_duration_minutes")
    private Integer lockoutDurationMinutes;

    // Configuration avancée
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_config", columnDefinition = "jsonb")
    private Map<String, Object> uiConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_rules", columnDefinition = "jsonb")
    private Map<String, Object> notificationRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "integration_config", columnDefinition = "jsonb")
    private Map<String, Object> integrationConfig;

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "custom_head_html", columnDefinition = "TEXT")
    private String customHeadHtml;

    // Métadonnées
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.version == null) this.version = 1;
        // Defaults
        if (this.primaryColor == null) this.primaryColor = "#6366F1";
        if (this.secondaryColor == null) this.secondaryColor = "#8B5CF6";
        if (this.accentColor == null) this.accentColor = "#EC4899";
        if (this.surfaceColor == null) this.surfaceColor = "#FFFFFF";
        if (this.backgroundColor == null) this.backgroundColor = "#F8FAFC";
        if (this.textPrimaryColor == null) this.textPrimaryColor = "#1E293B";
        if (this.textSecondaryColor == null) this.textSecondaryColor = "#64748B";
        if (this.successColor == null) this.successColor = "#10B981";
        if (this.warningColor == null) this.warningColor = "#F59E0B";
        if (this.errorColor == null) this.errorColor = "#EF4444";
        if (this.infoColor == null) this.infoColor = "#3B82F6";
        if (this.primaryFont == null) this.primaryFont = "Inter";
        if (this.secondaryFont == null) this.secondaryFont = "Inter";
        if (this.headingFont == null) this.headingFont = "Inter";
        if (this.monoFont == null) this.monoFont = "JetBrains Mono";
        if (this.locale == null) this.locale = "fr";
        if (this.supportedLocales == null) this.supportedLocales = List.of("fr", "en");
        if (this.timezone == null) this.timezone = "Africa/Douala";
        if (this.country == null) this.country = "CM";
        if (this.currency == null) this.currency = "XAF";
        if (this.dateFormat == null) this.dateFormat = "dd/MM/yyyy";
        if (this.timeFormat == null) this.timeFormat = "HH:mm";
        if (this.dateTimeFormat == null) this.dateTimeFormat = "dd/MM/yyyy HH:mm";
        if (this.phoneCountryCode == null) this.phoneCountryCode = "+237";
        if (this.weekStartDay == null) this.weekStartDay = 1;
        if (this.lowBandEnabled == null) this.lowBandEnabled = false;
        if (this.publicDirectoryEnabled == null) this.publicDirectoryEnabled = false;
        if (this.legacyMigrationEnabled == null) this.legacyMigrationEnabled = false;
        if (this.offlineMode == null) this.offlineMode = "LECTURE";
        if (this.analyticsEnabled == null) this.analyticsEnabled = true;
        if (this.aiFeaturesEnabled == null) this.aiFeaturesEnabled = true;
        if (this.chatEnabled == null) this.chatEnabled = true;
        if (this.academyEnabled == null) this.academyEnabled = false;
        if (this.marketplaceEnabled == null) this.marketplaceEnabled = false;
        if (this.apiAccessEnabled == null) this.apiAccessEnabled = false;
        if (this.customDomainEnabled == null) this.customDomainEnabled = false;
        if (this.ssoEnabled == null) this.ssoEnabled = false;
        if (this.twoFactorRequired == null) this.twoFactorRequired = false;
        if (this.passwordPolicyEnabled == null) this.passwordPolicyEnabled = true;
        if (this.sessionTimeoutMinutes == null) this.sessionTimeoutMinutes = 60;
        if (this.maxFailedLoginAttempts == null) this.maxFailedLoginAttempts = 5;
        if (this.lockoutDurationMinutes == null) this.lockoutDurationMinutes = 30;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.version != null) this.version++;
    }

    // Méthode helper pour générer les variables CSS --brand-*
    public String generateBrandingCss() {
        StringBuilder css = new StringBuilder();
        css.append(":root {");
        css.append("  --brand-primary: ").append(primaryColor).append(";");
        css.append("  --brand-secondary: ").append(secondaryColor).append(";");
        css.append("  --brand-accent: ").append(accentColor).append(";");
        css.append("  --brand-surface: ").append(surfaceColor).append(";");
        css.append("  --brand-background: ").append(backgroundColor).append(";");
        css.append("  --brand-text-primary: ").append(textPrimaryColor).append(";");
        css.append("  --brand-text-secondary: ").append(textSecondaryColor).append(";");
        css.append("  --brand-success: ").append(successColor).append(";");
        css.append("  --brand-warning: ").append(warningColor).append(";");
        css.append("  --brand-error: ").append(errorColor).append(";");
        css.append("  --brand-info: ").append(infoColor).append(";");
        css.append("  --brand-font-primary: \"").append(primaryFont).append("\", sans-serif;");
        css.append("  --brand-font-secondary: \"").append(secondaryFont).append("\", sans-serif;");
        css.append("  --brand-font-heading: \"").append(headingFont).append("\", sans-serif;");
        css.append("  --brand-font-mono: \"").append(monoFont).append("\", monospace;");
        css.append("  --brand-logo-url: url(\"").append(logoUrl != null ? logoUrl : "").append("\");");
        css.append("  --brand-logo-dark-url: url(\"").append(logoDarkUrl != null ? logoDarkUrl : "").append("\");");
        css.append("  --brand-cover-url: url(\"").append(coverUrl != null ? coverUrl : "").append("\");");
        css.append("}");
        css.append("@media (prefers-color-scheme: dark) {");
        css.append("  :root { --brand-logo-url: url(\"").append(logoDarkUrl != null ? logoDarkUrl : (logoUrl != null ? logoUrl : "")).append("\"); }");
        css.append("}");
        if (customCss != null && !customCss.isBlank()) {
            css.append(customCss);
        }
        return css.toString();
    }
}