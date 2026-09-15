package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
public class TenantSettingsWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final TenantSettingsService settingsService;

    public TenantSettingsWebSocketController(SimpMessagingTemplate messagingTemplate, TenantSettingsService settingsService) {
        this.messagingTemplate = messagingTemplate;
        this.settingsService = settingsService;
    }

    @MessageMapping("/tenant/{tenantId}/settings/refresh")
    @PreAuthorize("isAuthenticated()")
    public void handleSettingsRefresh(@DestinationVariable UUID tenantId) {
        // Vérifier que l'utilisateur appartient à ce tenant
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
            throw new SecurityException("Accès non autorisé à ce tenant");
        }

        // Renvoyer la config complète au client demandeur
        Map<String, Object> branding = settingsService.getPublicBranding(tenantId);
        String css = settingsService.generateBrandingCss(tenantId);

        messagingTemplate.convertAndSendToUser(
                TenantContext.getCurrentUserId().toString(),
                "/queue/tenant/settings/response",
                Map.of(
                        "type", "SETTINGS_REFRESH",
                        "tenantId", tenantId.toString(),
                        "branding", branding,
                        "css", css,
                        "timestamp", java.time.Instant.now().toString()
                )
        );
    }

    @MessageMapping("/tenant/{tenantId}/branding/refresh")
    @PreAuthorize("isAuthenticated()")
    public void handleBrandingRefresh(@DestinationVariable UUID tenantId) {
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
            throw new SecurityException("Accès non autorisé à ce tenant");
        }

        String css = settingsService.generateBrandingCss(tenantId);
        Map<String, Object> branding = settingsService.getPublicBranding(tenantId);

        messagingTemplate.convertAndSendToUser(
                TenantContext.getCurrentUserId().toString(),
                "/queue/tenant/branding/response",
                Map.of(
                        "type", "BRANDING_REFRESH",
                        "tenantId", tenantId.toString(),
                        "css", css,
                        "branding", branding,
                        "timestamp", java.time.Instant.now().toString()
                )
        );
    }
}