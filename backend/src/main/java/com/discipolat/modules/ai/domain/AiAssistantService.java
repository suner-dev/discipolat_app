package com.discipolat.modules.ai.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.interactions.domain.Interaction;
import com.discipolat.modules.interactions.domain.InteractionRepository;
import com.discipolat.modules.prayers.domain.PrayerRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SpiritualScoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Assistant IA (déterministe, sans dépendance externe) :
 * - analyse du profil d'une âme et détection de signaux de risque ;
 * - suggestions d'actions de suivi ;
 * - encouragements bibliques contextualisés ;
 * - résumé automatique de l'activité.
 * Choix technique : moteur de règles expertes déterministe — robuste, sans coût,
 * sans envoi de données personnelles à un tiers (RGPD), extensible plus tard
 * vers un LLM externe via une interface commune.
 */
@Service
@Transactional
public class AiAssistantService {

    private final SoulRepository soulRepository;
    private final MakerReportRepository makerReportRepository;
    private final InteractionRepository interactionRepository;
    private final PrayerRepository prayerRepository;
    private final SpiritualScoreService spiritualScoreService;

    public AiAssistantService(SoulRepository soulRepository,
                              MakerReportRepository makerReportRepository,
                              InteractionRepository interactionRepository,
                              PrayerRepository prayerRepository,
                              SpiritualScoreService spiritualScoreService) {
        this.soulRepository = soulRepository;
        this.makerReportRepository = makerReportRepository;
        this.interactionRepository = interactionRepository;
        this.prayerRepository = prayerRepository;
        this.spiritualScoreService = spiritualScoreService;
    }

    /** Analyse intelligente d'une âme : signaux, suggestions, encouragement. */
    @Transactional(readOnly = true)
    public Map<String, Object> analyze(UUID soulId) {
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("Soul", soulId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("soulId", soulId);
        result.put("nom", soul.getNomComplet());
        result.put("score", spiritualScoreService.computeScore(soulId));

        List<Map<String, Object>> signaux = detectSignaux(soul);
        result.put("signaux", signaux);

        List<Map<String, Object>> suggestions = suggestActions(soul, signaux);
        result.put("suggestions", suggestions);

        result.put("encouragement", buildEncouragement(soul, signaux));
        result.put("resume", buildResume(soul));

        return result;
    }

    /** Résumé automatique de l'activité récente d'une âme. */
    @Transactional(readOnly = true)
    public String resume(UUID soulId) {
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("Soul", soulId));
        return buildResume(soul);
    }

