package com.discipolat.modules.payments.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P12 — Don récurrent (dîme mensuelle, offrande hebdomadaire…).
 *
 * Le scheduler {@link RecurringDonationScheduler} crée automatiquement
 * des PaymentIntent à chaque échéance.
 */
@Entity
@Table(name = "recurring_donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class RecurringDonation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false)
    private PaymentIntent.Operator operator;

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
    private PaymentIntent.Purpose purpose = PaymentIntent.Purpose.DIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    @Builder.Default
    private Frequency frequency = Frequency.MONTHLY;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "next_donation_date")
    private LocalDate nextDonationDate;

    @Column(name = "total_donated", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDonated = BigDecimal.ZERO;

    @Column(name = "donation_count", nullable = false)
    @Builder.Default
    private int donationCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Frequency {
        WEEKLY("Hebdomadaire"),
        BIWEEKLY("Bimensuel"),
        MONTHLY("Mensuel"),
        QUARTERLY("Trimestriel"),
        YEARLY("Annuel");

        private final String label;

        Frequency(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
