package com.discipolat.modules.finances.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "finance_donations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FinanceDonation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "donor_name", nullable = false, length = 200)
    private String donorName;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

        @Column(name = "devise", nullable = false, length = 3)
    /**
     * A3 (M9) — plus de valeur par défaut « XOF ».
     *
     * <p>La devise est désormais TOUJOURS renseignée explicitement par le service
     * ({@code TenantCurrencyResolver.resolve}), qui lit la devise primaire du
     * tenant. Un défaut muet transformait une absence de choix en donnée
     * comptable : un compte créé en EUR se retrouvait en XOF, sans trace.</p>
     *
     * <p>Le setter direct reste possible : c'est un setter JPA, pas une API
     * d'écriture métier. Toute création passe par le service.</p>
     */
    private String devise;

    @Column(name = "donation_date", nullable = false)
    private Instant donationDate;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "is_anonymous", nullable = false)
    @Builder.Default
    private boolean isAnonymous = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.donationDate == null) this.donationDate = Instant.now();
    }
}
