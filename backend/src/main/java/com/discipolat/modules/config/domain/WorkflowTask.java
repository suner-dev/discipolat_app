package com.discipolat.modules.config.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_task", indexes = {
    @Index(name = "idx_wf_task_tenant", columnList = "tenant_id"),
    @Index(name = "idx_wf_task_instance", columnList = "instance_id"),
    @Index(name = "idx_wf_task_assignee", columnList = "assignee_id"),
    @Index(name = "idx_wf_task_status_due", columnList = "status, due_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "assignee_role", length = 80)
    private String assigneeRole;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "escalated_from_task_id")
    private UUID escalatedFromTaskId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}