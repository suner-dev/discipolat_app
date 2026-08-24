package com.discipolat.modules.executiveInsights.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "executive_insights")
public class ExecutiveInsight {

    public enum Severity { INFO, WARNING, CRITICAL, OPPORTUNITY }
    public enum Category { GROWTH, RETENTION, ENGAGEMENT, FINANCE, OPERATIONS, SPIRITUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.INFO;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(columnDefinition = "TEXT")
    private String recommendedAction;

    private String metricValue;
    private String metricChange;

    private Boolean isRead = false;
    private Boolean isDismissed = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getMetricValue() { return metricValue; }
    public void setMetricValue(String metricValue) { this.metricValue = metricValue; }
    public String getMetricChange() { return metricChange; }
    public void setMetricChange(String metricChange) { this.metricChange = metricChange; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public Boolean getIsDismissed() { return isDismissed; }
    public void setIsDismissed(Boolean isDismissed) { this.isDismissed = isDismissed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
