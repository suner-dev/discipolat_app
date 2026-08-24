package com.discipolat.modules.voicereports.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rapport Vocal IA — Synoptique de Terrain.
 *
 * Le faiseur dicte 1 à 2 minutes hors réseau ; la transcription arrive ici
 * et l'IA (déterministe, 100 % locale) en extrait :
 * - les personnes mentionnées (« j'ai vu Jean », « avec Marie »)
 * - l'humeur dominante
 * - les besoins de prière
 * - les actions à faire (« organiser une visite », « appeler demain »)
 */
@Service
@Transactional
public class VoiceReportService {

    private static final Logger log = LoggerFactory.getLogger(VoiceReportService.class);

    private static final Pattern PERSONNE_APRES = Pattern.compile(
            "(?:vu|avec|chez|pour|rencontré|rencontre|appelé|appele|visité|visite)\\s+([A-ZÉÈÀ][a-zéèêàçôîû-]+)");
    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "(organiser|organise|planifier|planifie|appeler|appelle|contacter|visiter|envoyer|rappeler|préparer|prepare)\\s+(?:une?\\s+|le\\s+|la\\s+|demain|hier|cette semaine)?([a-zéèêàçôîû]+(?:\\s+[a-zéèêàçôîû]+)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> HUMEURS = Map.ofEntries(
            Map.entry("joie", "JOYEUX"), Map.entry("heureux", "JOYEUX"), Map.entry("content", "JOYEUX"),
            Map.entry("triste", "TRISTE"), Map.entry("découragé", "DECOURAGE"), Map.entry("decourage", "DECOURAGE"),
            Map.entry("anxieux", "ANXIEUX"), Map.entry("inquiet", "ANXIEUX"), Map.entry("peur", "ANXIEUX"),
            Map.entry("malade", "SOUFFRANT"), Map.entry("souffre", "SOUFFRANT"), Map.entry("fatigué", "FATIGUE"),
            Map.entry("reconnaissant", "RECONNAISSANT"), Map.entry("gratitude", "RECONNAISSANT"));

    private final VoiceReportRepository repository;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public VoiceReportService(VoiceReportRepository repository,
                              EntityPropagationPublisher propagationPublisher,
                              SecurityUtils securityUtils) {
        this.repository = repository;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    /** Crée un rapport vocal et lance l'extraction d'entités. */
    public VoiceReport create(VoiceReport report) {
        report.setTenantId(securityUtils.getCurrentTenantId());
        if (report.getAuthorId() == null) {
            report.setAuthorId(securityUtils.getCurrentUserId());
        }
        report.setExtractedEntities(extractEntities(report.getTranscript()));
        report.setProcessed(true);
        VoiceReport saved = repository.save(report);
        propagationPublisher.publishCreated("VOICE_REPORT", saved.getId(),
                Map.of("authorId", saved.getAuthorId().toString(),
                        "durationSeconds", saved.getDurationSeconds()),
                "Rapport vocal synchronisé (" + saved.getDurationSeconds() + "s)");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<VoiceReport> recent() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<VoiceReport> mine() {
        return repository.findByAuthorIdOrderByCreatedAtDesc(securityUtils.getCurrentUserId());
    }

    /**
     * Extraction d'entités IA — déterministe, sans appel réseau.
     * Retourne un JSON simple : {"personnes":[],"humeur":"","besoinPriere":false,"actions":[]}
     */
    public String extractEntities(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return "{\"personnes\":[],\"humeur\":\"\",\"besoinPriere\":false,\"actions\":[]}";
        }
        Set<String> personnes = new LinkedHashSet<>();
        Matcher m = PERSONNE_APRES.matcher(transcript);
        while (m.find()) {
            personnes.add(m.group(1));
        }

        String humeur = "";
        String lower = transcript.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : HUMEURS.entrySet()) {
            if (lower.contains(e.getKey())) {
                humeur = e.getValue();
                break;
            }
        }

        boolean besoinPriere = lower.contains("prière") || lower.contains("priere")
                || lower.contains("prier pour") || lower.contains("intercéder");

        List<String> actions = new ArrayList<>();
        Matcher am = ACTION_PATTERN.matcher(transcript);
        while (am.find() && actions.size() < 5) {
            actions.add((am.group(1) + " " + am.group(2)).trim());
        }

        return "{\"personnes\":" + jsonList(personnes)
                + ",\"humeur\":\"" + humeur + "\""
                + ",\"besoinPriere\":" + besoinPriere
                + ",\"actions\":" + jsonList(actions) + "}";
    }

    private static String jsonList(Collection<String> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String item : items) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(item.replace("\"", "'")).append("\"");
        }
        return sb.append("]").toString();
    }

    /**
     * P14 — Génère un rapport structuré Markdown à partir des entités extraites.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> generateStructuredReport(java.util.UUID reportId) {
        VoiceReport report = repository.findById(reportId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("VoiceReport", reportId));

        String entities = report.getExtractedEntities();
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", report.getId());
        result.put("authorId", report.getAuthorId());
        result.put("createdAt", report.getCreatedAt().toString());
        result.put("durationSeconds", report.getDurationSeconds());

        // Parse extracted entities JSON
        String personnes = extractJsonArray(entities, "personnes");
        String humeur = extractJsonValue(entities, "humeur");
        String besoinsPriere = extractJsonValue(entities, "besoinPriere");
        String actions = extractJsonArray(entities, "actions");

        // Build structured markdown
        StringBuilder md = new StringBuilder();
        md.append("# Rapport Pastoral Structuré\n\n");
        md.append("**Date :** ").append(report.getCreatedAt() != null ? report.getCreatedAt().toLocalDate() : "N/A").append("\n");
        md.append("**Durée :** ").append(report.getDurationSeconds()).append(" secondes\n\n");

        if (!personnes.isEmpty()) {
            md.append("## Personnes mentionnées\n");
            for (String p : personnes.split(",")) {
                md.append("- ").append(p.trim()).append("\n");
            }
            md.append("\n");
        }

        if (!humeur.isEmpty()) {
            md.append("## Humeur dominante\n");
            md.append(humeur).append("\n\n");
        }

        if ("true".equals(besoinsPriere)) {
            md.append("## Besoins de prière\n");
            md.append("Oui — des personnes mentionnées ont besoin de prière.\n\n");
        }

        if (!actions.isEmpty()) {
            md.append("## Actions à suivre\n");
            for (String a : actions.split(",")) {
                md.append("- [ ] ").append(a.trim()).append("\n");
            }
            md.append("\n");
        }

        md.append("---\n");
        md.append("*Rapport généré automatiquement à partir de la transcription vocale.*");

        result.put("markdown", md.toString());
        result.put("personnes", personnes.isEmpty() ? java.util.List.of() : java.util.List.of(personnes.split(",")));
        result.put("humeur", humeur);
        result.put("besoinPriere", "true".equals(besoinsPriere));
        result.put("actions", actions.isEmpty() ? java.util.List.of() : java.util.List.of(actions.split(",")));

        return result;
    }

    /**
     * P14 — Extrait toutes les actions à suivre de tous les rapports.
     */
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> extractAllActionItems() {
        List<VoiceReport> reports = repository.findTop50ByOrderByCreatedAtDesc();
        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();

        for (VoiceReport report : reports) {
            String entities = report.getExtractedEntities();
            String actions = extractJsonArray(entities, "actions");
            if (!actions.isEmpty()) {
                for (String action : actions.split(",")) {
                    java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("reportId", report.getId());
                    item.put("authorId", report.getAuthorId());
                    item.put("createdAt", report.getCreatedAt().toString());
                    item.put("action", action.trim());
                    item.put("done", false);
                    items.add(item);
                }
            }
        }
        return items;
    }

    private String extractJsonArray(String json, String key) {
        if (json == null) return "";
        String searchKey = "\"" + key + "\":[";
        int start = json.indexOf(searchKey);
        if (start < 0) return "";
        start = json.indexOf('[', start);
        int end = json.indexOf(']', start);
        if (end < 0) return "";
        String content = json.substring(start + 1, end);
        return content.replace("\"", "").trim();
    }

    private String extractJsonValue(String json, String key) {
        if (json == null) return "";
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf('"', start);
        if (end < 0) return "";
        return json.substring(start, end);
    }
}
