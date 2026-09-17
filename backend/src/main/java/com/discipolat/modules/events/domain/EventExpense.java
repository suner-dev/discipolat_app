package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_expense", indexes = {
    @Index(name = "idx_ee_church_event", columnList = "church_event_id"),
    @Index(name = "idx_ee_expense", columnList = "expense_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "church_event_id", nullable = false)
    private UUID churchEventId;

    @Column(name = "expense_id", nullable = false)
    private UUID expenseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}