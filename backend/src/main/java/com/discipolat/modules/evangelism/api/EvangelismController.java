package com.discipolat.modules.evangelism.api;

import com.discipolat.modules.evangelism.domain.EvangelismEtape;
import com.discipolat.modules.evangelism.domain.EvangelismService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evangelism")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
public class EvangelismController {

    private final EvangelismService evangelismService;

    public EvangelismController(EvangelismService evangelismService) {
        this.evangelismService = evangelismService;
    }

    /** Funnel : nombre d'âmes par étape du pipeline. */
    @GetMapping("/stats")
    public ResponseEntity<EvangelismStatsResponse> stats() {
        return ResponseEntity.ok(evangelismService.stats());
    }

    /** Liste des tracks, filtrée par étape et/ou recherche. */
    @GetMapping
    public ResponseEntity<List<EvangelismTrackResponse>> findAll(
            @RequestParam(required = false) EvangelismEtape etape,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(evangelismService.findAll(etape, search));
    }

    /** Track d'une âme (créé automatiquement si absent). */
    @GetMapping("/souls/{soulId}")
    public ResponseEntity<EvangelismTrackResponse> getOrCreate(@PathVariable UUID soulId) {
        return ResponseEntity.ok(evangelismService.getOrCreate(soulId));
    }

    /** Avance / recule l'âme dans le pipeline. */
    @PutMapping("/souls/{soulId}")
    public ResponseEntity<EvangelismTrackResponse> updateStage(
            @PathVariable UUID soulId,
            @Valid @RequestBody UpdateEvangelismRequest request) {
        return ResponseEntity.ok(evangelismService.updateStage(soulId, request));
    }

    /** Historique des franchissements d'étapes d'une âme. */
    @GetMapping("/souls/{soulId}/history")
    public ResponseEntity<List<Map<String, Object>>> history(@PathVariable UUID soulId) {
        return ResponseEntity.ok(evangelismService.history(soulId));
    }
}
