package com.discipolat.modules.programs.api;

import com.discipolat.modules.programs.domain.ProgramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    /** Tous les types de programmes (configuration complète). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<ProgramTypeResponse>> findAll(
            @RequestParam(defaultValue = "false") boolean all) {
        return ResponseEntity.ok(programService.findAll(all));
    }

    /** Types actifs uniquement (pour la saisie de présence côté membre). */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<ProgramTypeResponse>> findActive() {
        return ResponseEntity.ok(programService.findActifs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<ProgramTypeResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(programService.findById(id));
    }

    /** Création / configuration d'un type de programme (pasteur). */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<ProgramTypeResponse> create(@Valid @RequestBody ProgramTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<ProgramTypeResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody ProgramTypeRequest request) {
        return ResponseEntity.ok(programService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        programService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Type de programme supprimé"));
    }
}
