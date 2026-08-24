package com.discipolat.modules.executiveInsights.api;

import com.discipolat.modules.executiveInsights.domain.ExecutiveInsight;
import com.discipolat.modules.executiveInsights.domain.ExecutiveInsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/executive-insights")
public class ExecutiveInsightsController {

    private final ExecutiveInsightsService service;
    public ExecutiveInsightsController(ExecutiveInsightsService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.listActive()); }

    @PostMapping("/generate")
    public ResponseEntity<?> generate() { return ResponseEntity.ok(service.generateInsights()); }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable UUID id) { return ResponseEntity.ok(service.markRead(id)); }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<?> dismiss(@PathVariable UUID id) { service.dismiss(id); return ResponseEntity.ok("OK"); }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() { return ResponseEntity.ok(service.getStats()); }
}
