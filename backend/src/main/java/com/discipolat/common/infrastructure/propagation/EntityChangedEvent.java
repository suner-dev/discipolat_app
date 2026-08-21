package com.discipolat.common.infrastructure.propagation;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event published whenever a core entity is mutated.
 * Carries the old and new values of changed fields so that
 * listeners can perform downstream propagation (notifications,
 * history, search reindex, statistics, audit) consistently.
 *
 * One entity = one source of truth. This event ensures that
 * every consumer of the data sees the same truth.
 */
public class EntityChangedEvent extends ApplicationEvent {

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED,
        SOFT_DELETED,
        RESTORED,
        STATUS_CHANGED,
        REASSIGNED
    }

    private final String entityType;
    private final UUID entityId;
    private final ChangeType changeType;
    private final Map<String, Object> oldValues;
    private final Map<String, Object> newValues;
    private final UUID actorId;
    private final LocalDateTime occurredAt;
    private final String description;

    public EntityChangedEvent(Object source, String entityType, UUID entityId,
                              ChangeType changeType,
                              Map<String, Object> oldValues,
                              Map<String, Object> newValues,
                              UUID actorId, String description) {
        super(source);
        this.entityType = entityType;
        this.entityId = entityId;
        this.changeType = changeType;
        this.oldValues = oldValues != null ? Map.copyOf(oldValues) : Map.of();
        this.newValues = newValues != null ? Map.copyOf(newValues) : Map.of();
        this.actorId = actorId;
        this.occurredAt = LocalDateTime.now();
        this.description = description;
    }

    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public ChangeType getChangeType() { return changeType; }
    public Map<String, Object> getOldValues() { return oldValues; }
    public Map<String, Object> getNewValues() { return newValues; }
    public UUID getActorId() { return actorId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getDescription() { return description; }

    /**
     * Returns true if the given field was changed between old and new values.
     */
    public boolean fieldChanged(String fieldName) {
        Object oldVal = oldValues.get(fieldName);
        Object newVal = newValues.get(fieldName);
        if (oldVal == null && newVal == null) return false;
        if (oldVal == null || newVal == null) return true;
        return !oldVal.equals(newVal);
    }

    /**
     * Returns the previous value of the given field, or null.
     */
    public Object previousValue(String fieldName) {
        return oldValues.get(fieldName);
    }

    /**
     * Returns the new value of the given field, or null.
     */
    public Object currentValue(String fieldName) {
        return newValues.get(fieldName);
    }

    @Override
    public String toString() {
        return "EntityChangedEvent{" +
                "entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", changeType=" + changeType +
                ", actorId=" + actorId +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
