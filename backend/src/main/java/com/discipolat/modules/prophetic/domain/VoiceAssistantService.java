package com.discipolat.modules.prophetic.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.ai.domain.AiAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P0 #5 — Assistant vocal conversationnel "PasteurBot" offline.
 *
 * Architecture :
 *   1. Le microphone capture l'audio (mobile/web)
 *   2. La transcription est envoyée ici (texte)
 *   3. Ce service analyse l'intention et génère une réponse
 *   4. La réponse peut être lue à voix haute (TTS côté client)
 *
 * Mode offline : un modèle léger (Whisper local sur mobile + LLM embarqué)
 * permet de fonctionner sans connexion. Ce backend est le fallback online.
 *
 * Commandes vocales supportées :
 *   - "Montre-moi les familles en décrochement"
 *   - "Combien de nouveaux convertis ce mois ?"
 *   - "Génère un rapport de la semaine"
 *   - "Envoie un encouragement à [nom]"
 *   - "Quels sont les prochains événements ?"
 */
@Service
@Transactional
public class VoiceAssistantService {

    private static final Logger log = LoggerFactory.getLogger(VoiceAssistantService.class);

    private final AiAssistantService aiAssistant;
    private final SecurityUtils securityUtils;

    /** Historique des sessions vocales par utilisateur */
    private final Map<UUID, List<Map<String, Object>>> voiceSessions = new ConcurrentHashMap<>();

    public VoiceAssistantService(AiAssistantService aiAssistant, SecurityUtils securityUtils) {
        this.aiAssistant = aiAssistant;
        this.securityUtils = securityUtils;
    }

    /**
     * Point d'entrée : transcrit un message vocal et retourne une réponse.
     * En mode online, le Whisper backend fait la transcription.
     * Le texte transcrit est traité comme une question texte classique.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> processVoiceMessage(UUID userId, String transcribedText, UUID sessionId) {
        // Détecter l'intention
        VoiceIntent intent = detectIntent(transcribedText);

        // Construire la réponse
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("transcription", transcribedText);
        response.put("intent", intent.name());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("sessionId", sessionId != null ? sessionId.toString() : UUID.randomUUID().toString());

        // Déléguer à l'assistant IA pastoral pour la réponse contextuelle
        Map<String, Object> aiResponse = aiAssistant.chat(transcribedText, userId);
        response.put("reply", aiResponse.getOrDefault("reply", "Commande non reconnue."));
        response.put("sources", aiResponse.getOrDefault("sources", List.of()));

        // Suggestions de commandes vocales suivantes
        response.put("suggestions", getFollowUpSuggestions(intent));

        return response;
    }

    /**
     * Détecte l'intention d'un message vocal.
     */
    private VoiceIntent detectIntent(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("famille") && (lower.contains("risque") || lower.contains("décrochement") || lower.contains("absent")))
            return VoiceIntent.FAMILIES_AT_RISK;
        if (lower.contains("convertis") || lower.contains("nouveau"))
            return VoiceIntent.NEW_CONVERTS;
        if (lower.contains("rapport") || lower.contains("résumé") || lower.contains("synthèse"))
            return VoiceIntent.GENERATE_REPORT;
        if (lower.contains("événement") || lower.contains("prochain") || lower.contains("calendrier"))
            return VoiceIntent.UPCOMING_EVENTS;
        if (lower.contains("présence") || lower.contains("taux") || lower.contains("assiduité"))
            return VoiceIntent.PRESENCE_STATS;
        if (lower.contains("encouragement") || lower.contains("message") || lower.contains("envoie"))
            return VoiceIntent.SEND_ENCOURAGEMENT;
        if (lower.contains("prière") || lower.contains("prières"))
            return VoiceIntent.PRAYER_REQUEST;
        if (lower.contains("aide") || lower.contains("commande") || lower.contains("help"))
            return VoiceIntent.HELP;
        return VoiceIntent.GENERAL_QUERY;
    }

    /**
     * Suggestions de commandes vocales selon l'intention détectée.
     */
    private List<Map<String, String>> getFollowUpSuggestions(VoiceIntent intent) {
        return switch (intent) {
            case FAMILIES_AT_RISK -> List.of(
                    Map.of("command", "Planifie des visites pour ces familles", "icon", "🏠"),
                    Map.of("command", "Envoie un encouragement au chef de famille", "icon", "💬"),
                    Map.of("command", "Affiche le détail d'une famille", "icon", "📊"));
            case NEW_CONVERTS -> List.of(
                    Map.of("command", "Montre le parcours des nouveaux convertis", "icon", "🌱"),
                    Map.of("command", "Envoie un message de bienvenue", "icon", "👋"),
                    Map.of("command", "Planifie un RDV pastoral", "icon", "📅"));
            case GENERATE_REPORT -> List.of(
                    Map.of("command", "Génère le rapport PDF", "icon", "📄"),
                    Map.of("command", "Envoye le rapport par email", "icon", "📧"),
                    Map.of("command", "Montre les tendances du mois", "icon", "📈"));
            default -> List.of(
                    Map.of("command", "Montre-moi les familles en décrochement", "icon", "🏠"),
                    Map.of("command", "Combien de nouveaux convertis ?", "icon", "🌱"),
                    Map.of("command", "Génère un rapport de la semaine", "icon", "📋"));
        };
    }

    /**
     * Commandes vocales prédéfinies pour le tutoriel.
     */
    public List<Map<String, String>> getVoiceCommands() {
        return List.of(
                Map.of("command", "Montre-moi les familles en décrochement",
                        "description", "Affiche les familles à risque de décrochage",
                        "category", "SUIVI"),
                Map.of("command", "Combien de nouveaux convertis ce mois ?",
                        "description", "Nombre et liste des nouveaux convertis",
                        "category", "STATISTIQUES"),
                Map.of("command", "Génère un rapport de la semaine",
                        "description", "Synthèse des rapports hebdomadaires",
                        "category", "RAPPORTS"),
                Map.of("command", "Quels sont les prochains événements ?",
                        "description", "Calendrier des prochains événements",
                        "category", "ÉVÉNEMENTS"),
                Map.of("command", "Envoie un encouragement à [nom]",
                        "description", "Génère et envoie un message personnalisé",
                        "category", "COMMUNICATION"),
                Map.of("command", "Montre le taux de présence",
                        "description", "Statistiques de présence de la semaine",
                        "category", "STATISTIQUES"),
                Map.of("command", "Quelles sont les alertes actives ?",
                        "description", "Liste des alertes en cours",
                        "category", "ALERTES"));
    }

    /** Health check de l'assistant vocal. */
    @Transactional(readOnly = true)
    public Map<String, Object> healthCheck() {
        Map<String, Object> aiHealth = aiAssistant.checkHealth();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("aiBackend", aiHealth);
        result.put("supportedLanguages", List.of("fr", "en", "pt"));
        result.put("offlineMode", "Whisper local + LLM embarqué (mobile)");
        return result;
    }

    public enum VoiceIntent {
        FAMILIES_AT_RISK, NEW_CONVERTS, GENERATE_REPORT,
        UPCOMING_EVENTS, PRESENCE_STATS, SEND_ENCOURAGEMENT,
        PRAYER_REQUEST, HELP, GENERAL_QUERY
    }
}
