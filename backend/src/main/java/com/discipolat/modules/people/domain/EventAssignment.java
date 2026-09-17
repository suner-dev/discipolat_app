package com.discipolat.modules.people.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_assignment", indexes = {
    @Index(name = "idx_ea_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ea_person", columnList = "person_id"),
    @Index(name = "idx_ea_event", columnList = "event_id"),
    @Index(name = "idx_ea_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "role_code", nullable = false, length = 60)
    private String roleCode;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ASSIGNED";

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