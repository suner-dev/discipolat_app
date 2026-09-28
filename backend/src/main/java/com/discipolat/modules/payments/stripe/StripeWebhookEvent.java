package com.discipolat.modules.payments.stripe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Journal d'idempotence des événements webhook Stripe (event.id = clé).
 * Garantit qu'un événement reçu deux fois (retry Stripe) n'est traité
 * qu'une seule fois.
 */
@Entity
@Table(name = "stripe_webhook_events")
public class StripeWebhookEvent {

    @Id
    @Column(name = "event_id", length = 80)
    private String eventId;

    @Column(name = "type", nullable = false, length = 80)
    private String type;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error")
    private String error;

    public StripeWebhookEvent() {
    }

    public StripeWebhookEvent(String eventId, String type) {
        this.eventId = eventId;
        this.type = type;
        this.receivedAt = Instant.now();
        this.status = "RECEIVED";
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
