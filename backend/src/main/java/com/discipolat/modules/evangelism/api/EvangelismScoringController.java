package com.discipolat.modules.evangelism.api;

import com.discipolat.modules.evangelism.domain.ConversionScoringService;
import com.discipolat.modules.evangelism.domain.EvangelismTrack;
import com.discipolat.modules.evangelism.domain.EvangelismTrackRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evangelism/scoring")
public class EvangelismScoringController {

    private final ConversionScoringService scoringService;
    private final EvangelismTrackRepository trackRepository;

    public EvangelismScoringController(ConversionScoringService scoringService,
                                       EvangelismTrackRepository trackRepository) {
        this.scoringService = scoringService;
        this.trackRepository = trackRepository;
    }

    /** Score de conversion d'un prospect précis. */
    @GetMapping("/{soulId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> score(@PathVariable UUID soulId) {
        return ResponseEntity.ok(scoringService.scoreFor(soulId));
    }

    /** Scores de tous les pipelines, triés du plus prometteur au plus à risque. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> allScores() {
        List<EvangelismTrack> tracks = trackRepository.findAll();
        return ResponseEntity.ok(scoringService.scoreAll(tracks));
    }
}
