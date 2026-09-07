package com.discipolat.modules.prophetic.domain;

/**
 * Contrat d'intégration Text-to-Speech.
 *
 * <p>Permet de brancher n'importe quel fournisseur de synthèse vocale
 * (OpenAI TTS, Google TTS, ElevenLabs...) sans coupler le domaine métier
 * à un prestataire.</p>
 *
 * <p>Une implémentation ne doit JAMAIS simuler une synthèse vocale.</p>
 */
public interface TextToSpeechProvider {

    /**
     * Nom technique du fournisseur (ex : {@code openai-tts}).
     */
    String name();

    /**
     * Synthétise un texte en audio.
     *
     * @param text     texte à synthétiser
     * @param language langue (ex : {@code fr}) ou {@code null}
     * @param voice    voix à utiliser (ex : {@code alloy}, {@code nova}) ou {@code null}
     * @return les bytes audio (MP3)
     * @throws TextToSpeechException si la synthèse échoue
     */
    byte[] synthesize(String text, String language, String voice);

    /**
     * Indique si le fournisseur est réellement configuré (clé/URL présentes).
     */
    boolean isConfigured();
}
