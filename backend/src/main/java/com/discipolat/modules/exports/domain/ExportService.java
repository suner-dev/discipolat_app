package com.discipolat.modules.exports.domain;

import java.util.List;
import java.util.UUID;

public interface ExportService {
    ExportResult createExport(ExportRequest request, UUID userId);
    ExportResult getExportStatus(UUID exportId);
    List<ExportResult> getExportHistory(UUID userId);
    byte[] generateExportFile(ExportRequest request);
    String getMimeType(String format);
    String getFileExtension(String format);
}