package com.discipolat.modules.souls.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "soul_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoulHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ame_id", nullable = false)
    private UUID ameId;

    @Column(name = "type_evenement", nullable = false)
    private String typeEvenement;

    @Column(name = "description")
    private String description;

    @Column(name = "ancien_statut")
    private String ancienStatut;

    @Column(name = "nouveau_statut")
    private String nouveauStatut;

    @Column(name = "ancien_faiseur_id")
    private UUID ancienFaiseurId;

    @Column(name = "nouveau_faiseur_id")
    private UUID nouveauFaiseurId;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
