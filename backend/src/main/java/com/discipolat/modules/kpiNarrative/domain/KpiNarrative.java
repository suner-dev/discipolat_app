package com.discipolat.modules.kpiNarrative.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Narration automatique pour un KPI.
 * Quand un utilisateur clique sur un KPI, le système génère une narration
 * décrivant la tendance, les causes probables et les recommandations.
 *
 * Exemple : "Le taux de présence a baissé de 5% ce mois, principalement
 * dans le département Jeunesse en raison de 3 absences prolongées."
 */
@Entity
@Table(name = "kpi_narratives")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class KpiNarrative {

    public enum TypeKPI {
        PRÉSENCE,           // Taux de présence
        CROISSANCE,         // Croissance des membres
        RÉTENTION,          // Taux de rétention
        ENGAGEMENT,         // Score d'engagement
        FINANCES,           // Revenus/dépenses
        RAPPORTS,           // Soumission des rapports
        PRIÈRES,            // Prières actives
        ÉVÉNEMENTS,         // Participation aux événements
        SCORE_SPIRITUEL,    // Score spirituel moyen
        ALERTES,            // Alertes en attente
    }

    public enum Tendance { HAUSSE, BAISSE, STABLE, SIGNIFICATIVE_HAUSSE, SIGNIFICATIVE_BAISSE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeKPI typeKPI;

    /** Période concernée (ex: "2026-08") */
    @Column(nullable = false)
    private String période;

    /** Valeur actuelle du KPI */
    private double valeurActuelle;

    /** Valeur précédente (pour calcul tendance) */
    private double valeurPrécédente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tendance tendance = Tendance.STABLE;

    /** Variation en pourcentage */
    private double variationPct;

    /** Narration générée par le moteur IA */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String narration;

    /** Causes identifiées (JSON array) */
    @Column(columnDefinition = "TEXT")
    private String causes;

    /** Recommandations d'actions */
    @Column(columnDefinition = "TEXT")
    private String recommandations;

    /** Département concerné (nullable =全局) */
    private UUID départementId;

    @Column(nullable = false)
    private LocalDateTime généréLe = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public TypeKPI getTypeKPI() { return typeKPI; }
    public void setTypeKPI(TypeKPI typeKPI) { this.typeKPI = typeKPI; }
    public String getPériode() { return période; }
    public void setPériode(String période) { this.période = période; }
    public double getValeurActuelle() { return valeurActuelle; }
    public void setValeurActuelle(double valeurActuelle) { this.valeurActuelle = valeurActuelle; }
    public double getValeurPrécédente() { return valeurPrécédente; }
    public void setValeurPrécédente(double valeurPrécédente) { this.valeurPrécédente = valeurPrécédente; }
    public Tendance getTendance() { return tendance; }
    public void setTendance(Tendance tendance) { this.tendance = tendance; }
    public double getVariationPct() { return variationPct; }
    public void setVariationPct(double variationPct) { this.variationPct = variationPct; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
    public String getCauses() { return causes; }
    public void setCauses(String causes) { this.causes = causes; }
    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String recommandations) { this.recommandations = recommandations; }
    public UUID getDépartementId() { return départementId; }
    public void setDépartementId(UUID départementId) { this.départementId = départementId; }
    public LocalDateTime getGénéréLe() { return généréLe; }
    public void setGénéréLe(LocalDateTime généréLe) { this.généréLe = généréLe; }
}
