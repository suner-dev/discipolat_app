package com.discipolat.modules.whatsapp.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.whatsapp.domain.WhatsAppConfigRepository;
import com.discipolat.modules.whatsapp.domain.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Webhook WhatsApp Business Cloud API (public — vérification Meta + événements).
 * Route : /api/v1/public/whatsapp (permitAll côté SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/public/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppService whatsAppService;
    private final WhatsAppWebhookResolver webhookResolver;

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
     * Réception des événements Meta. Le tenant est résolu depuis le phone_number_id
     * du payload (multi-tenant) puis les messages sont traités.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receive(@RequestBody Map<String, Object> payload) {
        var tenantOpt = webhookResolver.resolveTenantFromPayload(payload);
        if (tenantOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("received", false, "reason", "tenant inconnu"));
        }
        UUID tenantId = tenantOpt.get();
        TenantContext.runAsTenant(tenantId, () -> whatsAppService.handleWebhook(tenantId, payload));
        // Meta exige un 200 rapide
        return ResponseEntity.ok(Map.of("received", true));
    }
}
