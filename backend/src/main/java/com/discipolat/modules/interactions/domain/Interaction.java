package com.discipolat.modules.interactions.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "soul_interactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "soul_id", nullable = false)
    private UUID soulId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InteractionType type;

    @Column(name = "canal")
    private String canal;

    @Column(name = "objet")
    private String objet;

    @Column(name = "contenu")
    private String contenu;

    @Column(name = "date_interaction", nullable = false)
    private LocalDateTime dateInteraction;

    @Column(name = "a_faire_par")
    private UUID aFairePar;

    @Column(name = "rappel_le")
    private LocalDateTime rappelLe;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.dateInteraction == null) this.dateInteraction = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
