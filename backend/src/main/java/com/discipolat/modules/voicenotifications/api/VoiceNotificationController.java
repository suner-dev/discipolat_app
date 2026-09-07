package com.discipolat.modules.voicenotifications.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.voicenotifications.domain.VoiceNotificationDTO;
import com.discipolat.modules.voicenotifications.domain.VoiceNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST pour les notifications vocales — test et déclenchement manuel.
 *
 * <p>Permet de tester le flux WebSocket en déclenchant des notifications
 * vocales ciblées par rôle ou par utilisateur.</p>
 */
@RestController
@RequestMapping("/api/v1/voice-notifications")
public class VoiceNotificationController {

    private final VoiceNotificationService voiceNotificationService;
    private final SecurityUtils securityUtils;

    public VoiceNotificationController(VoiceNotificationService voiceNotificationService,
                                       SecurityUtils securityUtils) {
        this.voiceNotificationService = voiceNotificationService;
        this.securityUtils = securityUtils;
    }

    /**
     * Envoie une notification vocale de test au rôle spécifié.
     * POST /api/v1/voice-notifications/test/role/{role}
     */
    @PostMapping("/test/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<?> testNotifyRole(@PathVariable String role, @RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "Test notification");
        String messageBody = body.getOrDefault("body", "Ceci est un test de notification vocale");

        VoiceNotificationDTO notification = VoiceNotificationDTO.forRole(
                title, messageBody, role, null, VoiceNotificationDTO.Priority.NORMAL);

        voiceNotificationService.notifyRole(notification, role);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "targetRole", role,
                "notificationId", notification.id().toString()
        ));
    }

    /**
     * Envoie une notification vocale de test à un utilisateur spécifique.
     * POST /api/v1/voice-notifications/test/user/{userId}
     */
    @PostMapping("/test/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> testNotifyUser(@PathVariable UUID userId, @RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "Test notification");
        String messageBody = body.getOrDefault("body", "Ceci est un test de notification vocale");

        VoiceNotificationDTO notification = VoiceNotificationDTO.forUser(
                title, messageBody, userId, null, VoiceNotificationDTO.Priority.NORMAL);

        voiceNotificationService.notifyUser(notification, userId);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "targetUserId", userId.toString(),
                "notificationId", notification.id().toString()
        ));
    }

    /**
     * Envoie une notification vocale globale (tous les rôles).
     * POST /api/v1/voice-notifications/test/all
     */
    @PostMapping("/test/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<?> testNotifyAll(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "Notification globale");
        String messageBody = body.getOrDefault("body", "Notification vocale globale");

        VoiceNotificationDTO notification = VoiceNotificationDTO.forAll(
                title, messageBody, null, VoiceNotificationDTO.Priority.HIGH);

        voiceNotificationService.notifyAll(notification);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "target", "all",
                "notificationId", notification.id().toString()
        ));
    }

    /**
     * Statut du service de notifications vocales.
     * GET /api/v1/voice-notifications/status
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "websocketEndpoint", "/ws-church",
                "topics", List.of(
                        "/topic/voice/all",
                        "/topic/voice/role/{role}",
                        "/topic/voice/user/{userId}",
                        "/topic/voice/tenant/{tenantId}"
                )
        ));
    }
}
