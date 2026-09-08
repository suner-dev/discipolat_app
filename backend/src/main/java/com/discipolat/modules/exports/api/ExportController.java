package com.discipolat.modules.exports.api;

import com.discipolat.modules.exports.domain.ExportRequest;
import com.discipolat.modules.exports.domain.ExportResult;
import com.discipolat.modules.exports.domain.ExportService;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {

    private final ExportService exportService;
    private final SecurityUtils securityUtils;

    public ExportController(ExportService exportService, SecurityUtils securityUtils) {
        this.exportService = exportService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExportResult> createExport(@RequestBody ExportRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        ExportResult result = exportService.createExport(request, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{exportId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExportResult> getExportStatus(@PathVariable UUID exportId) {
        ExportResult result = exportService.getExportStatus(exportId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExportResult>> getExportHistory() {
        UUID userId = securityUtils.getCurrentUserId();
        List<ExportResult> history = exportService.getExportHistory(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/download/{exportId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> downloadExport(@PathVariable UUID exportId) {
        ExportResult result = exportService.getExportStatus(exportId);
        if (result == null || !"COMPLETED".equals(result.getStatus())) {
            return ResponseEntity.notFound().build();
        }

        ExportRequest request = new ExportRequest();
        request.setType(result.getType());
        request.setFormat(result.getFormat());

        byte[] fileData = exportService.generateExportFile(request);
        ByteArrayResource resource = new ByteArrayResource(fileData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(exportService.getMimeType(result.getFormat())));
        headers.setContentDispositionFormData("attachment", result.getFileName());
        headers.setContentLength(fileData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getExportTypes() {
        List<String> types = java.util.Arrays.stream(com.discipolat.modules.exports.domain.ExportType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/formats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getExportFormats() {
        return ResponseEntity.ok(List.of("csv", "excel", "pdf", "json", "zip"));
    }
}