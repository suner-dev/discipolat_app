package com.discipolat.modules.discipleship.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disciple_progress", indexes = {
        @Index(name = "idx_disc_progress_tenant", columnList = "tenant_id"),
        @Index(name = "idx_disc_progress_disciple", columnList = "tenant_id, disciple_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DiscipleProgress {

    public enum ProgressStatus { NOT_STARTED, IN_PROGRESS, STALLED, COMPLETED, ABANDONED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "disciple_id", nullable = false)
    private UUID discipleId;

    @Column(name = "journey_id", nullable = false)
    private Long journeyId;

    @Column(name = "current_stage_id")
    private Long currentStageId;

    @Column(name = "completed_stages", nullable = false)
    @Builder.Default
    private int completedStages = 0;

    @Column(name = "total_stages", nullable = false)
    @Builder.Default
    private int totalStages = 0;

    @Column(name = "completed_requirements", nullable = false)
    @Builder.Default
    private int completedRequirements = 0;

    @Column(name = "total_requirements", nullable = false)
    @Builder.Default
    private int totalRequirements = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    @Column(name = "next_milestone_date")
    private Instant nextMilestoneDate;

    @Column(name = "next_milestone_name", length = 200)
    private String nextMilestoneName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.startedAt == null) this.startedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = Instant.now(); }
}
