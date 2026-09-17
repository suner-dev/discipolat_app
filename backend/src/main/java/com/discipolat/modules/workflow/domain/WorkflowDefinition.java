package com.discipolat.modules.workflow.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

/**
 * G2.5 — WorkflowDefinition : un processus modélisé par un espace, sans code.
 * Annexe A §A.3. space_id nullable : workflow du tenant ou propre à un espace.
 */
@Entity
@Table(name = "workflow_definition", indexes = {
        @Index(name = "idx_wf_def_tenant", columnList = "tenant_id"),
        @Index(name = "idx_wf_def_tenant_code", columnList = "tenant_id, code"),
        @Index(name = "idx_wf_def_space", columnList = "space_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
