package com.discipolat.modules.familyResources.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.familyResources.domain.FamilyResource;
import com.discipolat.modules.familyResources.domain.FamilyResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/family-resources")
public class FamilyResourceController {

    private final FamilyResourceService service;

    public FamilyResourceController(FamilyResourceService service) {
        this.service = service;
    }

    @GetMapping("/family/{familleId}")
    public ResponseEntity<List<FamilyResource>> listByFamily(@PathVariable UUID familleId) {
        return ResponseEntity.ok(service.listByFamily(familleId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyResource> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FamilyResource> create(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        FamilyResource resource = service.create(
                UUID.fromString(body.get("familleId")),
                body.get("titre"),
                body.get("description"),
                body.get("type"),
                body.get("url"),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
