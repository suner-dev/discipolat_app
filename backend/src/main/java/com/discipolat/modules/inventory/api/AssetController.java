package com.discipolat.modules.inventory.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.inventory.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Asset Controller (G3.5) — Checkout/return workflow and maintenance tracking.
 */
@RestController
@RequestMapping("/api/v1/assets")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
public class AssetController {

    private final InventoryService inventoryService;
    private final AssetCheckoutRepository checkoutRepository;
    private final AssetMaintenanceRepository maintenanceRepository;

    public AssetController(InventoryService inventoryService,
                          AssetCheckoutRepository checkoutRepository,
                          AssetMaintenanceRepository maintenanceRepository) {
        this.inventoryService = inventoryService;
        this.checkoutRepository = checkoutRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @PostMapping("/{itemId}/checkout")
    public ResponseEntity<AssetCheckout> checkout(@PathVariable UUID itemId, @RequestBody CheckoutRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        AssetCheckout checkout = new AssetCheckout();
        checkout.setTenantId(tenantId);
        checkout.setItemId(itemId);
        checkout.setMemberId(request.memberId());
        checkout.setSpaceId(request.spaceId());
        checkout.setEventId(request.eventId());
        checkout.setDueBackAt(request.dueBackAt());
        checkout.setConditionOnCheckout(request.condition());
        checkout.setCheckedOutBy(userId);
        checkout.setNotes(request.notes());
        checkout.setStatus("CHECKED_OUT");

        AssetCheckout saved = checkoutRepository.save(checkout);
        inventoryService.markCheckedOut(tenantId, itemId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/{itemId}/return")
    public ResponseEntity<AssetCheckout> returnAsset(@PathVariable UUID itemId, @RequestBody ReturnRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        AssetCheckout checkout = checkoutRepository
                .findFirstByTenantIdAndItemIdAndStatus(tenantId, itemId, "CHECKED_OUT")
                .orElseThrow(() -> new RuntimeException("No active checkout found for item"));

        checkout.setReturnedAt(LocalDateTime.now());
        checkout.setConditionOnReturn(request.condition());
        checkout.setReturnedBy(userId);
        checkout.setStatus(request.condition() != null && request.condition().contains("DAMAGED") ? "DAMAGED" : "RETURNED");
        checkout.setNotes(request.notes());

        AssetCheckout saved = checkoutRepository.save(checkout);
        inventoryService.markReturned(tenantId, itemId);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/checkouts")
    public ResponseEntity<List<AssetCheckout>> listCheckouts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID memberId) {
        UUID tenantId = TenantContext.requireTenantId();
        List<AssetCheckout> checkouts;
        if (status != null) {
            checkouts = checkoutRepository.findByTenantIdAndStatus(tenantId, status);
        } else if (memberId != null) {
            checkouts = checkoutRepository.findByTenantIdAndMemberId(tenantId, memberId);
        } else {
            checkouts = checkoutRepository.findAll().stream()
                    .filter(c -> c.getTenantId().equals(tenantId))
                    .toList();
        }
        return ResponseEntity.ok(checkouts);
    }

    public record CheckoutRequest(UUID memberId, UUID spaceId, UUID eventId,
                                   LocalDateTime dueBackAt, String condition, String notes) {}

    public record ReturnRequest(String condition, String notes) {}
}
