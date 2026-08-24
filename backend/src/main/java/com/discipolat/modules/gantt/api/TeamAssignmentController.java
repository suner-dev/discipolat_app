package com.discipolat.modules.gantt.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.gantt.domain.TeamAssignment;
import com.discipolat.modules.gantt.domain.TeamAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team-gantt")
public class TeamAssignmentController {

    private final TeamAssignmentService service;

    public TeamAssignmentController(TeamAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TeamAssignment>> list(
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(service.listByRange(LocalDateTime.parse(start), LocalDateTime.parse(end)));
    }

    @GetMapping("/team/{equipeId}")
    public ResponseEntity<List<TeamAssignment>> listByTeam(@PathVariable UUID equipeId) {
        return ResponseEntity.ok(service.listByTeam(equipeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamAssignment> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamAssignment> create(@RequestBody Map<String, Object> body) {
        TeamAssignment assignment = service.create(
                UUID.fromString((String) body.get("equipeId")),
                body.get("evenementId") != null ? UUID.fromString((String) body.get("evenementId")) : null,
                (String) body.get("role"),
                body.get("membreId") != null ? UUID.fromString((String) body.get("membreId")) : null,
                LocalDateTime.parse((String) body.get("debut")),
                LocalDateTime.parse((String) body.get("fin")),
                (String) body.get("notes")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamAssignment> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatut(id, body.get("statut")));
    }

    @GetMapping("/overloads/{equipeId}")
    public ResponseEntity<Map<String, Object>> detectOverloads(
            @PathVariable UUID equipeId,
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(service.detectOverloads(equipeId, LocalDateTime.parse(start), LocalDateTime.parse(end)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
