package com.discipolat.modules.visits.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PastoralCase — confidential pastoral care case with strict access control.
 * Separated from public visit data. Each access is audited.
 */
@Entity
@Table(name = "pastoral_case", indexes = {
        @Index(name = "idx_pastoral_case_tenant", columnList = "tenant_id"),
        @Index(name = "idx_pastoral_case_person", columnList = "person_id"),
        @Index(name = "idx_pastoral_case_assigned", columnList = "assigned_to")
})
public class PastoralCase {

    public enum ConfidentialityLevel { STRICT, INTERNAL, GENERAL }
    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED, REFERRED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality_level", nullable = false)
    private ConfidentialityLevel confidentialityLevel = ConfidentialityLevel.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "visibility_scope", length = 50)
    private String visibilityScope;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPersonId() { return personId; }
    public void setPersonId(UUID personId) { this.personId = personId; }
    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }
    public ConfidentialityLevel getConfidentialityLevel() { return confidentialityLevel; }
    public void setConfidentialityLevel(ConfidentialityLevel confidentialityLevel) { this.confidentialityLevel = confidentialityLevel; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVisibilityScope() { return visibilityScope; }
    public void setVisibilityScope(String visibilityScope) { this.visibilityScope = visibilityScope; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
