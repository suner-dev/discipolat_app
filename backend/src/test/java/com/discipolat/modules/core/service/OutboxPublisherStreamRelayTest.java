package com.discipolat.modules.core.service;

import com.discipolat.modules.core.domain.OutboxEvent;
import com.discipolat.modules.core.repository.OutboxEventRepository;
import com.discipolat.modules.core.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * §G5.8 — Le relais stream doit partir dès le commit (garantie < 5 s, sans
 * attendre le poll du dispatcher durable) et ne JAMAIS casser la persistance
 * outbox si la diffusion temps réel échoue (best-effort, la file durable reste
 * la garantie de non-perte).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutboxPublisherStreamRelayTest {

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private ProcessedEventRepository processedRepository;

    private OutboxPublisher publisher;
    private static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxRepository, processedRepository);
        when(outboxRepository.saveAndFlush(any(OutboxEvent.class))).thenAnswer(inv -> {
            OutboxEvent e = inv.getArgument(0);
            e.setId(System.nanoTime());
            return e;
        });
    }

    @Test
    void relayFiresImmediatelyWithSavedEvent() {
        List<OutboxEvent> received = new ArrayList<>();
        publisher.addStreamRelay(received::add);

        publisher.publish(TENANT, "Soul", UUID.randomUUID(), "NewVisitor", Map.of("name", "A"));

        assertEquals(1, received.size());
        assertEquals("NewVisitor", received.get(0).getEventType());
        assertEquals(TENANT, received.get(0).getTenantId());
        assertNotNull(received.get(0).getId(), "le relais doit recevoir l'entité SAUVÉE (id poll)");
    }

    @Test
    void relayFailureDoesNotBreakPersistence() {
        publisher.addStreamRelay(e -> {
            throw new IllegalStateException("broker down");
        });

        assertDoesNotThrow(() ->
                publisher.publish(TENANT, "Asset", UUID.randomUUID(), "AssetCheckedOut", Map.of()));

        // L'événement est bien en base : la garantie de diffusion durable tient.
        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).saveAndFlush(saved.capture());
        assertEquals("PENDING", saved.getValue().getStatus());
    }

    @Test
    void allRelaysAreInvokedEvenIfOneFails() {
        List<String> order = new ArrayList<>();
        publisher.addStreamRelay(e -> order.add("first"));
        publisher.addStreamRelay(e -> {
            order.add("boom");
            throw new RuntimeException("nope");
        });
        publisher.addStreamRelay(e -> order.add("third"));

        publisher.publish(TENANT, "Space", UUID.randomUUID(), "SpaceConfigChanged", Map.of());

        assertEquals(List.of("first", "boom", "third"), order);
    }
}
