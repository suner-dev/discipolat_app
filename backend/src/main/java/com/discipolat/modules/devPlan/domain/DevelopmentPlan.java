package com.discipolat.modules.devPlan.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #37 - Plan de développement individuel
 */
@Entity
@Table(name = "development_plans")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DevelopmentPlan {

    public enum Statut { BROUILLON, ACTIF, EN_COURS, TERMINE, ABANDONNE }
    public enum Priorite { BASSE, MOYENNE, HAUTE, CRITIQUE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID membreId;

    private UUID departementId;
    private UUID creeParId;

    @Column(nullable = false)
    private String objectif;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.BROUILLON;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorite priorite = Priorite.MOYENNE;

    private LocalDate dateDebut;
    private LocalDate dateEcheance;

    @Column(nullable = false)
    private int progression = 0;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime modifieLe;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public UUID getDepartementId() { return departementId; }
    public void setDepartementId(UUID d) { this.departementId = d; }
    public UUID getCreeParId() { return creeParId; }
    public void setCreeParId(UUID id) { this.creeParId = id; }
    public String getObjectif() { return objectif; }
    public void setObjectif(String o) { this.objectif = o; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut s) { this.statut = s; }
    public Priorite getPriorite() { return priorite; }
    public void setPriorite(Priorite p) { this.priorite = p; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate d) { this.dateDebut = d; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate d) { this.dateEcheance = d; }
    public int getProgression() { return progression; }
    public void setProgression(int p) { this.progression = p; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String c) { this.commentaire = c; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime l) { this.creeLe = l; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime l) { this.modifieLe = l; }
}
