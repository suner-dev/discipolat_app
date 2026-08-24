package com.discipolat.modules.mentoring.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Suggestion d'accompagnement IA pour les chefs de famille.
 * L'IA analyse le profil de chaque faiseur et suggère des approches d'accompagnement
 * adaptées à son style d'apprentissage, ses forces et ses zones de croissance.
 */
@Entity
@Table(name = "mentor_suggestions")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MentorSuggestion {

    public enum Priorité { HAUTE, MOYENNE, BASSE }
    public enum Catégorie { ACCOMPAGNEMENT, FORMATION, DÉLÉGATION, RECONNAISSANCE, MISE_EN_GARDIEN }
    public enum Statut { ACTIVE, LUE, ARCHIVÉE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    /** Le chef de famille qui reçoit la suggestion */
    @Column(nullable = false)
    private UUID chefDeFamilleId;

    /** Le faiseur concerné par la suggestion */
    @Column(nullable = false)
    private UUID faiseurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorité priorité = Priorité.MOYENNE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Catégorie catégorie = Catégorie.ACCOMPAGNEMENT;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String analyse;

    @Column(columnDefinition = "TEXT")
    private String actionRecommandée;

    @Column(columnDefinition = "TEXT")
    private String raisonnement; // Explication du pourquoi

    /** Score de confiance de la suggestion (0.0 - 1.0) */
    private double confiance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getChefDeFamilleId() { return chefDeFamilleId; }
    public void setChefDeFamilleId(UUID chefDeFamilleId) { this.chefDeFamilleId = chefDeFamilleId; }
    public UUID getFaiseurId() { return faiseurId; }
    public void setFaiseurId(UUID faiseurId) { this.faiseurId = faiseurId; }
    public Priorité getPriorité() { return priorité; }
    public void setPriorité(Priorité priorité) { this.priorité = priorité; }
    public Catégorie getCatégorie() { return catégorie; }
    public void setCatégorie(Catégorie catégorie) { this.catégorie = catégorie; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getAnalyse() { return analyse; }
    public void setAnalyse(String analyse) { this.analyse = analyse; }
    public String getActionRecommandée() { return actionRecommandée; }
    public void setActionRecommandée(String actionRecommandée) { this.actionRecommandée = actionRecommandée; }
    public String getRaisonnement() { return raisonnement; }
    public void setRaisonnement(String raisonnement) { this.raisonnement = raisonnement; }
    public double getConfiance() { return confiance; }
    public void setConfiance(double confiance) { this.confiance = confiance; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
