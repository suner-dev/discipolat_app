package com.discipolat.modules.webhooks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, UUID> {
    List<WebhookDeliveryLog> findTop50ByOrderByCreatedAtDesc();
    List<WebhookDeliveryLog> findByWebhookIdOrderByCreatedAtDesc(UUID webhookId);
}