    /** Encourager : verset/encouragement adapté à la situation. */
    @Transactional(readOnly = true)
    public Map<String, String> encouragement(UUID soulId) {
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("Soul", soulId));
        List<Map<String, Object>> signaux = detectSignaux(soul);
        String texte = buildEncouragement(soul, signaux);
        return Map.of("texte", texte, "source", "Bibliothèque d'encouragements Discipolat");
    }

    // ============================================================
    // Détection des signaux
    // ============================================================

    private List<Map<String, Object>> detectSignaux(Soul soul) {
        List<Map<String, Object>> signaux = new ArrayList<>();

        if (soul.getStatut() == StatutAme.DECROCHE) {
            signaux.add(signal("CRITIQUE", "Décrochage", "L'âme est marquée comme décrochée.", "Suivi intensif et visite pastorale recommandés."));
        }
        if (soul.getStatut() == StatutAme.EN_VEILLE) {
            signaux.add(signal("ELEVE", "Veille", "L'âme est en veille.", "Reprendre un contact bienveillant."));
        }
        if ("EN_DIFFICULTE".equals(soul.getEtatSpirituel())) {
            signaux.add(signal("ELEVE", "Difficulté spirituelle", "État spirituel en difficulté.", "Accompagnement spirituel renforcé."));
        }

        if (soul.getDateDernierContact() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    soul.getDateDernierContact().toLocalDate(), LocalDate.now());
            if (days >= 21) {
                signaux.add(signal("ELEVE", "Absence prolongée",
                        "Aucun contact depuis " + days + " jours.", "Relancer par téléphone ou visite."));
            } else if (days >= 7) {
                signaux.add(signal("MOYEN", "Contact datant de " + days + " jours",
                        "Le dernier contact date de " + days + " jours.", "Prendre contact dans la semaine."));
            }
        }

        // Absences répétées dans les 4 dernières semaines
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int absences = 0;
        for (int i = 0; i < 4; i++) {
            List<MakerReport> reports = makerReportRepository.findByAmeIdAndSemaine(
                    soul.getId(), monday.minusWeeks(i));
            for (MakerReport r : reports) {
                if (r.getPresencesParCulte() != null && !r.getPresencesParCulte().isEmpty()) {
                    boolean allAbsent = r.getPresencesParCulte().values().stream().noneMatch(b -> b);
                    if (allAbsent) absences++;
                }
            }
        }
        if (absences >= 3) {
            signaux.add(signal("ELEVE", "Absences répétées",
                    absences + " semaines d'absences sur les 4 dernières.", "Discuter de la situation avec l'âme."));
        }

        return signaux;
    }

    // ============================================================
    // Suggestions d'actions
    // ============================================================

    private List<Map<String, Object>> suggestActions(Soul soul, List<Map<String, Object>> signaux) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        boolean hasCritical = signaux.stream().anyMatch(s -> "CRITIQUE".equals(s.get("severite")));
        boolean hasHigh = signaux.stream().anyMatch(s -> "ELEVE".equals(s.get("severite")));

        if (hasCritical) {
            suggestions.add(action("ORGANISER_VISITE", "Planifier une visite pastorale",
                    "Proposer une visite au membre pour comprendre sa situation et le soutenir."));
            suggestions.add(action("INFORMER_CHAINE", "Remonter à la chaîne d'encadrement",
                    "Signaler la situation au chef de famille et au pasteur."));
        }
        if (hasHigh) {
            suggestions.add(action("CONTACTER", "Contacter dans les 48h",
                    "Un appel ou un message bienveillant peut raviver le lien."));
            suggestions.add(action("FIXER_RDV", "Fixer un rendez-vous",
                    "Proposer un rendez-vous pour échanger sur le parcours."));
        }
        if (soul.getDateNaissance() != null
                && MonthDayNow().equals(java.time.MonthDay.from(soul.getDateNaissance()))) {
            suggestions.add(action("ANNIVERSAIRE", "Souhaiter l'anniversaire",
                    "Un message d'anniversaire renforce l'appartenance."));
        }

        // Engagement positif
        if ("MATURE".equals(soul.getEtatSpirituel()) || soul.getNiveauCroissance() != null && soul.getNiveauCroissance() >= 4) {
            suggestions.add(action("CONFIER", "Confier une responsabilité",
                    "Ce membre est prêt à accompagner d'autres disciples."));
        }

        if (suggestions.isEmpty()) {
            suggestions.add(action("ENTRETIEN", "Maintenir le contact",
                    "Programmer une interaction de suivi dans les 15 jours."));
        }
        return suggestions;
    }

    // ============================================================
    // Encouragements bibliques
    // ============================================================

    private String buildEncouragement(Soul soul, List<Map<String, Object>> signaux) {
        boolean hasCritical = signaux.stream().anyMatch(s -> "CRITIQUE".equals(s.get("severite")));
        boolean hasHigh = signaux.stream().anyMatch(s -> "ELEVE".equals(s.get("severite")));

        if (hasCritical) {
            return "« L'Éternel est près de ceux qui ont le cœur brisé » (Psaume 34:19). "
                    + "Chaque retour est une fête. L'église est là pour vous, sans jugement.";
        }
        if (hasHigh) {
            return "« Celui qui a commencé en vous une bonne œuvre la rendra parfaite » (Philippiens 1:6). "
                    + "Ne vous découragez pas : chaque pas compte dans la marche.";
        }
        if ("MATURE".equals(soul.getEtatSpirituel()) || soul.getNiveauCroissance() != null && soul.getNiveauCroissance() >= 4) {
            return "« Toi donc, mon enfant, fortifie-toi dans la grâce » (2 Timothée 2:1). "
                    + "Votre fidélité est un exemple pour toute la famille de disciples.";
        }
        return "« Je puis tout par celui qui me fortifie » (Philippiens 4:13). "
                + "Continuez à grandir, l'église avance avec vous.";
    }

    // ============================================================
    // Résumé automatique
    // ============================================================

    private String buildResume(Soul soul) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<MakerReport> reports = makerReportRepository.findByAmeIdAndSemaine(soul.getId(), monday);
        boolean rapportSoumis = !reports.isEmpty() && reports.get(0).isSoumis();
        long interactions = interactionRepository.countBySoulId(soul.getId());
        long prieres = prayerRepository.findByAmeIdAndDeletedFalse(soul.getId()).size();

        StringBuilder sb = new StringBuilder();
        sb.append(soul.getNomComplet())
          .append(" est ")
          .append(soul.getStatut() == StatutAme.ACTIF ? "actif" : "en " + soul.getStatut().name().toLowerCase())
          .append(" avec un niveau de croissance de ")
          .append(soul.getNiveauCroissance() == null ? 1 : soul.getNiveauCroissance())
          .append("/5. Rapport hebdomadaire ")
          .append(rapportSoumis ? "soumis" : "non soumis")
          .append(", ")
          .append(interactions)
          .append(" interaction(s) enregistrée(s), ")
          .append(prieres)
          .append(" sujet(s) de prière lié(s).");
        return sb.toString();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Map<String, Object> signal(String severite, String type, String message, String action) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("severite", severite);
        s.put("type", type);
        s.put("message", message);
        s.put("actionConseillee", action);
        return s;
    }

    private Map<String, Object> action(String type, String titre, String description) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("type", type);
        a.put("titre", titre);
        a.put("description", description);
        return a;
    }

    private java.time.MonthDay MonthDayNow() {
        return java.time.MonthDay.now();
    }
}
