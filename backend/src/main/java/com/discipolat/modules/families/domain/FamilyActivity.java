package com.discipolat.modules.families.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "family_activity", indexes = {
    @Index(name = "idx_fa_family", columnList = "family_id"),
    @Index(name = "idx_fa_date", columnList = "activity_date"),
    @Index(name = "idx_fa_ref", columnList = "reference_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PLANNED";

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