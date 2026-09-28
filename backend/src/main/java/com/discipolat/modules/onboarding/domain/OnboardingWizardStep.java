package com.discipolat.modules.onboarding.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une étape du wizard d'onboarding d'un tenant.
 *
 * <p>Colonne {@code skip_reason} ajoutée par la migration V183 : elle n'existait
 * pas, le motif d'un saut était donc perdu alors que le contrat §3.1 impose
 * {@code skipRequiresReason} et un motif obligatoire pour certaines étapes.
 *
 * <p>Le champ {@code config} (JSON) n'est plus utilisé par la logique : il est
 * conservé en base (aucune suppression de colonne, cf. A10.3) et n'est plus
 * exposé par l'API — le contrat §3.1 expose {@code completedData}, pas
 * {@code config}.
 */
@Entity
@Table(name = "onboarding_wizard_steps")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class OnboardingWizardStep {

    public enum StepType { CHURCH_IDENTITY, MEMBER_IMPORT, STRUCTURE, ROLES, FIRST_EVENT, BRANDING, MODULES }
    public enum Status { PENDING, IN_PROGRESS, COMPLETED, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType stepType;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON legacy, plus utilisée par la logique

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String completedData; // JSON résultat (objet, pas chaîne pour le client)

    @Column(name = "skip_reason", columnDefinition = "TEXT")
    private String skipReason;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public StepType getStepType() { return stepType; }
    public void setStepType(StepType stepType) { this.stepType = stepType; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getCompletedData() { return completedData; }
    public void setCompletedData(String completedData) { this.completedData = completedData; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
