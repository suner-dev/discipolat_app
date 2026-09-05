package com.discipolat.modules.aiPredictions.domain;

import java.util.UUID;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_predictions")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class AiPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prediction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PredictionType predictionType;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "predicted_value")
    private Double predictedValue;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "generated_by")
    private String generatedBy = "AI_ENGINE_V1";

    public enum PredictionType {
        GROWTH_FORECAST,
        CHURN_RISK,
        ATTENDANCE_TREND,
        GIVING_TREND,
        ENGAGEMENT_SCORE,
        DEPARTMENT_PERFORMANCE
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PredictionType getPredictionType() { return predictionType; }
    public void setPredictionType(PredictionType predictionType) { this.predictionType = predictionType; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public Double getPredictedValue() { return predictedValue; }
    public void setPredictedValue(Double predictedValue) { this.predictedValue = predictedValue; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
