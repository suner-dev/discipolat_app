package com.discipolat.modules.whatsapp.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.whatsapp.domain.WhatsAppMessage;
import com.discipolat.modules.whatsapp.domain.WhatsAppService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administration du pont WhatsApp : configuration Cloud API,
 * journal des messages, diffusion d'annonces, statistiques.
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class WhatsAppAdminController {

    private final WhatsAppService whatsAppService;

    public WhatsAppAdminController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(whatsAppService.getConfig(TenantContext.requireTenantId()));
    }

    public record SaveConfigRequest(
            String phoneNumberId,
            String displayPhoneNumber,
            String accessToken,
            String webhookVerifyToken,
            Boolean enabled,
            String welcomeMessage) {}

    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody SaveConfigRequest request) {
        whatsAppService.saveConfig(TenantContext.requireTenantId(),
                request.phoneNumberId(), request.displayPhoneNumber(),
                request.accessToken(), request.webhookVerifyToken(),
                Boolean.TRUE.equals(request.enabled()), request.welcomeMessage());
        return ResponseEntity.ok(whatsAppService.getConfig(TenantContext.requireTenantId()));
    }

    @PostMapping("/config/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        return ResponseEntity.ok(whatsAppService.testConnection(TenantContext.requireTenantId()));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<WhatsAppMessage>> messages(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(whatsAppService.recentMessages(TenantContext.requireTenantId(), limit));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(whatsAppService.stats(TenantContext.requireTenantId()));
    }

    public record BroadcastRequest(@NotBlank String titre, @NotBlank String contenu) {}

    /** Diffuse une annonce WhatsApp à tous les membres opt-in. */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody BroadcastRequest request) {
        return ResponseEntity.ok(whatsAppService.broadcastAnnouncement(
                TenantContext.requireTenantId(), request.titre(), request.contenu(), null, null));
    }
}
