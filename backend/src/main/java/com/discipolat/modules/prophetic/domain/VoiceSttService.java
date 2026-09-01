package com.discipolat.modules.prophetic.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrateur Speech-to-Text.
 *
 * <p>Délègue la transcription audio au fournisseur réellement configuré.
 * La transcription est ensuite transmise à {@link VoiceAssistantService}
 * pour l'interprétation et la réponse. Aucune transcription n'est simulée.</p>
 */
@Service
public class VoiceSttService {

    private static final Logger log = LoggerFactory.getLogger(VoiceSttService.class);

    private final List<SpeechToTextProvider> providers;
    private final VoiceAssistantService voiceAssistantService;
    private final SecurityUtils securityUtils;

    public VoiceSttService(List<SpeechToTextProvider> providers,
                           VoiceAssistantService voiceAssistantService,
                           SecurityUtils securityUtils) {
        this.providers = providers;
        this.voiceAssistantService = voiceAssistantService;
        this.securityUtils = securityUtils;
    }

    public SpeechToTextStatus status() {
        List<String> configured = new ArrayList<>();
        for (SpeechToTextProvider p : providers) {
            if (p.isConfigured()) configured.add(p.name());
        }
        return new SpeechToTextStatus(configured.isEmpty() ? null : configured, configured);
    }

    /**
     * Transcrit l'audio puis le traite comme une commande vocale.
     *
     * @return la réponse complète (transcription + intention + réponse IA)
     */
    public Object transcribeAndProcess(byte[] audio, String filename, String language, String sessionId) {
        SpeechToTextProvider provider = providers.stream()
                .filter(SpeechToTextProvider::isConfigured)
                .findFirst()
                .orElseThrow(() -> new SpeechToTextException(
                        "Aucun fournisseur Speech-to-Text configuré (APP_SPEECH_API_URL / APP_SPEECH_API_KEY). "
                                + "Configurez un fournisseur réel pour activer la dictée vocale."));

        String transcription = provider.transcribe(audio, filename, language);
        log.info("[STT] Transcription obtenue via {}", provider.name());

        java.util.UUID sessionUuid = sessionId != null && !sessionId.isBlank()
                ? java.util.UUID.fromString(sessionId)
                : null;
        return voiceAssistantService.processVoiceMessage(
                securityUtils.getCurrentUserId(), transcription, sessionUuid);
    }

    public record SpeechToTextStatus(String activeProvider, List<String> configuredProviders) {}
}