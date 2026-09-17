package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.service.PermissionResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Endpoint pour récupérer les permissions courantes de l'utilisateur connecté.
 * Utilisé par le frontend et le mobile pour s'abonner aux changements de rôle
 * (G4.4 — Rôles vivantes).
 *
 * Le client écoute les événements WebSocket {@code permissions-changed} et,
 * à la réception, rappelle cet endpoint pour rafraîchir son cache de permissions.
 */
@RestController
public class MeController {

    private final PermissionResolver permissionResolver;

    public MeController(PermissionResolver permissionResolver) {
        this.permissionResolver = permissionResolver;
    }

    /**
     * Retourne les permissions courantes de l'utilisateur ainsi que la version
     * du cache. Le client compare la version pour savoir s'il doit rafraîchir.
     */
    @GetMapping("/api/v1/me/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentPermissions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun tenant de sélectionné"));
        }

        Set<String> permissions = permissionResolver.resolvePermissions(tenantId, userId);
        long version = permissionResolver.getPermissionVersion(tenantId, userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId.toString());
        result.put("tenantId", tenantId.toString());
        result.put("permissions", permissions);
        result.put("version", version);
        result.put("timestamp", java.time.OffsetDateTime.now().toString());

        return ResponseEntity.ok(result);
    }
}
