package com.discipolat.modules.notifications.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.notifications.domain.AccessRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * G5.4 (§55-2) — Bouton « Demander l'accès » des gardes frontend.
 * Le tenant et le demandeur sont résolus CÔTÉ SERVEUR (JWT + contexte
 * multi-tenant) : le client ne fournit que la clé de permission visée.
 */
@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    public record AccessRequestPayload(String permissionKey, String resourceLabel, String reason) {}

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> requestAccess(@RequestBody AccessRequestPayload payload) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        try {
            AccessRequestService.AccessRequestOutcome outcome = accessRequestService.request(
                    tenantId, userId, payload.permissionKey(), payload.resourceLabel(), payload.reason());
            return ResponseEntity.ok(Map.of(
                    "status", outcome.status(),
                    "notifiedResponsibles", outcome.notifiedResponsibles(),
                    "requestId", outcome.requestId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
