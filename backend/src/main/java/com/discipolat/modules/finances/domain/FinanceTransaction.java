package com.discipolat.modules.finances.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction financière de l'église (recette ou dépense).
 * Montants enregistrés dans la devise de l'église ; suppression = archivage
 * (soft delete) pour préserver l'historique comptable.
 */
@Entity
@Table(name = "finance_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceTransaction {

    public enum TransactionType {
        RECETTE, DEPENSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "categorie", nullable = false, length = 50)
    private String categorie;

    @Column(name = "montant", nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "date_transaction", nullable = false)
    private LocalDate dateTransaction;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
