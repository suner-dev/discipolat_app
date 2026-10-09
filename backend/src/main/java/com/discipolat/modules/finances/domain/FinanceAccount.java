package com.discipolat.modules.finances.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "finance_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FinanceAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "balance", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

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

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
