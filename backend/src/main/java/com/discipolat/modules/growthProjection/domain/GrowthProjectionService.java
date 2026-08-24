package com.discipolat.modules.growthProjection.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class GrowthProjectionService {

    private final GrowthProjectionRepository repo;

    public GrowthProjectionService(GrowthProjectionRepository repo) { this.repo = repo; }

    public List<GrowthProjection> listAll() {
        return repo.findByTenantIdOrderByCalculeLeDesc(TenantContext.getCurrentTenantId());
    }

    public GrowthProjection get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("GrowthProjection", id));
    }

    /**
     * Simulate growth projection using exponential model:
     * projected = current * (1 + rate/12)^months + conversions - departures
     */
    public GrowthProjection simulate(GrowthProjection proj) {
        proj.setTenantId(TenantContext.getCurrentTenantId());
        double monthlyRate = proj.getTauxCroissanceAnnuel() / 100.0 / 12.0;
        int projected = (int) Math.round(proj.getEffectifActuel() * Math.pow(1 + monthlyRate, proj.getMoisProjection()));
        proj.setEffectifProjete(projected);
        proj.setCalculeLe(LocalDateTime.now());

        // Auto-generate recommendations
        if (projected > proj.getEffectifActuel() * 1.1) {
            proj.setRecommandations("Croissance forte prévue — prévoir l'intégration de nouveaux membres et le renforcement des équipes.");
        } else if (projected < proj.getEffectifActuel() * 0.95) {
            proj.setRecommandations("Déclin prévu — déclencher le plan de revitalisation et renforcer l'accompagnement pastoral.");
        } else {
            proj.setRecommandations("Croissance stable — maintenir les efforts actuels et identifier des leviers de croissance.");
        }
        return repo.save(proj);
    }

    public GrowthProjection save(GrowthProjection proj) {
        proj.setTenantId(TenantContext.getCurrentTenantId());
        proj.setCalculeLe(LocalDateTime.now());
        return repo.save(proj);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    // ======================== P3 #103 — PROPHÉTIE DE CROISSANCE ========================

    @jakarta.persistence.PersistenceContext
    private transient jakarta.persistence.EntityManager em;

    /**
     * Modèle prédictif basé sur l'historique réel des conversions et retraits
     * des 12 derniers mois pour anticiper l'effectif et les besoins sur 12 mois.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> prophesy() {
        UUID tenantId = TenantContext.getCurrentTenantId();

        Long effectifActuel = ((Number) em.createNativeQuery(
                        "SELECT count(*) FROM souls WHERE tenant_id = :t AND deleted = false")
                .setParameter("t", tenantId).getSingleResult()).longValue();

        List<Object[]> conversionsRows = asRows(em.createNativeQuery("""
                SELECT to_char(date_trunc('month', date_conversion), 'YYYY-MM') AS mois, count(*)
                FROM souls
                WHERE tenant_id = :t AND deleted = false AND date_conversion >= :from
                GROUP BY 1 ORDER BY 1
                """).setParameter("t", tenantId)
                .setParameter("from", java.time.LocalDate.now().minusMonths(12))
                .getResultList());

        List<Object[]> exitsRows = asRows(em.createNativeQuery("""
                SELECT to_char(date_trunc('month', se.date_sortie), 'YYYY-MM') AS mois, count(*)
                FROM soul_exits se
                JOIN souls s ON s.id = se.ame_id AND s.tenant_id = :t
                WHERE se.date_sortie >= :from
                GROUP BY 1 ORDER BY 1
                """).setParameter("t", tenantId)
                .setParameter("from", java.time.LocalDate.now().minusMonths(12))
                .getResultList());

        double avgConv = conversionsRows.stream().mapToLong(r -> ((Number) r[1]).longValue()).average().orElse(0);
        double avgExit = exitsRows.stream().mapToLong(r -> ((Number) r[1]).longValue()).average().orElse(0);
        double netGrowth = avgConv - avgExit;
        double recentNet = lastN(conversionsRows, 3).orElse(avgConv) - avgExit;
        double monthlyRate = 0.7 * netGrowth + 0.3 * recentNet; // mixte historique/tendance

        return buildForecast(effectifActuel, avgConv, avgExit, monthlyRate, tenantId);
    }

    private Map<String, Object> buildForecast(Long effectifActuel, double avgConv, double avgExit,
                                              double monthlyRate, UUID tenantId) {
        List<Map<String, Object>> forecast = new ArrayList<>();
        double projected = effectifActuel;
        LocalDate month = LocalDate.now().withDayOfMonth(1);
        for (int i = 1; i <= 12; i++) {
            projected += monthlyRate * Math.pow(0.97, i); // légère décroissance du taux
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("mois", month.plusMonths(i).toString().substring(0, 7));
            f.put("effectifProjete", (int) Math.round(projected));
            forecast.add(f);
        }

        int finalProjection = (int) Math.round(projected);
        String scenario = finalProjection > effectifActuel * 1.15 ? "CROISSANCE_FORTE"
                : finalProjection > effectifActuel ? "CROISSANCE_MODEREE"
                : finalProjection >= effectifActuel * 0.95 ? "STAGNATION" : "DECROISSANCE";

        int faiseursRequis = (int) Math.ceil(projected / 5.0); // 1 faiseur pour 5 âmes
        long faiseursActuels = ((Number) em.createNativeQuery(
                        "SELECT count(DISTINCT faiseur_id) FROM souls WHERE tenant_id = :t AND deleted = false AND faiseur_id IS NOT NULL")
                .setParameter("t", tenantId).getSingleResult()).longValue();
        int chefsFamillesRequis = (int) Math.ceil(projected / 15.0); // 1 famille ≈ 15 âmes

        StringBuilder besoins = new StringBuilder();
        if (faiseursRequis > faiseursActuels) {
            besoins.append("Former ").append(faiseursRequis - faiseursActuels)
                    .append(" faiseurs supplémentaires pour maintenir le ratio de suivi (1/5). ");
        } else {
            besoins.append("Effectif de faiseurs suffisant pour la croissance projetée. ");
        }
        switch (scenario) {
            case "CROISSANCE_FORTE" -> besoins.append("Prévoir salles supplémentaires et intégration structurée des nouveaux convertis.");
            case "STAGNATION" -> besoins.append("Lancer un plan d'évangélisation ciblé et réactiver les âmes sans contact récent.");
            case "DECROISSANCE" -> besoins.append("Urgence pastorale : programme de rétention et visites des familles à risque.");
            default -> besoins.append("Maintenir le rythme actuel et suivre les indicateurs mensuellement.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("genereLe", LocalDateTime.now());
        result.put("effectifActuel", effectifActuel.intValue());
        result.put("moyenneConversionsMensuelles", round1(avgConv));
        result.put("moyenneSortiesMensuelles", round1(avgExit));
        result.put("croissanceNetteMensuelle", round1(monthlyRate));
        result.put("scenario", scenario);
        result.put("projection12Mois", finalProjection);
        result.put("forecast", forecast);
        result.put("faiseursActuels", faiseursActuels);
        result.put("faiseursRequisProjetes", faiseursRequis);
        result.put("chefsFamillesRequisProjetes", chefsFamillesRequis);
        result.put("besoinsAnticipes", besoins.toString());
        return result;
    }

    private Optional<Double> lastN(List<Object[]> rows, int n) {
        if (rows.isEmpty()) return Optional.empty();
        List<Object[]> tail = rows.subList(Math.max(0, rows.size() - n), rows.size());
        return Optional.of(tail.stream().mapToLong(r -> ((Number) r[1]).longValue()).average().orElse(0));
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private static List<Object[]> asRows(List<?> raw) {
        List<Object[]> rows = new ArrayList<>();
        for (Object o : raw) rows.add((Object[]) o);
        return rows;
    }
}
