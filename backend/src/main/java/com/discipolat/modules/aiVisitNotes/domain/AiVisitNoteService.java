package com.discipolat.modules.aiVisitNotes.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class AiVisitNoteService {

    private static final Logger log = LoggerFactory.getLogger(AiVisitNoteService.class);

    @Value("${app.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${app.ai.model:llama3}")
    private String modelName;

    private final AiVisitNoteRepository noteRepo;

    public AiVisitNoteService(AiVisitNoteRepository noteRepo) { this.noteRepo = noteRepo; }

    public AiVisitNote create(AiVisitNote note) {
        note.setTenantId(TenantContext.getCurrentTenantId());
        note.setCreatedAt(LocalDateTime.now());
        // Generate AI summary from transcription
        if (note.getRawTranscription() != null) {
            note.setAiSummary(generateSummary(note.getRawTranscription()));
            note.setAiActionItems(generateActionItems(note.getRawTranscription()));
            note.setAiSentiment(analyzeSentiment(note.getRawTranscription()));
        }
        return noteRepo.save(note);
    }

    public List<AiVisitNote> listByMember(UUID memberId) {
        return noteRepo.findByTenantIdAndMemberIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), memberId);
    }

    public List<AiVisitNote> listAll() {
        return noteRepo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public AiVisitNote verify(UUID id) {
        AiVisitNote note = noteRepo.findById(id).orElseThrow();
        note.setIsVerified(true);
        return noteRepo.save(note);
    }

    /**
     * Appelle Ollama pour générer un résumé de la transcription (2-3 phrases).
     * Retombe sur l'heuristique locale si Ollama n'est pas disponible.
     */
    private String generateSummary(String transcription) {
        String systemPrompt = "Tu es un assistant pastoral. Tu réponds uniquement en français, de façon concise.";
        String userPrompt = "Résume en 2-3 phrases cette transcription de visite pastorale :\n\n" + transcription;
        String aiSummary = callOllama(systemPrompt, userPrompt);
        if (aiSummary != null && !aiSummary.isBlank()) return aiSummary.trim();
        return fallbackSummary(transcription);
    }

    /**
     * Appelle Ollama pour extraire les actions de suivi (tableau JSON de chaînes courtes).
     * Retombe sur l'heuristique locale si Ollama n'est pas disponible.
     */
    private String generateActionItems(String transcription) {
        String systemPrompt = "Tu es un assistant pastoral. Tu réponds uniquement en français, "
                + "uniquement avec un tableau JSON de chaînes courtes, sans texte additionnel.";
        String userPrompt = "Extrais de cette transcription de visite pastorale les actions de suivi à entreprendre. "
                + "Réponds UNIQUEMENT avec un tableau JSON de chaînes courtes, ex : [\"action 1\",\"action 2\"] "
                + "(tableau vide [] si aucune action) :\n\n" + transcription;
        String content = callOllama(systemPrompt, userPrompt);
        if (content != null && !content.isBlank()) {
            String json = stripMarkdownFences(content);
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                return json.substring(start, end + 1);
            }
        }
        return fallbackActionItems(transcription);
    }

    /**
     * Appelle Ollama pour analyser le ton de la visite.
     * Retourne uniquement POSITIVE / NEUTRAL / CONCERNING / CRITICAL,
     * sinon retombe sur l'heuristique locale.
     */
    private String analyzeSentiment(String transcription) {
        String systemPrompt = "Tu es un assistant pastoral. Tu réponds uniquement en français, "
                + "par un seul mot parmi : POSITIVE, NEUTRAL, CONCERNING, CRITICAL.";
        String userPrompt = "Analyse le ton de cette transcription de visite pastorale. "
                + "Réponds UNIQUEMENT par un seul mot parmi POSITIVE, NEUTRAL, CONCERNING, CRITICAL :\n\n" + transcription;
        String content = callOllama(systemPrompt, userPrompt);
        if (content != null) {
            String upper = stripMarkdownFences(content).trim().toUpperCase(Locale.ROOT);
            if (upper.contains("POSITIVE")) return "POSITIVE";
            if (upper.contains("CRITICAL")) return "CRITICAL";
            if (upper.contains("CONCERNING")) return "CONCERNING";
            if (upper.contains("NEUTRAL")) return "NEUTRAL";
        }
        return fallbackSentiment(transcription);
    }

    /**
     * Heuristique locale de secours : résumé simple de la transcription.
     */
    private String fallbackSummary(String transcription) {
        int words = transcription.split("\\s+").length;
        return String.format("Visite pastorale enregistrée (%d mots). Résumé IA : %s", words,
            transcription.length() > 200 ? transcription.substring(0, 200) + "..." : transcription);
    }

    /**
     * Heuristique locale de secours : détection de mots-clés pour les actions de suivi.
     */
    private String fallbackActionItems(String transcription) {
        List<String> items = new ArrayList<>();
        String lower = transcription.toLowerCase();
        if (lower.contains("prière") || lower.contains("prier")) items.add("Planifier une session de prière de suivi");
        if (lower.contains("maladie") || lower.contains("santé")) items.add("Organiser un soutien médical");
        if (lower.contains("besoin") || lower.contains("difficulté")) items.add("Évaluer les besoins matériels");
        if (lower.contains("formation") || lower.contains("apprendre")) items.add("Proposer une formation adaptée");
        if (items.isEmpty()) items.add("Suivi standard recommandé");
        return items.toString();
    }

    /**
     * Heuristique locale de secours : détection de mots-clés pour le ton de la visite.
     */
    private String fallbackSentiment(String transcription) {
        String lower = transcription.toLowerCase();
        int positive = 0, negative = 0;
        for (String w : List.of("merci", "joie", "bien", "progrès", "grâce", "victoire")) if (lower.contains(w)) positive++;
        for (String w : List.of("problème", "difficulté", "souffrance", "peur", "triste", "colère")) if (lower.contains(w)) negative++;
        if (negative > positive + 2) return "CRITICAL";
        if (negative > positive) return "CONCERNING";
        if (positive > negative) return "POSITIVE";
        return "NEUTRAL";
    }

    /**
     * Supprime les balises markdown (```json ... ```) éventuelles autour de la réponse du LLM.
     */
    private String stripMarkdownFences(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
            if (cleaned.startsWith("json")) cleaned = cleaned.substring(4);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    /**
     * Appelle Ollama (/api/chat, non-streaming) pour générer une réponse IA.
     * Retourne null si Ollama n'est pas disponible.
     */
    private String callOllama(String systemPrompt, String userPrompt) {
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelName);
            body.put("stream", false);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = rt.postForEntity(ollamaUrl + "/api/chat", request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) response.getBody().get("message");
                if (msg != null && msg.containsKey("content")) {
                    return (String) msg.get("content");
                }
            }
        } catch (Exception e) {
            log.info("Ollama not available ({}), using heuristic fallback", e.getMessage());
        }
        return null;
    }
}
