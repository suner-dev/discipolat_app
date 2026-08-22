package com.discipolat.modules.webhooks.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/** Webhook configuré : URL de réception + événements écoutés + secret HMAC. */
@Entity
@Table(name = "webhook_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class WebhookRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "secret", nullable = false)
    private String secret;

    /** Événements écoutés, séparés par virgule. « * » = tous. */
    @Column(name = "events", nullable = false)
    @Builder.Default
    private String events = "*";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean listensTo(String eventType) {
        if ("*".equals(events)) return true;
        return Arrays.stream(events.split(","))
                .map(String::trim)
                .anyMatch(e -> e.equalsIgnoreCase(eventType));
    }
}
