package com.discipolat.modules.familyCohesion.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_cohesion")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FamilyCohesion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID familleId;

    private double tauxParticipation = 0;

    // NB « diversite_âmes » : la colonne vitique conserve le « â » (V114) —
    // la seule exception du schéma à ne pas être translittérée en ASCII.
    @Column(name = "diversite_âmes")
    private int diversitéÂmes = 0;

    @Column(name = "equilibre_charges")
    private int équilibreCharges = 0;

    @Column(name = "score_cohesion")
    private double scoreCohésion = 0;

    @Column(columnDefinition = "TEXT")
    private String recommandations;

    @Column(name = "calcule_le", nullable = false)
    private LocalDateTime calculéLe = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getFamilleId() { return familleId; }
    public void setFamilleId(UUID familleId) { this.familleId = familleId; }
    public double getTauxParticipation() { return tauxParticipation; }
    public void setTauxParticipation(double tauxParticipation) { this.tauxParticipation = tauxParticipation; }
    public int getDiversitéÂmes() { return diversitéÂmes; }
    public void setDiversitéÂmes(int diversitéÂmes) { this.diversitéÂmes = diversitéÂmes; }
    public int getÉquilibreCharges() { return équilibreCharges; }
    public void setÉquilibreCharges(int équilibreCharges) { this.équilibreCharges = équilibreCharges; }
    public double getScoreCohésion() { return scoreCohésion; }
    public void setScoreCohésion(double scoreCohésion) { this.scoreCohésion = scoreCohésion; }
    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String recommandations) { this.recommandations = recommandations; }
    public LocalDateTime getCalculéLe() { return calculéLe; }
    public void setCalculéLe(LocalDateTime calculéLe) { this.calculéLe = calculéLe; }
}
