package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LOT 2 §GR — un <b>groupe d'onglets</b> de la navigation.
 *
 * <p>La barre latérale empilait ~175 entrées en sections plates. Un groupe
 * ajoute un niveau d'indirection : au clic, il révèle la liste de ses
 * sous-onglets. Les groupes sont <b>hiérarchiques</b> ({@code parentGroupId}) :
 * une église qui veut 3 niveaux (Dénomination &gt; Régions &gt; Départements)
 * n'a rien à coder.
 *
 * <p><b>Héritage.</b> {@code tenantId == null} = groupe <b>global</b>, livré par
 * défaut et commun à toutes les églises. {@code tenantId} renseigné = groupe
 * d'<b>une</b> église, qui écrase (override) le global de même {@code key} —
 * même sémantique que {@code ConfigurationResolver}
 * ({@code DEFAULT}/{@code INHERITED}/{@code OVERRIDDEN}).
 *
 * <p><b>Aucune perte.</b> Une entrée de menu sans affectation explicite reste
 * visible : le résolveur la rattache au groupe dont la {@code key}/{@code label}
 * correspond à sa {@code section}. Supprimer un groupe ne supprime donc aucune
 * entrée.
 */
@Entity
@Table(name = "navigation_groups", indexes = {
        @Index(name = "idx_navigation_groups_tenant", columnList = "tenant_id"),
        @Index(name = "idx_navigation_groups_parent", columnList = "parent_group_id"),
        @Index(name = "idx_navigation_groups_module", columnList = "module_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** {@code null} = groupe global (défaut commun) ; sinon groupe d'une église. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "key", nullable = false, length = 60)
    private String key;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    /** Imbrication : groupe conteneur. {@code null} = groupe de premier niveau. */
    @Column(name = "parent_group_id")
    private UUID parentGroupId;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /** Rôles autorisés à voir le groupe ; vide = tous les rôles. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Column(name = "module_key", length = 50)
    private String moduleKey;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** {@code true} (défaut) = sous-onglets repliés jusqu'au clic sur le groupe. */
    @Column(name = "collapsed_by_default", nullable = false)
    @Builder.Default
    private boolean collapsedByDefault = true;

    @Column(name = "show_count", nullable = false)
    @Builder.Default
    private boolean showCount = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Vrai si le groupe est visible pour au moins un des rôles donnés. */
    public boolean isVisibleForRoles(java.util.Collection<String> candidateRoles) {
        if (!enabled) {
            return false;
        }
        if (roles == null || roles.isEmpty()) {
            return true;
        }
        if (candidateRoles == null) {
            return false;
        }
        return roles.stream().anyMatch(candidateRoles::contains);
    }

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
        NavigationGroup that = (NavigationGroup) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}