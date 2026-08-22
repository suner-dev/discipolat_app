package com.discipolat.modules.skillsMatrix.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "skill_evaluations")
public class SkillEvaluation {

    public enum Niveau { DEBUTANT, INTERMEDIAIRE, AVANCE, EXPERT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID membreId;

    @Column(nullable = false)
    private String competence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Niveau niveau = Niveau.DEBUTANT;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(nullable = false)
    private UUID evaluePar;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public String getCompetence() { return competence; }
    public void setCompetence(String competence) { this.competence = competence; }
    public Niveau getNiveau() { return niveau; }
    public void setNiveau(Niveau niveau) { this.niveau = niveau; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public UUID getEvaluePar() { return evaluePar; }
    public void setEvaluePar(UUID evaluePar) { this.evaluePar = evaluePar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
