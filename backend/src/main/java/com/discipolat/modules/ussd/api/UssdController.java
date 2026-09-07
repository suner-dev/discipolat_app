package com.discipolat.modules.ussd.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.ussd.domain.UssdProperties;
import com.discipolat.modules.ussd.domain.UssdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller USSD — callback Africa's Talking + administration.
 *
 * <p>Le callback POST /api/v1/ussd/callback est appelé par Africa's Talking
 * lorsqu'un utilisateur compose le code USSD. Ce endpoint doit être public
 * (pas d'authentification JWT) mais protégé par le secret webhook.</p>
 *
 * <p>Format de requête Africa's Talking (application/x-www-form-urlencoded) :
 * <ul>
 *   <li>sessionId — ID unique de session</li>
 *   <li>phoneNumber — numéro de l'utilisateur</li>
 *   <li>text — texte saisi (vide au début, "1*2" si navigation rapide)</li>
 *   <li>serviceCode — code du service USSD</li>
 *   <li>networkCode — code réseau (optionnel)</li>
 * </ul>
 *
 * <p>Réponse : texte brut préfixé "CON" (continuer) ou "END" (terminer).</p>
 */
@RestController
@RequestMapping("/api/v1/ussd")
public class UssdController {

    private static final Logger log = LoggerFactory.getLogger(UssdController.class);

    private final UssdService ussdService;
    private final UssdProperties properties;
    private final SecurityUtils securityUtils;

    public UssdController(UssdService ussdService, UssdProperties properties, SecurityUtils securityUtils) {
        this.ussdService = ussdService;
        this.properties = properties;
        this.securityUtils = securityUtils;
    }

    /**
     * Callback USSD entrant — appelé par Africa's Talking.
     * Public (pas de JWT) mais validé par le secret webhook.
     */
    @PostMapping(value = "/callback", produces = "text/plain")
    public ResponseEntity<String> callback(
            @RequestHeader(value = "X-Ussd-Secret", required = false) String providedSecret,
            @RequestParam String sessionId,
            @RequestParam String phoneNumber,
            @RequestParam(required = false, defaultValue = "") String text,
            @RequestParam String serviceCode,
            @RequestParam(required = false) String networkCode) {

        // Vérifier que le service est configuré
        if (!properties.isConfigured()) {
            log.warn("[USSD] Callback reçu mais service non configuré");
            return ResponseEntity.ok("END Service non disponible. Réessayez plus tard.");
        }

        // Validation du secret webhook (si configuré)
        if (properties.getWebhookSecret() != null && !properties.getWebhookSecret().isBlank()) {
            if (!properties.getWebhookSecret().equals(providedSecret)) {
                log.warn("[USSD] Callback avec secret invalide — sessionId={}", sessionId);
                return ResponseEntity.status(401).body("END Accès refusé.");
            }
        }

        log.info("[USSD] Callback — session={}, phone={}, text={}, network={}",
                sessionId, phoneNumber, text, networkCode);

        String response = ussdService.handleUssdCallback(sessionId, phoneNumber, text, serviceCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques USSD (admin).
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(ussdService.getStats(resolveTenantId()));
    }

    /**
     * Statut de la configuration USSD.
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "enabled", properties.isEnabled(),
                "configured", properties.isConfigured(),
                "serviceCode", properties.getServiceCode() != null ? properties.getServiceCode() : "",
                "baseUrl", properties.getBaseUrl() != null ? properties.getBaseUrl() : ""
        ));
    }

    private String resolveTenantId() {
        try {
            UUID tenantId = securityUtils.getCurrentTenantId();
            return tenantId != null ? tenantId.toString() : "default";
        } catch (Exception e) {
            return "default";
        }
    }
}
