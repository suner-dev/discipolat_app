package com.discipolat.modules.twin.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.evangelism.domain.EvangelismTrackRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Jumeau Numérique de l'Assemblée — simulations « et si ».
 *
 * Repart des statistiques réelles du tenant (âmes, faiseurs, pipeline,
 * décrochages) et projette la croissance selon des scénarios :
 * - doubler les faiseurs
 * - améliorer la rétention (moins de décrochages)
 * - accélérer le pipeline d'évangélisation
 */
@Service
@Transactional(readOnly = true)
public class DigitalTwinService {

    private final SoulRepository soulRepository;
    private final EvangelismTrackRepository trackRepository;

    public DigitalTwinService(SoulRepository soulRepository,
                              EvangelismTrackRepository trackRepository) {
        this.soulRepository = soulRepository;
        this.trackRepository = trackRepository;
    }

    /** Photographie actuelle de l'assemblée = point de départ du jumeau. */
    public Map<String, Object> snapshot() {
        long totalSouls = soulRepository.countByDeletedFalse();
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("totalSouls", totalSouls);
        snap.put("active", soulRepository.countByStatut(StatutAme.ACTIF));
        snap.put("nouveauxConvertiS", soulRepository.countByStatut(StatutAme.NOUVEAU_CONVERTI));
        snap.put("enIntegration", soulRepository.countByStatut(StatutAme.EN_INTEGRATION));
        snap.put("enVeille", soulRepository.countByStatut(StatutAme.EN_VEILLE));
        snap.put("decroches", soulRepository.countByStatut(StatutAme.DECROCHE));
        snap.put("pipelinesActifs", trackRepository.findAll().size());
        return snap;
    }

    /**
     * Simulation : projette le nombre d'âmes mois par mois.
     *
     * Modèle de croissance mensuel :
     *   conversions ≈ pipelines actifs × taux de conversion mensuel × pipelineBoost × facteurFaiseurs
     *   pertes      ≈ âmes actives × taux d'attrition mensuel ÷ (1 + retentionGain/100)
     */
    public Map<String, Object> simulate(double faiseurMultiplier,
                                        int retentionGain,
                                        double pipelineBoost,
                                        int months) {
        months = Math.max(1, Math.min(36, months));
        faiseurMultiplier = Math.max(0.5, Math.min(5, faiseurMultiplier));
        pipelineBoost = Math.max(0.5, Math.min(5, pipelineBoost));
        retentionGain = Math.max(0, Math.min(80, retentionGain));

        Map<String, Object> snap = snapshot();
        double activeSouls = ((Number) snap.get("active")).doubleValue()
                + ((Number) snap.get("enIntegration")).doubleValue();
        double pipelines = ((Number) snap.get("pipelinesActifs")).doubleValue();
        double decroches = ((Number) snap.get("decroches")).doubleValue();

        double conversionRate = 0.12;          // 12 % des pipelines aboutissent chaque mois
        double attritionRate = activeSouls > 0 ? Math.min(0.05, (decroches / 6.0) / activeSouls) : 0.02;

        List<Map<String, Object>> projection = new ArrayList<>();
        double souls = soulRepository.countByDeletedFalse();
        double bestCase = souls, worstCase = souls;

        for (int m = 1; m <= months; m++) {
            double gains = pipelines * conversionRate * pipelineBoost * faiseurMultiplier;
            double losses = Math.max(activeSouls, 1) * attritionRate / (1 + retentionGain / 100.0);
            souls += gains - losses;
            activeSouls = Math.max(0, activeSouls + gains - losses);

            bestCase += gains * 1.3 - losses * 0.7;   // scénario optimiste
            worstCase += gains * 0.7 - losses * 1.3;  // scénario pessimiste

            projection.add(Map.of(
                    "month", m,
                    "souls", Math.round(souls),
                    "bestCase", Math.round(Math.max(bestCase, souls)),
                    "worstCase", Math.round(Math.max(worstCase, 0))));
        }

        // Besoin en leaders : ~1 faiseur pour 8 disciples
        long currentLeaders = countFaiseurs();
        long neededLeaders = Math.round(souls / 8.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseline", snapshot());
        result.put("assumptions", Map.of(
                "faiseurMultiplier", faiseurMultiplier,
                "retentionGainPercent", retentionGain,
                "pipelineBoost", pipelineBoost,
                "months", months));
        result.put("projection", projection);
        result.put("projectedTotal", Math.round(souls));
        result.put("growthPercent", souls > 0
                ? Math.round((souls / Math.max(1, soulRepository.countByDeletedFalse()) - 1) * 100) : 0);
        result.put("currentLeaders", currentLeaders);
        result.put("neededLeaders", neededLeaders);
        result.put("leaderGap", Math.max(0, neededLeaders - currentLeaders));
        result.put("recommendation", buildRecommendation(faiseurMultiplier, retentionGain, neededLeaders, currentLeaders));
        return result;
    }

    private long countFaiseurs() {
        // Approximation via les pipelines au stade LEADER
        return trackRepository.findByEtapeOrderByDateEtapeDesc(
                com.discipolat.modules.evangelism.domain.EvangelismEtape.LEADER).size();
    }

    private String buildRecommendation(double faiseurMultiplier, int retentionGain, long needed, long current) {
        if (needed > current) {
            return "Former " + (needed - current)
                    + " faiseurs supplémentaires pour soutenir cette croissance — prioriser les scores de conversion ≥ 65.";
        }
        if (faiseurMultiplier >= 2 && retentionGain >= 20) {
            return "Scénario ambitieux : sécuriser l'encadrement avant d'accélérer davantage.";
        }
        return "Trajectoire saine : maintenir le rythme de suivi et surveiller les familles à risque.";
    }
}
