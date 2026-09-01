package com.discipolat.modules.payments.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Intention de paiement Mobile Money (dîmes & offrandes 2.0).
 *
 * Cycle : PENDING → (webhook opérateur) → CONFIRMED | FAILED | CANCELLED
 */
@Entity
@Table(name = "payment_intents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "soul_id")
    private UUID soulId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false)
    private Operator operator;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "XOF";

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    @Builder.Default
    private Purpose purpose = Purpose.OFFRANDE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "provider_reference")
    private String providerReference;

    /** Transaction financière créée à la confirmation (lien module Finances). */
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "failure_reason")
    private String failureReason;

    /** Nom du provider opérateur réellement utilisé (MTN MoMo / Orange / M-Pesa). */
    @Column(name = "provider_name", length = 40)
    private String providerName;

    /** URL de redirection/checkout opérateur (providers à redirection externe). */
    @Column(name = "checkout_url")
    private String checkoutUrl;

    /** Instructions affichées au client (ex. code OTP reçu par SMS). */
    @Column(columnDefinition = "TEXT")
    private String instructions;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Opérateurs Mobile Money supportés. */
    public enum Operator {
        M_PESA("M-Pesa"),
        MTN_MOMO("MTN Mobile Money"),
        ORANGE_MONEY("Orange Money"),
        AIRTEL_MONEY("Airtel Money"),
        WAVE("Wave"),
        CARD("Carte bancaire"),
        CASH("Espèces");

        private final String label;

        Operator(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum Purpose { DIME, OFFRANDE, PROMESSE, PROJET_SPECIAL, DON_DIASPORA }

    public enum Status { PENDING, CONFIRMED, FAILED, CANCELLED }
}
