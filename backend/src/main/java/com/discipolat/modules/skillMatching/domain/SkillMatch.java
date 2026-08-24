package com.discipolat.modules.skillMatching.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #35 - Matching membres ↔ compétences
 */
@Entity
@Table(name = "skill_matches")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SkillMatch {

    public enum Statut { PROPOSE, ACCEPTE, REFUSE, EN_COURS }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID membreId;

    @Column(nullable = false)
    private UUID departementId;

    @Column(nullable = false)
    private String competence;

    @Column(nullable = false)
    private int scoreMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.PROPOSE;

    @Column(columnDefinition = "TEXT")
    private String justification;

    private UUID creePar;
    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime reponduLe;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public UUID getDepartementId() { return departementId; }
    public void setDepartementId(UUID departementId) { this.departementId = departementId; }
    public String getCompetence() { return competence; }
    public void setCompetence(String competence) { this.competence = competence; }
    public int getScoreMatch() { return scoreMatch; }
    public void setScoreMatch(int scoreMatch) { this.scoreMatch = scoreMatch; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getJustification() { return justification; }
    public void setJustification(String j) { this.justification = j; }
    public UUID getCreePar() { return creePar; }
    public void setCreePar(UUID creePar) { this.creePar = creePar; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime l) { this.creeLe = l; }
    public LocalDateTime getReponduLe() { return reponduLe; }
    public void setReponduLe(LocalDateTime l) { this.reponduLe = l; }
}
