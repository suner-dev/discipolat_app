package com.discipolat.modules.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcher {

    private final OutboxPublisher outboxPublisher;

    /**
     * Polling toutes les 5 secondes pour traiter les événements en attente.
     * Batch de 100 événements max par exécution.
     */
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

    /**
     * Nettoyage quotidien des événements publiés anciens (> 30 jours).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupPublishedEvents() {
        try {
            // TODO: Implémenter la suppression des événements publiés > 30 jours
            log.info("Nettoyage outbox événements publiés > 30 jours");
        } catch (Exception e) {
            log.error("Erreur nettoyage outbox: {}", e.getMessage(), e);
        }
    }
}