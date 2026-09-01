package com.discipolat.modules.prophetic.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Adapter Speech-to-Text compatible OpenAI Whisper
 * ({@code POST /v1/audio/transcriptions}, champ multipart {@code file}).
 *
 * <p>Configuration via variables d'environnement :</p>
 * <ul>
 *   <li>{@code APP_SPEECH_API_URL} — URL de base de l'API (ex : https://api.openai.com/v1)</li>
 *   <li>{@code APP_SPEECH_API_KEY} — clé d'API</li>
 *   <li>{@code APP_SPEECH_MODEL} — modèle (ex : whisper-1)</li>
 * </ul>
 *
 * <p>Aucun credential n'est stocké en code. Une seule instance par tenant
 * est utilisée ; les quotas et erreurs sont remontés à l'appelant.</p>
 */
@Component
public class WhisperSpeechToTextProvider implements SpeechToTextProvider {

    private static final Logger log = LoggerFactory.getLogger(WhisperSpeechToTextProvider.class);

    private final String apiUrl;
    private final String apiKey;
    private final String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public WhisperSpeechToTextProvider(
            @Value("${app.speech.api-url:}") String apiUrl,
            @Value("${app.speech.api-key:}") String apiKey,
            @Value("${app.speech.model:whisper-1}") String model) {
        this.apiUrl = trimToNull(apiUrl);
        this.apiKey = trimToNull(apiKey);
        this.model = model;
    }

    @Override
    public String name() {
        return "whisper";
    }

    @Override
    public boolean isConfigured() {
        return apiUrl != null && apiKey != null;
    }

    @Override
    public String transcribe(byte[] audioBytes, String filename, String language) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new SpeechToTextException("Aucun audio reçu pour la transcription");
        }
        if (!isConfigured()) {
            throw new SpeechToTextException(
                    "Fournisseur STT non configuré (app.speech.api-url / app.speech.api-key absents). "
                            + "Aucune transcription n'est possible tant qu'un fournisseur réel n'est pas configuré.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename != null && !filename.isBlank() ? filename : "audio.m4a";
                }
            });
            body.add("model", model);
            if (language != null && !language.isBlank()) {
                body.add("language", language);
            }

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    apiUrl + "/audio/transcriptions", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String text = (String) response.getBody().get("text");
                if (text != null && !text.isBlank()) {
                    log.info("[STT] Transcription {} caractères via {}", text.length(), name());
                    return text.trim();
                }
            }
            throw new SpeechToTextException("Réponse STT invalide (" + response.getStatusCode() + ")");
        } catch (SpeechToTextException e) {
            throw e;
        } catch (Exception e) {
            log.error("[STT] Échec de transcription via {}", name(), e);
            throw new SpeechToTextException("La transcription a échoué auprès du fournisseur " + name(), e);
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}