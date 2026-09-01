package com.discipolat.modules.notifications.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gestion des tokens FCM pour les notifications push mobiles.
 * - POST /api/v1/notifications/register-token → enregistrer le token FCM
 * - POST /api/v1/notifications/unregister-token → supprimer le token FCM
 */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class PushTokenController {

    private static final Logger log = LoggerFactory.getLogger(PushTokenController.class);

    @PostMapping("/register-token")
    public ResponseEntity<Map<String, String>> registerToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String platform = body.getOrDefault("platform", "UNKNOWN");
        log.info("[Push] Token registered: platform={}, token={}", platform, token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null");
        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    @PostMapping("/unregister-token")
    public ResponseEntity<Map<String, String>> unregisterToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        log.info("[Push] Token unregistered: token={}", token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null");
        return ResponseEntity.ok(Map.of("status", "unregistered"));
    }
}
