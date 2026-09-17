package com.discipolat.modules.statuses.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * G2.7 — Événement émis à chaque changement de statut, consommé par l'audit,
 * les notifications (G2.10) et le workflow engine (G2.5).
 * Annexe E : StatusChanged.
 */
public class StatusChangedEvent {

    private final UUID tenantId;
    private final String entityType;
    private final UUID entityId;
    private final String fromCode;
    private final String toCode;
    private final UUID spaceId;
    private final UUID actorId;
    private final Instant changedAt;

    public StatusChangedEvent(UUID tenantId, String entityType, UUID entityId,
                              String fromCode, String toCode, UUID spaceId, UUID actorId) {
        this.tenantId = tenantId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.spaceId = spaceId;
        this.actorId = actorId;
        this.changedAt = Instant.now();
    }

    public UUID getTenantId() { return tenantId; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getFromCode() { return fromCode; }
    public String getToCode() { return toCode; }
    public UUID getSpaceId() { return spaceId; }
    public UUID getActorId() { return actorId; }
    public Instant getChangedAt() { return changedAt; }

    public String getEventType() { return "StatusChanged"; }
}
