package com.discipolat.modules.core.service;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.core.domain.OutboxEvent;
import com.discipolat.modules.core.domain.ProcessedEvent;
import com.discipolat.modules.core.repository.OutboxEventRepository;
import com.discipolat.modules.core.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ProcessedEventRepository processedRepository;

    private final Map<String, Consumer<OutboxEvent>> consumers = new LinkedHashMap<>();

    /**
     * Enregistre un événement dans l'outbox (dans la MÊME transaction que la mutation métier).
     * Appelé par les services métier pour émettre des événements de domaine.
     */
    @Transactional
    public void publish(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        OutboxEvent event = OutboxEvent.builder()
                .tenantId(tenantId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payloadJson(payload)
                .status("PENDING")
                .attempts(0)
                .availableAt(OffsetDateTime.now())
                .build();

        outboxRepository.save(event);
    }

    /**
     * Enregistre un événement avec tenantId explicite (pour jobs système).
     */
    @Transactional
    public void publish(UUID tenantId, String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
        OutboxEvent event = OutboxEvent.builder()
                .tenantId(tenantId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payloadJson(payload)
                .status("PENDING")
                .attempts(0)
                .availableAt(OffsetDateTime.now())
                .build();

        outboxRepository.save(event);
    }

    /**
     * Enregistre un consommateur pour un type d'événement.
     */
    public void registerConsumer(String eventType, Consumer<OutboxEvent> consumer) {
        consumers.put(eventType, consumer);
    }

    /**
     * Traite les événements en attente (polling par le dispatcher).
     * Appelé périodiquement par un @Scheduled.
     */
    @Transactional
    public int processPendingEvents(int batchSize) {
        List<OutboxEvent> events = outboxRepository.findByStatusAndAvailableAtBeforeOrderByAvailableAtAsc(
                "PENDING", OffsetDateTime.now());

        int processed = 0;
        for (OutboxEvent event : events) {
            if (processed >= batchSize) break;

            if (processEvent(event)) {
                processed++;
            }
        }
        return processed;
    }

    /**
     * Traite un événement spécifique (pour retry manuel).
     */
    @Transactional
    public boolean processEvent(OutboxEvent event) {
        String consumerName = "default";

        // Vérifier idempotence
        if (processedRepository.existsByConsumerAndEventId(consumerName, event.getId())) {
            event.setStatus("DISCARDED");
            event.setPublishedAt(OffsetDateTime.now());
            outboxRepository.save(event);
            return true;
        }

        try {
            // Appeler le consommateur enregistré
            Consumer<OutboxEvent> consumer = consumers.get(event.getEventType());
            if (consumer != null) {
                consumer.accept(event);
            } else {
                // Pas de consommateur spécifique, logger seulement
                log.debug("Aucun consommateur pour l'événement: {}", event.getEventType());
            }

            // Marquer comme traité
            ProcessedEvent pe = new ProcessedEvent();
            pe.setConsumer(consumerName);
            pe.setEventId(event.getId());
            processedRepository.save(pe);

            event.setStatus("PUBLISHED");
            event.setPublishedAt(OffsetDateTime.now());
            event.setAttempts(event.getAttempts() + 1);
            outboxRepository.save(event);

            log.debug("Événement {} traité: {}", event.getEventType(), event.getId());
            return true;

        } catch (Exception e) {
            event.setAttempts(event.getAttempts() + 1);
            if (event.getAttempts() >= 5) {
                event.setStatus("FAILED");
                log.error("Événement {} échoué définitivement après {} tentatives: {}",
                        event.getEventType(), event.getAttempts(), e.getMessage());
            } else {
                // Retry exponentiel
                long delayMinutes = (long) Math.pow(2, event.getAttempts());
                event.setAvailableAt(OffsetDateTime.now().plusMinutes(delayMinutes));
            }
            outboxRepository.save(event);
            return false;
        }
    }

    /**
     * Rejoue les événements en échec (dead letter).
     */
    @Transactional
    public int replayFailedEvents() {
        List<OutboxEvent> failed = outboxRepository.findByStatusOrderByCreatedAtAsc("FAILED",
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();

        int replayed = 0;
        for (OutboxEvent event : failed) {
            event.setStatus("PENDING");
            event.setAttempts(0);
            event.setAvailableAt(OffsetDateTime.now());
            outboxRepository.save(event);
            replayed++;
        }
        return replayed;
    }
}