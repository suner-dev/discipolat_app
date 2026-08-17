package com.discipolat.modules.platform.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Page personnalisée du Page Builder : une page contient une liste de
 * blocs (KPI, tableau, liste, texte, liens, recherche, images) configurés
 * par l'administrateur et publiés pour des rôles donnés. Les données des
 * blocs sont résolues côté serveur sur les entités réelles — jamais de
 * statistique fictive. Chaque mutation est versionnée (config_revisions).
 */
@Entity
@Table(name = "custom_pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomPage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "key", nullable = false, unique = true, length = 50)
    private String key;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "layout", nullable = false, length = 20)
    @Builder.Default
    private String layout = "STACK";

    /** Blocs de la page : liste de {type, config} — config = Map libre. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blocks", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> blocks = new ArrayList<>();

    /** Rôles autorisés (vide = tous les rôles authentifiés). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(name = "created_by")
    private UUID createdBy;

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

    /** Une page visible pour le rôle donné (liste vide = tous). */
    public boolean isVisibleForRole(String role) {
        return roles == null || roles.isEmpty() || roles.contains(role);
    }
}
