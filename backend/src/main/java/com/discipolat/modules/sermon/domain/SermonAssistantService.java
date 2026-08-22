package com.discipolat.modules.sermon.domain;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Assistant IA pour la rédaction de sermons.
 *
 * Génère 3 à 5 structures de sermon (accroche, points, illustrations,
 * application, appel) à partir d'un passage biblique et d'un thème.
 *
 * IA déterministe (règles + bibliothèque de structures éprouvées) :
 * aucune donnée ne quitte le serveur — conforme RGPD. Un LLM optionnel
 * peut être branché plus tard derrière la même interface.
 */
@Service
public class SermonAssistantService {

    private static final List<String> ACCROCHES = List.of(
        "Imaginez la scène : %s. C'est exactement là que %s nous rejoint.",
        "Une question nous poursuit aujourd'hui : que faire quand %s ? Le texte de %s répond.",
        "Si vous ne deviez retenir qu'une chose ce matin, ce serait celle-ci : %s.",
        "Il était une fois… non, pas un conte, mais %s — une réalité vécue par %s.",
        "Le monde dit une chose ; %s en dit une autre. Voyons laquelle tient debout.");

    private static final String[][] STRUCTURES = {
        {"Expositrice", "Le texte dans son contexte", "La vérité centrale révélée", "Ce que cela change pour nous", "L'appel de cette semaine"},
        {"Narrative", "Un personnage aux prises avec %THEME%", "Le tournant du récit", "La main de Dieu dans l'histoire", "Notre propre tournant"},
        {"Problème-Solution", "Le problème : %THEME% nous dépasse", "Les solutions humaines qui échouent", "La réponse de l'Évangile", "Vivre la solution dès aujourd'hui"},
        {"Interrogative", "Pourquoi %THEME% ?", "Que fait Dieu face à %THEME% ?", "Comment répondre selon le texte ?", "Trois engagements concrets"},
        {"Testimoniale", "Une vie transformée malgré %THEME%", "Le moment de la rencontre", "La transformation en cours", "Et vous, quel est votre prochain pas ?"}
    };

    /** Déclaré AVANT le bloc statique qui l'initialise (ordre d'exécution textuel). */
    private static final List<String> DEFAULT_APPLICATIONS = new ArrayList<>();

    private static final Map<String, List<String>> APPLICATIONS_PAR_AUDIENCE = new HashMap<>();

    static {
        APPLICATIONS_PAR_AUDIENCE.put("JEUNES", List.of(
                "Défi 7 jours : une action concrète par jour",
                "Petit groupe de discussion après le culte",
                "Verset à mémoriser en semaine"));
        APPLICATIONS_PAR_AUDIENCE.put("FAMILLES", List.of(
                "Temps de prière familiale 10 minutes chaque soir",
                "Réconciliation concrète à initier cette semaine",
                "Tableau de gratitude affiché à la maison"));
        APPLICATIONS_PAR_AUDIENCE.put("FAISEURS", List.of(
                "Identifier une âme à visiter avant dimanche prochain",
                "Partager le message avec son trio de suivi",
                "Rapport hebdo enrichi d'un témoignage"));
        DEFAULT_APPLICATIONS.addAll(List.of(
                "Un engagement personnel écrit et daté",
                "Un pas d'obéissance concret avant dimanche",
                "Inviter une personne à vivre la même découverte"));
    }

