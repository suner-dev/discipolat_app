package com.discipolat.modules.config.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "workflow_step", indexes = {
    @Index(name = "idx_wf_step_workflow", columnList = "workflow_id"),
    @Index(name = "idx_wf_step_order", columnList = "workflow_id, step_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_type", nullable = false, length = 30)
    private String stepType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> conditionsJson;

    @Column(name = "assignee_role", length = 80)
    private String assigneeRole;

    @Column(name = "assignee_scope", length = 40)
    private String assigneeScope;

    @Column(name = "timeout_hours")
    private Integer timeoutHours;

    @Column(name = "escalation_role", length = 80)
    private String escalationRole;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auto_action_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> autoActionJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.conditionsJson == null) this.conditionsJson = Map.of();
        if (this.autoActionJson == null) this.autoActionJson = Map.of();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}