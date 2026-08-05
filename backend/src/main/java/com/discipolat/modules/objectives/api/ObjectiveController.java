package com.discipolat.modules.objectives.api;

import com.discipolat.modules.objectives.domain.Objective;
import com.discipolat.modules.objectives.domain.ObjectiveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/objectives")
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    public ObjectiveController(ObjectiveService objectiveService) {
        this.objectiveService = objectiveService;
    }

    /** Liste des objectifs actifs (tous rôles confondus) — pour l'admin. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Objective>> findAll() {
        return ResponseEntity.ok(objectiveService.findAll());
    }

    /** Progression automatique des objectifs du rôle actif de l'utilisateur. */
    @GetMapping("/my-progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<ObjectiveProgressResponse>> myProgress() {
        return ResponseEntity.ok(objectiveService.myProgress());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Objective> create(@Valid @RequestBody CreateObjectiveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(objectiveService.create(request));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Objective> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(objectiveService.toggle(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        objectiveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
