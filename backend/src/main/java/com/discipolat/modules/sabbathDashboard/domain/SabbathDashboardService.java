package com.discipolat.modules.sabbathDashboard.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * P3 #106 — Tableau de bord sabbatique.
 * Vue consolidée de l'état spirituel de l'église avec indicateurs de maturité
 * sur les 12 axes (présence, prière, étude, communion, service, évangélisation,
 * dons, discipline, mentorat, adoration, témoignage, intercession).
 */
@Service
@Transactional(readOnly = true)
public class SabbathDashboardService {

    @PersistenceContext
    private EntityManager em;

    public Map<String, Object> getDashboard() {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.getCurrentTenantId();
        LocalDateTime since30 = LocalDateTime.now().minusDays(30);

        long totalAmes = count("SELECT count(*) FROM souls WHERE tenant_id = :t AND deleted = false", "t", tenantId);
        if (totalAmes == 0) totalAmes = 1;

        long amesActives = count("SELECT count(*) FROM souls WHERE tenant_id = :t AND deleted = false AND statut = 'ACTIF'", "t", tenantId);
        long faiseursActifs = countDistinct("SELECT count(DISTINCT faiseur_id) FROM souls WHERE tenant_id = :t AND deleted = false AND faiseur_id IS NOT NULL", "t", tenantId);
        long contactsRecents = count("SELECT count(*) FROM souls WHERE tenant_id = :t AND deleted = false AND date_dernier_contact >= :p", "p", since30);
        long interactions30j = count("SELECT count(*) FROM soul_interactions WHERE created_at >= :p", "p", since30);
        long prieres30j = count("SELECT count(*) FROM prayers WHERE created_at >= :p", "p", since30);
        long formationsActives = count("SELECT count(*) FROM courses", null, null);
        long participationsFormation = count("SELECT count(*) FROM course_enrollments", null, null);
        long famillesARisque = count("SELECT count(*) FROM families WHERE deleted = false AND niveau_risque = 'ELEVE'", null, null);
        long dons30j = count("SELECT count(*) FROM finance_transactions WHERE created_at >= :p", "p", since30);
        long visites30j = count("SELECT count(*) FROM visits WHERE date_prevue >= :d", "d",
                java.time.LocalDate.now().minusDays(30));

        // 12 axes de maturité (0-100), normalisés par rapport aux âmes actives
        double base = Math.max(amesActives, 1);
        List<Map<String, Object>> axes = new ArrayList<>();
        addAxis(axes, "Présence & assiduité", pct(contactsRecents, base));
        addAxis(axes, "Vie de prière", pct(prieres30j * 4, base));
        addAxis(axes, "Étude biblique & formation", pct(participationsFormation * 8, base));
        addAxis(axes, "Communion fraternelle", pct(interactions30j * 3, base));
        addAxis(axes, "Service & bénévolat", pct(faiseursActifs * 6, base));
        addAxis(axes, "Évangélisation", pct(visites30j * 5, base));
        addAxis(axes, "Libéralité & dons", pct(dons30j * 2, base));
        addAxis(axes, "Discipline spirituelle", pct((contactsRecents + prieres30j) * 2, base));
        addAxis(axes, "Mentorat (faiseur ↔ disciple)", pct(faiseursActifs * 10, base));
        addAxis(axes, "Adoration", pct(prieres30j * 2, base));
        addAxis(axes, "Témoignage", pct(interactions30j, base));
        addAxis(axes, "Intercession", pct(visites30j * 3, base));

        int maturiteGlobale = axes.stream().mapToInt(a -> (int) a.get("score")).sum() / axes.size();

        String saisonSpirituelle;
        String orientationPastorale;
        if (maturiteGlobale >= 70) {
            saisonSpirituelle = "Moisson — l'église est en pleine maturité";
            orientationPastorale = "Multipliez les envoyeurs : lancement d'églises filles, mentorat de nouveaux leaders, projets d'impact communautaire.";
        } else if (maturiteGlobale >= 45) {
            saisonSpirituelle = "Croissance — fondations solides, croissance en cours";
            orientationPastorale = "Consolidation : renforcez les axes les plus faibles et structurez le suivi des nouveaux convertis.";
        } else {
            saisonSpirituelle = "Semis — phase de fondation";
            orientationPastorale = "Priorité aux fondations : enseignement de base, intégration des nouveaux, mobilisation des premiers faiseurs.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("genereLe", LocalDateTime.now().toString());
        result.put("totalAmes", totalAmes);
        result.put("amesActives", amesActives);
        result.put("tauxActivite", pct(amesActives, totalAmes));
        result.put("faiseursActifs", faiseursActifs);
        result.put("ratioFaiseurs", pct(faiseursActifs, Math.max(amesActives, 1)));
        result.put("famillesARisque", famillesARisque);
        result.put("maturiteGlobale", maturiteGlobale);
        result.put("saisonSpirituelle", saisonSpirituelle);
        result.put("orientationPastorale", orientationPastorale);
        result.put("axes", axes);
        return result;
    }

    private static double pct(long part, double total) {
        return Math.min(100.0, Math.round(part / total * 1000.0));
    }

    private static void addAxis(List<Map<String, Object>> axes, String nom, double score) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("axe", nom);
        a.put("score", (int) score);
        a.put("niveau", score >= 70 ? "MATURE" : score >= 40 ? "EN_CROISSANCE" : "EMBRYONNAIRE");
        axes.add(a);
    }

    private long count(String sql, String paramName, Object param) {
        var q = em.createNativeQuery(sql);
        if (paramName != null) q.setParameter(paramName, param);
        return ((Number) q.getSingleResult()).longValue();
    }

    private long countDistinct(String sql, String paramName, Object param) {
        return count(sql, paramName, param);
    }
}
