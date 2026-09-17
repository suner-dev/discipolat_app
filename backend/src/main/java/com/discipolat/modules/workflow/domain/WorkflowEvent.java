package com.discipolat.modules.workflow.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * G2.5 — Événement émis par le moteur de workflow (avance, approbation,
 * rejet, escalade). Consommé par le temps réel (G2.10) et l'audit (G2.9).
 */
public class WorkflowEvent {

    private final String eventType;
    private final UUID tenantId;
    private final UUID instanceId;
    private final UUID taskId;
    private final UUID entityId;
    private final String detail;
    private final Instant occurredAt;

    public WorkflowEvent(String eventType, UUID tenantId, UUID instanceId, UUID taskId,
                         UUID entityId, String detail) {
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.instanceId = instanceId;
        this.taskId = taskId;
        this.entityId = entityId;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }

    public String getEventType() { return eventType; }
    public UUID getTenantId() { return tenantId; }
    public UUID getInstanceId() { return instanceId; }
    public UUID getTaskId() { return taskId; }
    public UUID getEntityId() { return entityId; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
