package com.discipolat.modules.gantt.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.gantt.domain.TeamTask;
import com.discipolat.modules.gantt.domain.TeamTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team-tasks")
public class TeamTaskController {

    private final TeamTaskService service;

    public TeamTaskController(TeamTaskService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<TeamTask>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamTask> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<TeamTask> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TeamTask task = service.create(
                (String) body.get("titre"),
                (String) body.getOrDefault("description", ""),
                (String) body.getOrDefault("priorite", "MOYENNE"),
                LocalDate.parse((String) body.get("dateDebut")),
                LocalDate.parse((String) body.get("dateFin")),
                body.get("assigneA") != null ? UUID.fromString((String) body.get("assigneA")) : null,
                body.get("departmentId") != null ? UUID.fromString((String) body.get("departmentId")) : null,
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<TeamTask> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatut(id, body.get("statut")));
    }

    @PatchMapping("/{id}/progression")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<TeamTask> updateProgression(@PathVariable UUID id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(service.updateProgression(id, body.getOrDefault("progression", 0)));
    }
}
