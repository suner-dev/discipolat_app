package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Annonce du département (communication interne) : destinée à tous les
 * membres, à une équipe (teamId) ou à un poste (positionId).
 */
@Entity
@Table(name = "department_announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentAnnouncement {

    public enum Cible {
        TOUS, EQUIPE, POSTE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "cible", nullable = false)
    @Builder.Default
    private Cible cible = Cible.TOUS;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "position_id")
    private UUID positionId;

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
}
