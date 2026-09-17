package com.discipolat.modules.workflow.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

/**
 * G2.5 — WorkflowInstance : exécution d'un processus sur un objet métier
 * (ex. une demande d'achat de matériel). Annexe A §A.3.
 */
@Entity
@Table(name = "workflow_instance", indexes = {
        @Index(name = "idx_wf_inst_tenant", columnList = "tenant_id"),
        @Index(name = "idx_wf_inst_workflow", columnList = "workflow_id"),
        @Index(name = "idx_wf_inst_entity", columnList = "entity_id"),
        @Index(name = "idx_wf_inst_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private WorkflowInstanceStatus status = WorkflowInstanceStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
