package com.discipolat.modules.dataMigration.api;

import com.discipolat.modules.dataMigration.domain.DataMigrationJob;
import com.discipolat.modules.dataMigration.domain.DataMigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P3 #101 — Assistant de migration de données.
 */
@RestController
@RequestMapping("/api/v1/data-migration")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
public class DataMigrationController {

    private final DataMigrationService service;

    public DataMigrationController(DataMigrationService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) body.getOrDefault("headers", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> sampleRows = (List<Map<String, String>>) body.getOrDefault("sampleRows", List.of());
        String targetType = (String) body.getOrDefault("targetType", "SOULS");
        return ResponseEntity.ok(service.analyze(targetType, headers, sampleRows));
    }

    @PostMapping
    public ResponseEntity<DataMigrationJob> create(@RequestBody DataMigrationJob job) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(job));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<DataMigrationJob> execute(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String mapping = body.get("fieldMapping") == null ? "{}" : body.get("fieldMapping").toString();
        int totalRows = body.containsKey("totalRows") ? ((Number) body.get("totalRows")).intValue() : 0;
        int importedRows = body.containsKey("importedRows") ? ((Number) body.get("importedRows")).intValue() : 0;
        String errorsLog = body.get("errorsLog") == null ? "" : body.get("errorsLog").toString();
        return ResponseEntity.ok(service.execute(id, mapping, totalRows, importedRows, errorsLog));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<DataMigrationJob>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataMigrationJob> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
