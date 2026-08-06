package com.discipolat.modules.members.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Demande envoyée par un membre : suggestion, demande de rendez-vous
 * ou signalement de problème. La demande est adressée à une cible
 * (PASTEUR, RESPONSABLE ou CHEF_DE_FAMILLE) et peut être portée
 * par un département ou une famille (visibilité scoping).
 */
@Entity
@Table(name = "member_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequest {

    public enum Type { SUGGESTION, RENDEZ_VOUS, SIGNALEMENT }
    public enum Cible { PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE }
    public enum Statut { OUVERT, EN_COURS, RESOLU, REJETE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "cible", nullable = false)
    private Cible cible;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private Statut statut = Statut.OUVERT;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "family_id")
    private UUID familyId;

    @Column(name = "reponse", length = 2000)
    private String reponse;

    @Column(name = "traite_par")
    private UUID traitePar;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

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
