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
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpi")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getKPI(
            @RequestParam(required = false) LocalDate periodeDebut,
            @RequestParam(required = false) LocalDate periodeFin,
            @RequestParam(required = false) UUID departementId,
            @RequestParam(required = false) UUID familleId) {
        return ResponseEntity.ok(dashboardService.getKPI(periodeDebut, periodeFin, departementId, familleId));
    }

    @GetMapping("/presence-trend")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getPresenceTrend(
            @RequestParam(defaultValue = "12") int mois) {
        return ResponseEntity.ok(dashboardService.getPresenceTrend(mois));
    }

    @GetMapping("/family-risk")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getFamilyRisk(
            @RequestParam(defaultValue = "50") double seuil) {
        return ResponseEntity.ok(dashboardService.getFamilyRisk(seuil));
    }

    @GetMapping("/report-completion")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE_GETREPORTCOMPLETION', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getReportCompletion() {
        return ResponseEntity.ok(dashboardService.getReportCompletion());
    }

    @GetMapping("/summary")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/my-metrics")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE_GETMYMETRICS', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getMyMetrics() {
        return ResponseEntity.ok(dashboardService.getCurrentUserMetrics());
    }

    // ======================== PHASE 2: ROLE-SPECIFIC DASHBOARDS ========================

    @GetMapping("/pasteur")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getPasteurDashboard() {
        return ResponseEntity.ok(dashboardService.getPasteurDashboard());
    }

    @GetMapping("/chef-famille")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE_GETCHEFFAMILLEDASHBOARD', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getChefFamilleDashboard(
            @RequestParam(required = false) UUID familleId) {
        return ResponseEntity.ok(dashboardService.getChefFamilleDashboard(familleId));
    }

    @GetMapping("/responsable")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE_GETRESPONSABLEDASHBOARD', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getResponsableDashboard(
            @RequestParam(required = false) UUID deptId) {
        return ResponseEntity.ok(dashboardService.getResponsableDashboard(deptId));
    }

    // ======================== PHASE 3: CRM FAISEUR ========================

    @GetMapping("/crm-faiseur")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE_GETCRMFAISEUR', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getCrmFaiseur() {
        return ResponseEntity.ok(dashboardService.getCrmFaiseurDashboard());
    }

    // ======================== PASTEUR: TENDANCES & FIL D'ACTIVITÉ ========================

    @GetMapping("/pasteur/presence-trend")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getPasteurPresenceTrend(
            @RequestParam(defaultValue = "12") int mois) {
        return ResponseEntity.ok(dashboardService.getPresenceTrend(mois));
    }

    @GetMapping("/pasteur/kpis")
    @PreAuthorize("@authz.can('DASHBOARD_EXECUTE', 'TENANT', null)")
    public ResponseEntity<Map<String, Object>> getPasteurKpis() {
        return ResponseEntity.ok(dashboardService.getPasteurKpis());
    }
}
