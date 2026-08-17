package com.discipolat.modules.finances.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget annuel par catégorie de dépense. La consommation est calculée
 * sur les transactions réelles de l'année (même catégorie).
 */
@Entity
@Table(name = "finance_budgets", uniqueConstraints = @UniqueConstraint(columnNames = {"categorie", "annee"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "categorie", nullable = false, length = 50)
    private String categorie;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    @Column(name = "montant", nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

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
