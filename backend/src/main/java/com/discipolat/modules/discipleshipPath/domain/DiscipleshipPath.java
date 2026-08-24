package com.discipolat.modules.discipleshipPath.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discipleship_paths")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DiscipleshipPath {

    public enum Stage { DISCOVERY, FOUNDATION, GROWTH, SERVICE, LEADERSHIP, MATURITY }
    public enum Status { ACTIVE, COMPLETED, PAUSED, SKIP }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stage currentStage = Stage.DISCOVERY;

    @Column(columnDefinition = "TEXT")
    private String recommendedNextStep;

    private Double progressPercent = 0.0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String aiNotes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime startDate = LocalDateTime.now();
    private LocalDateTime lastActivityAt;
    private LocalDateTime completedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }
    public Stage getCurrentStage() { return currentStage; }
    public void setCurrentStage(Stage currentStage) { this.currentStage = currentStage; }
    public String getRecommendedNextStep() { return recommendedNextStep; }
    public void setRecommendedNextStep(String recommendedNextStep) { this.recommendedNextStep = recommendedNextStep; }
    public Double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Double progressPercent) { this.progressPercent = progressPercent; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getAiNotes() { return aiNotes; }
    public void setAiNotes(String aiNotes) { this.aiNotes = aiNotes; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
