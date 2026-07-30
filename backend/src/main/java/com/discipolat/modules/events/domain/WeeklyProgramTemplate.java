package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Template for recurring weekly events.
 * The Pasteur defines templates like "Culte du dimanche" (DIMANCHE, 09:00)
 * and can generate a week's events from these templates in one click.
 */
@Entity
@Table(name = "weekly_program_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyProgramTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "type_evenement", nullable = false)
    private String typeEvenement;

    @Column(name = "jour_semaine", nullable = false)
    private String jourSemaine;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "couleur")
    private String couleur;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
