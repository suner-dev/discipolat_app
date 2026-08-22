package com.discipolat.modules.tontine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Versement d'un membre pour un tour donné. */
@Entity
@Table(name = "tontine_contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TontineContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "tour", nullable = false)
    @Builder.Default
    private int tour = 1;

    @Column(name = "montant", nullable = false)
    @Builder.Default
    private BigDecimal montant = BigDecimal.ZERO;

    @Column(name = "paye", nullable = false)
    @Builder.Default
    private boolean paye = false;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
