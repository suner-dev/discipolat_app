package com.discipolat.modules.dataMigration.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P3 #101 — Assistant de migration de données.
 * Job de migration depuis Excel/CSV/autres logiciels avec mapping intelligent des champs.
 */
@Entity
@Table(name = "data_migration_jobs")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DataMigrationJob {

    public enum SourceType { EXCEL, CSV, GOOGLE_SHEETS, AUTRE_LOGICIEL }
    public enum Status { MAPPING, IMPORTING, COMPLETED, FAILED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType = SourceType.CSV;

    /** Cible d'import : SOULS, MEMBERS, FAMILIES, EVENTS, FINANCES */
    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.MAPPING;

    /** Mapping JSON : { "colonneSource": "champCible", ... } */
    @Column(name = "field_mapping", columnDefinition = "TEXT")
    private String fieldMapping;

    private int totalRows;
    private int importedRows;
    private int errorRows;

    @Column(columnDefinition = "TEXT")
    private String errorsLog;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getFieldMapping() { return fieldMapping; }
    public void setFieldMapping(String fieldMapping) { this.fieldMapping = fieldMapping; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getImportedRows() { return importedRows; }
    public void setImportedRows(int importedRows) { this.importedRows = importedRows; }
    public int getErrorRows() { return errorRows; }
    public void setErrorRows(int errorRows) { this.errorRows = errorRows; }
    public String getErrorsLog() { return errorsLog; }
    public void setErrorsLog(String errorsLog) { this.errorsLog = errorsLog; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
