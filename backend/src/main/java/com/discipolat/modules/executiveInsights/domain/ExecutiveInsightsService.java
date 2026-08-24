package com.discipolat.modules.executiveInsights.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ExecutiveInsightsService {

    private final ExecutiveInsightRepository insightRepo;

    public ExecutiveInsightsService(ExecutiveInsightRepository insightRepo) { this.insightRepo = insightRepo; }

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

    /** Generate AI insights based on current data */
    public List<ExecutiveInsight> generateInsights() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<ExecutiveInsight> insights = new ArrayList<>();

        // Sample insights — in production, these would analyze real data
        ExecutiveInsight i1 = new ExecutiveInsight();
        i1.setTenantId(tenantId);
        i1.setTitle("Tendance de présence en baisse");
        i1.setDescription("La présence moyenne a diminué de 8% sur les 3 dernières semaines, principalement chez les 18-25 ans.");
        i1.setSeverity(ExecutiveInsight.Severity.WARNING);
        i1.setCategory(ExecutiveInsight.Category.ENGAGEMENT);
        i1.setRecommendedAction("Organiser un événement ciblé jeunesse et contacter les absents.");
        i1.setMetricValue("62%");
        i1.setMetricChange("-8%");
        insights.add(insightRepo.save(i1));

        ExecutiveInsight i2 = new ExecutiveInsight();
        i2.setTenantId(tenantId);
        i2.setTitle("Opportunité : Nouveau groupe de maison");
        i2.setDescription("Le quartier Nord a 15 membres actifs sans groupe de maison. Potentiel de création d'un nouveau groupe.");
        i2.setSeverity(ExecutiveInsight.Severity.OPPORTUNITY);
        i2.setCategory(ExecutiveInsight.Category.GROWTH);
        i2.setRecommendedAction("Identifier un leader potentiel dans le quartier Nord et planifier une réunion de lancement.");
        i2.setMetricValue("15");
        i2.setMetricChange("+15 membres");
        insights.add(insightRepo.save(i2));

        ExecutiveInsight i3 = new ExecutiveInsight();
        i3.setTenantId(tenantId);
        i3.setTitle("Finances : Dons en hausse de 12%");
        i3.setDescription("Les dons du mois de juillet ont augmenté de 12% par rapport au mois précédent. Tendance positive.");
        i3.setSeverity(ExecutiveInsight.Severity.INFO);
        i3.setCategory(ExecutiveInsight.Category.FINANCE);
        i3.setRecommendedAction("Continuer la communication sur les projets financés par les dons.");
        i3.setMetricValue("€4,250");
        i3.setMetricChange("+12%");
        insights.add(insightRepo.save(i3));

        return insights;
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        var active = insightRepo.findByTenantIdAndIsDismissedFalseOrderByCreatedAtDesc(tenantId);
        stats.put("total", active.size());
        stats.put("critical", active.stream().filter(i -> i.getSeverity() == ExecutiveInsight.Severity.CRITICAL).count());
        stats.put("warnings", active.stream().filter(i -> i.getSeverity() == ExecutiveInsight.Severity.WARNING).count());
        stats.put("opportunities", active.stream().filter(i -> i.getSeverity() == ExecutiveInsight.Severity.OPPORTUNITY).count());
        stats.put("unread", active.stream().filter(i -> !i.getIsRead()).count());
        return stats;
    }
}
