package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * LOT 2 §LB — surcharge d'un <b>libellé</b> par l'église.
 *
 * <p>Objectif produit : « chaque nom doit être paramétrable ». Plutôt que de
 * demander au code de declaraer une nouvelle chaîne à chaque besoin, le frontend
 * interroge ce store <b>avant</b> le dictionnaire i18n : toute chaîne déjà
 * traduite devient renommable par l'administration, sans toucher une seule page.
 *
 * <p><b>Résolution</b> : {@code nodeId} (réglage d'un nœud du réseau) →
 * {@code tenantId} (réglage d'église) → global, et pour une même portée la
 * langue exacte avant {@code "*"} (toutes langues).
 *
 * <p>Une surcharge {@code enabled = false} est conservée mais ignorée : l'admin
 * peut « suspendre » un renommage sans en perdre le texte.
 */
@Entity
@Table(name = "ui_label_overrides", indexes = {
        @Index(name = "idx_ui_label_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ui_label_key", columnList = "label_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UiLabelOverride {

    /** Langue « toutes » : la surcharge s'applique quelle que soit la locale. */
    public static final String ANY_LOCALE = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** {@code null} = réglage global livré par défaut. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** {@code null} = réglage d'église ; sinon réglage propre à un nœud. */
    @Column(name = "node_id")
    private UUID nodeId;

    @Column(name = "label_key", nullable = false, length = 150)
    private String labelKey;

    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private String locale = ANY_LOCALE;

    @Column(name = "value", nullable = false, length = 400)
    private String value;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

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
        UiLabelOverride that = (UiLabelOverride) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}