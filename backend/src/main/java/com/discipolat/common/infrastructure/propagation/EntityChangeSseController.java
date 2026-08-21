package com.discipolat.common.infrastructure.propagation;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SSE (Server-Sent Events) endpoint for real-time entity change push.
 *
 * Clients connect to {@code GET /api/v1/events/entity-changes} and receive
 * entity change events in real-time as they occur in the backend.
 *
 * Each connected client receives events only for their tenant.
 * Optional query parameter {@code entityTypes} allows filtering by
 * entity type (comma-separated, e.g. ?entityTypes=SOUL,USER).
 *
 * Event format (SSE):
 *   event: entity-change
 *   data: {"entityType":"SOUL","entityId":"...","changeType":"UPDATED","oldValues":{},"newValues":{},"actorId":"...","description":"...","occurredAt":"..."}
 *
 * The connection is kept alive with Spring's SseEmitter default timeout (30 min).
 * Clients should reconnect automatically on disconnect.
 *
 * Usage from frontend:
 *   const eventSource = new EventSource('/api/v1/events/entity-changes?token=JWT');
 *   eventSource.addEventListener('entity-change', (e) => {
 *     const change = JSON.parse(e.data);
 *     // Update UI based on change.entityType, change.changeType, etc.
 *   });
 */
@RestController
@RequestMapping("/api/v1/events")
public class EntityChangeSseController {

    private static final Logger log = LoggerFactory.getLogger(EntityChangeSseController.class);

    private final EntityChangeBroadcaster broadcaster;
    private final SecurityUtils securityUtils;

    public EntityChangeSseController(EntityChangeBroadcaster broadcaster,
                                     SecurityUtils securityUtils) {
        this.broadcaster = broadcaster;
        this.securityUtils = securityUtils;
    }

    /**
     * SSE endpoint for entity change events.
     *
     * @param entityTypes  optional comma-separated list of entity types to filter
     * @return SseEmitter that will receive entity-change events
     */
    @GetMapping(value = "/entity-changes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam(required = false, name = "entityTypes") String entityTypes) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        UUID userId = securityUtils.getCurrentUserId();

        log.info("SSE subscription: tenant={}, user={}, entityTypes={}", tenantId, userId, entityTypes);

        // Create emitter with 30-minute timeout
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // Parse entity type filter
        Set<String> filter = null;
        if (entityTypes != null && !entityTypes.isBlank()) {
            filter = Set.of(entityTypes.split(",")).stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
        }

        // Register with the broadcaster
        broadcaster.register(tenantId, emitter, filter);

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "message", "Connected to entity change stream",
                            "tenantId", tenantId.toString(),
                            "entityTypes", filter != null ? filter : "ALL",
                            "connectedClients", broadcaster.getConnectedCount(tenantId)
                    )));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE event: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * Health check for the SSE endpoint — returns connection stats.
     * Restreint au rôle ADMIN : expose le NOMBRE de locataires connectés,
     * jamais leurs identifiants (évite toute fuite d'informations cross-tenant).
     */
    @GetMapping("/entity-changes/stats")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Map<String, Object> stats() {
        return Map.of(
                "totalConnectedClients", broadcaster.getTotalConnectedCount(),
                "connectedTenants", broadcaster.getConnectedTenantIds().size()
        );
    }
}
