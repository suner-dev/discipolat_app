package com.discipolat.modules.visits.api;

import com.discipolat.modules.visits.domain.VisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping
    public ResponseEntity<VisitResponse> create(@Valid @RequestBody CreateVisitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<VisitResponse>> myVisits() {
        return ResponseEntity.ok(visitService.myVisits());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<VisitResponse>> upcoming() {
        return ResponseEntity.ok(visitService.upcoming());
    }

    @GetMapping("/souls/{soulId}")
    public ResponseEntity<List<VisitResponse>> findBySoul(@PathVariable UUID soulId) {
        return ResponseEntity.ok(visitService.findBySoul(soulId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VisitResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVisitRequest request) {
        return ResponseEntity.ok(visitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        visitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
