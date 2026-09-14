package com.discipolat.modules.dresscode.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Dress Code entity — programmed dress code for an event or service per space.
 * Each space (Accueil, Chorale, Enfants, Protocole…) can program dress codes
 * by event/service and by group.
 */
@Entity
@Table(name = "dress_code", indexes = {
        @Index(name = "idx_dress_code_tenant", columnList = "tenant_id"),
        @Index(name = "idx_dress_code_space", columnList = "space_id"),
        @Index(name = "idx_dress_code_event", columnList = "event_id")
})
public class DressCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "begins_at")
    private Instant beginsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(length = 50)
    private String status;

    @Column(name = "created_by")
    private UUID createdBy;

    private boolean archived;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = "DRAFT";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instant getBeginsAt() { return beginsAt; }
    public void setBeginsAt(Instant beginsAt) { this.beginsAt = beginsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
