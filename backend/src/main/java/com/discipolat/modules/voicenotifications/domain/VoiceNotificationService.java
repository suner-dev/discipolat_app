package com.discipolat.modules.voicenotifications.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service d'émission des notifications vocales en temps réel via WebSocket STOMP.
 *
 * <p>Ce service encapsule {@link SimpMessagingTemplate} pour envoyer des notifications
 * ciblées aux clients connectés (React Web et Flutter Mobile).</p>
 *
 * <p>Canaux disponibles :</p>
 * <ul>
 *   <li>{@code /topic/voice/all} — Toutes les notifications globales</li>
 *   <li>{@code /topic/voice/role/{role}} — Notifications par rôle (ex: ADMIN-PASTEUR)</li>
 *   <li>{@code /topic/voice/user/{userId}} — Notifications personnelles par utilisateur</li>
 *   <li>{@code /topic/voice/tenant/{tenantId}} — Notifications par église/tenant</li>
 * </ul>
 */
@Service
public class VoiceNotificationService {

    private static final Logger log = LoggerFactory.getLogger(VoiceNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public VoiceNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Envoie une notification vocale ciblée par rôle.
     *
     * @param notification la notification à envoyer
     * @param targetRole  le rôle cible (ex: "ADMIN-PASTEUR", "MEMBRE")
     */
    public void notifyRole(VoiceNotificationDTO notification, String targetRole) {
        String destination = "/topic/voice/role/" + targetRole;
        messagingTemplate.convertAndSend(destination, notification);
        log.info("[VoiceNotification] Rôle {} — {} : {}", targetRole, notification.title(), notification.body());
    }

    /**
     * Envoie une notification vocale ciblée par utilisateur.
     *
     * @param notification la notification à envoyer
     * @param userId      l'ID de l'utilisateur cible
     */
    public void notifyUser(VoiceNotificationDTO notification, UUID userId) {
        String destination = "/topic/voice/user/" + userId;
        messagingTemplate.convertAndSend(destination, notification);
        log.info("[VoiceNotification] User {} — {} : {}", userId, notification.title(), notification.body());
    }

    /**
     * Envoie une notification vocale globale (tous les rôles, tous les utilisateurs).
     *
     * @param notification la notification à envoyer
     */
    public void notifyAll(VoiceNotificationDTO notification) {
        String destination = "/topic/voice/all";
        messagingTemplate.convertAndSend(destination, notification);
        log.info("[VoiceNotification] ALL — {} : {}", notification.title(), notification.body());
    }

    /**
     * Envoie une notification vocale à tous les membres d'un tenant (église).
     *
     * @param notification la notification à envoyer
     * @param tenantId    l'ID du tenant
     */
    public void notifyTenant(VoiceNotificationDTO notification, String tenantId) {
        String destination = "/topic/voice/tenant/" + tenantId;
        messagingTemplate.convertAndSend(destination, notification);
        log.info("[VoiceNotification] Tenant {} — {} : {}", tenantId, notification.title(), notification.body());
    }

    /**
     * Envoie une notification à plusieurs rôles simultanément.
     *
     * @param notification la notification à envoyer
     * @param roles       la liste des rôles cibles
     */
    public void notifyRoles(VoiceNotificationDTO notification, java.util.List<String> roles) {
        for (String role : roles) {
            notifyRole(notification, role);
        }
    }

    /**
     * Notifie le rôle et l'utilisateur spécifique (double ciblage).
     *
     * @param notification la notification à envoyer
     * @param targetRole  le rôle cible
     * @param userId      l'utilisateur cible (peut être null)
     */
    public void notifyRoleAndUser(VoiceNotificationDTO notification, String targetRole, UUID userId) {
        notifyRole(notification, targetRole);
        if (userId != null) {
            notifyUser(notification, userId);
        }
    }
}
