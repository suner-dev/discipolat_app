package com.discipolat.modules.voicenotifications.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO unifié pour les notifications vocales en temps réel.
 *
 * <p>Ce DTO est publié sur le topic WebSocket STOMP et consommé par les clients
 * React (Web Speech API) et Flutter (flutter_tts) pour la lecture vocale.</p>
 *
 * <p>Structure du message JSON envoyé aux clients :</p>
 * <pre>
 * {
 *   "id": "uuid",
 *   "title": "Nouveau membre",
 *   "body": "Jean Dupont a rejoint l'église",
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "targetRole": "ADMIN-PASTEUR",
 *   "targetUserId": "uuid ou null",
 *   "actionUrl": "/members/123",
 *   "priority": "HIGH",
 *   "locale": "fr-FR"
 * }
 * </pre>
 */
public record VoiceNotificationDTO(
        UUID id,
        String title,
        String body,
        Instant timestamp,
        String targetRole,
        UUID targetUserId,
        String actionUrl,
        Priority priority,
        String locale
) {
    /**
     * Priorité de la notification — influence le comportement TTS.
     */
    public enum Priority {
        LOW,      // Notification silencieuse, pas de lecture automatique
        NORMAL,   // Lecture vocale standard
        HIGH,     // Lecture vocale immédiate
        URGENT    // Lecture vocale répétée jusqu'à acknowledgement
    }

    /**
     * Crée une notification ciblée par rôle.
     */
    public static VoiceNotificationDTO forRole(String title, String body, String targetRole,
                                                String actionUrl, Priority priority) {
        return new VoiceNotificationDTO(
                UUID.randomUUID(), title, body, Instant.now(),
                targetRole, null, actionUrl, priority, "fr-FR"
        );
    }

    /**
     * Crée une notification ciblée par utilisateur.
     */
    public static VoiceNotificationDTO forUser(String title, String body, UUID targetUserId,
                                                String actionUrl, Priority priority) {
        return new VoiceNotificationDTO(
                UUID.randomUUID(), title, body, Instant.now(),
                null, targetUserId, actionUrl, priority, "fr-FR"
        );
    }

    /**
     * Crée une notification globale (tous les rôles).
     */
    public static VoiceNotificationDTO forAll(String title, String body,
                                               String actionUrl, Priority priority) {
        return new VoiceNotificationDTO(
                UUID.randomUUID(), title, body, Instant.now(),
                null, null, actionUrl, priority, "fr-FR"
        );
    }
}
