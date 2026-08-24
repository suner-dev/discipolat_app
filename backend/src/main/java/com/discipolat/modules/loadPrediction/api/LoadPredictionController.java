package com.discipolat.modules.loadPrediction.api;

import com.discipolat.modules.loadPrediction.domain.LoadPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * P3 #102 — Prédiction de charge (pics d'activité).
 */
@RestController
@RequestMapping("/api/v1/load-prediction")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE')")
public class LoadPredictionController {

    private final LoadPredictionService service;

    public LoadPredictionController(LoadPredictionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> predict() {
        return ResponseEntity.ok(service.predict());
    }
}
