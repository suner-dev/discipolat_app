package com.discipolat.modules.whatsapp.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.whatsapp.domain.WhatsAppConfigRepository;
import com.discipolat.modules.whatsapp.domain.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Webhook WhatsApp Business Cloud API (public — vérification Meta + événements).
 * Route : /api/v1/public/whatsapp (permitAll côté SecurityConfig).
 *
 * SÉCURITÉ: Le POST endpoint vérifie la signature HMAC X-Hub-Signature-256
 * fournie par Meta. Si whatsapp.app-secret n'est pas configuré, le webhook
 * POST est refusé (503) — seul le GET de vérification est ouvert.
 */
@RestController
@RequestMapping("/api/v1/public/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppService whatsAppService;
    private final WhatsAppWebhookResolver webhookResolver;

    @Value("${app.whatsapp.app-secret:}")
    private String appSecret;

    public WhatsAppWebhookController(WhatsAppService whatsAppService,
                                     WhatsAppWebhookResolver webhookResolver) {
        this.whatsAppService = whatsAppService;
        this.webhookResolver = webhookResolver;
    }

    /** Vérification initiale du webhook par Meta (hub.mode, hub.verify_token, hub.challenge). */
    @GetMapping("/webhook")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        if (!"subscribe".equals(mode)) {
            return ResponseEntity.badRequest().body("mode invalide");
        }
        Optional<UUID> tenantId = webhookResolver.resolveTenantByVerifyToken(verifyToken);
        if (tenantId.isEmpty()) {
            return ResponseEntity.status(403).body("verify_token inconnu");
        }
        UUID tid = tenantId.get();
        TenantContext.runAsTenant(tid, () ->
                log.info("Webhook WhatsApp vérifié pour le tenant {}", tid));
        return ResponseEntity.ok(challenge);
    }

    /**
     * Réception des événements Meta.
     * SÉCURITÉ: Vérifie X-Hub-Signature-256 (HMAC-SHA256) si app-secret est configuré.
     * Sans app-secret configuré, retourne 503.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        if (appSecret.isBlank()) {
            log.warn("WhatsApp webhook POST rejeté: app-whatsapp.app-secret non configuré");
            return ResponseEntity.status(503).body(Map.of("received", false, "reason", "webhook not configured"));
        }

        if (signature == null || !verifySignature(rawBody, signature, appSecret)) {
            log.warn("WhatsApp webhook POST rejeté: signature HMAC invalide");
            return ResponseEntity.status(403).body(Map.of("received", false, "reason", "invalid signature"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = parsePayload(rawBody);
        var tenantOpt = webhookResolver.resolveTenantFromPayload(payload);
        if (tenantOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("received", false, "reason", "tenant inconnu"));
        }
        UUID tenantId = tenantOpt.get();
        TenantContext.runAsTenant(tenantId, () -> whatsAppService.handleWebhook(tenantId, payload));
        return ResponseEntity.ok(Map.of("received", true));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String rawBody) {
        try {
            return (Map<String, Object>) new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(rawBody, Map.class);
        } catch (Exception e) {
            log.error("Erreur parsing webhook body", e);
            return Map.of("raw", rawBody);
        }
    }

    private boolean verifySignature(String payload, String signatureHeader, String secret) {
        try {
            String expectedSig = "sha256=" + hmacSha256(secret, payload);
            return java.security.MessageDigest.isEqual(
                    expectedSig.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Erreur vérification signature WhatsApp", e);
            return false;
        }
    }

    private String hmacSha256(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
