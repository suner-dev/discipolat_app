package com.discipolat.modules.workflow.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * G2.5 — WorkflowTransition : arête entre deux étapes déclenchée par un événement.
 * Contrat annexe A §A.3 : clé (from_step, to_step, on_event) — implémentée ici par
 * une clé de substitution + contrainte unique (adaptation aux conventions du code).
 */
@Entity
@Table(name = "workflow_transition", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wf_transition", columnNames = {"from_step_id", "to_step_id", "on_event"})
}, indexes = {
        @Index(name = "idx_wf_trans_from", columnList = "from_step_id"),
        @Index(name = "idx_wf_trans_workflow", columnList = "workflow_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "from_step_id", nullable = false)
    private UUID fromStepId;

    @Column(name = "to_step_id", nullable = false)
    private UUID toStepId;

    /** Événement déclencheur : APPROVE, REJECT, TIMEOUT, SUBMIT… */
    @Column(name = "on_event", nullable = false, length = 40)
    private String onEvent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
