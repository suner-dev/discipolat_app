package com.discipolat.modules.dataMigration.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * G4.6 — Trace d'une session de migration legacy (dry-run, migration réelle ou rollback).
 * Jamais destructeur : les données sources ne sont ni déplacées ni effacées.
 */
@Entity
@Table(name = "migration_job")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MigrationJob {

    public enum Mode { DRY_RUN, MIGRATE, ROLLBACK }
    public enum Status { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Module migré : PEOPLES, SPACES, EVENTS (cf. catalogue LegacyMigrationService.MAPS). */
    @Column(name = "module_code", nullable = false, length = 50)
    private String moduleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "rows_seen", nullable = false)
    private int rowsSeen;

    @Column(name = "rows_migrated", nullable = false)
    private int rowsMigrated;

    @Column(name = "rows_merged", nullable = false)
    private int rowsMerged;

    @Column(name = "rows_skipped", nullable = false)
    private int rowsSkipped;

    @Column(name = "rows_conflicts", nullable = false)
    private int rowsConflicts;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "report_json", columnDefinition = "jsonb")
    private Map<String, Object> reportJson = new LinkedHashMap<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = Status.PENDING;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getRowsSeen() { return rowsSeen; }
    public void setRowsSeen(int rowsSeen) { this.rowsSeen = rowsSeen; }
    public int getRowsMigrated() { return rowsMigrated; }
    public void setRowsMigrated(int rowsMigrated) { this.rowsMigrated = rowsMigrated; }
    public int getRowsMerged() { return rowsMerged; }
    public void setRowsMerged(int rowsMerged) { this.rowsMerged = rowsMerged; }
    public int getRowsSkipped() { return rowsSkipped; }
    public void setRowsSkipped(int rowsSkipped) { this.rowsSkipped = rowsSkipped; }
    public int getRowsConflicts() { return rowsConflicts; }
    public void setRowsConflicts(int rowsConflicts) { this.rowsConflicts = rowsConflicts; }
    public Map<String, Object> getReportJson() { return reportJson; }
    public void setReportJson(Map<String, Object> reportJson) { this.reportJson = reportJson; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
