package com.discipolat.modules.whatsapp.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P0 #1 — Pont WhatsApp : rappels automatiques programmés.
 * Chaque rappel est lié à un événement ou un suivi et envoyé automatiquement.
 */
@Entity
@Table(name = "whatsapp_reminders")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class WhatsAppReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Événement ou suivi concerné */
    @Column(name = "reference_type", nullable = false)
    private String referenceType; // EVENT, FOLLOWUP, PRAYER

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    /** Numéro WhatsApp du destinataire */
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    /** Message de rappel */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Quand envoyer le rappel */
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    /** Quand le rappel a été effectivement envoyé */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** Statut : PENDING, SENT, FAILED, CANCELLED */
    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
