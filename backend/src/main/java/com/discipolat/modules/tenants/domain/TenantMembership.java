package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_membership_user_tenant", columnNames = {"user_id", "tenant_id"}),
        @UniqueConstraint(name = "uk_tenant_membership_user_tenant_scope", columnNames = {"user_id", "tenant_id", "scope_type", "scope_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TenantMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "role", nullable = false, length = 50)
    private String roleLegacy;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private MembershipScopeType scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    // ============================================================
    // Traçabilité du transfert — SPEC_ORGANISATION_DENOMINATION_V2 §5 / V223
    // ============================================================
    // D5 : le transfert d'un membre ne SUPPRIME pas son appartenance, il la
    // trace. L'historique pastoral (« qui était membre, depuis quand, dans
    // quelle église ») est une exigence d'audit ET une réalité de suivi.
    // Ces quatre colonnes répondent à « il est parti quand, pour aller où,
    // et pourquoi ».

    /** Instant du transfert. {@code null} = appartenance jamais transférée. */
    @Column(name = "transferred_at")
    private Instant transferredAt;

    /** Organisation d'accueil du transfert (membershipActive de la nouvelle). */
    @Column(name = "transferred_to_tenant_id")
    private UUID transferredToTenantId;

    /** Acteur ayant effectué le transfert (le membre, ou un admin plateforme). */
    @Column(name = "transferred_by_user_id")
    private UUID transferredByUserId;

    /** Motif du transfert, conservé pour l'audit. */
    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.joinedAt = Instant.now();
        if (this.status == null) this.status = MembershipStatus.ACTIVE;
        if (this.scopeType == null) this.scopeType = MembershipScopeType.TENANT;
        // §G6.4 — la colonne legacy « role » est NOT NULL : elle reflète toujours
        // la clé du rôle moderne quand l'appelant ne la fournit pas (sinon
        // l'acceptation d'invitation et le seed de memberships échouaient en silence).
        if (this.roleLegacy == null && this.role != null) this.roleLegacy = this.role.getKey();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Marque cette appartenance comme transférée (D5).
     *
     * <p>Le statut passe à {@link MembershipStatus#REVOKED} — une valeur qui
     * existe déjà dans l'enum : l'appartenance n'est plus active, mais elle
     * reste en base et demeure interrogeable. Supprimer la ligne ferait
     * perdre l'historique que ce modèle cherche justement à préserver.
     *
     * @param targetTenantId organisation d'accueil (obligatoire)
     * @param actorUserId    auteur du transfert — le membre ou un admin
     * @param reason         motif conservé pour l'audit
     */
    public void markTransferred(UUID targetTenantId, UUID actorUserId, String reason) {
        if (targetTenantId == null) {
            throw new IllegalArgumentException("Organisation d'accueil du transfert requise");
        }
        this.status = MembershipStatus.REVOKED;
        this.transferredAt = Instant.now();
        this.transferredToTenantId = targetTenantId;
        this.transferredByUserId = actorUserId;
        this.transferReason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    /** Cette appartenance a-t-elle déjà été transférée ? */
    public boolean isTransferred() {
        return transferredAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantMembership that = (TenantMembership) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Explicit getters/setters for Lombok compatibility
}