package com.discipolat.modules.churchComparison.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #47 - Comparaison d'églises (réseau)
 */
@Entity
@Table(name = "church_comparisons")
public class ChurchComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String nomEglise;

    private int effectif;
    private double tauxPresence;
    private double tauxConversion;
    private double tauxRetention;
    private double scoreSpirituelMoyen;
    private double generositeMoyenne;
    private int nbDepartements;
    private int nbFamilles;

    private String categorie;
    private String pays;
    private String denomination;

    private LocalDateTime analyseLe = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getNomEglise() { return nomEglise; }
    public void setNomEglise(String nom) { this.nomEglise = nom; }
    public int getEffectif() { return effectif; }
    public void setEffectif(int e) { this.effectif = e; }
    public double getTauxPresence() { return tauxPresence; }
    public void setTauxPresence(double t) { this.tauxPresence = t; }
    public double getTauxConversion() { return tauxConversion; }
    public void setTauxConversion(double t) { this.tauxConversion = t; }
    public double getTauxRetention() { return tauxRetention; }
    public void setTauxRetention(double t) { this.tauxRetention = t; }
    public double getScoreSpirituelMoyen() { return scoreSpirituelMoyen; }
    public void setScoreSpirituelMoyen(double s) { this.scoreSpirituelMoyen = s; }
    public double getGenerositeMoyenne() { return generositeMoyenne; }
    public void setGenerositeMoyenne(double g) { this.generositeMoyenne = g; }
    public int getNbDepartements() { return nbDepartements; }
    public void setNbDepartements(int n) { this.nbDepartements = n; }
    public int getNbFamilles() { return nbFamilles; }
    public void setNbFamilles(int n) { this.nbFamilles = n; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String c) { this.categorie = c; }
    public String getPays() { return pays; }
    public void setPays(String p) { this.pays = p; }
    public String getDenomination() { return denomination; }
    public void setDenomination(String d) { this.denomination = d; }
    public LocalDateTime getAnalyseLe() { return analyseLe; }
    public void setAnalyseLe(LocalDateTime l) { this.analyseLe = l; }
}
