package com.discipolat.modules.rewards.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P3 #108 — Récompenses avancées : certificats et mentions honorifiques tangibles.
 */
@Entity
@Table(name = "reward_certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipient_name")
    private String recipientName;

    /** Titre du certificat (ex: Faiseur fidèle). */
    @Column(nullable = false)
    private String title;

    /** Mention : HONNEUR, EXCELLENCE, FIDELITE, ENCOURAGEMENT. */
    private String mention;

    private String description;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMention() { return mention; }
    public void setMention(String mention) { this.mention = mention; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}
