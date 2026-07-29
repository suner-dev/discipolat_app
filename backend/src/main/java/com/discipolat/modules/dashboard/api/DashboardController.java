package com.discipolat.modules.dashboard.api;

import com.discipolat.modules.dashboard.domain.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpi")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getKPI(
            @RequestParam(required = false) LocalDate periodeDebut,
            @RequestParam(required = false) LocalDate periodeFin,
            @RequestParam(required = false) UUID departementId,
            @RequestParam(required = false) UUID familleId) {
        return ResponseEntity.ok(dashboardService.getKPI(periodeDebut, periodeFin, departementId, familleId));
    }

    @GetMapping("/presence-trend")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getPresenceTrend(
            @RequestParam(defaultValue = "12") int mois) {
        return ResponseEntity.ok(dashboardService.getPresenceTrend(mois));
    }

    @GetMapping("/family-risk")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getFamilyRisk(
            @RequestParam(defaultValue = "50") double seuil) {
        return ResponseEntity.ok(dashboardService.getFamilyRisk(seuil));
    }

    @GetMapping("/report-completion")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getReportCompletion() {
        return ResponseEntity.ok(dashboardService.getReportCompletion());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
