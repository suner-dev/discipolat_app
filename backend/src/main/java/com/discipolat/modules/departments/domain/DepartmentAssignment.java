package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Affectation d'un membre (âme) à une équipe et/ou un poste du
 * département, avec période et rôle. Toute modification est
 * tracée (created_by + journal d'activité du département).
 */
@Entity
@Table(name = "department_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentAssignment {

    public enum AssignmentRole {
        CHEF, ADJOINT, MEMBRE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "position_id")
    private UUID positionId;

    /** Membre affecté (soul_id). */
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private AssignmentRole role = AssignmentRole.MEMBRE;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "created_by")
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
