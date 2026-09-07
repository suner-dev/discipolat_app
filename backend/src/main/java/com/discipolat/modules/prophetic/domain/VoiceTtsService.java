package com.discipolat.modules.prophetic.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrateur Text-to-Speech.
 *
 * <p>Délègue la synthèse vocale au fournisseur réellement configuré.
 * Aucune synthèse n'est simulée.</p>
 */
@Service
public class VoiceTtsService {

    private static final Logger log = LoggerFactory.getLogger(VoiceTtsService.class);

    private final List<TextToSpeechProvider> providers;

    public VoiceTtsService(List<TextToSpeechProvider> providers) {
        this.providers = providers;
    }

    /**
     * Statut des fournisseurs TTS configurés.
     */
    public TtsStatus status() {
        List<String> configured = new ArrayList<>();
        for (TextToSpeechProvider p : providers) {
            if (p.isConfigured()) configured.add(p.name());
        }
        return new TtsStatus(configured.isEmpty() ? null : String.join(",", configured), configured);
    }

    /**
     * Synthétise un texte en audio MP3.
     *
     * @return les bytes audio MP3
     * @throws TextToSpeechException si aucun fournisseur n'est configuré ou si la synthèse échoue
     */
    public byte[] synthesize(String text, String language, String voice) {
        TextToSpeechProvider provider = providers.stream()
                .filter(TextToSpeechProvider::isConfigured)
                .findFirst()
                .orElseThrow(() -> new TextToSpeechException(
                        "Aucun fournisseur Text-to-Speech configuré (app.tts.api-url / app.tts.api-key). " +
                                "Configurez un fournisseur réel pour activer la synthèse vocale."));

        return provider.synthesize(text, language, voice);
    }

    public record TtsStatus(String activeProvider, List<String> configuredProviders) {}
}
