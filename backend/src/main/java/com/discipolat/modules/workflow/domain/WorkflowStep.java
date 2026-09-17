package com.discipolat.modules.workflow.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * G2.5 — WorkflowStep : une étape ordonnée d'un processus.
 * Types : APPROVAL | AUTO_ACTION | NOTIFY | EXPENSE | ASSET_STATUS | FORM.
 */
@Entity
@Table(name = "workflow_step", indexes = {
        @Index(name = "idx_wf_step_workflow", columnList = "workflow_id"),
        @Index(name = "idx_wf_step_order", columnList = "workflow_id, step_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private WorkflowStepType stepType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> conditionsJson = Map.of();

    /** Rôle attendu pour l'approbateur (« CHEF_DEPARTEMENT », « FINANCE »…). */
    @Column(name = "assignee_role", length = 80)
    private String assigneeRole;

    /** Portée de l'assignation (« SPACE », « TENANT », « ANCESTOR »…). */
    @Column(name = "assignee_scope", length = 40)
    private String assigneeScope;

    @Column(name = "timeout_hours")
    private Integer timeoutHours;

    /** Rôle supérieur vers qui escalader en cas de dépassement de délai. */
    @Column(name = "escalation_role", length = 80)
    private String escalationRole;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auto_action_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> autoActionJson = Map.of();

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
