package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §B — <b>intitulé affiché</b> d'un
 * rôle-capacité, redéfinissable par église/nœud.
 *
 * <p>Le {@link Role} porte la CAPACITÉ (permissions, portable) ; cette
 * table porte seulement le NOM (« Diacre » / « Pasteur assistant » /
 * « Ancien »). <b>Une permission ne dépend jamais du label</b> (V3-B).
 * {@code nodeId = null} = intitulé par défaut du tenant ; sinon
 * l'intitulé résolu pour CE nœud (fallback : nœud → tenant → global).
 */
@Entity
@Table(name = "role_titles",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_title_tenant_role_node",
                columnNames = {"tenant_id", "role_id", "node_id"}),
        indexes = {
                @Index(name = "idx_role_title_lookup", columnList = "role_id, node_id"),
                @Index(name = "idx_role_title_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /** {@code null} = intitulé par défaut du tenant ; sinon intitulé pour ce nœud. */
    @Column(name = "node_id")
    private UUID nodeId;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "label_plural", length = 120)
    private String labelPlural;

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
        RoleTitle that = (RoleTitle) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
