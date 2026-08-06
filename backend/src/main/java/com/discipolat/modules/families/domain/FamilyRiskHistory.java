package com.discipolat.modules.families.domain;

import com.discipolat.common.enums.NiveauRisque;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historique des changements de niveau de risque d'une famille.
 * Conservé de manière immuable : aucune donnée d'audit n'est supprimée.
 */
@Entity
@Table(name = "family_risk_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyRiskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_niveau")
    private NiveauRisque ancienNiveau;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_niveau", nullable = false)
    private NiveauRisque nouveauNiveau;

    @Column(name = "score_risque")
    private Integer scoreRisque;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "raison")
    private String raison;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
