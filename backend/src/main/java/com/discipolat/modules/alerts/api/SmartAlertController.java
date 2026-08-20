package com.discipolat.modules.alerts.api;

import com.discipolat.modules.alerts.domain.SmartAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/smart-alerts")
@PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE')")
public class SmartAlertController {

    private final SmartAlertService smartAlertService;

    public SmartAlertController(SmartAlertService smartAlertService) {
        this.smartAlertService = smartAlertService;
    }

    /**
     * Manually trigger all anomaly detection checks.
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> runChecksNow() {
        Map<String, Object> result = smartAlertService.runChecksNow();
        return ResponseEntity.ok(result);
    }

    /**
     * Get anomaly summary for dashboard.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAnomalySummary() {
        Map<String, Object> summary = smartAlertService.getAnomalySummary();
        return ResponseEntity.ok(summary);
    }
}
