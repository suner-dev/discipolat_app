package com.discipolat.modules.discipleship.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_meetings", indexes = {
        @Index(name = "idx_disc_meetings_tenant", columnList = "tenant_id"),
        @Index(name = "idx_disc_meetings_assignment", columnList = "tenant_id, assignment_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MentorMeeting {

    public enum MeetingStatus { SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "disciple_id", nullable = false)
    private UUID discipleId;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "actual_at")
    private Instant actualAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;

    @Column(name = "next_steps", columnDefinition = "TEXT")
    private String nextSteps;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "is_group", nullable = false)
    @Builder.Default
    private boolean isGroup = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = Instant.now(); }
}
