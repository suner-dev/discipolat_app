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
 * Score spirituel à 12 axes (0–100).
 * 1.Santé  2.Fidélité  3.Engagement  4.Participation
 * 5.Prière  6.Service  7.Témoignage  8.Étude biblique
 * 9.Générosité  10.Leadership  11.Discipline  12.Unité communautaire
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

    @Transactional(readOnly = true)
    public Map<String, Object> computeScore(UUID soulId) {
        soulService.assertAccessible(soulId);
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("Soul", soulId));

        int sante = calculateSante(soul);
        int fidelite = calculateFidelite(soul);
        int engagement = calculateEngagement(soul);
        int participation = calculateParticipation(soul);
        int priere = calculatePriere(soul);
        int service = calculateService(soul);
        int temoignage = calculateTemoignage(soul);
        int etudeBiblique = calculateEtudeBiblique(soul);
        int generosite = calculateGenerosite(soul);
        int leadership = calculateLeadership(soul);
        int discipline = calculateDiscipline(soul);
        int unite = calculateUnite(soul);

        int global = Math.round((sante + fidelite + engagement + participation + priere + service
                + temoignage + etudeBiblique + generosite + leadership + discipline + unite) / 12.0f);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("soulId", soulId);
        result.put("global", global);
        result.put("sante", sante);
        result.put("fidelite", fidelite);
        result.put("engagement", engagement);
        result.put("participation", participation);
        result.put("priere", priere);
        result.put("service", service);
        result.put("temoignage", temoignage);
        result.put("etudeBiblique", etudeBiblique);
        result.put("generosite", generosite);
        result.put("leadership", leadership);
        result.put("discipline", discipline);
        result.put("unite", unite);
        result.put("axesCount", 12);
        result.put("label", labelFor(global));
        result.put("tendance", calculateTendance(soulId));
        result.put("semaine", LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString());
        return result;
    }

    private String calculateTendance(UUID soulId) {
        var history = scoreRepository.findBySoulIdOrderBySemaineAsc(soulId);
        if (history.size() < 2) return "STABLE";
        var last = history.get(history.size() - 1);
        var prev = history.get(history.size() - 2);
        int diff = last.getScoreGlobal() - prev.getScoreGlobal();
        if (diff > 5) return "EN_HAUSSE";
        if (diff < -5) return "EN_BAISSE";
        return "STABLE";
    }

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
    // Axes 1-4 (existantes)
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
        if (soul.getNiveauCroissance() != null) score += Math.min(10, soul.getNiveauCroissance() * 2);
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
        if (soul.getUserId() != null) {
            long registrations = eventRegistrationRepository
                    .countByUtilisateurIdAndStatutInscription(soul.getUserId(), "INSCRIT");
            score += Math.min(15, registrations * 3);
        }
        return clamp(score);
    }

    // ============================================================
    // Axes 5-12 (nouvelles)
    // ============================================================

    private int calculatePriere(Soul soul) {
        int score = 50;
        if ("MATURE".equals(soul.getEtatSpirituel())) score += 30;
        else if ("EN_CROISSANCE".equals(soul.getEtatSpirituel())) score += 15;
        if (soul.getNiveauCroissance() != null && soul.getNiveauCroissance() >= 3) score += 15;
        if (soul.getStatut() == StatutAme.ACTIF) score += 10;
        return clamp(score);
    }

    private int calculateService(Soul soul) {
        int score = 50;
        if (soul.getNiveauCroissance() != null) score += Math.min(25, soul.getNiveauCroissance() * 5);
        if ("MATURE".equals(soul.getEtatSpirituel())) score += 15;
        if ("EN_CROISSANCE".equals(soul.getEtatSpirituel())) score += 10;
        return clamp(score);
    }

    private int calculateTemoignage(Soul soul) {
        int score = 50;
        if (soul.getStatut() == StatutAme.ACTIF) score += 15;
        if (soul.getNiveauCroissance() != null && soul.getNiveauCroissance() >= 3) score += 20;
        if ("MATURE".equals(soul.getEtatSpirituel())) score += 15;
        return clamp(score);
    }

    private int calculateEtudeBiblique(Soul soul) {
        int score = 50;
        if (soul.getNiveauCroissance() != null) score += Math.min(30, soul.getNiveauCroissance() * 6);
        if ("MATURE".equals(soul.getEtatSpirituel())) score += 15;
        return clamp(score);
    }

    private int calculateGenerosite(Soul soul) {
        int score = 50;
        if (soul.getStatut() == StatutAme.ACTIF) score += 20;
        if (soul.getNiveauCroissance() != null && soul.getNiveauCroissance() >= 2) score += 15;
        if ("MATURE".equals(soul.getEtatSpirituel())) score += 10;
        return clamp(score);
    }

    private int calculateLeadership(Soul soul) {
        int score = 30;
        if (soul.getNiveauCroissance() != null) score += Math.min(40, soul.getNiveauCroissance() * 8);
        if ("MATURE".equals(soul.getEtatSpirituel())) score += 20;
        if ("EN_CROISSANCE".equals(soul.getEtatSpirituel())) score += 10;
        return clamp(score);
    }

    private int calculateDiscipline(Soul soul) {
        int score = 50;
        if (soul.getStatut() == StatutAme.ACTIF) score += 20;
        if (soul.getDateDernierContact() != null) {
            if (soul.getDateDernierContact().plusDays(14).isAfter(LocalDateTime.now())) score += 15;
            else if (soul.getDateDernierContact().plusDays(30).isAfter(LocalDateTime.now())) score -= 5;
            else score -= 20;
        }
        if (soul.getNiveauCroissance() != null && soul.getNiveauCroissance() >= 3) score += 10;
        return clamp(score);
    }

    private int calculateUnite(Soul soul) {
        int score = 50;
        if (soul.getFamilleId() != null) score += 15;
        if (soul.getFaiseurId() != null) score += 15;
        if (soul.getStatut() == StatutAme.ACTIF) score += 10;
        if (soul.getNiveauCroissance() != null && soul.getNiveauCroissance() >= 2) score += 10;
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
