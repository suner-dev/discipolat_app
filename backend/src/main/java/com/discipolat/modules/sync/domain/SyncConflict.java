package com.discipolat.modules.sync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * §G5.7 — Conflit détecté à l'application d'une opération hors-ligne :
 * l'entité visée a été modifiée côté serveur APRÈS la saisie terrain
 * (comparaison {@code client_op_at} vs {@code server_updated_at}, LWW).
 * La valeur la plus récente est appliquée (LWW), le conflit est tracé et
 * notifié au responsable pour réconciliation manuelle — jamais de perte silencieuse.
 */
@Entity
@Table(name = "sync_conflict")
public class SyncConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "client_uuid", length = 64)
    private String clientUuid;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(name = "field_name", length = 80)
    private String fieldName;

    @Column(name = "client_value", columnDefinition = "jsonb")
    private String clientValue;

    @Column(name = "server_value", columnDefinition = "jsonb")
    private String serverValue;

    @Column(name = "client_op_at")
    private LocalDateTime clientOpAt;

    @Column(name = "server_updated_at")
    private LocalDateTime serverUpdatedAt;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getOperationId() { return operationId; }
    public void setOperationId(UUID operationId) { this.operationId = operationId; }
    public String getClientUuid() { return clientUuid; }
    public void setClientUuid(String clientUuid) { this.clientUuid = clientUuid; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getClientValue() { return clientValue; }
    public void setClientValue(String clientValue) { this.clientValue = clientValue; }
    public String getServerValue() { return serverValue; }
    public void setServerValue(String serverValue) { this.serverValue = serverValue; }
    public LocalDateTime getClientOpAt() { return clientOpAt; }
    public void setClientOpAt(LocalDateTime clientOpAt) { this.clientOpAt = clientOpAt; }
    public LocalDateTime getServerUpdatedAt() { return serverUpdatedAt; }
    public void setServerUpdatedAt(LocalDateTime serverUpdatedAt) { this.serverUpdatedAt = serverUpdatedAt; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
