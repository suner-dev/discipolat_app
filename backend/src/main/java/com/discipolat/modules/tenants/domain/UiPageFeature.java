package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * LOT 2 §LB — fonctionnalité / bouton <b>activable ou retirable</b> sur un écran.
 *
 * <p>Traduit la demande « on peut choisir les fonctionnalités à ajouter sur
 * telles pages ou en enlever » : l'administration active, désactive et renomme
 * un bouton sans qu'une ligne de code ne soit touchée.
 *
 * <p><b>Défaut ouvert.</b> Une fonctionnalité <i>absente</i> de cette table
 * reste <b>visible</b> : on ne masque jamais par défaut, sinon le jour où
 * l'église enregistre son premier réglage, tout le reste de l'application
 * disparaîtrait. Seule une ligne explicite {@code enabled = false} retire une
 * fonctionnalité. C'est le choix sûr — « caché par défaut » casserait la
 * prod au premier clic d'admin.
 */
@Entity
@Table(name = "ui_page_features", indexes = {
        @Index(name = "idx_ui_page_feature_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ui_page_feature_page", columnList = "page_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UiPageFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** {@code null} = réglage global livré par défaut. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "node_id")
    private UUID nodeId;

    /** Écran : clé de route ({@code /souls}) ou identifiant ({@code soul-detail}). */
    @Column(name = "page_key", nullable = false, length = 100)
    private String pageKey;

    /** Fonctionnalité ou bouton : {@code export}, {@code delete}, {@code import}… */
    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    /** Renomme ce bouton précisément, sans toucher le dictionnaire global. */
    @Column(name = "label_override", length = 200)
    private String labelOverride;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "module_key", length = 50)
    private String moduleKey;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UiPageFeature that = (UiPageFeature) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}