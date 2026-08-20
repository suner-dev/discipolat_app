package com.discipolat.modules.souls.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Score spirituel : indice de progression de chaque membre (0–100).
 * Composantes : santé spirituelle, fidélité, engagement, participation.
 * Le score évolue automatiquement et son historique hebdomadaire est conservé
 * pour les courbes d'évolution.
 */
@Service
@Transactional
public class SpiritualScoreService {

    private final SpiritualScoreRepository scoreRepository;
    private final SoulRepository soulRepository;
    private final MakerReportRepository makerReportRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final SoulService soulService;

    public SpiritualScoreService(SpiritualScoreRepository scoreRepository,
                                 SoulRepository soulRepository,
                                 MakerReportRepository makerReportRepository,
                                 EventRegistrationRepository eventRegistrationRepository,
                                 SoulService soulService) {
        this.scoreRepository = scoreRepository;
        this.soulRepository = soulRepository;
        this.makerReportRepository = makerReportRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.soulService = soulService;
    }

    /**
     * Calcule le score actuel d'une âme (0-100) à partir des données réelles :
     * santé spirituelle, fidélité, engagement et participation.
     * Limite connue : la participation s'appuie sur les inscriptions événementielles
     * liées au compte utilisateur — une âme sans compte lié obtient 0 en participation,
     * ce qui peut abaisser son score global.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> computeScore(UUID soulId) {
        soulService.assertAccessible(soulId);
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("Soul", soulId));

        int sante = calculateSante(soul);
        int fidelite = calculateFidelite(soul);
        int engagement = calculateEngagement(soul);
        int participation = calculateParticipation(soul);
        int global = Math.round((sante + fidelite + engagement + participation) / 4.0f);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("soulId", soulId);
        result.put("global", global);
        result.put("sante", sante);
        result.put("fidelite", fidelite);
        result.put("engagement", engagement);
        result.put("participation", participation);
        result.put("label", labelFor(global));
        result.put("semaine", LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString());
        return result;
    }

    /** Historique des scores hebdomadaires (courbe d'évolution). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistory(UUID soulId) {
        soulService.assertAccessible(soulId);
        return scoreRepository.findBySoulIdOrderBySemaineAsc(soulId)
                .stream()
                .map(s -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("semaine", s.getSemaine().toString());
                    point.put("global", s.getScoreGlobal());
                    point.put("sante", s.getSante());
                    point.put("fidelite", s.getFidelite());
                    point.put("engagement", s.getEngagement());
                    point.put("participation", s.getParticipation());
                    return point;
                })
                .toList();
    }

    /** Échantillonne et stocke le score hebdomadaire courant pour toutes les âmes actives. */
    public int snapshotAll() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Soul> souls = soulRepository.findAll().stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> s.getStatut() != StatutAme.DECROCHE)
                .toList();
        int saved = 0;
        for (Soul soul : souls) {
            if (scoreRepository.findBySoulIdAndSemaine(soul.getId(), monday).isPresent()) continue;
            int sante = calculateSante(soul);
            int fidelite = calculateFidelite(soul);
            int engagement = calculateEngagement(soul);
            int participation = calculateParticipation(soul);
            int global = Math.round((sante + fidelite + engagement + participation) / 4.0f);
            scoreRepository.save(SpiritualScore.builder()
                    .soulId(soul.getId())
                    .semaine(monday)
                    .scoreGlobal(global)
                    .sante(sante)
                    .fidelite(fidelite)
                    .engagement(engagement)
                    .participation(participation)
                    .build());
            saved++;
        }
        return saved;
    }

    // ============================================================
    // Composantes
    // ============================================================

    private int calculateSante(Soul soul) {
        int score = 50;
        if (soul.getStatut() == StatutAme.ACTIF) score += 25;
        else if (soul.getStatut() == StatutAme.EN_INTEGRATION) score += 10;
        else if (soul.getStatut() == StatutAme.EN_VEILLE) score -= 20;
        else if (soul.getStatut() == StatutAme.DECROCHE) score -= 40;

        String etat = soul.getEtatSpirituel() == null ? "" : soul.getEtatSpirituel();
        if ("MATURE".equals(etat)) score += 20;
        else if ("EN_CROISSANCE".equals(etat)) score += 10;
        else if ("EN_DIFFICULTE".equals(etat)) score -= 25;

        if (soul.getNiveauCroissance() != null) {
            score += Math.min(10, soul.getNiveauCroissance() * 2);
        }
        return clamp(score);
    }

    private int calculateFidelite(Soul soul) {
        int score = 50;
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var reports = makerReportRepository.findByAmeIdAndSemaine(soul.getId(), monday);
        if (!reports.isEmpty()) {
            var r = reports.get(0);
            if (r.getPresencesParCulte() != null && !r.getPresencesParCulte().isEmpty()) {
                long presents = r.getPresencesParCulte().values().stream().filter(b -> b).count();
                long total = r.getPresencesParCulte().size();
                if (total > 0) score += (int) (presents * 40 / total);
            }
        }
        // Contact récent
        if (soul.getDateDernierContact() != null) {
            if (soul.getDateDernierContact().plusDays(7).isAfter(LocalDateTime.now())) score += 10;
            else if (soul.getDateDernierContact().plusDays(30).isAfter(LocalDateTime.now())) score -= 10;
            else score -= 25;
        }
        return clamp(score);
    }

    private int calculateEngagement(Soul soul) {
        int score = 50;
        if (soul.getNiveauCroissance() != null) {
            if (soul.getNiveauCroissance() >= 4) score += 25;
            else if (soul.getNiveauCroissance() >= 3) score += 15;
            else if (soul.getNiveauCroissance() >= 2) score += 5;
        }
        if ("EN_CROISSANCE".equals(soul.getEtatSpirituel()) || "MATURE".equals(soul.getEtatSpirituel())) score += 10;
        return clamp(score);
    }

    private int calculateParticipation(Soul soul) {
        int score = 50;
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var reports = makerReportRepository.findByAmeIdAndSemaine(soul.getId(), monday);
        if (!reports.isEmpty() && reports.get(0).isSoumis()) score += 25;
        // Participation aux événements (liée au compte utilisateur de l'âme)
        if (soul.getUserId() != null) {
            long registrations = eventRegistrationRepository
                    .countByUtilisateurIdAndStatutInscription(soul.getUserId(), "INSCRIT");
            score += Math.min(15, registrations * 3);
        }
        return clamp(score);
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String labelFor(int score) {
        if (score >= 80) return "EXCELLENT";
        if (score >= 65) return "BON";
        if (score >= 45) return "EN_PROGRESSION";
        if (score >= 25) return "FRAGILE";
        return "A_RISQUE";
    }
}
