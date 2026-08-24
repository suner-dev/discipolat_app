package com.discipolat.modules.executiveInsights.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P1 #15 — Dashboard exécutif avec insights IA.
 * Analyzes real data to generate actionable insights.
 */
@Service
@Transactional
public class ExecutiveInsightsService {

    private final ExecutiveInsightRepository insightRepo;
    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final AlertRepository alertRepository;
    private final MakerReportRepository makerReportRepository;

    public ExecutiveInsightsService(
            ExecutiveInsightRepository insightRepo,
            SoulRepository soulRepository,
            FamilyRepository familyRepository,
            AlertRepository alertRepository,
            MakerReportRepository makerReportRepository) {
        this.insightRepo = insightRepo;
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.alertRepository = alertRepository;
        this.makerReportRepository = makerReportRepository;
    }

    public List<ExecutiveInsight> listActive() {
        return insightRepo.findByTenantIdAndIsDismissedFalseOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public ExecutiveInsight markRead(UUID id) {
        ExecutiveInsight insight = insightRepo.findById(id).orElseThrow();
        insight.setIsRead(true);
        return insightRepo.save(insight);
    }

    public void dismiss(UUID id) {
        ExecutiveInsight insight = insightRepo.findById(id).orElseThrow();
        insight.setIsDismissed(true);
        insightRepo.save(insight);
    }

    /**
     * Generate AI insights based on REAL data analysis.
     */
    public List<ExecutiveInsight> generateInsights() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<ExecutiveInsight> insights = new ArrayList<>();

        analyzeReportCompletion(tenantId, insights);
        analyzeAtRiskFamilies(tenantId, insights);
        analyzeGrowth(tenantId, insights);
        analyzeAlerts(tenantId, insights);

        return insights;
    }

    private void analyzeReportCompletion(UUID tenantId, List<ExecutiveInsight> insights) {
        LocalDate currentWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        var reports = makerReportRepository.findBySemaine(currentWeek, PageRequest.of(0, 200));
        long submitted = reports.stream().filter(r -> r.isSoumis()).count();
        long total = reports.getNumberOfElements();

        if (total > 0 && submitted < total) {
            double completionRate = (double) submitted / total * 100;
            ExecutiveInsight insight = new ExecutiveInsight();
            insight.setTenantId(tenantId);
            insight.setTitle(String.format("Rapports: %d/%d soumis (%.0f%%)", submitted, total, completionRate));
            insight.setDescription(String.format("Il reste %d rapports à soumettre cette semaine.", total - submitted));
            insight.setSeverity(ExecutiveInsight.Severity.WARNING);
            insight.setCategory(ExecutiveInsight.Category.ENGAGEMENT);
            insight.setRecommendedAction("Contacter les faiseurs en retard et relancer la collecte.");
            insight.setMetricValue(String.valueOf(submitted));
            insight.setMetricChange(String.format("%.0f%%", completionRate));
            insights.add(insightRepo.save(insight));
        }
    }

    private void analyzeAtRiskFamilies(UUID tenantId, List<ExecutiveInsight> insights) {
        long totalSouls = soulRepository.count();
        long totalFamilies = familyRepository.count();

        if (totalSouls > 0 && totalFamilies > 0) {
            double avgPerFamily = (double) totalSouls / totalFamilies;
            if (avgPerFamily < 5) {
                ExecutiveInsight insight = new ExecutiveInsight();
                insight.setTenantId(tenantId);
                insight.setTitle(String.format("Taille moyenne des familles: %.1f âmes", avgPerFamily));
                insight.setDescription(String.format("Moyenne de %.1f âmes par famille (%d / %d).", avgPerFamily, totalSouls, totalFamilies));
                insight.setSeverity(ExecutiveInsight.Severity.OPPORTUNITY);
                insight.setCategory(ExecutiveInsight.Category.GROWTH);
                insight.setRecommendedAction("Identifier les familles petites et envisager des fusions ou croissances ciblées.");
                insight.setMetricValue(String.format("%.1f", avgPerFamily));
                insights.add(insightRepo.save(insight));
            }
        }
    }

    private void analyzeGrowth(UUID tenantId, List<ExecutiveInsight> insights) {
        long totalSouls = soulRepository.count();
        ExecutiveInsight insight = new ExecutiveInsight();
        insight.setTenantId(tenantId);
        insight.setTitle("Effectifs: " + totalSouls + " âmes");
        insight.setDescription("Total actuel des âmes enregistrées.");
        insight.setSeverity(ExecutiveInsight.Severity.INFO);
        insight.setCategory(ExecutiveInsight.Category.GROWTH);
        insight.setRecommendedAction("Maintenir l'effort d'évangélisation et d'intégration.");
        insight.setMetricValue(String.valueOf(totalSouls));
        insights.add(insightRepo.save(insight));
    }

    private void analyzeAlerts(UUID tenantId, List<ExecutiveInsight> insights) {
        long activeAlerts = alertRepository.countByStatut(com.discipolat.common.enums.StatutAlerte.ACTIVE);
        if (activeAlerts > 0) {
            ExecutiveInsight insight = new ExecutiveInsight();
            insight.setTenantId(tenantId);
            insight.setTitle(activeAlerts + " alertes actives");
            insight.setDescription("Des alertes nécessitent une attention pastorale.");
            insight.setSeverity(activeAlerts > 5 ? ExecutiveInsight.Severity.WARNING : ExecutiveInsight.Severity.INFO);
            insight.setCategory(ExecutiveInsight.Category.OPERATIONS);
            insight.setRecommendedAction("Consulter et résoudre les alertes prioritaires.");
            insight.setMetricValue(String.valueOf(activeAlerts));
            insights.add(insightRepo.save(insight));
        }
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        var active = insightRepo.findByTenantIdAndIsDismissedFalseOrderByCreatedAtDesc(tenantId);
        stats.put("total", (long) active.size());
        stats.put("critical", active.stream().filter(i -> i.getSeverity() == ExecutiveInsight.Severity.CRITICAL).count());
        stats.put("warning", active.stream().filter(i -> i.getSeverity() == ExecutiveInsight.Severity.WARNING).count());
        stats.put("opportunity", active.stream().filter(i -> i.getSeverity() == ExecutiveInsight.Severity.OPPORTUNITY).count());
        return stats;
    }
}
