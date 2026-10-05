package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §A — niveau hiérarchique <b>configurable</b>.
 *
 * <p>V2 fixait la hiérarchie dans l'enum {@link OrganizationNodeType}. V3 rend
 * chaque dénomination libre de <b>définir ses propres niveaux</b> (Région, Zone,
 * Circonscription, Campus…) : nom, ordre, parenté, icône, couleur. Un niveau
 * appartient à une <b>racine</b> ({@code rootTenantId}) — deux dénominations
 * n'ont jamais les mêmes niveaux.
 *
 * <p>Le {@link OrganizationNode#getType()} (enum) <b>reste</b> la source de la
 * logique transverse (agrégats, règles) ; {@code OrganizationLevel} n'apporte
 * que le libellé / l'ordre / la parenté métier. Le {@code semanticType} fait le
 * pont entre les deux.
 */
@Entity
@Table(name = "organization_levels",
        uniqueConstraints = @UniqueConstraint(name = "uk_org_level_root_order",
                columnNames = {"root_tenant_id", "depth_order"}),
        indexes = {
                @Index(name = "idx_org_level_root", columnList = "root_tenant_id"),
                @Index(name = "idx_org_level_parent", columnList = "parent_level_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Racine du réseau (= la dénomination) à laquelle ce niveau appartient. */
    @Column(name = "root_tenant_id", nullable = false)
    private UUID rootTenantId;

    /** Libellé affiché (« Zone »). */
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Pluriel affiché (« Zones »), optionnel. */
    @Column(name = "plural_name", length = 120)
    private String pluralName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 1 = juste sous la racine. Ordonne la hiérarchie du haut vers le bas. */
    @Column(name = "depth_order", nullable = false)
    private Integer depthOrder;

    /**
     * Type sémantique interne : conserve la logique transverse (agrégats, règles)
     * en la rattachant à une sémantique connue. Les valeurs autorisées sont celles
     * de l'enum {@link OrganizationNodeType} plus {@code CUSTOM} (niveau purement
     * inventé par la dénomination). Stocké en {@code String} pour admettre
     * {@code CUSTOM} sans faire dériver l'enum des nœuds.
     */
    @Column(name = "semantic_type", nullable = false, length = 30)
    @Builder.Default
    private String semanticType = "CUSTOM";

    /** {@code null} = le niveau se place directement sous la racine. */
    @Column(name = "parent_level_id")
    private UUID parentLevelId;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "color", length = 7)
    private String color;

    /** Ce niveau peut-il avoir des enfants du même niveau (ex. sous-zones) ? */
    @Column(name = "is_branching", nullable = false)
    @Builder.Default
    private Boolean branching = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.semanticType == null) this.semanticType = "CUSTOM";
        if (this.branching == null) this.branching = true;
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationLevel that = (OrganizationLevel) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
