package com.discipolat.modules.programs.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Sous-programme d'un type de programme.
 * Ex : Premier culte, Deuxième culte, Troisième culte, Culte des jeunes (pour le type Dimanche).
 */
@Entity
@Table(name = "program_sub_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramSubType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "program_type_id", nullable = false)
    private UUID programTypeId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "ordre", nullable = false)
    private Integer ordre = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
