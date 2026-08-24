package com.discipolat.modules.engagementAnalytics.api;

import com.discipolat.modules.engagementAnalytics.domain.EngagementAnalytics;
import com.discipolat.modules.engagementAnalytics.domain.EngagementAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/engagement-analytics")
public class EngagementAnalyticsController {

    private final EngagementAnalyticsService service;

    public EngagementAnalyticsController(EngagementAnalyticsService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.listAll()); }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> listByCategory(@PathVariable String category) { return ResponseEntity.ok(service.listByCategory(category)); }

    @PostMapping
    public ResponseEntity<?> record(@RequestBody EngagementAnalytics analytics) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.record(analytics));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() { return ResponseEntity.ok(service.getDashboard()); }
}
