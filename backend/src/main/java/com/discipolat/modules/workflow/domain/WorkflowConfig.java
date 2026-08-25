package com.discipolat.modules.workflow.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "workflow_key"})
})
public class WorkflowConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "workflow_key", nullable = false, length = 100)
    private String workflowKey;

    @Column(nullable = false)
    private String label;

    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(columnDefinition = "jsonb")
    private String rules;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public WorkflowConfig() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getWorkflowKey() { return workflowKey; }
    public void setWorkflowKey(String workflowKey) { this.workflowKey = workflowKey; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public WorkflowConfig withDefaults() {
        this.enabled = this.enabled != null ? this.enabled : true;
        this.createdAt = this.createdAt != null ? this.createdAt : LocalDateTime.now();
        this.updatedAt = this.updatedAt != null ? this.updatedAt : LocalDateTime.now();
        return this;
    }
}
