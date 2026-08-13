package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Journal d'activité du département : historique des mouvements
 * (équipe créée, membre affecté, tâche terminée…). Répond à la
 * traçabilité : qui a fait quoi, quand.
 */
@Entity
@Table(name = "department_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_nom")
    private String actorNom;

    /** Action normalisée : TEAM_CREATED, MEMBER_ASSIGNED, TASK_CREATED… */
    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "details")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
