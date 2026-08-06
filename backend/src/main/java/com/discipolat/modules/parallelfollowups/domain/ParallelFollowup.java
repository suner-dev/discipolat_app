package com.discipolat.modules.parallelfollowups.domain;

import com.discipolat.common.enums.RaisonSuiviParallele;
import com.discipolat.common.enums.StatutSuiviParallele;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parallel_followups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParallelFollowup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ame_id", nullable = false)
    private UUID ameId;

    @Column(name = "initiateur_id", nullable = false)
    private UUID initiateurId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "raison", nullable = false)
    private RaisonSuiviParallele raison;

    @Column(name = "raison_detail")
    private String raisonDetail;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutSuiviParallele statut = StatutSuiviParallele.EN_COURS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

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
