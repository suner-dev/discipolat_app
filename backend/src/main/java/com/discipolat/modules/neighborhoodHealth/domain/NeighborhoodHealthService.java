package com.discipolat.modules.neighborhoodHealth.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * P3 #104 — Analyse de santé spirituelle par quartier.
 * Agrégation géographique des âmes et familles par zone avec heatmap,
 * identification des zones à couverture faible.
 */
@Service
@Transactional(readOnly = true)
public class NeighborhoodHealthService {

    @PersistenceContext
    private EntityManager em;

    private static final double COVERAGE_THRESHOLD = 0.6; // 60 % d'âmes rattachées à un faiseur

    public Map<String, Object> getHealthByZone() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.getCurrentTenantId();

        // Agrégation par zone sur les âmes
        List<Object[]> rows = asRows(em.createNativeQuery("""
                SELECT coalesce(nullif(trim(zone), ''), 'NON_DEFINI') AS zone,
                       count(*) AS total,
                       count(faiseur_id) AS couverts,
                       count(*) FILTER (WHERE statut = 'ACTIF') AS actifs,
                       count(*) FILTER (WHERE date_dernier_contact >= :recency) AS contactsRecents,
                       avg(niveau_croissance) AS croissanceMoyenne
                FROM souls
                WHERE tenant_id = :tenant AND deleted = false
                GROUP BY 1 ORDER BY count(*) DESC
                """)
                .setParameter("tenant", tenantId)
                .setParameter("recency", LocalDateTime.now().minusDays(30))
                .getResultList());

        // Coordonnées moyennes par zone pour la heatmap
        List<Object[]> geoRows = asRows(em.createNativeQuery("""
                SELECT coalesce(nullif(trim(zone), ''), 'NON_DEFINI') AS zone,
                       avg(latitude) AS lat, avg(longitude) AS lng
                FROM souls
                WHERE tenant_id = :tenant AND deleted = false
                  AND latitude IS NOT NULL AND longitude IS NOT NULL
                GROUP BY 1
                """)
                .setParameter("tenant", tenantId)
                .getResultList());
        Map<String, double[]> geo = new HashMap<>();
        for (Object[] g : geoRows) {
            if (g[1] != null && g[2] != null) {
                geo.put(g[0].toString(), new double[]{((Number) g[1]).doubleValue(), ((Number) g[2]).doubleValue()});
            }
        }

        List<Map<String, Object>> zones = new ArrayList<>();
        for (Object[] row : rows) {
            String zone = row[0].toString();
            long total = ((Number) row[1]).longValue();
            long couverts = ((Number) row[2]).longValue();
            long actifs = ((Number) row[3]).longValue();
            long contactsRecents = ((Number) row[4]).longValue();
            double croissanceMoyenne = row[5] == null ? 0.0 : ((Number) row[5]).doubleValue();

            double tauxCouverture = total == 0 ? 0.0 : (double) couverts / total;
            double tauxContactRecent = total == 0 ? 0.0 : (double) contactsRecents / total;
            int healthScore = (int) Math.round(
                    tauxCouverture * 40 + tauxContactRecent * 30 + Math.min(croissanceMoyenne / 5.0, 1.0) * 30);

            String status = healthScore >= 70 ? "BONNE" : healthScore >= 45 ? "MOYENNE" : "FAIBLE";

            String actionRecommandee = switch (status) {
                case "FAIBLE" -> "Priorité pastorale : affecter de nouveaux faiseurs et organiser des visites porte-à-porte dans cette zone.";
                case "MOYENNE" -> "Renforcer le suivi : relancer les contacts anciens (> 30 jours) et recruter des faiseurs locaux.";
                default -> "Zone bien couverte — maintenir le rythme et identifier des leaders locaux potentiels.";
            };

            Map<String, Object> z = new LinkedHashMap<>();
            z.put("zone", zone);
            z.put("totalAmes", total);
            z.put("amesCouvertes", couverts);
            z.put("tauxCouverture", Math.round(tauxCouverture * 1000) / 1000.0);
            z.put("amesActives", actifs);
            z.put("contactsRecents30j", contactsRecents);
            z.put("croissanceMoyenne", Math.round(croissanceMoyenne * 10) / 10.0);
            z.put("healthScore", healthScore);
            z.put("statut", status);
            z.put("actionRecommandee", actionRecommandee);
            if (geo.containsKey(zone)) {
                z.put("latitude", geo.get(zone)[0]);
                z.put("longitude", geo.get(zone)[1]);
            }
            zones.add(z);
        }

        long weakZones = zones.stream().filter(z -> !"BONNE".equals(z.get("statut"))).count();
        long amesNonZonees = countSoulsWithoutZone(tenantId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("genereLe", LocalDateTime.now().toString());
        result.put("seuilCouverture", COVERAGE_THRESHOLD);
        result.put("totalZones", zones.size());
        result.put("zonesACouvertureFaible", weakZones);
        result.put("amesSansZone", amesNonZonees);
        result.put("zones", zones);
        return result;
    }

    private long countSoulsWithoutZone(UUID tenantId) {
        return ((Number) em.createNativeQuery(
                        "SELECT count(*) FROM souls WHERE tenant_id = :t AND deleted = false AND (zone IS NULL OR trim(zone) = '')")
                .setParameter("t", tenantId)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> asRows(List<?> raw) {
        List<Object[]> rows = new ArrayList<>();
        for (Object o : raw) rows.add((Object[]) o);
        return rows;
    }
}
