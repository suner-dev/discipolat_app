package com.discipolat.modules.followUpRequests.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P3 #112 — Demandes de suivi.
 * Un membre peut demander un faiseur ou un accompagnement spirituel depuis l'app.
 */
@Entity
@Table(name = "follow_up_requests")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FollowUpRequest {

    public enum Type { FAISEUR, ACCOMPAGNEMENT_SPIRITUEL, CONSEIL_PASTORAL }
    public enum Status { EN_ATTENTE, ASSIGNEE, EN_COURS, TERMINEE, REJETEE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "requester_name")
    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type = Type.FAISEUR;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** Famille souhaitée (optionnel). */
    @Column(name = "preferred_family_id")
    private UUID preferredFamilyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.EN_ATTENTE;

    @Column(name = "assigned_to_id")
    private UUID assignedToId;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getRequesterId() { return requesterId; }
    public void setRequesterId(UUID requesterId) { this.requesterId = requesterId; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public UUID getPreferredFamilyId() { return preferredFamilyId; }
    public void setPreferredFamilyId(UUID preferredFamilyId) { this.preferredFamilyId = preferredFamilyId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public UUID getAssignedToId() { return assignedToId; }
    public void setAssignedToId(UUID assignedToId) { this.assignedToId = assignedToId; }
    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
