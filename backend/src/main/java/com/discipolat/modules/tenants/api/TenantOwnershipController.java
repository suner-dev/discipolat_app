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

    /**
     * Membres promouvables administrateur — alimente le sélecteur de
     * délégation de l'écran « Propriété & délégation » (T-W7).
     *
     * <p><b>Garde alignée sur {@code isTenantAdmin()}, pas
     * {@code isTenantOwner()}.</b> La garde de <i>métier</i> reste sur
     * {@code promote-admin}/{@code transfer} : un administrateur délégué peut
     * consulter la liste, mais seul le propriétaire peut en faire usage. La
     * lecture est déjà scopée au tenant courant, donc l'alignement de cette
     * route sur celle de l'overview évite de faire diverger les deux écrans.
     */
    @GetMapping("/promotable")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> promotable() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(ownershipService.promotableMembers(tenantId));
    }

    @PostMapping("/transfer")
    @PreAuthorize("@authz.isTenantOwner()")
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
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> requestReplacement(
            @RequestBody(required = false) ReplacementRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(ownershipService.requestReplacement(
                tenantId, actor, request == null ? null : request.reason()));
    }

    @PostMapping("/members/{userId}/promote-admin")
    @PreAuthorize("@authz.isTenantOwner()")
    public ResponseEntity<Void> promoteAdmin(@PathVariable UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        ownershipService.promoteAdmin(tenantId, actor, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/members/{userId}/demote-admin")
    @PreAuthorize("@authz.isTenantOwner()")
    public ResponseEntity<Void> demoteAdmin(@PathVariable UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        ownershipService.demoteAdmin(tenantId, actor, userId);
        return ResponseEntity.ok().build();
    }
}
