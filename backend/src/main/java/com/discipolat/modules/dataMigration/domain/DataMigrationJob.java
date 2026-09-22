package com.discipolat.modules.dataMigration.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P3 #101 — Assistant de migration de données.
 * Job de migration depuis Excel/CSV/autres logiciels avec mapping intelligent des champs.
 * G4.6 — entités créées par ce job (UUIDs séparés par des virgules), pour rollback.
 */
@Entity
@Table(name = "data_migration_jobs")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@lombok.Getter
@lombok.Setter
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class DataMigrationJob {

    public enum SourceType { EXCEL, CSV, GOOGLE_SHEETS, AUTRE_LOGICIEL }
    public enum Status { MAPPING, IMPORTING, COMPLETED, FAILED, CANCELLED, ROLLED_BACK }

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

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_entity_ids", columnDefinition = "TEXT")
    private String createdEntityIds;

    @Column(name = "last_run_dry")
    private Boolean lastRunDry = false;

    @Column(name = "rolled_back_at")
    private LocalDateTime rolledBackAt;
}
