package com.discipolat.modules.souls.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulExitService;
import com.discipolat.modules.souls.domain.SoulService;
import com.discipolat.modules.souls.domain.SoulRetractionRequestService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/souls")
public class SoulController {

    private final SoulService soulService;
    private final SoulRetractionRequestService retractionRequestService;
    private final SoulExitService soulExitService;

    public SoulController(SoulService soulService, SoulRetractionRequestService retractionRequestService,
                          SoulExitService soulExitService) {
        this.soulService = soulService;
        this.retractionRequestService = retractionRequestService;
        this.soulExitService = soulExitService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SoulResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID faiseurId,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) TypeDisciple typeDisciple,
            @RequestParam(required = false) StatutAme statut,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());
        Page<Soul> souls = soulService.findAll(faiseurId, familleId, typeDisciple, statut, pageable);
        Page<SoulResponse> response = souls.map(SoulResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SoulResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(SoulResponse.from(soulService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<SoulResponse> create(@Valid @RequestBody CreateSoulRequest request) {
        Soul soul = soulService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SoulResponse.from(soul));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<SoulResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateSoulRequest request) {
        Soul soul = soulService.update(id, request);
        return ResponseEntity.ok(SoulResponse.from(soul));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        soulService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<SoulHistoryResponse>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(soulService.getHistory(id));
    }

    @PatchMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<SoulResponse> reassign(
            @PathVariable UUID id, @RequestBody @Valid ReassignSoulRequest request) {
        Soul soul = soulService.reassign(id, request.newFaiseurId());
        return ResponseEntity.ok(SoulResponse.from(soul));
    }

    @GetMapping("/by-faiseur/{faiseurId}")
    public ResponseEntity<List<SoulResponse>> findByFaiseur(@PathVariable UUID faiseurId) {
        return ResponseEntity.ok(soulService.findByFaiseurId(faiseurId)
                .stream().map(SoulResponse::from).toList());
    }

    @GetMapping("/by-famille/{familleId}")
    public ResponseEntity<List<SoulResponse>> findByFamille(@PathVariable UUID familleId) {
        return ResponseEntity.ok(soulService.findByFamilleId(familleId)
                .stream().map(SoulResponse::from).toList());
    }

    /** US-15: Auto-suggest least loaded faiseur for a family */
    @GetMapping("/suggest-faiseur/{familleId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<Map<String, UUID>> suggestFaiseur(@PathVariable UUID familleId) {
        UUID suggested = soulService.suggestLeastLoadedFaiseur(familleId);
        return ResponseEntity.ok(Map.of("faiseurId", suggested != null ? suggested : UUID.randomUUID()));
    }

    /** US-24: Find all souls "en difficulté" (Pasteur only) */
    @GetMapping("/en-difficulte")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<SoulResponse>> findEnDifficulte() {
        return ResponseEntity.ok(soulService.findAllEnDifficulte()
                .stream().map(SoulResponse::from).toList());
    }

    // ======================== US-60: RESTORE SOUL ========================

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<SoulResponse> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(SoulResponse.from(soulService.restore(id)));
    }

    /** US-04/32: Request soul retraction */
    @PostMapping("/retraction-request")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<SoulRetractionRequestResponse> createRetractionRequest(
            @Valid @RequestBody CreateSoulRetractionRequest request) {
        var retraction = retractionRequestService.create(request.ameId(), request.justification());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SoulRetractionRequestResponse.from(retraction));
    }

    /** Get retraction requests for a soul */
    @GetMapping("/{soulId}/retraction-requests")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<List<SoulRetractionRequestResponse>> getRetractionRequests(
            @PathVariable UUID soulId) {
        return ResponseEntity.ok(retractionRequestService.findByAmeId(soulId)
                .stream().map(SoulRetractionRequestResponse::from).toList());
    }

    /** Approve a retraction request */
    @PatchMapping("/retraction-request/{id}/approve")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<SoulRetractionRequestResponse> approveRetraction(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(SoulRetractionRequestResponse.from(
                retractionRequestService.approve(id, body.get("commentaire"))));
    }

    /** Reject a retraction request */
    @PatchMapping("/retraction-request/{id}/reject")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<SoulRetractionRequestResponse> rejectRetraction(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(SoulRetractionRequestResponse.from(
                retractionRequestService.reject(id, body.get("commentaire"))));
    }

    // ======================== US-22: SOUL EXIT ========================

    @PostMapping("/{id}/exit")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> markAsExited(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        String motif = (String) body.get("motif");
        String motifDetail = (String) body.get("motifDetail");
        boolean peutReintegrer = body.containsKey("peutReintegrer") ? (Boolean) body.get("peutReintegrer") : true;
        soulExitService.markAsExited(id, motif, motifDetail, peutReintegrer);
        return ResponseEntity.ok(Map.of("message", "Soul marked as exited", "soulId", id));
    }

    @PostMapping("/{id}/reintegrate")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<SoulResponse> reintegrate(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        StatutAme statut = body.containsKey("statut") ? StatutAme.valueOf(body.get("statut")) : null;
        return ResponseEntity.ok(SoulResponse.from(soulExitService.reintegrate(id, statut)));
    }

    @GetMapping("/{id}/exits")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> getExits(@PathVariable UUID id) {
        return ResponseEntity.ok(soulExitService.findExitsForSoul(id).stream()
                .map(ex -> Map.<String, Object>of(
                        "id", ex.getId(),
                        "motif", ex.getMotif(),
                        "motifDetail", ex.getMotifDetail() != null ? ex.getMotifDetail() : "",
                        "dateSortie", ex.getDateSortie().toString(),
                        "peutReintegrer", ex.isPeutReintegrer()))
                .toList());
    }

    // ======================== US-23: FILTER SOULS BY SPIRITUAL STATE ========================

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<SoulResponse>> filterSouls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String etatSpirituel,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID faiseurId,
            @RequestParam(required = false) UUID familleId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Soul> souls = soulService.filterSouls(etatSpirituel, statut, faiseurId, familleId, pageable);
        Page<SoulResponse> response = souls.map(SoulResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }
}
