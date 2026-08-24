package com.discipolat.modules.intelligence.api;

import com.discipolat.modules.intelligence.domain.IntelligenceCenterService;
import com.discipolat.modules.intelligence.domain.IntelligenceKpi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private final IntelligenceCenterService intelligenceService;

    public IntelligenceController(IntelligenceCenterService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(intelligenceService.listAll());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> listByCategory(@PathVariable IntelligenceKpi.Category category) {
        return ResponseEntity.ok(intelligenceService.listByCategory(category));
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> alerts() {
        return ResponseEntity.ok(intelligenceService.listAlerts());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(intelligenceService.getDashboard());
    }

    @PostMapping("/initialize")
    public ResponseEntity<?> initialize() {
        return ResponseEntity.ok(intelligenceService.initializeKpis());
    }

    @PutMapping("/{id}/value")
    public ResponseEntity<?> updateValue(@PathVariable UUID id, @RequestBody Map<String, Double> body) {
        return ResponseEntity.ok(intelligenceService.updateValue(id, body.get("value")));
    }
}
