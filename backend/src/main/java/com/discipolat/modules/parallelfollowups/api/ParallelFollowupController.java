package com.discipolat.modules.parallelfollowups.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowup;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parallel-followups")
public class ParallelFollowupController {

    private final ParallelFollowupService service;

    public ParallelFollowupController(ParallelFollowupService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<ParallelFollowupResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID initiateurId,
            @RequestParam(required = false) String statut) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "dateDebut"));
        Page<ParallelFollowup> results = service.findAll(initiateurId, statut, pageable);
        Page<ParallelFollowupResponse> response = results.map(ParallelFollowupResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<ParallelFollowupResponse> create(@Valid @RequestBody CreateParallelFollowupRequest request) {
        ParallelFollowup followup = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ParallelFollowupResponse.from(followup));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParallelFollowupResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ParallelFollowupResponse.from(service.findById(id)));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<ParallelFollowupResponse> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ParallelFollowupResponse.from(service.close(id)));
    }

    @GetMapping("/active")
    public ResponseEntity<PageResponse<ParallelFollowupResponse>> findActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ParallelFollowup> results = service.findActive(pageable);
        Page<ParallelFollowupResponse> response = results.map(ParallelFollowupResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }
}
