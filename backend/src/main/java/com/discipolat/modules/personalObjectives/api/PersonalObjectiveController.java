package com.discipolat.modules.personalObjectives.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.personalObjectives.domain.PersonalObjective;
import com.discipolat.modules.personalObjectives.domain.PersonalObjectiveService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/personal-objectives")
public class PersonalObjectiveController {

    private final PersonalObjectiveService service;

    public PersonalObjectiveController(PersonalObjectiveService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PersonalObjective>> list() {
        return ResponseEntity.ok(service.listByMember(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonalObjective> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonalObjective> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String deadlineStr = (String) body.get("deadline");
        LocalDateTime deadline = deadlineStr != null ? LocalDateTime.parse(deadlineStr) : null;
        PersonalObjective obj = service.create(userId, (String) body.get("titre"),
                (String) body.get("description"), (String) body.get("catégorie"),
                body.get("objectifCible") != null ? (int) body.get("objectifCible") : 1, deadline);
        return ResponseEntity.status(HttpStatus.CREATED).body(obj);
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonalObjective> progress(@PathVariable UUID id) {
        return ResponseEntity.ok(service.progress(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PersonalObjective> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatut(id, body.get("statut")));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats(SecurityUtils.getCurrentUserId()));
    }
}
