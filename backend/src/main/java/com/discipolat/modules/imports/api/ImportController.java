package com.discipolat.modules.imports.api;

import com.discipolat.modules.imports.domain.ImportResult;
import com.discipolat.modules.imports.domain.ImportService;
import com.discipolat.modules.imports.domain.ImportType;
import com.discipolat.modules.imports.domain.ImportValidationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/validate/{type}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportValidationResult> validate(
            @PathVariable ImportType type,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(importService.validate(type, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ImportValidationResult.builder()
                            .success(false)
                            .totalRows(0)
                            .validRows(0)
                            .invalidRows(0)
                            .errors(List.of(new com.discipolat.modules.imports.domain.ImportValidationError(
                                    0, "file", e.getMessage(), ""
                            )))
                            .build()
            );
        }
    }

    @PostMapping(value = "/{type}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportResult> importData(
            @PathVariable ImportType type,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(importService.importData(type, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ImportResult.builder()
                            .imported(0)
                            .skipped(0)
                            .errors(List.of(e.getMessage()))
                            .build()
            );
        }
    }
}