package com.discipolat.common.infrastructure.propagation;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central publisher for entity change propagation events.
 * Provides convenience methods that automatically resolve the
 * current actor (user performing the mutation) and publish
 * a well-structured {@link EntityChangedEvent}.
 *
 * Every service that mutates a core entity should inject this
 * publisher and call the appropriate method after saving the
 * entity to the database. This ensures consistent propagation
 * across all consumers: notifications, history, audit, search,
 * and statistics.
 */
@Service
public class EntityPropagationPublisher {

    private static final Logger log = LoggerFactory.getLogger(EntityPropagationPublisher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtils securityUtils;

    public EntityPropagationPublisher(ApplicationEventPublisher eventPublisher,
                                      SecurityUtils securityUtils) {
        this.eventPublisher = eventPublisher;
        this.securityUtils = securityUtils;
    }

    /**
     * Publish an entity creation event.
     */
    public void publishCreated(String entityType, UUID entityId,
                               Map<String, Object> newValues, String description) {
        publishEvent(entityType, entityId, EntityChangedEvent.ChangeType.CREATED,
                Map.of(), newValues, description);
    }

    /**
     * Publish an entity update event with old and new values.
     */
    public void publishUpdated(String entityType, UUID entityId,
                               Map<String, Object> oldValues,
                               Map<String, Object> newValues,
                               String description) {
        publishEvent(entityType, entityId, EntityChangedEvent.ChangeType.UPDATED,
                oldValues, newValues, description);
    }

    /**
     * Publish an entity deletion event (soft or hard).
     */
    public void publishDeleted(String entityType, UUID entityId,
                               Map<String, Object> oldValues, String description) {
        publishEvent(entityType, entityId, EntityChangedEvent.ChangeType.DELETED,
                oldValues, Map.of(), description);
    }

    /**
     * Publish a soft-delete event.
     */
    public void publishSoftDeleted(String entityType, UUID entityId,
                                   Map<String, Object> oldValues, String description) {
        publishEvent(entityType, entityId, EntityChangedEvent.ChangeType.SOFT_DELETED,
                oldValues, Map.of(), description);
    }

    /**
     * Publish a restoration event.
     */
    public void publishRestored(String entityType, UUID entityId,
                                Map<String, Object> newValues, String description) {
        publishEvent(entityType, entityId, EntityChangedEvent.ChangeType.RESTORED,
                Map.of(), newValues, description);
    }

    /**
     * Publish a status change event.
     */
    public void publishStatusChanged(String entityType, UUID entityId,
                                     String oldStatus, String newStatus, String description) {
        publishEvent(entityType, entityId, EntityChangedEvent.ChangeType.STATUS_CHANGED,
                Map.of("statut", oldStatus), Map.of("statut", newStatus), description);
    }

    /**
     * Publish a reassignment event (e.g., faiseur change, department change).
     */
    public void publishReassigned(String entityType, UUID entityId,
                                  String field,
                                  UUID oldOwnerId, UUID newOwnerId,
                                  String description) {
        publishEvent(entityType, entityId, EntityChangedEvent.ChangeType.REASSIGNED,
                Map.of(field, oldOwnerId != null ? oldOwnerId.toString() : null),
                Map.of(field, newOwnerId != null ? newOwnerId.toString() : null),
                description);
    }

    /**
     * Low-level publish with automatic actor resolution.
     */
    private void publishEvent(String entityType, UUID entityId,
                              EntityChangedEvent.ChangeType changeType,
                              Map<String, Object> oldValues,
                              Map<String, Object> newValues,
                              String description) {
        // Defense-in-depth : retire les valeurs null des payloads. Les maps
        // immuables (Map.of) REJETTENT toute valeur null au call-site, mais une
        // map mutable (LinkedHashMap) peut contenir un getter null. Sans cette
        // neutralisation, Map.copyOf dans EntityChangedEvent lève une NPE et
        // casse la transaction métier principale (effet de bord du refactor).
        oldValues = sanitize(oldValues);
        newValues = sanitize(newValues);

        UUID actorId = null;
        try {
            actorId = securityUtils.getCurrentUserId();
        } catch (Exception e) {
            // System operations (jobs, migrations) have no user context
        }

        EntityChangedEvent event = new EntityChangedEvent(
                this, entityType, entityId, changeType,
                oldValues, newValues, actorId, description);

        log.debug("Publishing entity change: {} {} {} by actor {}",
                changeType, entityType, entityId, actorId);

        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish propagation event for {} {}: {}",
                    entityType, entityId, e.getMessage());
            // Propagation failure must never block the main transaction
        }
    }

    /**
     * Retire les entrées dont la clé ou la valeur est null, pour éviter toute
     * NPE dans {@code Map.copyOf} du {@link EntityChangedEvent}.
     */
    private static Map<String, Object> sanitize(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> cleaned = new LinkedHashMap<>();
        values.forEach((k, v) -> {
            if (k != null && v != null) {
                cleaned.put(k, v);
            }
        });
        return cleaned;
    }
}
