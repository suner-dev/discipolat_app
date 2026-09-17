package com.discipolat.modules.people.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "membership", indexes = {
    @Index(name = "idx_membership_tenant", columnList = "tenant_id"),
    @Index(name = "idx_membership_person", columnList = "person_id"),
    @Index(name = "idx_membership_status", columnList = "membership_status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "membership_status", nullable = false, length = 30)
    private String membershipStatus = "MEMBRE";

    @Column(name = "joined_at", nullable = false)
    private LocalDate joinedAt;

    @Column(name = "left_at")
    private LocalDate leftAt;

    @Column(name = "source", length = 30)
    private String source;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.joinedAt == null) this.joinedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}