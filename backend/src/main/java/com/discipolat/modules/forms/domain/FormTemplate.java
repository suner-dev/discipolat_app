package com.discipolat.modules.forms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #13 - Générateur de formulaires intelligents (drag & drop)
 */
@Entity
@Table(name = "form_templates")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FormTemplate {

    public enum Statut { BROUILLON, PUBLIE, ARCHIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.BROUILLON;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String fieldsJson = "[]";

    private String categorie;
    private boolean anonyme = false;
    private UUID creePar;

    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime publieLe;
    private LocalDateTime expireLe;

    @Column(nullable = false)
    private int nbReponses = 0;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String f) { this.fieldsJson = f; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String c) { this.categorie = c; }
    public boolean isAnonyme() { return anonyme; }
    public void setAnonyme(boolean a) { this.anonyme = a; }
    public UUID getCreePar() { return creePar; }
    public void setCreePar(UUID cp) { this.creePar = cp; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime l) { this.creeLe = l; }
    public LocalDateTime getPublieLe() { return publieLe; }
    public void setPublieLe(LocalDateTime l) { this.publieLe = l; }
    public LocalDateTime getExpireLe() { return expireLe; }
    public void setExpireLe(LocalDateTime l) { this.expireLe = l; }
    public int getNbReponses() { return nbReponses; }
    public void setNbReponses(int n) { this.nbReponses = n; }
}
