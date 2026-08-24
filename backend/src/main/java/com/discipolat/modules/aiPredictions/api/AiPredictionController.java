package com.discipolat.modules.aiPredictions.api;

import com.discipolat.modules.aiPredictions.domain.AiPrediction;
import com.discipolat.modules.aiPredictions.domain.AiPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-predictions")
public class AiPredictionController {

    private final AiPredictionService service;

    public AiPredictionController(AiPredictionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AiPrediction>> list(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.listByTenant(tenantId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AiPrediction>> listByType(
            @RequestParam Long tenantId,
            @PathVariable AiPrediction.PredictionType type) {
        return ResponseEntity.ok(service.listByType(tenantId, type));
    }

    @GetMapping("/risks")
    public ResponseEntity<List<AiPrediction>> listRisks(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.listRisks(tenantId));
    }

    @PostMapping("/generate")
    public ResponseEntity<List<AiPrediction>> generate(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.generatePredictions(tenantId));
    }

    @PostMapping
    public ResponseEntity<AiPrediction> save(@RequestBody AiPrediction prediction) {
        return ResponseEntity.ok(service.save(prediction));
    }
}
