package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

import com.discipolat.modules.tenants.enums.SubscriptionStatus;

@Entity
@Table(name = "tenant_subscriptions", indexes = {
        @Index(name = "idx_subscription_tenant", columnList = "tenant_id"),
        @Index(name = "idx_subscription_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plan_key", nullable = false, length = 30)
    private String planKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "billing_cycle", length = 10)
    private String billingCycle;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    @Builder.Default
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "quotas_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String quotasJson;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadataJson;

    // ===== TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.4 (V248, additif) =====
    // Figeage du prix à la souscription (contrat §2.4) : une hausse de tarif
    // décidée par le super-admin ne doit pas modifier la facture d'un
    // abonnement en cours avant son renouvellement.

    /** Devise du prix figé. */
    @Column(name = "price_currency", length = 3)
    private String priceCurrency;

    /** Prix figé en unités mineures ISO-4217 (contrat §2.1). */
    @Column(name = "price_minor")
    private Long priceMinor;

    @Column(name = "price_cycle", length = 20)
    private String priceCycle;

    /** STRIPE | MOMO | STRONG | MANUAL | ADMIN | MIGRATION. */
    @Column(name = "price_source", nullable = false, length = 20)
    @Builder.Default
    private String priceSource = "MIGRATION";

    /** Instantané du plan au moment du figeage, pour expliquer une évolution de tarif. */
    @Column(name = "price_snapshot_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String priceSnapshotJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) this.status = SubscriptionStatus.TRIAL;
        if (this.billingCycle == null) this.billingCycle = "monthly";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantSubscription that = (TenantSubscription) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}