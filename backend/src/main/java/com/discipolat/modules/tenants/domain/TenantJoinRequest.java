package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS — demande d'accès émise quand un code est en mode
 * {@link JoinMode#APPROVAL} (rejointure différée, cf. V219).
 */
@Entity
@Table(name = "tenant_join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantJoinRequest {

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "org_node_id")
    private UUID orgNodeId;

    /** Compte connecté qui a demandé à rejoindre (null si pré-inscription par email). */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "message", length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "handled_at")
    private Instant handledAt;

    @Column(name = "handled_by")
    private UUID handledBy;

    /**
     * Invitation émise à l'approbation pour une demande SANS compte (V253).
     *
     * <p>{@code null} = aucune invitation : soit la demande concerne un compte
     * déjà connecté, soit elle a été approuvée avant V253, soit l'émission a
     * échoué. C'est ce lien qui rend la <b>relance</b> possible : on régénère le
     * jeton sur CETTE invitation, et pas sur une invitation homonyme.
     */
    @Column(name = "invitation_id")
    private UUID invitationId;

    /** Dernier envoi du lien d'activation — permet de distinguer « jamais parti » d'« ancien ». */
    @Column(name = "invitation_sent_at")
    private java.time.Instant invitationSentAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) this.status = Status.PENDING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantJoinRequest that = (TenantJoinRequest) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
