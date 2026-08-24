package com.discipolat.modules.onboarding.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_wizard_steps")
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
    private String config; // JSON config for the step

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String completedData; // JSON result data

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
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
