package com.discipolat.modules.volunteers.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #62 - Gestion avancée des bénévoles
 */
@Entity
@Table(name = "volunteers")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Volunteer {

    public enum Statut { ACTIF, INACTIF, EN_ATTENTE, BLOQUE }
    public enum Disponibilite { PLEIN_TEMPS, WEEK_END, SOIR, MATIN, OCCASIONNEL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID membreId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ACTIF;

    @Enumerated(EnumType.STRING)
    private Disponibilite disponibilite = Disponibilite.OCCASIONNEL;

    @Column(columnDefinition = "TEXT")
    private String competencesJson = "[]";

    @Column(columnDefinition = "TEXT")
    private String domainesInteretJson = "[]";

    private int heuresMois = 0;
    private int nbEvenements = 0;
    private LocalDate depuis;
    private LocalDateTime inscritLe = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public Disponibilite getDisponibilite() { return disponibilite; }
    public void setDisponibilite(Disponibilite d) { this.disponibilite = d; }
    public String getCompetencesJson() { return competencesJson; }
    public void setCompetencesJson(String c) { this.competencesJson = c; }
    public String getDomainesInteretJson() { return domainesInteretJson; }
    public void setDomainesInteretJson(String d) { this.domainesInteretJson = d; }
    public int getHeuresMois() { return heuresMois; }
    public void setHeuresMois(int h) { this.heuresMois = h; }
    public int getNbEvenements() { return nbEvenements; }
    public void setNbEvenements(int n) { this.nbEvenements = n; }
    public LocalDate getDepuis() { return depuis; }
    public void setDepuis(LocalDate d) { this.depuis = d; }
    public LocalDateTime getInscritLe() { return inscritLe; }
    public void setInscritLe(LocalDateTime l) { this.inscritLe = l; }
}
