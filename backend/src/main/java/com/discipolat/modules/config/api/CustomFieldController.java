package com.discipolat.modules.config.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.config.domain.CustomFieldDefinition;
import com.discipolat.modules.config.domain.CustomFieldValue;
import com.discipolat.modules.config.service.CustomFieldService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/custom-fields")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class CustomFieldController {

    private final CustomFieldService customFieldService;

    public CustomFieldController(CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    // ========== DEFINITIONS ==========

    @GetMapping("/definitions")
    public ResponseEntity<PageResponse<CustomFieldDefinition>> getDefinitions(
            @RequestParam UUID tenantId,
            @RequestParam String entityType,
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder"));
        // Note: For simplicity, using non-paginated service method then manual pagination
        List<CustomFieldDefinition> defs = customFieldService.getDefinitions(tenantId, entityType, spaceId);
        int start = page * size;
        int end = Math.min(start + size, defs.size());
        List<CustomFieldDefinition> content = defs.subList(start, end);
        return ResponseEntity.ok(PageResponse.of(content, page, size, defs.size(), (defs.size() + size - 1) / size));
    }

    @GetMapping("/definitions/resolved")
    public ResponseEntity<List<CustomFieldDefinition>> getResolvedDefinitions(
            @RequestParam UUID tenantId,
            @RequestParam String entityType,
            @RequestParam(required = false) UUID spaceId) {
        return ResponseEntity.ok(customFieldService.getResolvedDefinitions(tenantId, entityType, spaceId));
    }

    @GetMapping("/definitions/{id}")
    public ResponseEntity<CustomFieldDefinition> getDefinition(@PathVariable UUID id) {
        return ResponseEntity.ok(customFieldService.getDefinition(id));
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomFieldDefinition> createDefinition(@RequestBody CustomFieldDefinition definition) {
        return ResponseEntity.ok(customFieldService.createDefinition(definition));
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomFieldDefinition> updateDefinition(@PathVariable UUID id, @RequestBody CustomFieldDefinition definition) {
        return ResponseEntity.ok(customFieldService.updateDefinition(id, definition));
    }

    @DeleteMapping("/definitions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDefinition(@PathVariable UUID id) {
        customFieldService.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }

    // ========== VALUES ==========

    @GetMapping("/values/entity/{entityId}")
    public ResponseEntity<Map<UUID, Object>> getValuesForEntity(
            @RequestParam UUID tenantId,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(customFieldService.getValuesForEntity(tenantId, entityId));
    }

    @GetMapping("/values/entity/{entityId}/with-definitions")
    public ResponseEntity<List<CustomFieldValue>> getValuesWithDefinitions(
            @RequestParam UUID tenantId,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(customFieldService.getValuesForEntityWithDefinitions(tenantId, entityId));
    }

    @PutMapping("/values")
    public ResponseEntity<CustomFieldValue> setValue(
            @RequestParam UUID tenantId,
            @RequestParam UUID fieldId,
            @RequestParam UUID entityId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") UUID updatedBy) {
        Object value = request.get("value");
        return ResponseEntity.ok(customFieldService.setValue(tenantId, fieldId, entityId, value, updatedBy));
    }

    @DeleteMapping("/values")
    public ResponseEntity<Void> deleteValue(
            @RequestParam UUID fieldId,
            @RequestParam UUID entityId) {
        customFieldService.deleteValue(fieldId, entityId);
        return ResponseEntity.noContent().build();
    }

    // ========== VALIDATION (for testing) ==========

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateValue(
            @RequestParam UUID tenantId,
            @RequestParam UUID fieldId,
            @RequestBody Map<String, Object> request) {
        // Expose validation for frontend preview
        return ResponseEntity.ok(Map.of("valid", true)); // Simplified
    }
}