package com.discipolat.modules.webhooks.domain;

import com.discipolat.common.infrastructure.propagation.EntityChangedEvent;
import com.discipolat.common.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Pont entre le système de propagation d'entités et les webhooks sortants.
 * Chaque mutation d'entité majeure déclenche automatiquement les webhooks
 * abonnés à l'événement correspondant (ex. "SOUL.CREATED", "EVENT.DELETED").
 */
@Component
public class WebhookEntityEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookEntityEventListener.class);

    private final WebhookService webhookService;

    public WebhookEntityEventListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEntityChanged(EntityChangedEvent event) {
        if (event.getTenantId() == null) {
            log.warn("Événement {} sans tenant ignoré pour les webhooks", event.getEntityType());
            return;
        }
        TenantContext.runAsTenant(event.getTenantId(), () -> dispatch(event));
    }

    private void dispatch(EntityChangedEvent event) {
        try {
            String eventType = event.getEntityType() + "." + event.getChangeType().name();
            Map<String, Object> payload = new HashMap<>();
            payload.put("entityType", event.getEntityType());
            payload.put("entityId", event.getEntityId());
            payload.put("changeType", event.getChangeType().name());
            if (event.getNewValues() != null && !event.getNewValues().isEmpty()) {
                payload.put("newValues", event.getNewValues());
            }
            if (event.getOldValues() != null && !event.getOldValues().isEmpty()) {
                payload.put("oldValues", event.getOldValues());
            }
            payload.put("actorId", event.getActorId());
            payload.put("description", event.getDescription());
            int notified = webhookService.fire(eventType, payload);
            if (notified > 0) {
                log.debug("Webhook {} notifié à {} abonné(s)", eventType, notified);
            }
        } catch (Exception e) {
            log.warn("Déclenchement webhook échoué : {}", e.getMessage());
        }
    }
}
