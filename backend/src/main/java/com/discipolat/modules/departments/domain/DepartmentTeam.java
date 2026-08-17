package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Équipe / sous-département d'un département.
 * <p>
 * Hiérarchie récursive via {@code parentId} (profondeur illimitée) :
 * Département → Sous-département → Sous-équipe → …
 * <p>
 * {@code type} distingue les sous-départements, équipes permanentes et
 * équipes temporaires (avec période {@code dateDebut}/{@code dateFin}).
 */
@Entity
@Table(name = "department_teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DepartmentTeam {

    public enum TeamType {
        SOUS_DEPARTEMENT, EQUIPE_PERMANENTE, EQUIPE_TEMPORAIRE
    }

    public enum TeamStatus {
        ACTIVE, ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    /** Équipe parente (null = racine). Permet les sous-départements récursifs. */
    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private TeamType type = TeamType.EQUIPE_PERMANENTE;

    /** Événement du département auquel cette équipe temporaire est rattachée (null sinon). */
    @Column(name = "event_id")
    private UUID eventId;

    /** Responsable de l'équipe (compte utilisateur). */
    @Column(name = "chef_id")
    private UUID chefId;

    /** Adjoint (compte utilisateur). */
    @Column(name = "adjoint_id")
    private UUID adjointId;

    @Column(name = "objectif")
    private String objectif;

    @Column(name = "description")
    private String description;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private TeamStatus statut = TeamStatus.ACTIVE;

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
