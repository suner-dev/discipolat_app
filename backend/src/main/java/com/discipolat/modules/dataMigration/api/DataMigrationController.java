package com.discipolat.modules.dataMigration.api;

import com.discipolat.modules.dataMigration.domain.DataMigrationJob;
import com.discipolat.modules.dataMigration.domain.DataMigrationResult;
import com.discipolat.modules.dataMigration.domain.DataMigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

        /** G4.6 — exécute l'import d'un fichier CSV multipart. */
    @PostMapping(value = "/{id}/execute", consumes = "multipart/form-data")
    public ResponseEntity<DataMigrationResult> execute(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        try {
            List<String[]> rows = parseCsv(file);
            return ResponseEntity.ok(service.execute(id, rows, dryRun));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(DataMigrationResult.error(id, e.getMessage()));
        }
    }

    /** G4.6 — replay : relit un fichier et ré‑exécute l'import. */
    @PostMapping(value = "/{id}/replay", consumes = "multipart/form-data")
    public ResponseEntity<DataMigrationResult> replay(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        try {
            List<String[]> rows = parseCsv(file);
            return ResponseEntity.ok(service.replay(id, rows));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(DataMigrationResult.error(id, e.getMessage()));
        }
    }

    /** G4.6 — rollback des entités créées par le job. */
    @DeleteMapping("/{id}")
    public ResponseEntity<DataMigrationResult> rollback(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(service.rollback(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(DataMigrationResult.error(id, e.getMessage()));
        }
    }

         private List<String[]> parseCsv(MultipartFile file) throws Exception {
        // G6.6 — garde-fous CSV : 5MB / 10000 lignes / 100000 chars par ligne / .csv uniquement.
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Fichier vide");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Fichier trop volumineux (max 5MB)");
        String dmName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!dmName.endsWith(".csv")) throw new IllegalArgumentException("Seuls les fichiers CSV sont acceptés");
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 100_000) throw new IllegalArgumentException("Ligne CSV trop longue");
                if (rows.size() >= 10_000) throw new IllegalArgumentException("Trop de lignes (max 10000)");
                if (line.trim().isEmpty() && rows.isEmpty()) continue;
                rows.add(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1));
            }
        }
        return rows;
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
