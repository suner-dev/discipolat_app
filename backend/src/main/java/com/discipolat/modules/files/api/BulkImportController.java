package com.discipolat.modules.files.api;

import com.discipolat.modules.files.domain.BulkImportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/import")
public class BulkImportController {

    private final BulkImportService bulkImportService;
    private final BulkImportValidator validator;

    public BulkImportController(BulkImportService bulkImportService, BulkImportValidator validator) {
        this.bulkImportService = bulkImportService;
        this.validator = validator;
    }

    // ======================== VALIDATION (preview) ========================

    /**
     * Valide les données CSV sans les importer.
     * Retourne les erreurs par ligne pour affichage côté UI.
     */
    @PostMapping("/validate/families")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<BulkImportValidationResult> validateFamilies(
            @RequestBody List<Map<String, String>> rows) {
        List<BulkImportRow> importRows = toImportRows(rows);
        List<BulkImportValidationResult.RowError> errors = validator.validateFamilies(importRows);
        return ResponseEntity.ok(buildResult(rows.size(), errors));
    }

    @PostMapping("/validate/users")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<BulkImportValidationResult> validateUsers(
            @RequestBody List<Map<String, String>> rows) {
        List<BulkImportRow> importRows = toImportRows(rows);
        List<BulkImportValidationResult.RowError> errors = validator.validateUsers(importRows);
        return ResponseEntity.ok(buildResult(rows.size(), errors));
    }

    @PostMapping("/validate/souls")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<BulkImportValidationResult> validateSouls(
            @RequestBody List<Map<String, String>> rows) {
        List<BulkImportRow> importRows = toImportRows(rows);
        List<BulkImportValidationResult.RowError> errors = validator.validateSouls(importRows);
        return ResponseEntity.ok(buildResult(rows.size(), errors));
    }

    // ======================== IMPORT ========================

    @PostMapping("/families")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> importFamilies(@RequestBody List<Map<String, String>> families) {
        // Pre-validation
        List<BulkImportRow> importRows = toImportRows(families);
        List<BulkImportValidationResult.RowError> errors = validator.validateFamilies(importRows);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation échouée",
                "totalRows", families.size(),
                "invalidRows", (int) errors.stream().map(BulkImportValidationResult.RowError::rowNumber).distinct().count(),
                "errors", errors
            ));
        }
        return ResponseEntity.ok(bulkImportService.importFamilies(families));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> importUsers(@RequestBody List<Map<String, String>> users) {
        // Pre-validation
        List<BulkImportRow> importRows = toImportRows(users);
        List<BulkImportValidationResult.RowError> errors = validator.validateUsers(importRows);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation échouée",
                "totalRows", users.size(),
                "invalidRows", (int) errors.stream().map(BulkImportValidationResult.RowError::rowNumber).distinct().count(),
                "errors", errors
            ));
        }
        return ResponseEntity.ok(bulkImportService.importUsers(users));
    }

    @PostMapping("/souls")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> importSouls(@RequestBody List<Map<String, String>> souls) {
        // Pre-validation
        List<BulkImportRow> importRows = toImportRows(souls);
        List<BulkImportValidationResult.RowError> errors = validator.validateSouls(importRows);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation échouée",
                "totalRows", souls.size(),
                "invalidRows", (int) errors.stream().map(BulkImportValidationResult.RowError::rowNumber).distinct().count(),
                "errors", errors
            ));
        }
        return ResponseEntity.ok(bulkImportService.importSouls(souls));
    }

    // ======================== HELPERS ========================

    private List<BulkImportRow> toImportRows(List<Map<String, String>> rows) {
        return IntStream.range(0, rows.size())
                .mapToObj(i -> new BulkImportRow(i + 1, rows.get(i)))
                .toList();
    }

    private BulkImportValidationResult buildResult(int totalRows,
                                                     List<BulkImportValidationResult.RowError> errors) {
        if (errors.isEmpty()) {
            return BulkImportValidationResult.success(totalRows);
        }
        int invalidCount = (int) errors.stream()
                .map(BulkImportValidationResult.RowError::rowNumber)
                .distinct().count();
        return new BulkImportValidationResult(
                false, totalRows, totalRows - invalidCount, invalidCount, errors);
    }
}
