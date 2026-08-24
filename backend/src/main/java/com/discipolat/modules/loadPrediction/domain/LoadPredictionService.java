package com.discipolat.modules.loadPrediction.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * P3 #102 — Prédiction de charge (pics d'activité).
 * Anticipe les pics d'activité (événements, inscriptions, rapports) pour
 * dimensionner les ressources. Modèle déterministe : historique 8 semaines
 * + événements planifiés sur 8 semaines.
 */
@Service
@Transactional(readOnly = true)
public class LoadPredictionService {

    @PersistenceContext
    private EntityManager em;

    public Map<String, Object> predict() {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusWeeks(8);

        List<Object[]> upcoming = asRows(em.createNativeQuery("""
                SELECT date(debut) AS jour, count(*) AS nb
                FROM events
                WHERE date_debut >= :from AND date_debut < :to AND deleted = false
                GROUP BY date(debut) ORDER BY date(debut)
                """)
                .setParameter("from", today.atStartOfDay())
                .setParameter("to", horizon.atStartOfDay())
                .getResultList());

        LocalDateTime histFrom = today.minusWeeks(8).atStartOfDay();
        long histRegistrations = count("SELECT count(*) FROM event_registrations WHERE date_inscription >= :p", histFrom);
        long histReports = count("SELECT count(*) FROM maker_reports WHERE created_at >= :p", histFrom);
        double avgReg = histRegistrations / 8.0;
        double avgRep = histReports / 8.0;

        Map<DayOfWeek, Integer> dowWeights = new EnumMap<>(DayOfWeek.class);
        List<Object[]> pastDow = asRows(em.createNativeQuery("""
                SELECT extract(dow from date(debut))::int AS dow, count(*) AS nb
                FROM events
                WHERE date_debut >= :from AND date_debut < now() AND deleted = false
                GROUP BY 1
                """)
                .setParameter("from", today.minusWeeks(12).atStartOfDay())
                .getResultList());
        int maxDow = 1;
        for (Object[] row : pastDow) {
            DayOfWeek dow = DayOfWeek.of(((Number) row[0]).intValue() + 1); // Postgres: 0=dimanche
            dowWeights.merge(dow, ((Number) row[1]).intValue(), Integer::sum);
            maxDow = Math.max(maxDow, dowWeights.get(dow));
        }

        double baselineLoad = avgReg * 0.4 + avgRep * 0.6;
        return buildWeeks(today, upcoming, baselineLoad, dowWeights, maxDow, avgReg, avgRep);
    }

    private Map<String, Object> buildWeeks(LocalDate today, List<Object[]> upcoming,
                                           double baselineLoad, Map<DayOfWeek, Integer> dowWeights,
                                           int maxDow, double avgReg, double avgRep) {
        List<Map<String, Object>> weeks = new ArrayList<>();
        for (int w = 0; w < 8; w++) {
            LocalDate weekStart = today.plusWeeks(w);
            LocalDate weekEnd = weekStart.plusDays(7);

            int eventsInWeek = 0;
            List<String> peakDays = new ArrayList<>();
            for (Object[] row : upcoming) {
                LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
                if (!d.isBefore(weekStart) && d.isBefore(weekEnd)) {
                    eventsInWeek += ((Number) row[1]).intValue();
                    if (dowWeights.getOrDefault(d.getDayOfWeek(), 0) >= maxDow * 0.6) {
                        peakDays.add(d.toString());
                    }
                }
            }
            double estimatedLoad = Math.round((baselineLoad + eventsInWeek * 15.0) * 10) / 10.0;

            String level = estimatedLoad >= baselineLoad + 60 ? "CRITIQUE"
                    : estimatedLoad >= baselineLoad + 30 ? "ELEVE"
                    : estimatedLoad >= baselineLoad ? "NORMAL" : "FAIBLE";

            String recommendation = switch (level) {
                case "CRITIQUE" -> "Pic majeur prévu — pré-renforcer les équipes et augmenter la capacité serveur/notifications.";
                case "ELEVE" -> "Charge supérieure à la moyenne — anticiper les rappels et vérifier les effectifs bénévoles.";
                case "FAIBLE" -> "Semaine creuse — opportunité pour maintenance, formations ou campagnes de réengagement.";
                default -> "Charge normale — fonctionnement standard.";
            };

            Map<String, Object> week = new LinkedHashMap<>();
            week.put("semaine", w + 1);
            week.put("debut", weekStart.toString());
            week.put("fin", weekEnd.minusDays(1).toString());
            week.put("evenementsPlanifies", eventsInWeek);
            week.put("joursDePics", peakDays);
            week.put("chargeEstimee", estimatedLoad);
            week.put("niveau", level);
            week.put("recommandation", recommendation);
            weeks.add(week);
        }

        long criticalWeeks = weeks.stream().filter(x -> "CRITIQUE".equals(x.get("niveau"))).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("genereLe", LocalDateTime.now().toString());
        result.put("baselineChargeHebdo", Math.round(baselineLoad * 10) / 10.0);
        result.put("moyenneInscriptionsHebdo", Math.round(avgReg * 10) / 10.0);
        result.put("moyenneRapportsHebdo", Math.round(avgRep * 10) / 10.0);
        result.put("joursForts", dowWeights.entrySet().stream()
                .sorted(Map.Entry.<DayOfWeek, Integer>comparingByValue().reversed())
                .limit(3).map(e -> e.getKey().name()).toList());
        result.put("semainesCritiques", criticalWeeks);
        result.put("predictions", weeks);
        return result;
    }

    private long count(String sql, Object param) {
        return ((Number) em.createNativeQuery(sql).setParameter("p", param).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> asRows(List<?> raw) {
        List<Object[]> rows = new ArrayList<>();
        for (Object o : raw) rows.add((Object[]) o);
        return rows;
    }
}
