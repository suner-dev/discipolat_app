package com.discipolat.modules.platform.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Identité et marque de l'église.
 *
 * Ligne unique (singleton) configurée par l'administrateur : nom de l'église,
 * nom de la plateforme, slogan, logo, favicon, couleurs, typographie et
 * coordonnées. Consommée par le Web (thème dynamique), le Mobile et la
 * landing page — aucun changement de code nécessaire.
 */
@Entity
@Table(name = "church_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ChurchSettings {

    public static final String DEFAULT_PRIMARY_COLOR = "#16a34a";
    public static final String DEFAULT_ACCENT_COLOR = "#f59e0b";
    public static final String DEFAULT_BUTTON_COLOR = "#16a34a";
    public static final String DEFAULT_FONT_FAMILY = "Inter";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "church_name", nullable = false)
    @Builder.Default
    private String churchName = "Discipolat";

    @Column(name = "platform_name", nullable = false)
    @Builder.Default
    private String platformName = "Discipolat";

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "description")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "primary_color", nullable = false)
    @Builder.Default
    private String primaryColor = DEFAULT_PRIMARY_COLOR;

    @Column(name = "accent_color", nullable = false)
    @Builder.Default
    private String accentColor = DEFAULT_ACCENT_COLOR;

    @Column(name = "button_color", nullable = false)
    @Builder.Default
    private String buttonColor = DEFAULT_BUTTON_COLOR;

    @Column(name = "font_family", nullable = false)
    @Builder.Default
    private String fontFamily = DEFAULT_FONT_FAMILY;

    @Column(name = "allow_dark_mode", nullable = false)
    @Builder.Default
    private boolean allowDarkMode = true;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_links", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> socialLinks = new HashMap<>();

    @Column(name = "contact_notes")
    private String contactNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
