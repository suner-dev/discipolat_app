package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_space", uniqueConstraints = {
    @UniqueConstraint(name = "uk_church_event_space", columnNames = {"church_event_id", "space_id"})
}, indexes = {
    @Index(name = "idx_es_church_event", columnList = "church_event_id"),
    @Index(name = "idx_es_space", columnList = "space_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "church_event_id", nullable = false)
    private UUID churchEventId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "role", length = 30)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}