package com.discipolat.modules.intelligence.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class IntelligenceCenterService {

    private final IntelligenceKpiRepository kpiRepo;

    // 50+ KPI definitions
    private static final List<Map<String, Object>> KPI_DEFINITIONS = List.of(
        // PRESENCE (8)
        Map.of("name", "Taux de présence global", "category", "PRESENCE", "unit", "%", "order", 0),
        Map.of("name", "Présence dimanche matin", "category", "PRESENCE", "unit", "%", "order", 1),
        Map.of("name", "Présence culte du soir", "category", "PRESENCE", "unit", "%", "order", 2),
        Map.of("name", "Présence réunion prière", "category", "PRESENCE", "unit", "%", "order", 3),
        Map.of("name", "Nouveaux visiteurs/semaine", "category", "PRESENCE", "unit", "count", "order", 4),
        Map.of("name", "Taux de conversion visiteurs", "category", "PRESENCE", "unit", "%", "order", 5),
        Map.of("name", "Présence moyenne événements", "category", "PRESENCE", "unit", "count", "order", 6),
        Map.of("name", "Heure moyenne d'arrivée", "category", "PRESENCE", "unit", "h", "order", 7),
        // GROWTH (7)
        Map.of("name", "Croissance effectifs (mensuelle)", "category", "GROWTH", "unit", "%", "order", 0),
        Map.of("name", "Baptêmes (trimestre)", "category", "GROWTH", "unit", "count", "order", 1),
        Map.of("name", "Membres actifs / total", "category", "GROWTH", "unit", "%", "order", 2),
        Map.of("name", "Nouveaux membres (mois)", "category", "GROWTH", "unit", "count", "order", 3),
        Map.of("name", "Taux de rétention annuel", "category", "GROWTH", "unit", "%", "order", 4),
        Map.of("name", "Familles actives", "category", "GROWTH", "unit", "count", "order", 5),
        Map.of("name", "Ratio disciples/faiseurs", "category", "GROWTH", "unit", "ratio", "order", 6),
        // ENGAGEMENT (6)
        Map.of("name", "Score d'engagement moyen", "category", "ENGAGEMENT", "unit", "/100", "order", 0),
        Map.of("name", "Taux de participation aux petits groupes", "category", "ENGAGEMENT", "unit", "%", "order", 1),
        Map.of("name", "Messages envoyés/mois", "category", "ENGAGEMENT", "unit", "count", "order", 2),
        Map.of("name", "Actions sur l'app/mois", "category", "ENGAGEMENT", "unit", "count", "order", 3),
        Map.of("name", "Taux complétion formations", "category", "ENGAGEMENT", "unit", "%", "order", 4),
        Map.of("name", "Notes de prière/mois", "category", "ENGAGEMENT", "unit", "count", "order", 5),
        // SPIRITUAL (7)
        Map.of("name", "Score spirituel moyen", "category", "SPIRITUAL", "unit", "/100", "order", 0),
        Map.of("name", "Taux de prière quotidienne", "category", "SPIRITUAL", "unit", "%", "order", 1),
        Map.of("name", "Lecture biblique hebdo (min)", "category", "SPIRITUAL", "unit", "min", "order", 2),
        Map.of("name", "Défis complétés (mois)", "category", "SPIRITUAL", "unit", "count", "order", 3),
        Map.of("name", "Témoignages partagés", "category", "SPIRITUAL", "unit", "count", "order", 4),
        Map.of("name", "Membres en mentorat", "category", "SPIRITUAL", "unit", "count", "order", 5),
        Map.of("name", "Croissance spirituelle 6 mois", "category", "SPIRITUAL", "unit", "%", "order", 6),
        // FINANCES (6)
        Map.of("name", "Dons moyens/membre", "category", "FINANCES", "unit", "currency", "order", 0),
        Map.of("name", "Taux de tithes régulières", "category", "FINANCES", "unit", "%", "order", 1),
        Map.of("name", "Revenus totaux/mois", "category", "FINANCES", "unit", "currency", "order", 2),
        Map.of("name", "Dépenses/mois", "category", "FINANCES", "unit", "currency", "order", 3),
        Map.of("name", "Trésorerie disponible", "category", "FINANCES", "unit", "currency", "order", 4),
        Map.of("name", "Dons en ligne (% total)", "category", "FINANCES", "unit", "%", "order", 5),
        // OUTREACH (6)
        Map.of("name", "Contacts évangélisation/mois", "category", "OUTREACH", "unit", "count", "order", 0),
        Map.of("name", "Conversions/mois", "category", "OUTREACH", "unit", "count", "order", 1),
        Map.of("name", "Visites pastorales/mois", "category", "OUTREACH", "unit", "count", "order", 2),
        Map.of("name", "Familles en difficulté accompagnées", "category", "OUTREACH", "unit", "count", "order", 3),
        Map.of("name", "Aides d'urgence distribuées", "category", "OUTREACH", "unit", "count", "order", 4),
        Map.of("name", "Parrainages convertis", "category", "OUTREACH", "unit", "count", "order", 5),
        // ALERTS (5)
        Map.of("name", "Alertes actives", "category", "ALERTS", "unit", "count", "order", 0),
        Map.of("name", "Membres à risque décrochage", "category", "ALERTS", "unit", "count", "order", 1),
        Map.of("name", "Absences soutenues (>3 sem)", "category", "ALERTS", "unit", "count", "order", 2),
        Map.of("name", "Tickets en attente", "category", "ALERTS", "unit", "count", "order", 3),
        Map.of("name", "Demandes admin en attente", "category", "ALERTS", "unit", "count", "order", 4),
        // OPERATIONS (6)
        Map.of("name", "Événements ce mois", "category", "OPERATIONS", "unit", "count", "order", 0),
        Map.of("name", "Équipes fonctionnelles", "category", "OPERATIONS", "unit", "count", "order", 1),
        Map.of("name", "Taux remplissage équipes", "category", "OPERATIONS", "unit", "%", "order", 2),
        Map.of("name", "Tâches complétées/mois", "category", "OPERATIONS", "unit", "count", "order", 3),
        Map.of("name", "Automatisations actives", "category", "OPERATIONS", "unit", "count", "order", 4),
        Map.of("name", "Taux satisfaction en ligne", "category", "OPERATIONS", "unit", "%", "order", 5),
        // QUALITY (5)
        Map.of("name", "Score satisfaction fidèles", "category", "QUALITY", "unit", "/100", "order", 0),
        Map.of("name", "NPS église", "category", "QUALITY", "unit", "score", "order", 1),
        Map.of("name", "Taux résolution tickets", "category", "QUALITY", "unit", "%", "order", 2),
        Map.of("name", "Temps moyen réponse pastoral", "category", "QUALITY", "unit", "days", "order", 3),
        Map.of("name", "Taux complétion checklists", "category", "QUALITY", "unit", "%", "order", 4)
    );

    public IntelligenceCenterService(IntelligenceKpiRepository kpiRepo) {
        this.kpiRepo = kpiRepo;
    }

    public List<IntelligenceKpi> listAll() {
        return kpiRepo.findByTenantIdOrderByCategoryAscDisplayOrderAsc(TenantContext.getCurrentTenantId());
    }

    public List<IntelligenceKpi> listByCategory(IntelligenceKpi.Category category) {
        return kpiRepo.findByTenantIdAndCategoryOrderByDisplayOrderAsc(TenantContext.getCurrentTenantId(), category);
    }

    public List<IntelligenceKpi> listAlerts() {
        return kpiRepo.findByTenantIdAndIsAlertTrueOrderByDisplayOrderAsc(TenantContext.getCurrentTenantId());
    }

    /** Initialize all 50+ KPIs for a tenant */
    public List<IntelligenceKpi> initializeKpis() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var existing = kpiRepo.findByTenantIdOrderByCategoryAscDisplayOrderAsc(tenantId);
        if (existing.size() >= KPI_DEFINITIONS.size()) return existing;

        List<IntelligenceKpi> kpis = new ArrayList<>();
        for (var def : KPI_DEFINITIONS) {
            IntelligenceKpi kpi = new IntelligenceKpi();
            kpi.setTenantId(tenantId);
            kpi.setName((String) def.get("name"));
            kpi.setCategory(IntelligenceKpi.Category.valueOf((String) def.get("category")));
            kpi.setUnit((String) def.get("unit"));
            kpi.setDisplayOrder((Integer) def.get("order"));
            kpi.setCurrentValue(0.0);
            kpi.setPreviousValue(0.0);
            kpi.setPercentageChange(0.0);
            kpi.setNarrative("KPI initialisé — données à alimenter.");
            kpis.add(kpiRepo.save(kpi));
        }
        return kpis;
    }

    public IntelligenceKpi updateValue(UUID id, Double value) {
        IntelligenceKpi kpi = kpiRepo.findById(id).orElseThrow();
        kpi.setPreviousValue(kpi.getCurrentValue());
        kpi.setCurrentValue(value);
        if (kpi.getPreviousValue() != null && kpi.getPreviousValue() != 0) {
            kpi.setPercentageChange(Math.round((value - kpi.getPreviousValue()) / kpi.getPreviousValue() * 10000.0) / 100.0);
        }
        kpi.setTrend(kpi.getPercentageChange() > 1 ? IntelligenceKpi.Trend.UP : kpi.getPercentageChange() < -1 ? IntelligenceKpi.Trend.DOWN : IntelligenceKpi.Trend.STABLE);
        // Auto-alert if critical drop
        if (kpi.getPercentageChange() < -10) {
            kpi.setIsAlert(true);
            kpi.setNarrative("⚠️ Chute significative de " + kpi.getName() + " : " + kpi.getPercentageChange() + "% — action requise.");
        } else {
            kpi.setIsAlert(false);
            kpi.setNarrative(kpi.getTrend() == IntelligenceKpi.Trend.UP ? "📈 En hausse (" + kpi.getPercentageChange() + "%)" : kpi.getTrend() == IntelligenceKpi.Trend.DOWN ? "📉 En baisse (" + kpi.getPercentageChange() + "%)" : "➡️ Stable");
        }
        kpi.setUpdatedAt(LocalDateTime.now());
        return kpiRepo.save(kpi);
    }

    public Map<String, Object> getDashboard() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var all = kpiRepo.findByTenantIdOrderByCategoryAscDisplayOrderAsc(tenantId);
        var alerts = kpiRepo.findByTenantIdAndIsAlertTrueOrderByDisplayOrderAsc(tenantId);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalKpis", all.size());
        dashboard.put("activeAlerts", alerts.size());
        dashboard.put("categories", all.stream().map(IntelligenceKpi::getCategory).distinct().collect(Collectors.toList()));
        dashboard.put("kpisByCategory", all.stream().collect(Collectors.groupingBy(IntelligenceKpi::getCategory)));
        dashboard.put("trendSummary", Map.of(
            "up", all.stream().filter(k -> k.getTrend() == IntelligenceKpi.Trend.UP).count(),
            "down", all.stream().filter(k -> k.getTrend() == IntelligenceKpi.Trend.DOWN).count(),
            "stable", all.stream().filter(k -> k.getTrend() == IntelligenceKpi.Trend.STABLE).count()
        ));
        return dashboard;
    }
}
