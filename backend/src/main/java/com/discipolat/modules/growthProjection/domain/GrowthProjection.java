package com.discipolat.modules.growthProjection.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #44 - Projection de croissance familiale/église
 */
@Entity
@Table(name = "growth_projections")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class GrowthProjection {

    public enum TypeProjection { FAMILLE, DEPARTEMENT, EGLISE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeProjection typeProjection;

    private UUID cibleId;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private int effectifActuel;

    @Column(nullable = false)
    private int effectifProjete;

    private double tauxCroissanceAnnuel;
    private int moisProjection;

    @Column(columnDefinition = "TEXT")
    private String hypotheses;

    @Column(columnDefinition = "TEXT")
    private String recommandations;

    private LocalDateTime calculeLe = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public TypeProjection getTypeProjection() { return typeProjection; }
    public void setTypeProjection(TypeProjection type) { this.typeProjection = type; }
    public UUID getCibleId() { return cibleId; }
    public void setCibleId(UUID cibleId) { this.cibleId = cibleId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getEffectifActuel() { return effectifActuel; }
    public void setEffectifActuel(int e) { this.effectifActuel = e; }
    public int getEffectifProjete() { return effectifProjete; }
    public void setEffectifProjete(int e) { this.effectifProjete = e; }
    public double getTauxCroissanceAnnuel() { return tauxCroissanceAnnuel; }
    public void setTauxCroissanceAnnuel(double t) { this.tauxCroissanceAnnuel = t; }
    public int getMoisProjection() { return moisProjection; }
    public void setMoisProjection(int m) { this.moisProjection = m; }
    public String getHypotheses() { return hypotheses; }
    public void setHypotheses(String h) { this.hypotheses = h; }
    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String r) { this.recommandations = r; }
    public LocalDateTime getCalculeLe() { return calculeLe; }
    public void setCalculeLe(LocalDateTime l) { this.calculeLe = l; }
}
