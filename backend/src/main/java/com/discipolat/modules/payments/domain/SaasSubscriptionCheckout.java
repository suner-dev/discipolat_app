package com.discipolat.modules.payments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Passage en caisse d'abonnement SaaS payé par redirection externe
 * (agrégateur CinetPay : Orange Money, MTN MoMo, Wave, Visa/Mastercard).
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — continuité T6.
 *
 * <p>Rôle : porter le rapprochement {@code provider_reference → (tenant, plan,
 * cycle, prix)} qu'une {@link PaymentIntent} seule ne sait pas exprimer, afin
 * qu'au webhook de confirmation on active le bon abonnement au bon tarif figé.</p>
 *
 * <p>Table de corrélation globale comme le catalogue : elle n'est pas filtrée
 * par tenant à la lecture du webhook (contexte non authentifié), d'où l'absence
 * de {@code @Filter} ici — la sécurité vient de la {@code provider_reference}
 * imposée par Discipolat et re-validée auprès de l'API prestataire.</p>
 */
@Entity
@Table(name = "saas_subscription_checkouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasSubscriptionCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "plan_key", nullable = false, length = 30)
    private String planKey;

    @Column(name = "billing_cycle", nullable = false, length = 20)
    private String billingCycle;

    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Montant figé en unités mineures ISO-4217 (contrat §2.1) — ce qui est débité. */
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    /** Canal retenu : CARD (Visa/MC) ou MOMO. */
    @Column(name = "payment_channel", nullable = false, length = 20)
    private String paymentChannel;

    /** Opérateur routé au prestataire : ORANGE_MONEY / MTN_MOMO / WAVE / CARD. */
    @Column(name = "operator", nullable = false, length = 30)
    private String operator;

    @Column(name = "payment_intent_id")
    private UUID paymentIntentId;

    /** transaction_id CPS = provider_reference de l'intention — clé idempotente du webhook. */
    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionCheckoutStatus status = SubscriptionCheckoutStatus.PENDING;

    @Column(name = "checkout_url", columnDefinition = "TEXT")
    private String checkoutUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) this.status = SubscriptionCheckoutStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaasSubscriptionCheckout that = (SaasSubscriptionCheckout) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
