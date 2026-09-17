package com.discipolat.modules.config.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_instance", indexes = {
    @Index(name = "idx_wf_inst_tenant", columnList = "tenant_id"),
    @Index(name = "idx_wf_inst_workflow", columnList = "workflow_id"),
    @Index(name = "idx_wf_inst_entity", columnList = "entity_id"),
    @Index(name = "idx_wf_inst_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "current_step_id")
    private UUID currentStepId;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "RUNNING";

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.startedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}