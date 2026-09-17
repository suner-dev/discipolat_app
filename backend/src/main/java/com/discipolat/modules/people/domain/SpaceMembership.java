package com.discipolat.modules.people.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "space_membership", indexes = {
    @Index(name = "idx_sm_space", columnList = "space_id"),
    @Index(name = "idx_sm_person", columnList = "person_id"),
    @Index(name = "idx_sm_status", columnList = "status"),
    @Index(name = "idx_sm_tenant", columnList = "tenant_id"),
    @Index(name = "uk_sm_person_space", columnList = "person_id, space_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "membership_type", nullable = false, length = 30)
    private String membershipType = "MEMBER";

    @Column(name = "responsibility", columnDefinition = "text")
    private String responsibility;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}