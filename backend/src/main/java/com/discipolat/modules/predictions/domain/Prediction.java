package com.discipolat.modules.predictions.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "predictions")
public class Prediction {

    public enum Type { WORKFORCE_GROWTH, ATTENDANCE, BAPTISMS, DROPOUT, ENGAGEMENT, FINANCES }
    public enum Trend { UP, DOWN, STABLE, VOLATILE }
    public enum Confidence { LOW, MEDIUM, HIGH }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type predictionType;

    private Integer periodMonths = 6; // prediction horizon

    private Double currentValue;
    private Double predictedValue;
    private Double growthRate; // percentage

    @Enumerated(EnumType.STRING)
    private Trend trend = Trend.STABLE;

    @Enumerated(EnumType.STRING)
    private Confidence confidence = Confidence.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String narrative; // AI-generated explanation

    @Column(columnDefinition = "TEXT")
    private String factors; // JSON: key factors influencing prediction

    private UUID departmentId; // null = church-wide

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Type getPredictionType() { return predictionType; }
    public void setPredictionType(Type predictionType) { this.predictionType = predictionType; }
    public Integer getPeriodMonths() { return periodMonths; }
    public void setPeriodMonths(Integer periodMonths) { this.periodMonths = periodMonths; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    public Double getPredictedValue() { return predictedValue; }
    public void setPredictedValue(Double predictedValue) { this.predictedValue = predictedValue; }
    public Double getGrowthRate() { return growthRate; }
    public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }
    public Trend getTrend() { return trend; }
    public void setTrend(Trend trend) { this.trend = trend; }
    public Confidence getConfidence() { return confidence; }
    public void setConfidence(Confidence confidence) { this.confidence = confidence; }
    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }
    public String getFactors() { return factors; }
    public void setFactors(String factors) { this.factors = factors; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
}
