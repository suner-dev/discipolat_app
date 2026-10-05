package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §C — affiliation <b>multi-nœuds</b>.
 *
 * <p>« {@code userId} porte le rôle {@code roleId} sur le nœud
 * {@code nodeId} » (0..n), DÉCOUPLÉ de l'appartenance
 * ({@link TenantMembership}). {@code nodeId = null} = portée tenant.
 * Une permission est accordée si le membre porte un rôle la contenant
 * sur le nœud <b>ou un ancêtre</b> (descendance via {@code path}) —
 * résolution dans {@link AuthorizationService}.
 *
 * <p>Fin d'assignation = {@link AssignmentStatus#ENDED} (jamais de
 * purge physique — traçabilité).
 */
@Entity
@Table(name = "member_role_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_mra_active",
                columnNames = {"user_id", "role_id", "node_id", "status"}),
        indexes = {
                @Index(name = "idx_mra_user", columnList = "user_id, status"),
                @Index(name = "idx_mra_node", columnList = "node_id, role_id"),
                @Index(name = "idx_mra_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRoleAssignment {

    public enum AssignmentStatus { ACTIVE, SUSPENDED, ENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    /** {@code null} = portée tenant ; sinon portée du nœud (et de sa descendance). */
    @Column(name = "node_id")
    private UUID nodeId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.assignedAt == null) this.assignedAt = now;
        if (this.status == null) this.status = AssignmentStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberRoleAssignment that = (MemberRoleAssignment) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
