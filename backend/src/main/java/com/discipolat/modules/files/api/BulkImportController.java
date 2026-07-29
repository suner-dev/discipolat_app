package com.discipolat.modules.files.api;

import com.discipolat.modules.files.domain.BulkImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/import")
public class BulkImportController {

    private final BulkImportService bulkImportService;

    public BulkImportController(BulkImportService bulkImportService) {
        this.bulkImportService = bulkImportService;
    }

    @PostMapping("/families")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> importFamilies(@RequestBody List<Map<String, Object>> families) {
        return ResponseEntity.ok(bulkImportService.importFamilies(families));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> importUsers(@RequestBody List<Map<String, Object>> users) {
        return ResponseEntity.ok(bulkImportService.importUsers(users));
    }

    @PostMapping("/souls")
    @PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> importSouls(@RequestBody List<Map<String, Object>> souls) {
        return ResponseEntity.ok(bulkImportService.importSouls(souls));
    }
}
