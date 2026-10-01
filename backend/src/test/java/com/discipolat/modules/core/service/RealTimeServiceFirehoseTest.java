package com.discipolat.modules.core.service;

import com.discipolat.modules.core.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * §G5.8 — Firehose outbox → STOMP : TOUT événement commité doit être enveloppé
 * et poussé sur /topic/tenant:{id}/events avec l'eventId monotone (déduplication
 * et détection de trou côté client), JAMAIS sans tenant (pas de fuite cross-tenant).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RealTimeServiceFirehoseTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private RealTimeService service;

    private static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RealTimeService(messagingTemplate);
    }

    private OutboxEvent event(UUID tenantId) {
        return OutboxEvent.builder()
                .id(42L)
                .tenantId(tenantId)
                .aggregateType("INVENTORY_ITEM")
                .aggregateId(UUID.randomUUID())
                .eventType("AssetCheckedOut")
                .payloadJson(Map.of("spaceId", "sp-1", "memberId", "m-2"))
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void outboxEventIsPushedOnTenantFirehoseWithMonotonicId() {
        OutboxEvent e = event(TENANT);
        service.pushOutboxEvent(e);

        ArgumentCaptor<String> dest = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> msg = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(dest.capture(), msg.capture());

        assertEquals("/topic/tenant:" + TENANT + "/events", dest.getValue());
        Map<String, Object> message = (Map<String, Object>) msg.getValue();
        assertEquals("OUTBOX", message.get("type"));
        assertEquals(42L, message.get("eventId"));
        assertEquals("AssetCheckedOut", message.get("eventType"));
        assertEquals("INVENTORY_ITEM", message.get("aggregateType"));
        assertEquals(e.getAggregateId().toString(), message.get("aggregateId"));
        assertEquals("sp-1", message.get("spaceId"));
        assertNotNull(message.get("payload"));
        assertNotNull(message.get("timestamp"));
    }

    @Test
    void eventWithoutTenantIsNeverBroadcast() {
        service.pushOutboxEvent(event(null));
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void nullPayloadIsTolerated() {
        OutboxEvent e = event(TENANT);
        // payloadJson absent → agrège null, mais l'événement doit quand même passer.
        OutboxEvent noPayload = OutboxEvent.builder()
                .id(7L).tenantId(TENANT).aggregateType("Soul").aggregateId(null)
                .eventType("NewVisitor").build();
        service.pushOutboxEvent(noPayload);

        ArgumentCaptor<Object> msg = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tenant:" + TENANT + "/events"), msg.capture());
        Map<String, Object> message = (Map<String, Object>) msg.getValue();
        assertNull(message.get("aggregateId"));
        assertNull(message.get("spaceId"));
        assertEquals(7L, message.get("eventId"));
        assertNotNull(e);
    }
}
