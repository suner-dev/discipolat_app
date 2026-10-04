package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantOwnershipService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (D6) — propriété de l'église : vue, transfert
 * « façon WhatsApp », délégation (promote/demote) et demande de
 * remplacement assistée par la plateforme.
 */
@RestController
@RequestMapping("/api/v1/tenant/ownership")
public class TenantOwnershipController {

    private final TenantOwnershipService ownershipService;

    public TenantOwnershipController(TenantOwnershipService ownershipService) {
        this.ownershipService = ownershipService;
    }

    public record TransferRequest(@NotNull UUID toUserId) {
    }

    public record ReplacementRequest(String reason) {
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantOwnershipService.OwnershipView> overview() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(ownershipService.overview(tenantId));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<TenantOwnershipService.OwnershipView> transfer(
            @Valid @RequestBody TransferRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(ownershipService.transfer(tenantId, actor, request.toUserId()));
    }

    /**
     * Demande de remplacement quand l'owner est injoignable — réservée aux
     * admins délégués ; l'arbitrage final appartient à la plateforme.
     */
    @PostMapping("/request-replacement")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> requestReplacement(
            @RequestBody(required = false) ReplacementRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(ownershipService.requestReplacement(
                tenantId, actor, request == null ? null : request.reason()));
    }

    @PostMapping("/members/{userId}/promote-admin")
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<Void> promoteAdmin(@PathVariable UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        ownershipService.promoteAdmin(tenantId, actor, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/members/{userId}/demote-admin")
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<Void> demoteAdmin(@PathVariable UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        ownershipService.demoteAdmin(tenantId, actor, userId);
        return ResponseEntity.ok().build();
    }
}
