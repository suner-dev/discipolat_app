package com.discipolat.modules.admin.api;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.modules.dashboard.domain.DashboardService;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistiques Business Intelligence (dashboard BI web & mobile).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class AdminStatsController {

    private final DashboardService dashboardService;
    private final SoulRepository soulRepository;

    public AdminStatsController(DashboardService dashboardService, SoulRepository soulRepository) {
        this.dashboardService = dashboardService;
        this.soulRepository = soulRepository;
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> statsOverview(
            @RequestParam(defaultValue = "30d") String period) {
        Map<String, Object> overview = new LinkedHashMap<>();

        long totalMembers = soulRepository.count();
        long activeMembers = soulRepository.countByStatut(StatutAme.ACTIF);
        long newConverts = soulRepository.countByTypeDisciple(TypeDisciple.NOUVEAU_CONVERTI);

        Map<String, Object> kpi = dashboardService.getKPI(null, null, null, null);
        double attendanceRate = (Number) kpi.getOrDefault("tauxPresenceGlobal", 0.0);
        double growthRate = totalMembers > 0
                ? Math.round((double) newConverts / totalMembers * 1000.0) / 10.0
                : 0.0;

        overview.put("period", period);
        overview.put("totalMembers", totalMembers);
        overview.put("activeMembers", activeMembers);
        overview.put("growthRate", growthRate);
        overview.put("attendanceRate", attendanceRate);
        overview.put("newConverts", newConverts);

        long disciplesActifs = soulRepository.countByStatut(StatutAme.ACTIF)
                + soulRepository.countByStatut(StatutAme.EN_INTEGRATION);
        overview.put("activeDisciples", disciplesActifs);

        long reportsSubmitted = ((Number) kpi.getOrDefault("rapportsSoumis", 0)).longValue();
        long reportsPending = ((Number) kpi.getOrDefault("rapportsEnAttente", 0)).longValue();
        overview.put("reportsSubmitted", reportsSubmitted);
        overview.put("reportsPending", reportsPending);

        // Tendances hebdomadaires (présence réelle sur les 12 dernières semaines)
        Map<String, Object> trend = dashboardService.getPresenceTrend(12);
        List<Map<String, Object>> weeklyTrend = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) trend.getOrDefault("data", List.of());
        for (Map<String, Object> point : data) {
            double taux = ((Number) point.getOrDefault("taux", 0)).doubleValue();
            Map<String, Object> week = new LinkedHashMap<>();
            week.put("week", point.getOrDefault("semaine", "").toString().substring(0, 10));
            week.put("present", (long) Math.round(taux));
            week.put("absent", 100L - (long) Math.round(taux));
            weeklyTrend.add(week);
        }
        overview.put("weeklyTrend", weeklyTrend);

        // Performance par département : calculée depuis les évaluations quand disponibles
        overview.put("departmentPerformance", List.of());

        return ResponseEntity.ok(overview);
    }
}