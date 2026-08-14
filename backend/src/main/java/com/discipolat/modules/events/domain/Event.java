package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisateur_id", nullable = false)
    private UUID organisateurId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "type_evenement", nullable = false)
    private String typeEvenement;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "limite_places")
    private Integer limitePlaces;

    @Builder.Default
    @Column(name = "nb_inscrits", nullable = false)
    private Integer nbInscrits = 0;

    @Column(name = "statut", nullable = false)
    private String statut = "PLANIFIE";

    @Column(name = "compte_rendu")
    private String compteRendu;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id != null && id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
