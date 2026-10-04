package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantJoinRequest;
import com.discipolat.modules.tenants.domain.TenantJoinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-3) — rejointure côté membre et file d'approbation
 * côté admin tenant.
 */
@RestController
@RequestMapping("/api/v1/tenant")
public class TenantJoinController {

    private final TenantJoinService tenantJoinService;

    public TenantJoinController(TenantJoinService tenantJoinService) {
        this.tenantJoinService = tenantJoinService;
    }

    public record JoinRequest(String code, String slug) {
    }

    /** Un compte connecté rejoint l'église du code (OPEN direct, APPROVAL → demande). */
    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> join(@RequestBody JoinRequest request) {
        UUID userId = TenantContext.getCurrentUserId();
        boolean hasCode = request.code() != null && !request.code().isBlank();
        boolean hasSlug = request.slug() != null && !request.slug().isBlank();
        if (!hasCode && !hasSlug) {
            throw new IllegalArgumentException("code ou slug requis");
        }
        TenantJoinService.JoinOutcome outcome = hasCode
                ? tenantJoinService.join(userId, request.code())
                : tenantJoinService.joinBySlug(userId, request.slug());
        return ResponseEntity.ok(Map.of(
                "status", outcome.status(),
                "tenantName", outcome.tenantName() == null ? "" : outcome.tenantName(),
                "orgNodeLabel", outcome.orgNodeLabel() == null ? "" : outcome.orgNodeLabel()
        ));
    }

    @GetMapping("/join-requests")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> pendingRequests() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Map<String, Object>> body = tenantJoinService.pendingRequests(tenantId).stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "code", r.getCode(),
                        "email", r.getEmail() == null ? "" : r.getEmail(),
                        "userId", r.getUserId() == null ? "" : r.getUserId().toString(),
                        "status", r.getStatus().name(),
                        "createdAt", r.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/join-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        TenantJoinService.JoinOutcome outcome = tenantJoinService.approveRequest(id, tenantId, actor);
        return ResponseEntity.ok(Map.of("status", outcome.status()));
    }

    @PostMapping("/join-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        tenantJoinService.rejectRequest(id, tenantId, actor);
        return ResponseEntity.noContent().build();
    }
}
