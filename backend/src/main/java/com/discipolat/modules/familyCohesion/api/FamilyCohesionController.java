package com.discipolat.modules.familyCohesion.api;

import com.discipolat.modules.familyCohesion.domain.FamilyCohesion;
import com.discipolat.modules.familyCohesion.domain.FamilyCohesionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/family-cohesion")
public class FamilyCohesionController {

    private static final Logger log = LoggerFactory.getLogger(FamilyCohesionController.class);
    private final FamilyCohesionService service;

    public FamilyCohesionController(FamilyCohesionService service) {
        this.service = service;
    }

    @GetMapping("/{familleId}")
    public ResponseEntity<FamilyCohesion> getLatest(@PathVariable UUID familleId) {
        return ResponseEntity.ok(service.getLatest(familleId));
    }

    @PostMapping("/calculate/{familleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FamilyCohesion> calculate(@PathVariable UUID familleId, @RequestBody Map<String, Object> body) {
        FamilyCohesion cohesion = service.calculate(familleId,
                body.get("tauxParticipation") != null ? (double) body.get("tauxParticipation") : 0,
                body.get("diversité") != null ? (int) body.get("diversité") : 0,
                body.get("équilibre") != null ? (int) body.get("équilibre") : 0
        );
        return ResponseEntity.ok(cohesion);
    }


    /** Get cohesion summary for current user (mobile) */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserCohesion(
            @RequestParam(required = false) String userId) {
        log.debug("[FamilyCohesion] get cohesion for user {}", userId);
        return ResponseEntity.ok(Map.of(
            "score", 0,
            "indicators", Map.of(),
            "trend", "STABLE"
        ));
    }

}
