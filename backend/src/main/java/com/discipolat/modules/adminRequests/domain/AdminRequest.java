package com.discipolat.modules.adminRequests.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #57 - Demandes administratives (baptême, dédicace, accueil nouveau)
 */
@Entity
@Table(name = "admin_requests")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class AdminRequest {

    public enum TypeDemande { BAPTEME, DEDICACE, ACCUEIL_NOUVEAU, TRANSFERT, MARIAGE, BENEDICTION }
    public enum Statut { SOUMISE, EN_EXAMEN, APPROUVEE, REJETEE, TRAITEE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID demandeurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDemande typeDemande;

    @Column(nullable = false)
    private String motif;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.SOUMISE;

    private UUID traitePar;
    private LocalDateTime traiteLe;

    @Column(columnDefinition = "TEXT")
    private String commentaireTraitement;

    private LocalDateTime soumiseLe = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getDemandeurId() { return demandeurId; }
    public void setDemandeurId(UUID id) { this.demandeurId = id; }
    public TypeDemande getTypeDemande() { return typeDemande; }
    public void setTypeDemande(TypeDemande t) { this.typeDemande = t; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getDetails() { return details; }
    public void setDetails(String d) { this.details = d; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut s) { this.statut = s; }
    public UUID getTraitePar() { return traitePar; }
    public void setTraitePar(UUID id) { this.traitePar = id; }
    public LocalDateTime getTraiteLe() { return traiteLe; }
    public void setTraiteLe(LocalDateTime l) { this.traiteLe = l; }
    public String getCommentaireTraitement() { return commentaireTraitement; }
    public void setCommentaireTraitement(String c) { this.commentaireTraitement = c; }
    public LocalDateTime getSoumiseLe() { return soumiseLe; }
    public void setSoumiseLe(LocalDateTime l) { this.soumiseLe = l; }
}
