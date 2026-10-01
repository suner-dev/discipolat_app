package com.discipolat.modules.lowband.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** §G5.9 — Journal des interactions du portail basse connexion (WhatsApp / USSD). */
@Entity
@Table(name = "lowband_interaction")
@Getter
@Setter
public class LowBandInteraction {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** WHATSAPP | USSD */
    @Column(name = "channel", nullable = false, length = 16)
    private String channel;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    /** TENUE | PLANNING | DON | PRESENCE | PRIERE | NOTIFICATIONS | AIDE */
    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
