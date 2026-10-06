package com.discipolat.modules.discipleship.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_assignments", indexes = {
        @Index(name = "idx_disc_assign_tenant", columnList = "tenant_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MentorAssignment {

    public enum AssignmentStatus { PENDING, ACTIVE, ENDED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "disciple_id", nullable = false)
    private UUID discipleId;

    @Column(name = "journey_id", nullable = false)
    private Long journeyId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "meeting_frequency_days", nullable = false)
    @Builder.Default
    private int meetingFrequencyDays = 7;

    @Column(name = "last_meeting_at")
    private Instant lastMeetingAt;

    @Column(name = "next_meeting_at")
    private Instant nextMeetingAt;

    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) this.assignedAt = Instant.now();
    }
}
