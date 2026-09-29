package com.discipolat.modules.notifications.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.notifications.domain.PushNotificationService;
import com.discipolat.modules.notifications.domain.PushProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion des tokens FCM pour les notifications push mobiles.
 * - POST /api/v1/notifications/register-token → enregistrer le token FCM
 * - POST /api/v1/notifications/unregister-token → supprimer le token FCM
 * - GET /api/v1/notifications/push-status → état honnête de la chaîne push
 *
 * <p>Avant P0, ces endpoints se contentaient d'écrire une ligne de log : le
 * token était perdu à la fin de la requête, aucun envoi n'était possible, et
 * l'application mobile (qui embarque {@code firebase_messaging}) ne recevait
 * jamais de notification. Les tokens sont désormais persistés, ce qui les rend
 * exploitables par {@link PushNotificationService}.</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class PushTokenController {

    private static final Logger log = LoggerFactory.getLogger(PushTokenController.class);

    /** Code d'erreur aligné sur le précédent {@code STT_NOT_CONFIGURED}. */
    public static final String PUSH_NOT_CONFIGURED = "PUSH_NOT_CONFIGURED";

    private final PushNotificationService pushNotificationService;
    private final PushProperties pushProperties;

    public PushTokenController(PushNotificationService pushNotificationService,
                               PushProperties pushProperties) {
        this.pushNotificationService = pushNotificationService;
        this.pushProperties = pushProperties;
    }

    @PostMapping("/register-token")
    public ResponseEntity<Map<String, String>> registerToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String platform = body.getOrDefault("platform", "UNKNOWN");
        String appVersion = body.get("appVersion");
        log.info("[Push] Token reçu : platform={}, token={}", platform, mask(token));
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // Un token est toujours scoping-tenant : sans tenant, on refuse
            // d'écrire une ligne qui ne pourrait jamais être relue.
            log.warn("[Push] Enregistrement refusé : aucun contexte tenant");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Contexte tenant absent", "code", "PUSH_NO_TENANT"));
        }
        pushNotificationService.registerToken(
                tenantId, SecurityUtils.getCurrentUserId(), token, platform, appVersion);
        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    @PostMapping("/unregister-token")
    public ResponseEntity<Map<String, String>> unregisterToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        log.info("[Push] Retrait de token demandé : token={}", mask(token));
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Contexte tenant absent", "code", "PUSH_NO_TENANT"));
        }
        pushNotificationService.unregisterToken(tenantId, SecurityUtils.getCurrentUserId(), token);
        return ResponseEntity.ok(Map.of("status", "unregistered"));
    }

    /**
     * État réel de la chaîne push.
     *
     * <p>Réponse 200 : l'UI et l'application mobile doivent pouvoir afficher un
     * état même lorsque le push est désactivé — c'est précisément le moment où
     * l'information compte. Le corps porte {@code enabled}, {@code configured},
     * {@code dryRun} et {@code reason} (vide si tout est opérationnel), sur le
     * modèle de {@code GET /api/v1/voice/stt-status}.</p>
     */
    @GetMapping("/push-status")
    public ResponseEntity<Map<String, Object>> pushStatus() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", pushProperties.isEnabled());
        body.put("configured", pushProperties.isConfigured());
        body.put("dryRun", pushProperties.isDryRun());
        String reason = pushProperties.reason();
        body.put("reason", reason);
        if (!pushProperties.isEnabled() || !pushProperties.isConfigured()) {
            body.put("code", PUSH_NOT_CONFIGURED);
        }
        return ResponseEntity.ok(body);
    }

    /** Jamais le token en clair dans un log. */
    private static String mask(String token) {
        if (token == null) {
            return "null";
        }
        return token.substring(0, Math.min(10, token.length())) + "...";
    }
}
