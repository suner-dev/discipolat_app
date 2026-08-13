package com.discipolat.modules.souls.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Table de liaison ManyToMany âme ↔ département.
 * Un membre peut appartenir à plusieurs départements.
 */
@Entity
@Table(name = "soul_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(SoulDepartmentId.class)
public class SoulDepartment {

    @Id
    @Column(name = "soul_id")
    private java.util.UUID soulId;

    @Id
    @Column(name = "department_id")
    private java.util.UUID departmentId;

    @Column(name = "date_affectation", nullable = false, updatable = false)
    private LocalDateTime dateAffectation;

    @Column(name = "date_desaffectation")
    private LocalDateTime dateDesaffectation;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /** Compte utilisateur à l'origine du rattachement (traçabilité). */
    @Column(name = "created_by")
    private java.util.UUID createdBy;

    /** Origine du rattachement : MANUEL | SIGNUP | TRANSFERT. */
    @Column(name = "origine")
    private String origine;

    @PrePersist
    protected void onCreate() {
        if (this.dateAffectation == null) {
            this.dateAffectation = LocalDateTime.now();
        }
    }
}
