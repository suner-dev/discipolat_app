package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-1) — code court de rejointure.
 *
 * <p>Un code actif par valeur (index partiel {@code WHERE is_active}, cf. V219) ;
 * format {@code BETHEL-7K2X} (D2 : alphabet non ambigu, stocké en MAJUSCULES).
 * Rattaché au tenant, éventuellement à un {@code OrganizationNode} du même
 * tenant pour les sous-églises (D3). Volontairement SANS {@code @Filter}
 * tenantFilter : la résolution publique et la gestion multi-codes traversent
 * les tenants (les requêtes explicites filtrent par tenant_id).</p>
 */
@Entity
@Table(name = "tenant_join_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantJoinCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Sous-église (OrganizationNode) concernée ; null = code principal de l'église. */
    @Column(name = "org_node_id")
    private UUID orgNodeId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    /** Libellé affiché (« Église principale », « Campus Nord »…). */
    @Column(name = "label", length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_mode", nullable = false, length = 20)
    @Builder.Default
    private JoinMode joinMode = JoinMode.OPEN;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.joinMode == null) this.joinMode = JoinMode.OPEN;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantJoinCode that = (TenantJoinCode) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
