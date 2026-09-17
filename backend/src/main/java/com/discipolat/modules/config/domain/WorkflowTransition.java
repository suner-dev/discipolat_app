package com.discipolat.modules.config.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_transition")
@IdClass(WorkflowTransitionId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransition {

    @Id
    @Column(name = "from_step_id", nullable = false)
    private UUID fromStepId;

    @Id
    @Column(name = "to_step_id", nullable = false)
    private UUID toStepId;

    @Id
    @Column(name = "on_event", nullable = false, length = 40)
    private String onEvent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}