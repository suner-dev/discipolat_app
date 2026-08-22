package com.discipolat.modules.workflow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "automations")
public class Automation {

    public enum Statut { ACTIVE, PAUSEE, BROUILLON }
    public enum TriggerType { ABSENCE_SOUTENUE, NOUVEAU_MEMBRE, RAPPORT_SOUMIS, EVENEMENT_A_VENIR, CUSTOM }
    public enum ActionType { ENVOYER_MESSAGE, CREER_TACHE, NOTIFIER, ASSIGNER_Faiseur, CREER_EVENEMENT, EMAIL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerType triggerType = TriggerType.CUSTOM;

    @Column(columnDefinition = "TEXT")
    private String triggerConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType = ActionType.ENVOYER_MESSAGE;

    @Column(columnDefinition = "TEXT")
    private String actionConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.BROUILLON;

    private int nombreExecutions = 0;

    private LocalDateTime derniereExecution;

    @Column(nullable = false)
    private UUID creePar;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public String getTriggerConfig() { return triggerConfig; }
    public void setTriggerConfig(String triggerConfig) { this.triggerConfig = triggerConfig; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public String getActionConfig() { return actionConfig; }
    public void setActionConfig(String actionConfig) { this.actionConfig = actionConfig; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public int getNombreExecutions() { return nombreExecutions; }
    public void setNombreExecutions(int nombreExecutions) { this.nombreExecutions = nombreExecutions; }
    public LocalDateTime getDerniereExecution() { return derniereExecution; }
    public void setDerniereExecution(LocalDateTime derniereExecution) { this.derniereExecution = derniereExecution; }
    public UUID getCreePar() { return creePar; }
    public void setCreePar(UUID creePar) { this.creePar = creePar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
