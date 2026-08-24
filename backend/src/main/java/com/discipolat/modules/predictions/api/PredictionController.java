package com.discipolat.modules.predictions.api;

import com.discipolat.modules.predictions.domain.Prediction;
import com.discipolat.modules.predictions.domain.PredictionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(predictionService.listAll());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> listByType(@PathVariable Prediction.Type type) {
        return ResponseEntity.ok(predictionService.listByType(type));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, Object> body) {
        Prediction.Type type = Prediction.Type.valueOf((String) body.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.getOrDefault("historicalData", Map.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(predictionService.generatePrediction(type, data));
    }

    @PostMapping("/generate-all")
    public ResponseEntity<?> generateAll(@RequestBody Map<String, Map<String, Object>> dataByType) {
        return ResponseEntity.status(HttpStatus.CREATED).body(predictionService.generateAllPredictions(dataByType));
    }
}
