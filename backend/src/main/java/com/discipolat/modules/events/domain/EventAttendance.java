package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_attendance", indexes = {
    @Index(name = "idx_ea_church_event", columnList = "church_event_id"),
    @Index(name = "idx_ea_person", columnList = "person_id"),
    @Index(name = "idx_ea_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_attendance_church_event_person", columnNames = {"church_event_id", "person_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "church_event_id", nullable = false)
    private UUID churchEventId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PRESENT";

    @Column(name = "check_in_at")
    private OffsetDateTime checkInAt;

    @Column(name = "check_out_at")
    private OffsetDateTime checkOutAt;

    @Column(name = "check_in_method", length = 30)
    private String checkInMethod;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}