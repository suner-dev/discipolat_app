package com.discipolat.modules.prophetic.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Adapter Text-to-Speech compatible OpenAI TTS
 * ({@code POST /v1/audio/speech}).
 *
 * <p>Configuration via variables d'environnement :</p>
 * <ul>
 *   <li>{@code APP_TTS_API_URL} — URL de base de l'API (ex : https://api.openai.com/v1)</li>
 *   <li>{@code APP_TTS_API_KEY} — clé d'API</li>
 *   <li>{@code APP_TTS_MODEL} — modèle (ex : tts-1, tts-1-hd)</li>
 *   <li>{@code APP_TTS_VOICE} — voix par défaut (ex : alloy, nova, echo)</li>
 * </ul>
 *
 * <p>Aucun credential n'est stocké en code. Si non configuré →
 * réponse honnête {@code 503 TTS_NOT_CONFIGURED}.</p>
 */
@Component
public class OpenAiTextToSpeechProvider implements TextToSpeechProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTextToSpeechProvider.class);

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final String defaultVoice;

    private final RestTemplate restTemplate = new RestTemplate();

    public OpenAiTextToSpeechProvider(
            @Value("${app.tts.api-url:}") String apiUrl,
            @Value("${app.tts.api-key:}") String apiKey,
            @Value("${app.tts.model:tts-1}") String model,
            @Value("${app.tts.voice:alloy}") String defaultVoice) {
        this.apiUrl = trimToNull(apiUrl);
        this.apiKey = trimToNull(apiKey);
        this.model = model;
        this.defaultVoice = defaultVoice;
    }

    @Override
    public String name() {
        return "openai-tts";
    }

    @Override
    public boolean isConfigured() {
        return apiUrl != null && apiKey != null;
    }

    @Override
    public byte[] synthesize(String text, String language, String voice) {
        if (text == null || text.isBlank()) {
            throw new TextToSpeechException("Aucun texte à synthétiser");
        }
        if (!isConfigured()) {
            throw new TextToSpeechException(
                    "Fournisseur TTS non configuré (app.tts.api-url / app.tts.api-key absents). " +
                            "Aucune synthèse vocale n'est possible tant qu'un fournisseur réel n'est pas configuré.");
        }

        try {
            String effectiveVoice = voice != null && !voice.isBlank() ? voice : defaultVoice;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", text,
                    "voice", effectiveVoice,
                    "response_format", "mp3"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    apiUrl + "/audio/speech", request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                log.info("[TTS] Synthèse {} caractères → {} bytes audio via {}",
                        text.length(), response.getBody().length, name());
                return response.getBody();
            }
            throw new TextToSpeechException("Réponse TTS invalide (" + response.getStatusCode() + ")");
        } catch (TextToSpeechException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TTS] Échec de synthèse via {}", name(), e);
            throw new TextToSpeechException("La synthèse vocale a échoué auprès du fournisseur " + name(), e);
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
