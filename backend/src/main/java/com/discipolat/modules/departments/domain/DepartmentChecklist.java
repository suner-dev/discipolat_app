package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Checklist du département, rattachée optionnellement à une cible
 * (tâche, événement, équipe, membre) ou générale. Exemple : checklist
 * de préparation d'un événement (sono testée, caméras prêtes…).
 */
@Entity
@Table(name = "department_checklists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentChecklist {

    public enum CibleType {
        GENERAL, TACHE, EVENEMENT, EQUIPE, MEMBRE
    }

    public enum ChecklistStatus {
        OUVERTE, TERMINEE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(name = "cible_type", nullable = false)
    @Builder.Default
    private CibleType cibleType = CibleType.GENERAL;

    /** Identifiant de la cible (tâche, événement, équipe ou membre). */
    @Column(name = "cible_id")
    private UUID cibleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private ChecklistStatus statut = ChecklistStatus.OUVERTE;

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
