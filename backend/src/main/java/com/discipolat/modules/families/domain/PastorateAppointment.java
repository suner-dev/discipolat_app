package com.discipolat.modules.families.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pastorate_appointment", indexes = {
    @Index(name = "idx_pa_tenant", columnList = "tenant_id"),
    @Index(name = "idx_pa_pastor", columnList = "pastor_id"),
    @Index(name = "idx_pa_org_unit", columnList = "organization_unit_id"),
    @Index(name = "idx_pa_status", columnList = "status"),
    @Index(name = "idx_pa_start_date", columnList = "start_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PastorateAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "pastor_id", nullable = false)
    private UUID pastorId;

    @Column(name = "organization_unit_id", nullable = false)
    private UUID organizationUnitId;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "appointment_type", nullable = false, length = 50)
    private String appointmentType;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "previous_org_unit_id")
    private UUID previousOrgUnitId;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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