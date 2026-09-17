package com.discipolat.modules.spaces.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * G2.6 — Événement émis à chaque modification de configuration d'un espace.
 * Consommé par le moteur temps réel (G2.10) pour invalider le cache config
 * des clients web + mobile (< 5s) et par l'audit/historique (G2.9).
 *
 * Il sera relayé par l'outbox transactionnel (§G2.8, annexe E : SpaceConfigChanged).
 */
public class SpaceConfigChangedEvent {

    private final UUID tenantId;
    private final UUID spaceId;
    private final String spaceCode;
    private final String changeType;
    private final Instant changedAt;
    private final UUID actorId;

    public SpaceConfigChangedEvent(UUID tenantId, UUID spaceId, String spaceCode,
                                   String changeType, UUID actorId) {
        this.tenantId = tenantId;
        this.spaceId = spaceId;
        this.spaceCode = spaceCode;
        this.changeType = changeType;
        this.actorId = actorId;
        this.changedAt = Instant.now();
    }

    public UUID getTenantId() { return tenantId; }
    public UUID getSpaceId() { return spaceId; }
    public String getSpaceCode() { return spaceCode; }
    public String getChangeType() { return changeType; }
    public Instant getChangedAt() { return changedAt; }
    public UUID getActorId() { return actorId; }

    public String getEventType() { return "SpaceConfigChanged"; }
}
