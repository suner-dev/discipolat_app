package com.discipolat.modules.prophetic.domain;

/**
 * Contrat d'intégration Speech-to-Text.
 *
 * <p>Permet de brancher n'importe quel fournisseur de transcription
 * (Whisper cloud, API Google Speech, modèle local...) sans coupler le
 * domaine métier à un prestataire. Chaque fournisseur doit gérer la
 * configuration, les erreurs, les quotas et la suppression des fichiers.</p>
 *
 * <p>Une implémentation ne doit JAMAIS simuler une transcription.</p>
 */
public interface SpeechToTextProvider {

    /**
     * Nom technique du fournisseur (ex : {@code whisper}).
     */
    String name();

    /**
     * Transcrit un échantillon audio en texte.
     *
     * @param audioBytes contenu audio brut
     * @param filename   nom du fichier original (extension importante)
     * @param language   langue attendue (ex : {@code fr}) ou {@code null}
     * @return la transcription
     * @throws SpeechToTextException si la transcription échoue
     */
    String transcribe(byte[] audioBytes, String filename, String language);

    /**
     * Indique si le fournisseur est réellement configuré (clé/URL présentes).
     */
    boolean isConfigured();
}