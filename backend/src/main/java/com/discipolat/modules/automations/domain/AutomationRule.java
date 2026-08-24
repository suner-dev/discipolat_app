package com.discipolat.modules.automations.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Règle d'automatisation configurable.
 * Exemple : "Quand un membre est absent 3 semaines → envoyer message au faiseur"
 * Pattern : QUAND (trigger) + ALORS (action)
 */
@Entity
@Table(name = "automation_rules")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class AutomationRule {

    public enum TriggerEvent {
        ABSENCE_SOUTENUE,       // Membre absent X semaines consécutives
        NOUVEAU_MEMBRE,         // Nouveau membre rejoint
        RAPPORT_SOUMIS,         // Rapport faiseur soumis
        RAPPORT_EN_RETARD,      // Rapport non soumis avant vendredi
        PRIÈRE_CRÉÉE,           // Nouvelle prière créée
        ÉVÉNEMENT_CRÉÉ,         // Nouvel événement créé
        DEMANDE_TRANSFERT,      // Demande de transfert
        ALERT_CRÉÉE,            // Nouvelle alerte
        SCORE_SPRITUEL_BAISSE,  // Score spirituel en baisse
        QUOTIDIEN,              // Déclenché chaque jour
        HEBDOMADAIRE,           // Déclenché chaque semaine
        PERSONNALISÉ,           // Déclenché manuellement
    }

    public enum ActionType {
        ENVOYER_MESSAGE,        // Notification in-app
        ENVOYER_EMAIL,          // Email
        ASSIGNER_FISEUR,        // Assigner un faiseur
        CRÉER_ALERTE,           // Créer une alerte
        CRÉER_SUIVI,            // Créer un suivi
        METTRE_A_JOUR_STATUT,   // Mettre à jour un statut
        GÉNÉRER_RAPPORT,        // Générer un rapport
        NOTIFIER_ROLE,          // Notifier un rôle spécifique
    }

    public enum Statut { ACTIVE, EN_PAUSE, DÉSACTIVÉE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerEvent triggerEvent;

    @Column(columnDefinition = "TEXT")
    private String triggerParams; // JSON: { "semaines": 3, "deptId": "..." }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String actionParams; // JSON: { "message": "...", "cibleRole": "FAISEUR" }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ACTIVE;

    private int totalExécutions = 0;
    private LocalDateTime dernièreExécution;
    private int maxExécutions; // 0 = illimité

    @Column(nullable = false)
    private UUID crééPar;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TriggerEvent getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(TriggerEvent triggerEvent) { this.triggerEvent = triggerEvent; }
    public String getTriggerParams() { return triggerParams; }
    public void setTriggerParams(String triggerParams) { this.triggerParams = triggerParams; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public String getActionParams() { return actionParams; }
    public void setActionParams(String actionParams) { this.actionParams = actionParams; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public int getTotalExécutions() { return totalExécutions; }
    public void setTotalExécutions(int totalExécutions) { this.totalExécutions = totalExécutions; }
    public LocalDateTime getDernièreExécution() { return dernièreExécution; }
    public void setDernièreExécution(LocalDateTime dernièreExécution) { this.dernièreExécution = dernièreExécution; }
    public int getMaxExécutions() { return maxExécutions; }
    public void setMaxExécutions(int maxExécutions) { this.maxExécutions = maxExécutions; }
    public UUID getCrééPar() { return crééPar; }
    public void setCrééPar(UUID crééPar) { this.crééPar = crééPar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
