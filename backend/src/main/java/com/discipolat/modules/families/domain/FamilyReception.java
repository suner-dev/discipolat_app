package com.discipolat.modules.families.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_reception", indexes = {
    @Index(name = "idx_fr_family", columnList = "family_id"),
    @Index(name = "idx_fr_soul", columnList = "soul_id"),
    @Index(name = "idx_fr_date", columnList = "reception_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyReception {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "soul_id", nullable = false)
    private UUID soulId;

    @Column(name = "reception_date", nullable = false)
    private LocalDate receptionDate;

    @Column(name = "reception_type", nullable = false, length = 50)
    private String receptionType;

    @Column(name = "welcome_by")
    private UUID welcomeBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "assigned_faiseur_id")
    private UUID assignedFaiseurId;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}