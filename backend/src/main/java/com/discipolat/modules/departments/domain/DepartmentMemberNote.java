package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Note départementale sur un membre du département (dossier de gestion).
 * Distincte des notes de disciple : elle appartient à l'espace responsable
 * et reste scoped au département (un responsable ne voit que les notes de
 * SES départements).
 */
@Entity
@Table(name = "department_member_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentMemberNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    /** Membre concerné (soul_id). */
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(name = "contenu", nullable = false)
    private String contenu;

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
