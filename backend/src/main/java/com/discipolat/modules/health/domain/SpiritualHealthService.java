package com.discipolat.modules.health.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Observatoire de la Santé Spirituelle — IA prédictive de décrochage.
 *
 * Modèle léger déterministe (règles pondérées) qui estime pour chaque âme
 * un risque de décrochage à 2-3 semaines, agrège par famille/département,
 * et propose des interventions automatiques.
 */
@Service
@Transactional(readOnly = true)
public class SpiritualHealthService {

    private static final Logger log = LoggerFactory.getLogger(SpiritualHealthService.class);

    private final SoulRepository soulRepository;

    public SpiritualHealthService(SoulRepository soulRepository) {
        this.soulRepository = soulRepository;
    }

    /** Vue d'ensemble : KPIs, familles à risque, distribution des risques. */
    public Map<String, Object> observatory() {
        List<Soul> souls = soulRepository.findByDeletedFalse();

        List<Map<String, Object>> riskScores = new ArrayList<>();
        Map<UUID, List<Integer>> familyRisks = new HashMap<>();
        Map<UUID, String> familyNames = new HashMap<>();
        int critical = 0, high = 0, medium = 0, low = 0;

        for (Soul soul : souls) {
            int risk = riskScore(soul);
            String label = riskLabel(risk);
            switch (label) {
                case "CRITIQUE" -> critical++;
                case "ELEVE" -> high++;
                case "MOYEN" -> medium++;
                default -> low++;
            }
            if (risk >= 50 && !soul.isDeleted()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("soulId", soul.getId());
                entry.put("nom", soul.getNom() + " " + soul.getPrenom());
                entry.put("statut", soul.getStatut().name());
                entry.put("riskScore", risk);
                entry.put("riskLabel", label);
                entry.put("intervention", interventionFor(soul, risk));
                riskScores.add(entry);
            }
            if (soul.getFamilleId() != null) {
                familyRisks.computeIfAbsent(soul.getFamilleId(), k -> new ArrayList<>()).add(risk);
                familyNames.putIfAbsent(soul.getFamilleId(),
                        soul.getNom() + " " + soul.getPrenom());
            }
        }

        // Familles à risque = moyenne des risques > 50
        List<Map<String, Object>> familiesAtRisk = familyRisks.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("familyId", e.getKey());
                    m.put("avgRisk", (int) e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0));
                    m.put("members", e.getValue().size());
                    return m;
                })
                .filter(m -> (int) m.get("avgRisk") >= 50)
                .sorted((a, b) -> Integer.compare((int) b.get("avgRisk"), (int) a.get("avgRisk")))
                .limit(10)
                .collect(Collectors.toList());

        double globalHealth = souls.isEmpty() ? 100 :
                Math.round(souls.stream().mapToInt(this::riskScore).average().orElse(0));
        globalHealth = 100 - globalHealth; // santé = inverse du risque moyen

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthScore", Math.max(0, (int) globalHealth));
        result.put("totalSouls", souls.size());
        result.put("criticalCount", critical);
        result.put("highCount", high);
        result.put("mediumCount", medium);
        result.put("lowCount", low);
        result.put("soulsAtRisk", riskScores.stream()
                .sorted((a, b) -> Integer.compare((int) b.get("riskScore"), (int) a.get("riskScore")))
                .limit(25)
                .toList());
        result.put("familiesAtRisk", familiesAtRisk);
        result.put("predictionHorizon", "2-3 semaines");
        return result;
    }

    /**
     * Score de risque 0-100 d'une âme.
     * Poids : statut pastoral (40), dernier contact (35), état spirituel (15), croissance (10).
     */
    public int riskScore(Soul soul) {
        if (soul.getStatut() == StatutAme.DECROCHE) return 100;
        int risk = 0;

        switch (soul.getStatut()) {
            case EN_VEILLE -> risk += 40;
            case NOUVEAU_CONVERTI -> risk += 20;
            case NOUVEL_ARRIVANT -> risk += 15;
            case EN_INTEGRATION -> risk += 10;
            default -> risk += 0;
        }

        LocalDateTime lastContact = soul.getDateDernierContact();
        long daysSinceContact = lastContact == null ? 60
                : ChronoUnit.DAYS.between(lastContact, LocalDateTime.now());
        if (daysSinceContact >= 28) risk += 35;
        else if (daysSinceContact >= 21) risk += 30;
        else if (daysSinceContact >= 14) risk += 18;
        else if (daysSinceContact >= 7) risk += 8;

        String etat = soul.getEtatSpirituel();
        if (etat != null) {
            String e = etat.toUpperCase();
            if (e.contains("DIFFICULTE") || e.contains("CRISE")) risk += 15;
        }

        Integer niveau = soul.getNiveauCroissance();
        if (niveau != null && niveau <= 1) risk += 10;

        return Math.min(100, risk);
    }

    private String riskLabel(int risk) {
        if (risk >= 80) return "CRITIQUE";
        if (risk >= 55) return "ELEVE";
        if (risk >= 35) return "MOYEN";
        return "FAIBLE";
    }

    /** Intervention automatique recommandée selon le profil de risque. */
    private String interventionFor(Soul soul, int risk) {
        if (soul.getStatut() == StatutAme.DECROCHE) {
            return "Escalade pasteur + visite urgente sous 72h";
        }
        if (risk >= 80) {
            return "Visite à domicile cette semaine + appel du faiseur demain";
        }
        if (risk >= 55) {
            return "Contact téléphonique sous 7 jours + invitation culte";
        }
        return "Message d'encouragement + suivi hebdo renforcé";
    }
}