    /**
     * Génère les structures de sermon.
     *
     * @param passage   référence ou extrait biblique (ex: « Psaume 23 »)
     * @param theme     thème central (ex: « l'espérance dans l'épreuve »)
     * @param audience  JEUNES / FAMILLES / FAISEURS / autre (optionnel)
     */
    public Map<String, Object> generateOutlines(String passage, String theme,
                                                String audience, String durationMinutes) {
        if (passage == null || passage.isBlank()) {
            throw new IllegalArgumentException("Le passage biblique est obligatoire");
        }
        String sujet = theme == null || theme.isBlank() ? defaultTheme(passage) : theme.trim();
        String aud = audience == null ? "GENERALE" : audience.trim().toUpperCase(Locale.ROOT);
        int minutes;
        try {
            minutes = durationMinutes == null ? 20 : Integer.parseInt(durationMinutes);
        } catch (NumberFormatException e) {
            minutes = 20;
        }
        int count = Math.max(3, Math.min(5, minutes >= 40 ? 5 : minutes >= 25 ? 4 : 3));

        List<Map<String, Object>> outlines = new ArrayList<>();
        Random random = new Random(passage.toLowerCase(Locale.ROOT).hashCode());

        for (int i = 0; i < count; i++) {
            String[] structure = STRUCTURES[i % STRUCTURES.length];
            String type = structure[0];

            List<String> points = new ArrayList<>();
            for (int p = 1; p < structure.length; p++) {
                points.add(structure[p].replace("%THEME%", sujet));
            }

            Map<String, Object> outline = new LinkedHashMap<>();
            outline.put("type", type);
            outline.put("title", buildTitle(sujet, i, random));
            outline.put("accroche", ACCROCHES.get(random.nextInt(ACCROCHES.size()))
                    .formatted(shorten(sujet), passage));
            outline.put("points", points);
            outline.put("illustration", illustrationFor(i));
            outline.put("applications", APPLICATIONS_PAR_AUDIENCE.getOrDefault(aud, DEFAULT_APPLICATIONS));
            outline.put("appel", "Invitation à répondre : " + sujet + " — décision personnelle, mains levées ou prière guidée.");
            outline.put("dureeEstimee", estimateDuration(points.size()));
            outlines.add(outline);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passage", passage);
        result.put("theme", sujet);
        result.put("audience", aud);
        result.put("outlines", outlines);
        result.put("versetsSuggeres", suggestVerses(sujet));
        return result;
    }

    private String buildTitle(String theme, int index, Random random) {
        List<String> patterns = List.of(
                "%s — Quand Dieu parle",
                "%s au cœur du récit",
                "De %s à la victoire",
                "%s : le plan caché de Dieu",
                "Marcher dans %s dès aujourd'hui");
        String base = patterns.get((index + random.nextInt(patterns.size())) % patterns.size());
        return base.formatted(capitalize(shorten(theme)));
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String shorten(String theme) {
        return theme.length() > 60 ? theme.substring(0, 57) + "…" : theme;
    }

    private static String defaultTheme(String passage) {
        return "la fidélité de Dieu proclamée dans " + passage;
    }

    private static String illustrationFor(int index) {
        return switch (index % 3) {
            case 0 -> "Histoire vraie : un disciple découragé qui a vu Dieu tenir sa promesse en 48h.";
            case 1 -> "Image quotidienne : le bambou qui met des années à pousser puis grandit de 20 m en 6 semaines.";
            default -> "Analogie : le GPS recalcule à chaque erreur mais n'abandonne jamais le voyageur.";
        };
    }

    private static String estimateDuration(int points) {
        return switch (points) {
            case 2 -> "~20 minutes";
            case 3 -> "~30 minutes";
            default -> "~45 minutes";
        };
    }

    /** Mini-bibliothèque thématique de versets (étendue côté réseau). */
    private static List<Map<String, String>> suggestVerses(String theme) {
        String t = theme.toLowerCase();
        List<Map<String, String>> verses = new ArrayList<>();
        verses.add(Map.of("ref", "Ésaïe 41:10", "texte",
                "Ne crains rien, car je suis avec toi ; je te fortifie, je viens à ton secours."));
        if (t.contains("espoir") || t.contains("espérance") || t.contains("épreuve") || t.contains("epreuve")) {
            verses.add(Map.of("ref", "Jérémie 29:11", "texte",
                    "Car je connais les projets que j'ai formés sur vous, projets de paix et non de malheur."));
            verses.add(Map.of("ref", "Romains 15:13", "texte",
                    "Que le Dieu de l'espérance vous remplisse de toute joie et de toute paix dans la foi !"));
        }
        if (t.contains("famille") || t.contains("maison")) {
            verses.add(Map.of("ref", "Josué 24:15", "texte",
                    "Quant à moi et ma maison, nous servirons l'Éternel."));
        }
        if (t.contains("mission") || t.contains("évangélisation") || t.contains("evangelisation")) {
            verses.add(Map.of("ref", "Matthieu 28:19", "texte",
                    "Allez, faites de toutes les nations des disciples."));
        }
        verses.add(Map.of("ref", "Psaume 119:105", "texte",
                "Ta parole est une lampe à mes pieds, et une lumière sur mon sentier."));
        return verses;
    }
}
