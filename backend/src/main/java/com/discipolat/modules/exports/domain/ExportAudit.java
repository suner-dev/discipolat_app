package com.discipolat.modules.exports.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportAudit {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "export_type", nullable = false, length = 100)
    private String exportType;

    @Column(name = "export_format", nullable = false, length = 20)
    private String exportFormat;

    @Column(name = "filters_json", columnDefinition = "JSONB")
    private String filtersJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "columns_exported", columnDefinition = "jsonb")
    private String[] columnsExported;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}