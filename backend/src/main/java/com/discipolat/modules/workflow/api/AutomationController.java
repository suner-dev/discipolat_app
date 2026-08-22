package com.discipolat.modules.workflow.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.workflow.domain.Automation;
import com.discipolat.modules.workflow.domain.AutomationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/automations")
public class AutomationController {

    private final AutomationService service;

    public AutomationController(AutomationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Automation>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Automation> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Automation> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Automation a = service.create(
                (String) body.get("nom"),
                (String) body.getOrDefault("description", ""),
                (String) body.get("triggerType"),
                (String) body.getOrDefault("triggerConfig", "{}"),
                (String) body.get("actionType"),
                (String) body.getOrDefault("actionConfig", "{}"),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(a);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Automation> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(service.toggleStatut(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
