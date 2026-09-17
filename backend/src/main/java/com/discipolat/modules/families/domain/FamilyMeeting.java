package com.discipolat.modules.families.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "family_meeting", indexes = {
    @Index(name = "idx_fm_family", columnList = "family_id"),
    @Index(name = "idx_fm_date", columnList = "meeting_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "meeting_type", length = 50)
    private String meetingType;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PLANNED";

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}