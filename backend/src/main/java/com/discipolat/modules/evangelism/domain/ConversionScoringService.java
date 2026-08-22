package com.discipolat.modules.evangelism.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Scoring intelligent du pipeline d'évangélisation.
 *
 * Calcule pour chaque prospect une probabilité de conversion (0-100) basée sur :
 * - la profondeur dans le pipeline (étapes franchies)
 * - la vitesse de progression récente
 * - l'ancienneté du dernier changement d'étape (stagnation = risque)
 */
@Service
@Transactional(readOnly = true)
public class ConversionScoringService {

    /** Poids par étape : plus l'étape est avancée, plus la base est forte. */
    private static final Map<EvangelismEtape, Integer> ETAPE_BASE = Map.ofEntries(
            Map.entry(EvangelismEtape.NOUVELLE_AME, 5),
            Map.entry(EvangelismEtape.PREMIER_CONTACT, 15),
            Map.entry(EvangelismEtape.VISITE, 28),
            Map.entry(EvangelismEtape.INVITATION, 40),
            Map.entry(EvangelismEtape.PREMIER_CULTE, 52),
            Map.entry(EvangelismEtape.SUIVI, 62),
            Map.entry(EvangelismEtape.BAPTEME, 78),
            Map.entry(EvangelismEtape.DEPARTEMENT, 85),
            Map.entry(EvangelismEtape.FAMILLE, 90),
            Map.entry(EvangelismEtape.DISCIPOLAT, 95),
            Map.entry(EvangelismEtape.LEADER, 99));

    private static final int STAGNATION_ALERT_DAYS = 21;

    private final EvangelismTrackRepository trackRepository;
    private final EvangelismStageHistoryRepository historyRepository;

    public ConversionScoringService(EvangelismTrackRepository trackRepository,
                                    EvangelismStageHistoryRepository historyRepository) {
        this.trackRepository = trackRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Score de conversion d'un prospect : base étape + bonus vélocité - pénalité stagnation.
     */
    public Map<String, Object> scoreFor(UUID soulId) {
        return trackRepository.findBySoulId(soulId)
                .map(this::buildScore)
                .orElse(defaultScore());
    }

    /** Scores de tous les prospects actifs, triés du plus prometteur au plus à risque. */
    public List<Map<String, Object>> scoreAll(List<EvangelismTrack> tracks) {
        return tracks.stream()
                .map(this::buildScore)
                .sorted(Comparator.comparingLong(m -> -(long) m.get("score")))
                .toList();
    }

    private Map<String, Object> buildScore(EvangelismTrack track) {
        int base = ETAPE_BASE.getOrDefault(track.getEtape(), 10);

        long daysSinceChange = ChronoUnit.DAYS.between(
                track.getDateEtape() != null ? track.getDateEtape() : LocalDate.now(), LocalDate.now());

        long stagesPassed = historyRepository.countByTrackId(track.getId());

        // Bonus vélocité : progression rapide = engagement fort
        int velocityBonus = daysSinceChange <= 7 ? 8 : daysSinceChange <= 14 ? 4 : 0;

        // Pénalité stagnation
        int stagnationPenalty = 0;
        boolean stagnant = false;
        if (daysSinceChange > STAGNATION_ALERT_DAYS && track.getEtape() != EvangelismEtape.LEADER) {
            stagnationPenalty = (int) Math.min(30, (daysSinceChange - STAGNATION_ALERT_DAYS));
            stagnant = true;
        }

        long score = Math.max(2, Math.min(99, base + velocityBonus + Math.min(10, stagesPassed * 2) - stagnationPenalty));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("soulId", track.getSoulId());
        result.put("trackId", track.getId());
        result.put("etape", track.getEtape().name());
        result.put("score", score);
        result.put("label", labelFor(score));
        result.put("stagesPassed", stagesPassed);
        result.put("daysSinceLastChange", daysSinceChange);
        result.put("stagnant", stagnant);
        result.put("multiplicationPotential", multiplicationPotential(score, daysSinceChange));
        result.put("recommendation", recommendation(track.getEtape(), stagnant, daysSinceChange));
        return result;
    }

    private Map<String, Object> defaultScore() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", 0L);
        result.put("label", "Aucun pipeline");
        result.put("stagnant", false);
        return result;
    }

    private String labelFor(long score) {
        if (score >= 80) return "Conversion imminente";
        if (score >= 60) return "Très prometteur";
        if (score >= 40) return "En progression";
        if (score >= 20) return "À nourrir";
        return "Froid / à réactiver";
    }

    /** Prédiction de multiplication : chance de devenir faiseur dans ~18 mois. */
    private String multiplicationPotential(long score, long daysSinceChange) {
        if (score >= 85 && daysSinceChange <= 30) return "ÉLEVÉ — candidat faiseur < 12 mois";
        if (score >= 65) return "BON — potentiel faiseur sous 18 mois";
        if (score >= 45) return "MOYEN — poursuivre le suivi";
        return "FAIBLE — consolider les bases";
    }

    private String recommendation(EvangelismEtape etape, boolean stagnant, long daysSinceChange) {
        if (stagnant) {
            return "Stagne depuis " + daysSinceChange + " jours : organiser une visite personnelle cette semaine";
        }
        return switch (etape) {
            case NOUVELLE_AME -> "Planifier le premier contact téléphonique";
            case PREMIER_CONTACT -> "Programmer une visite à domicile";
            case VISITE -> "Inviter au prochain culte";
            case INVITATION -> "Confirmer sa venue et l'accueillir personnellement";
            case PREMIER_CULTE -> "Démarrer le suivi structuré (fiche disciple)";
            case SUIVI -> "Préparer le baptême";
            case BAPTEME -> "Intégrer dans un département selon ses dons";
            case DEPARTEMENT -> "Rattacher à une famille de disciples";
            case FAMILLE -> "Démarrer officiellement le discipolat";
            case DISCIPOLAT -> "Former pour multiplier : identifier des prospects à confier";
            case LEADER -> "Modèle accompli — faire de lui un mentor";
        };
    }
}
