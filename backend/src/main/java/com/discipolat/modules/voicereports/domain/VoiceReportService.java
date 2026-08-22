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
}
