package com.discipolat.modules.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_event", indexes = {
    @Index(name = "idx_processed_event_event_id", columnList = "event_id"),
    @Index(name = "idx_processed_consumer", columnList = "consumer")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_processed_consumer_event", columnNames = {"consumer", "event_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer", nullable = false, length = 100)
    private String consumer;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.processedAt = OffsetDateTime.now();
    }
}