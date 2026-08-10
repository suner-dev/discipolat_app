package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.DecisionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Décision d'un validateur sur une demande de transfert.
 * Toutes les décisions sont motivées, datées et conservées (immutables).
 */
@Entity
@Table(name = "transfer_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transfer_request_id", nullable = false)
    private UUID transferRequestId;

    @Column(name = "validateur_id", nullable = false)
    private UUID validateurId;

    /** Rôle ACTIF sous lequel le validateur a pris sa décision. */
    @Column(name = "role_validateur")
    private String roleValidateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private DecisionType decision;

    @Column(name = "motivation", length = 2000)
    private String motivation;

    /** Étape du circuit de validation concernée. */
    @Builder.Default
    @Column(name = "etape_ordre", nullable = false)
    private Integer etapeOrdre = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
