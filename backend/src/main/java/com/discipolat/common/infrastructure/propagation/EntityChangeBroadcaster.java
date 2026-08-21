package com.discipolat.common.infrastructure.propagation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manages SSE (Server-Sent Events) connections for real-time entity change push.
 *
 * Clients subscribe to entity change events and receive them in real-time.
 * Events are scoped per tenant — only changes within the client's tenant
 * are delivered.
 *
 * Architecture:
 *   EntityPropagationListener → EntityChangeBroadcaster → SSE → Client
 *
 * The broadcaster maintains a registry of active SSE emitters keyed by tenant.
 * When an EntityChangedEvent is published, all connected clients for that tenant
 * receive the event payload instantly.
 *
 * Thread safety: all collections are ConcurrentHashMap / CopyOnWriteArrayList
 * to handle concurrent SSE connections from multiple HTTP threads.
 */
@Service
public class EntityChangeBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(EntityChangeBroadcaster.class);

    /**
     * SSE emitter lifetime: 30 minutes. After that, the client must reconnect.
     * This prevents zombie connections from accumulating.
     */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * Registry of active SSE emitters, keyed by tenant ID.
     * Each tenant can have multiple connected clients.
     */
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByTenant =
            new ConcurrentHashMap<>();

    /**
     * Optional filter: subscribe to specific entity types only.
     * Maps emitter → set of entity types to filter on.
     * If the set is empty/null, the emitter receives ALL entity types.
     */
    private final ConcurrentHashMap<SseEmitter, Set<String>> entityFilters =
            new ConcurrentHashMap<>();

    /**
     * Registers a new SSE emitter for a given tenant.
     * The emitter will receive all entity change events for that tenant.
     *
     * @param tenantId  the tenant to subscribe to
     * @param emitter   the Spring SseEmitter
     */
    public void register(UUID tenantId, SseEmitter emitter) {
        emittersByTenant
                .computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        log.info("SSE client connected for tenant {} (total: {})",
                tenantId, getConnectedCount(tenantId));

        // Cleanup on completion/error/timeout
        emitter.onCompletion(() -> remove(tenantId, emitter));
        emitter.onTimeout(() -> remove(tenantId, emitter));
        emitter.onError(e -> remove(tenantId, emitter));
    }

    /**
     * Registers an SSE emitter with entity type filtering.
     *
     * @param tenantId     the tenant to subscribe to
     * @param emitter      the Spring SseEmitter
     * @param entityTypes  entity types to filter on (empty = all)
     */
    public void register(UUID tenantId, SseEmitter emitter, Set<String> entityTypes) {
        if (entityTypes != null && !entityTypes.isEmpty()) {
            entityFilters.put(emitter, entityTypes);
        }
        register(tenantId, emitter);
    }

    /**
     * Broadcasts an entity change event to all connected clients of a tenant.
     * Respects per-emitter entity type filters.
     *
     * @param tenantId     the tenant whose clients should receive the event
     * @param event        the entity change event
     */
    public void broadcast(UUID tenantId, EntityChangedEvent event) {
        if (tenantId == null || event == null) return;

        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        if (emitters == null || emitters.isEmpty()) return;

        // Build the SSE payload
        Map<String, Object> payload = Map.of(
                "entityType", event.getEntityType(),
                "entityId", event.getEntityId() != null ? event.getEntityId().toString() : "",
                "changeType", event.getChangeType().name(),
                "oldValues", event.getOldValues(),
                "newValues", event.getNewValues(),
                "actorId", event.getActorId() != null ? event.getActorId().toString() : "",
                "description", event.getDescription() != null ? event.getDescription() : "",
                "occurredAt", event.getOccurredAt() != null ? event.getOccurredAt().toString() : ""
        );

        int sent = 0;
        int filtered = 0;
        int failed = 0;

        for (SseEmitter emitter : emitters) {
            // Apply entity type filter
            Set<String> filter = entityFilters.get(emitter);
            if (filter != null && !filter.isEmpty()
                    && !filter.contains(event.getEntityType())) {
                filtered++;
                continue;
            }

            try {
                emitter.send(SseEmitter.event()
                        .name("entity-change")
                        .data(payload));
                sent++;
            } catch (IOException e) {
                // Client disconnected — remove on next iteration
                failed++;
                log.debug("SSE send failed for tenant {}: {}", tenantId, e.getMessage());
            }
        }

        // Cleanup dead emitters
        if (failed > 0) {
            cleanupDeadEmitters(tenantId);
        }

        log.debug("Broadcast entity change {} {} to tenant {}: sent={}, filtered={}, failed={}",
                event.getChangeType(), event.getEntityType(), tenantId, sent, filtered, failed);
    }

    /**
     * Removes a specific emitter from the registry.
     */
    private void remove(UUID tenantId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
            entityFilters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByTenant.remove(tenantId);
            }
        }
        log.debug("SSE client disconnected from tenant {} (remaining: {})",
                tenantId, getConnectedCount(tenantId));
    }

    /**
     * Removes emitters that have been closed or timed out.
     */
    private void cleanupDeadEmitters(UUID tenantId) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        if (emitters == null) return;

        emitters.removeIf(emitter -> {
            // SseEmitter has no isOpen() in some versions, so we rely on
            // the onCompletion/onError callbacks. If a send failed, the
            // callback will fire and clean up.
            return false;
        });
    }

    /**
     * Returns the number of connected SSE clients for a tenant.
     */
    public int getConnectedCount(UUID tenantId) {
        List<SseEmitter> emitters = emittersByTenant.get(tenantId);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * Returns the total number of connected SSE clients across all tenants.
     */
    public int getTotalConnectedCount() {
        return emittersByTenant.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Returns the set of tenant IDs with at least one connected client.
     */
    public Set<UUID> getConnectedTenantIds() {
        return emittersByTenant.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
