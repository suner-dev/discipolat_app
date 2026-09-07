package com.discipolat.modules.ussd.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler — nettoie les sessions USSD inactives (plus de 10 minutes sans activité).
 */
@Component
public class UssdSessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(UssdSessionCleanupScheduler.class);

    private final UssdService ussdService;

    public UssdSessionCleanupScheduler(UssdService ussdService) {
        this.ussdService = ussdService;
    }

    /**
     * Toutes les 5 minutes, termine les sessions inactives depuis plus de 10 minutes.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void cleanupInactiveSessions() {
        try {
            ussdService.cleanupInactiveSessions();
        } catch (Exception e) {
            log.error("[USSD] Erreur cleanup sessions", e);
        }
    }
}
