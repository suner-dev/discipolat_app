package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tâche du département : attribuée à un membre, avec priorité,
 * statut, échéance et avancement. Alimente la charge de travail
 * (tasks ouvertes / en retard par membre).
 */
@Entity
@Table(name = "department_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentTask {

    public enum TaskStatus {
        A_FAIRE, EN_COURS, BLOQUEE, TERMINEE, VALIDEE, ANNULEE
    }

    public enum TaskPriority {
        BASSE, MOYENNE, HAUTE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    /** Membre assigné (soul_id). */
    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", nullable = false)
    @Builder.Default
    private TaskPriority priorite = TaskPriority.MOYENNE;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private TaskStatus statut = TaskStatus.A_FAIRE;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "echeance")
    private LocalDate echeance;

    /** Avancement 0..100. */
    @Column(name = "avancement", nullable = false)
    @Builder.Default
    private int avancement = 0;

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

    /** Tâche encore ouverte (non terminée/validée/annulée). */
    public boolean isOpen() {
        return statut == TaskStatus.A_FAIRE || statut == TaskStatus.EN_COURS || statut == TaskStatus.BLOQUEE;
    }

    /** Tâche en retard : ouverte et échéance passée. */
    public boolean isOverdue() {
        return isOpen() && echeance != null && echeance.isBefore(LocalDate.now());
    }
}
