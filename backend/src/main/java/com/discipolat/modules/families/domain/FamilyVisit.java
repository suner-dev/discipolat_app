package com.discipolat.modules.families.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_visit", indexes = {
    @Index(name = "idx_fv_family", columnList = "family_id"),
    @Index(name = "idx_fv_soul", columnList = "soul_id"),
    @Index(name = "idx_fv_faiseur", columnList = "faiseur_id"),
    @Index(name = "idx_fv_date", columnList = "visit_date"),
    @Index(name = "idx_fv_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "soul_id", nullable = false)
    private UUID soulId;

    @Column(name = "faiseur_id", nullable = false)
    private UUID faiseurId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visit_type", nullable = false, length = 50)
    private String visitType;

    @Column(name = "subject", columnDefinition = "text")
    private String subject;

    @Column(name = "report", columnDefinition = "text")
    private String report;

    @Column(name = "decisions", columnDefinition = "text")
    private String decisions;

    @Column(name = "next_action_date")
    private LocalDate nextActionDate;

    @Column(name = "next_action_type", length = 100)
    private String nextActionType;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PLANNED";

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