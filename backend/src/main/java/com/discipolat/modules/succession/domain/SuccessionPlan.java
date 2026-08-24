package com.discipolat.modules.succession.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "succession_plans")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SuccessionPlan {

    public enum Statut { EN_PRÉPARATION, PRÊT, EN_TRANSITION, COMPLÉTÉ }
    public enum Readiness { DÉBUTANT, INTERMÉDIAIRE, AVANCÉ, PRÊT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID candidatId;

    @Column(nullable = false)
    private String rôleCible;

    private UUID mentorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Readiness readiness = Readiness.DÉBUTANT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_PRÉPARATION;

    @Column(columnDefinition = "TEXT")
    private String planFormation;

    @Column(columnDefinition = "TEXT")
    private String commentaires;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime targetDate;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCandidatId() { return candidatId; }
    public void setCandidatId(UUID candidatId) { this.candidatId = candidatId; }
    public String getRôleCible() { return rôleCible; }
    public void setRôleCible(String rôleCible) { this.rôleCible = rôleCible; }
    public UUID getMentorId() { return mentorId; }
    public void setMentorId(UUID mentorId) { this.mentorId = mentorId; }
    public Readiness getReadiness() { return readiness; }
    public void setReadiness(Readiness readiness) { this.readiness = readiness; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getPlanFormation() { return planFormation; }
    public void setPlanFormation(String planFormation) { this.planFormation = planFormation; }
    public String getCommentaires() { return commentaires; }
    public void setCommentaires(String commentaires) { this.commentaires = commentaires; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDateTime targetDate) { this.targetDate = targetDate; }
}
