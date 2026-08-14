package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Matériel / équipement du département (inventaire). Chaque équipement
 * peut être confié à un responsable et/ou affecté à un membre, avec un
 * état et une localisation. Module léger, activable par l'administration.
 */
@Entity
@Table(name = "department_equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentEquipment {

    public enum Etat {
        NEUF, BON, USAGE, REPARATION, HORS_SERVICE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "description")
    private String description;

    @Column(name = "quantite", nullable = false)
    @Builder.Default
    private int quantite = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    @Builder.Default
    private Etat etat = Etat.BON;

    /** Membre responsable de l'équipement (optionnel). */
    @Column(name = "responsable_id")
    private UUID responsableId;

    /** Membre à qui l'équipement est affecté (optionnel). */
    @Column(name = "affecte_a_id")
    private UUID affecteAId;

    @Column(name = "localisation")
    private String localisation;

    @Column(name = "date_acquisition")
    private LocalDate dateAcquisition;

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
