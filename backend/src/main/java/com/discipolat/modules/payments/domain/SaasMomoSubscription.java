package com.discipolat.modules.payments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Abonnement SaaS via Mobile Money (MTN MoMo tokenization).
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.3.
 *
 * <p>Seul MTN MoMo supporte la tokenization pour les prélèvements récurrents.
 * M-Pesa (STK Push) et Orange Money ne supportent pas le replay automatique
 * et sont exclus du récurrent — ils restent disponibles pour les paiements
 * ponctuels (dîme, offrande).</p>
 */
@Entity
@Table(name = "saas_momo_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_saas_momo_subscription_tenant",
                columnNames = {"tenant_id"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasMomoSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plan_key", nullable = false, length = 30)
    private String planKey;

    @Column(name = "price_id", nullable = false)
    private UUID priceId;

    /**
     * Montant figé en unités mineures ISO-4217 (contrat §2.1).
     * Ne jamais recalculer — c'est le montant qui sera débité à chaque échéance.
     */
    @Column(name = "plan_price_minor", nullable = false)
    private Long planPriceMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "operator", nullable = false, length = 30)
    private String operator;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MomoSubscriptionStatus status;

    @Column(name = "next_charge_at")
    private Instant nextChargeAt;

    /** Token de paiement MTN MoMo (API user token) pour prélèvements récurrents. */
    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) this.status = MomoSubscriptionStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaasMomoSubscription that = (SaasMomoSubscription) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}