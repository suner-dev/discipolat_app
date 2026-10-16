package com.discipolat.modules.succession.api;

import com.discipolat.modules.succession.domain.SuccessionPlan;
import com.discipolat.modules.succession.domain.SuccessionPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/succession")
public class SuccessionPlanController {

    private final SuccessionPlanService service;

    public SuccessionPlanController(SuccessionPlanService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SuccessionPlan>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessionPlan> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("@authz.can('SUCCESSION_EXECUTE', 'TENANT', null)")
    public ResponseEntity<SuccessionPlan> create(@RequestBody Map<String, String> body) {
        SuccessionPlan plan = service.create(
                UUID.fromString(body.get("candidatId")),
                body.get("rôleCible"),
                body.get("mentorId") != null ? UUID.fromString(body.get("mentorId")) : null,
                body.get("planFormation")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @PatchMapping("/{id}/readiness")
    @PreAuthorize("@authz.can('SUCCESSION_EXECUTE', 'TENANT', null)")
    public ResponseEntity<SuccessionPlan> updateReadiness(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateReadiness(id, body.get("readiness")));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.can('SUCCESSION_EXECUTE', 'TENANT', null)")
    public ResponseEntity<SuccessionPlan> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatut(id, body.get("statut")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can('SUCCESSION_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
