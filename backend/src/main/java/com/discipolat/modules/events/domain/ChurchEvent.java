package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "church_event", indexes = {
    @Index(name = "idx_church_event_tenant", columnList = "tenant_id"),
    @Index(name = "idx_church_event_status", columnList = "status"),
    @Index(name = "idx_church_event_start", columnList = "start_at"),
    @Index(name = "idx_church_event_tenant_start", columnList = "tenant_id, start_at"),
    @Index(name = "idx_church_event_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "recurrence_rule", columnDefinition = "text")
    private String recurrenceRule;

    @Column(name = "visibility", nullable = false, length = 30)
    private String visibility = "CHURCH";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}