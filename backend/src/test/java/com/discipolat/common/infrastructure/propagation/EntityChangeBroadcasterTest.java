package com.discipolat.common.infrastructure.propagation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EntityChangeBroadcaster} — SSE real-time push.
 */
@ExtendWith(MockitoExtension.class)
class EntityChangeBroadcasterTest {

    private EntityChangeBroadcaster broadcaster;

    private static final UUID TENANT_1 = UUID.randomUUID();
    private static final UUID TENANT_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        broadcaster = new EntityChangeBroadcaster();
    }

    @Test
    void register_increasesConnectedCount() {
        SseEmitter emitter = mock(SseEmitter.class);

        broadcaster.register(TENANT_1, emitter);

        assertThat(broadcaster.getConnectedCount(TENANT_1)).isEqualTo(1);
        assertThat(broadcaster.getTotalConnectedCount()).isEqualTo(1);
    }

    @Test
    void register_multipleClients_sameTenant() {
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);

        broadcaster.register(TENANT_1, e1);
        broadcaster.register(TENANT_1, e2);

        assertThat(broadcaster.getConnectedCount(TENANT_1)).isEqualTo(2);
        assertThat(broadcaster.getTotalConnectedCount()).isEqualTo(2);
    }

    @Test
    void register_differentTenants() {
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);

        broadcaster.register(TENANT_1, e1);
        broadcaster.register(TENANT_2, e2);

        assertThat(broadcaster.getConnectedCount(TENANT_1)).isEqualTo(1);
        assertThat(broadcaster.getConnectedCount(TENANT_2)).isEqualTo(1);
        assertThat(broadcaster.getTotalConnectedCount()).isEqualTo(2);
        assertThat(broadcaster.getConnectedTenantIds()).containsExactlyInAnyOrder(TENANT_1, TENANT_2);
    }

    @Test
    void broadcast_sendsToCorrectTenant() throws IOException {
        SseEmitter emitter1 = mock(SseEmitter.class);
        SseEmitter emitter2 = mock(SseEmitter.class);
        broadcaster.register(TENANT_1, emitter1);
        broadcaster.register(TENANT_2, emitter2);

        EntityChangedEvent event = buildEvent("SOUL", UUID.randomUUID(),
                EntityChangedEvent.ChangeType.UPDATED);

        broadcaster.broadcast(TENANT_1, event);

        verify(emitter1).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter2, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void broadcast_respectsEntityTypeFilter() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        // Subscribe only to SOUL events
        broadcaster.register(TENANT_1, emitter, Set.of("SOUL"));

        EntityChangedEvent soulEvent = buildEvent("SOUL", UUID.randomUUID(),
                EntityChangedEvent.ChangeType.UPDATED);
        EntityChangedEvent userEvent = buildEvent("USER", UUID.randomUUID(),
                EntityChangedEvent.ChangeType.CREATED);

        broadcaster.broadcast(TENANT_1, soulEvent);
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));

        reset(emitter);
        broadcaster.broadcast(TENANT_1, userEvent);
        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void broadcast_noListeners_noException() {
        EntityChangedEvent event = buildEvent("SOUL", UUID.randomUUID(),
                EntityChangedEvent.ChangeType.CREATED);

        // Should not throw
        broadcaster.broadcast(UUID.randomUUID(), event);
        broadcaster.broadcast(null, event);
        broadcaster.broadcast(TENANT_1, null);
    }

    @Test
    void broadcast_handlesIOException_gracefully() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("Client disconnected")).when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));
        broadcaster.register(TENANT_1, emitter);

        EntityChangedEvent event = buildEvent("SOUL", UUID.randomUUID(),
                EntityChangedEvent.ChangeType.UPDATED);

        // Should not throw — IOException is caught
        broadcaster.broadcast(TENANT_1, event);
    }

    @Test
    void getConnectedTenantIds_onlyReturnsNonEmpty() {
        SseEmitter e1 = mock(SseEmitter.class);
        broadcaster.register(TENANT_1, e1);

        assertThat(broadcaster.getConnectedTenantIds()).containsExactly(TENANT_1);
    }

    private EntityChangedEvent buildEvent(String entityType, UUID entityId,
                                           EntityChangedEvent.ChangeType changeType) {
        return new EntityChangedEvent(
                this, entityType, entityId, changeType,
                Map.of(), Map.of(), UUID.randomUUID(), "Test event");
    }
}
