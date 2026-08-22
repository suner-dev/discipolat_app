package com.discipolat.modules.compliance.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gdpr_requests")
public class GdprRequest {

    public enum TypeDemande { EXPORT, SUPPRESSION, RECTIFICATION, OPPOSITION, PORTABILITE }
    public enum Statut { EN_ATTENTE, EN_COURS, TRAITE, REJETE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDemande typeDemande;

    @Column(nullable = false)
    private UUID demandeurId;

    private UUID concerneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(columnDefinition = "TEXT")
    private String resultat;

    private LocalDateTime traiteLe;

    private UUID traitePar;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime deadlineAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public TypeDemande getTypeDemande() { return typeDemande; }
    public void setTypeDemande(TypeDemande typeDemande) { this.typeDemande = typeDemande; }
    public UUID getDemandeurId() { return demandeurId; }
    public void setDemandeurId(UUID demandeurId) { this.demandeurId = demandeurId; }
    public UUID getConcerneId() { return concerneId; }
    public void setConcerneId(UUID concerneId) { this.concerneId = concerneId; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getResultat() { return resultat; }
    public void setResultat(String resultat) { this.resultat = resultat; }
    public LocalDateTime getTraiteLe() { return traiteLe; }
    public void setTraiteLe(LocalDateTime traiteLe) { this.traiteLe = traiteLe; }
    public UUID getTraitePar() { return traitePar; }
    public void setTraitePar(UUID traitePar) { this.traitePar = traitePar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
}
