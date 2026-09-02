package com.discipolat.modules.visits.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.visits.domain.VisitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping
    public ResponseEntity<VisitResponse> create(@Valid @RequestBody CreateVisitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(request));
    }

    /** Liste paginée de toutes les visites (vue Pasteur / Administration). */
    @GetMapping
    public ResponseEntity<PageResponse<VisitResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String typeVisite) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "datePrevue"));
        Page<VisitResponse> result = visitService.findAll(statut, search, pageable);
        return ResponseEntity.ok(PageResponse.of(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
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
