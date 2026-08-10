package com.discipolat.modules.customfields.api;

import com.discipolat.modules.customfields.domain.CustomFieldDefinition;
import com.discipolat.modules.customfields.domain.CustomFieldService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/custom-fields")
public class CustomFieldController {

    private final CustomFieldService service;

    public CustomFieldController(CustomFieldService service) {
        this.service = service;
    }

    /* ======================== Définitions ======================== */

    @GetMapping("/definitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CustomFieldDefinition>> getDefinitions(
            @RequestParam String entiteType) {
        return ResponseEntity.ok(service.getDefinitions(entiteType));
    }

    @GetMapping("/definitions/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomFieldDefinition>> getAllDefinitions(
            @RequestParam(required = false, defaultValue = "SOUL") String entiteType) {
        return ResponseEntity.ok(service.getAllDefinitions(entiteType));
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomFieldDefinition> createDefinition(@RequestBody CustomFieldDefinition def) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDefinition(def));
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomFieldDefinition> updateDefinition(
            @PathVariable UUID id, @RequestBody CustomFieldDefinition def) {
        return ResponseEntity.ok(service.updateDefinition(id, def));
    }

    @DeleteMapping("/definitions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDefinition(@PathVariable UUID id) {
        service.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }

    /* ======================== Bundle (valeurs) ======================== */

    @GetMapping("/{entiteType}/{entiteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBundle(
            @PathVariable String entiteType, @PathVariable UUID entiteId) {
        return ResponseEntity.ok(service.getBundle(entiteType, entiteId));
    }

    @PutMapping("/{entiteType}/{entiteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveValues(
            @PathVariable String entiteType,
            @PathVariable UUID entiteId,
            @RequestBody Map<String, String> values) {
        service.saveValues(entiteType, entiteId, values);
        return ResponseEntity.ok().build();
    }
}