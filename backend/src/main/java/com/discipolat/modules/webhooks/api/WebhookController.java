package com.discipolat.modules.webhooks.api;

import com.discipolat.modules.webhooks.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/webhooks")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class WebhookController {

    private final WebhookService service;

    public WebhookController(WebhookService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<WebhookRegistration>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<WebhookRegistration> create(@RequestBody WebhookRegistration registration) {
        return ResponseEntity.ok(service.create(registration));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Envoie un événement de test au webhook. */
    @PostMapping("/{id}/test")
    public ResponseEntity<WebhookDeliveryLog> test(@PathVariable UUID id) {
        return ResponseEntity.ok(service.test(id));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<WebhookDeliveryLog>> logs() {
        return ResponseEntity.ok(service.deliveryLogs());
    }

    /* ------------------------------- Clés API ------------------------------- */

    @GetMapping("/api-keys")
    public ResponseEntity<List<ApiKey>> apiKeys() {
        return ResponseEntity.ok(service.listApiKeys());
    }

    @PostMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> createApiKey(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.createApiKey(
                body.getOrDefault("name", "Intégration"),
                body.get("scopes")));
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID id) {
        service.revokeApiKey(id);
        return ResponseEntity.noContent().build();
    }
}
