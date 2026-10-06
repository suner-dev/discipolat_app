package com.discipolat.modules.discipleship.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disciple_progress_requirements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DiscipleProgressRequirement {

    public enum RequirementStatus { PENDING, IN_PROGRESS, COMPLETED, VERIFIED, WAIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "progress_id", nullable = false)
    private Long progressId;

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RequirementStatus status = RequirementStatus.PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "verified_by")
    private UUID verifiedBy;
}
