package com.discipolat.common.infrastructure.config;

import com.discipolat.common.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * « Aucune IA disponible » — le pendant honnête de
 * {@code SpeechToTextException}/{@code STT_NOT_CONFIGURED} et
 * {@code TextToSpeechException}/{@code TTS_NOT_CONFIGURED} pour le chemin
 * Ollama.
 *
 * <p>Sans cette exception, un appel à un Ollama absent se manifeste par une
 * erreur de connexion, un 500 {@code An unexpected error occurred} ou pire,
 * un repli déterministe silencieux : l'appelant ne peut pas distinguer
 * « aucune IA configurée » d'« IA cassée ».</p>
 *
 * <p>Hérite de {@link DomainException} : le
 * {@code GlobalExceptionHandler} existant le traduit déjà en
 * {@code 503 Service Unavailable} avec pour titre le code
 * {@code AI_NOT_CONFIGURED} et pour détail le motif exact du blocage.</p>
 */
public class AiNotConfiguredException extends DomainException {

    /** Code stable, à tester côté client (comme {@code STT_NOT_CONFIGURED}). */
    public static final String CODE = "AI_NOT_CONFIGURED";

    public AiNotConfiguredException(String reason) {
        super("IA locale non configurée : " + reason, HttpStatus.SERVICE_UNAVAILABLE, CODE);
    }
}
