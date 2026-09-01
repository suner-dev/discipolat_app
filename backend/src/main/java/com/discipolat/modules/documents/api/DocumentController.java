package com.discipolat.modules.documents.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Gestion des documents (pièces jointe, rapports, exports).
 * - GET /api/v1/documents → lister les documents
 */
@RestController
@RequestMapping("/api/v1/documents")
@PreAuthorize("isAuthenticated()")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        log.debug("[Documents] list documents");
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        log.debug("[Documents] get document {}", id);
        return ResponseEntity.ok(Map.of("id", id, "name", "document-" + id));
    }
}
