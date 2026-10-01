package com.discipolat.modules.finances.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * §G6.4 — ligne de relevé bancaire importée, rapprochée automatiquement
 * (montant + date ± tolérance, ou référence commune) ou manuellement par le
 * responsable finance. Jamais de suppression : statut ONLY (audit).
 */
@Entity
@Table(name = "finance_bank_statement_line", indexes = {
    @Index(name = "idx_fbsl_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_fbsl_transaction", columnList = "matched_transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceBankStatementLine {

    public enum Status { UNMATCHED, MATCHED, IGNORED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "date_transaction", nullable = false)
    private LocalDate dateTransaction;

    @Column(name = "montant", nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Column(name = "description", length = 500)
    private String description;

    /** Référence externe (virement, chèque…) — accélère le rapprochement auto. */
    @Column(name = "reference", length = 120)
    private String reference;

    @Column(name = "matched_transaction_id")
    private UUID matchedTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.UNMATCHED;

    /** Empreinte d'import : évite les doublons si le même relevé est re-importé. */
    @Column(name = "external_key", length = 160)
    private String externalKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
