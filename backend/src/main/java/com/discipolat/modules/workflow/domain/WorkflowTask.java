package com.discipolat.modules.workflow.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

/**
 * G2.5 — WorkflowTask : tâche d'approbation assignée à un acteur,
 * avec délai (due_at) et escalade. Annexe A §A.3.
 */
@Entity
@Table(name = "workflow_task", indexes = {
        @Index(name = "idx_wf_task_tenant", columnList = "tenant_id"),
        @Index(name = "idx_wf_task_instance", columnList = "instance_id"),
        @Index(name = "idx_wf_task_assignee", columnList = "assignee_id"),
        @Index(name = "idx_wf_task_status_due", columnList = "status, due_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class WorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private WorkflowTaskStatus status = WorkflowTaskStatus.PENDING;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "escalated_from_task_id")
    private UUID escalatedFromTaskId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
