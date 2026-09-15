package com.discipolat.modules.platform.api;

import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.domain.ImpersonationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * §G1.9 — Endpoint canonique d'impersonation (POST /api/v1/platform/admin/impersonation).
 * La logique métier (validation super admin réel, anti-escalade par rôle, JWT cible
 * TTL court, journalisation) vit dans {@link ImpersonationService}.
 * {@link SuperAdminController} délègue au même service (endpoint de compatibilité).
 */
@RestController
@RequestMapping("/api/v1/platform/admin/impersonation")
public class ImpersonationController {

    private final ImpersonationService impersonationService;
    private final AuditService auditService;

    public ImpersonationController(ImpersonationService impersonationService, AuditService auditService) {
        this.impersonationService = impersonationService;
        this.auditService = auditService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> startImpersonation(@RequestBody Map<String, Object> requestBody,
                                                                  HttpServletRequest httpRequest) {
        var currentUserId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        var session = impersonationService.start(
                currentUserId,
                requestBody.get("tenantId") != null ? java.util.UUID.fromString((String) requestBody.get("tenantId")) : null,
                (String) requestBody.get("targetUserEmail"),
                (String) requestBody.get("reason"),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("impersonationToken", session.token());
        response.put("targetUserId", session.targetUserId().toString());
        response.put("tenantId", session.tenantId().toString());
        response.put("tenantName", session.tenantName());
        response.put("targetRole", session.targetRole());
        response.put("startTime", session.startTime().toString());
        response.put("expiresAt", session.expiresAt().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> stopImpersonation(@RequestBody(required = false) Map<String, Object> requestBody,
                                                  HttpServletRequest httpRequest) {
        String token = requestBody != null ? (String) requestBody.get("impersonationToken") : null;
        if (token != null && !token.isBlank()) {
            impersonationService.stop(token, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        } else {
            // Fin de session sans token (fermeture navigateur) : trace minimale.
            auditService.log(com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId(), null,
                    "IMPERSONATION_END", "PLATFORM", null, "SUCCESS", Map.of(),
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), httpRequest);
        }
        return ResponseEntity.noContent().build();
    }
}
