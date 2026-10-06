package com.discipolat.modules.finances.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Transaction financière de l'église (recette ou dépense).
 * Montants enregistrés dans la devise de l'église ; suppression = archivage
 * (soft delete) pour préserver l'historique comptable.
 *
 * <p>A3 (M9) — chaque transaction porte désormais sa devise ISO-4217 exacte
 * et son montant en unités mineures entières, plus la contre-valeur dans la
 * devise de base du tenant (audit multi-devises). Le champ historique
 * {@code montant} est conservé tel quel : aucune régression.</p>
 */
@Entity
@Table(name = "finance_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FinanceTransaction {

    public enum TransactionType {
        RECETTE, DEPENSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "categorie", nullable = false, length = 50)
    private String categorie;

    @Column(name = "montant", nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    /** Code ISO-4217 de la devise de saisie (XAF, EUR, USD, KES…). */
    @Column(name = "devise", nullable = false, length = 3)
    private String devise;

    /** Montant dans l'unité mineure de la devise (entier exact, pas de virgule). */
    @Column(name = "montant_minor")
    private Long montantMinor;

    /** Taux appliqué vers la devise de base du tenant à l'écriture (1 = devise de base). */
    @Column(name = "taux_vers_base", precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal tauxVersBase = BigDecimal.ONE;

    /** Contre-valeur dans la devise de base du tenant (audit). */
    @Column(name = "montant_base", precision = 14, scale = 2)
    private BigDecimal montantBase;

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

    @Column(name = "reconciled", nullable = false)
    @Builder.Default
    private boolean reconciled = false;

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
