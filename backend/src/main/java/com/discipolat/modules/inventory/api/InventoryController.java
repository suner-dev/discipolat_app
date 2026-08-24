package com.discipolat.modules.inventory.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.inventory.domain.InventoryItem;
import com.discipolat.modules.inventory.domain.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<Page<InventoryItem>> list(
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(inventoryService.findAll(tenantId, categorie, statut, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> get(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.findById(id));
    }

    @PostMapping
    public ResponseEntity<InventoryItem> create(@RequestBody InventoryItem item) {
        item.setTenantId(TenantContext.getTenantId());
        return ResponseEntity.ok(inventoryService.create(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> update(@PathVariable UUID id, @RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.update(id, item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<InventoryItem> assign(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        UUID memberId = body.get("memberId");
        return ResponseEntity.ok(inventoryService.assign(id, memberId));
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<InventoryItem> unassign(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.unassign(id));
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<InventoryItem> markMaintenance(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.markMaintenance(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(inventoryService.getStats(tenantId));
    }

    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> smartAlerts() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(inventoryService.getSmartAlerts(tenantId));
    }
}
