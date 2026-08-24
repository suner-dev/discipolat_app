package com.discipolat.modules.neighborhoodHealth.api;

import com.discipolat.modules.neighborhoodHealth.domain.NeighborhoodHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * P3 #104 — Analyse de santé spirituelle par quartier.
 */
@RestController
@RequestMapping("/api/v1/neighborhood-health")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE')")
public class NeighborhoodHealthController {

    private final NeighborhoodHealthService service;

    public NeighborhoodHealthController(NeighborhoodHealthService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> byZone() {
        return ResponseEntity.ok(service.getHealthByZone());
    }
}
