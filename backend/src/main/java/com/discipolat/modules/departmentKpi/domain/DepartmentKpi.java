package com.discipolat.modules.departmentKpi.domain;

import java.util.UUID;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "department_kpis")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DepartmentKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "target_value")
    private Double targetValue;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "unit")
    private String unit;

    @Column(name = "period")
    @Enumerated(EnumType.STRING)
    private KpiPeriod period = KpiPeriod.MONTHLY;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    public enum KpiPeriod {
        WEEKLY, MONTHLY, QUARTERLY, YEARLY
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public KpiPeriod getPeriod() { return period; }
    public void setPeriod(KpiPeriod period) { this.period = period; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
