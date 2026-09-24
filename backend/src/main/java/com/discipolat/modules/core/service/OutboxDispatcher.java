package com.discipolat.modules.core.service;

import com.discipolat.modules.core.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcher {

    private final OutboxPublisher outboxPublisher;
    private final OutboxEventRepository outboxEventRepository;

    /** Polling toutes les 5 secondes, avec un batch maximal de 100 événements. */
    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        try {
            int processed = outboxPublisher.processPendingEvents(100);
            if (processed > 0) {
                log.debug("Outbox: {} événements traités", processed);
            }
        } catch (Exception e) {
            log.error("Erreur lors du dispatch outbox: {}", e.getMessage(), e);
        }
    }

    /** Supprime uniquement les événements publiés depuis plus de 30 jours. */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupPublishedEvents() {
        try {
            int deleted = outboxEventRepository.deletePublishedBefore(OffsetDateTime.now().minusDays(30));
            if (deleted > 0) {
                log.info("Suppression de {} événements outbox publiés depuis plus de 30 jours", deleted);
            }
        } catch (Exception e) {
            log.error("Erreur nettoyage outbox: {}", e.getMessage(), e);
        }
    }
}
