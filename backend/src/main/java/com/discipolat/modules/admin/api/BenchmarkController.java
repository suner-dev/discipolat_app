package com.discipolat.modules.admin.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.alerts.domain.SmartAlertService;
import com.discipolat.modules.members.domain.MemberService;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.modules.souls.domain.SoulRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * BenchmarkController — anonymous cross-church comparison metrics.
 * Shows how a church compares to others (anonymized averages)
 * without revealing any individual church's data.
 */
@RestController
@RequestMapping("/api/v1/benchmark")
@PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN')")
@RequiredArgsConstructor
public class BenchmarkController {

    private final MemberService memberService;
    private final AlertRepository alertRepository;
    private final SoulRepository soulRepository;
    private final SecurityUtils securityUtils;

    /**
     * Get anonymized benchmark data comparing this church against averages.
     * All data is aggregated — no individual church is identifiable.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBenchmark() {
        UUID tenantId = TenantContext.getTenantId();

        // Current church metrics
        long currentMembers = soulRepository.count();
        long activeAlerts = alertRepository.countByStatut(StatutAlerte.ACTIVE);

        // In production: these would come from an anonymous aggregation service
        // For now, return mock benchmark data
        Map<String, Object> benchmark = new LinkedHashMap<>();
        benchmark.put("currentChurch", Map.of(
            "totalMembers", currentMembers,
            "activeAlerts", activeAlerts,
            "attendanceRate", 75.0,
            "growthRate", 3.5,
            "reportsSubmitted", 89,
            "disciplesActive", 45,
            "newConverts", 12,
            "volunteerRate", 35.0
        ));
        benchmark.put("averagePeers", Map.of(
            "totalMembers", 180,
            "activeAlerts", 8,
            "attendanceRate", 68.5,
            "growthRate", 2.1,
            "reportsSubmitted", 72,
            "disciplesActive", 32,
            "newConverts", 8,
            "volunteerRate", 28.0
        ));
        benchmark.put("topQuartile", Map.of(
            "totalMembers", 350,
            "activeAlerts", 3,
            "attendanceRate", 85.0,
            "growthRate", 6.0,
            "reportsSubmitted", 95,
            "disciplesActive", 60,
            "newConverts", 20,
            "volunteerRate", 45.0
        ));
        benchmark.put("percentile", Map.of(
            "attendanceRate", calculatePercentile(75.0, 68.5, 85.0),
            "growthRate", calculatePercentile(3.5, 2.1, 6.0),
            "reportsSubmitted", calculatePercentile(89, 72, 95),
            "volunteerRate", calculatePercentile(35.0, 28.0, 45.0)
        ));
        benchmark.put("generatedAt", LocalDateTime.now());
        benchmark.put("note", "Données anonymisées — aucune église identifiable");

        return ResponseEntity.ok(benchmark);
    }

    /**
     * Get trend comparison over time.
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getTrends() {
        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("attendanceTrend", List.of(
            Map.of("month", "Jan", "current", 72, "average", 65),
            Map.of("month", "Fév", "current", 75, "average", 67),
            Map.of("month", "Mar", "current", 78, "average", 68),
            Map.of("month", "Avr", "current", 80, "average", 69),
            Map.of("month", "Mai", "current", 75, "average", 68),
            Map.of("month", "Jun", "current", 73, "average", 66)
        ));
        trends.put("growthTrend", List.of(
            Map.of("month", "Jan", "current", 2.0, "average", 1.5),
            Map.of("month", "Fév", "current", 2.5, "average", 1.6),
            Map.of("month", "Mar", "current", 3.0, "average", 1.8),
            Map.of("month", "Avr", "current", 3.5, "average", 2.0),
            Map.of("month", "Mai", "current", 3.2, "average", 2.1),
            Map.of("month", "Jun", "current", 3.5, "average", 2.1)
        ));
        return ResponseEntity.ok(trends);
    }

    private double calculatePercentile(double current, double average, double topQuartile) {
        if (topQuartile == average) return 50.0;
        double percentile = ((current - average) / (topQuartile - average)) * 25 + 50;
        return Math.max(0, Math.min(100, percentile));
    }
}
