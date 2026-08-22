package com.discipolat.modules.quest.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entrée du registre XP — chaque action valorisée (présence, prière, visite,
 * rapport, évangélisation…) crédite des points au disciple.
 */
@Entity
@Table(name = "xp_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class XpLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private QuestAction action;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Actions valorisées par le système de quêtes. */
    public enum QuestAction {
        PRESENCE_CULTE(15),
        PRIERE(10),
        VISITE(20),
        RAPPORT_HEBDO(25),
        EVANGELISATION_PROSPECT(30),
        NOUVEAU_CONVERTI(50),
        FORMATION_TERMINEE(35),
        EVENEMENT_ORGANISE(40);

        private final int defaultPoints;

        QuestAction(int defaultPoints) {
            this.defaultPoints = defaultPoints;
        }

        public int getDefaultPoints() {
            return defaultPoints;
        }
    }
}
