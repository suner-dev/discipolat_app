package com.discipolat.modules.exports.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResult {
    private UUID exportId;
    private ExportType type;
    private String format;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private Long totalRecords;
    private String downloadUrl;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
    private Long fileSize;
    private String fileName;
}