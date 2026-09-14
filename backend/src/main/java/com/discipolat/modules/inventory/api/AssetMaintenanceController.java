package com.discipolat.modules.inventory.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.inventory.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Asset Maintenance Controller (G3.5) — Maintenance tracking and TCO.
 */
@RestController
@RequestMapping("/api/v1/assets")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
public class AssetMaintenanceController {

    private final InventoryService inventoryService;
    private final AssetMaintenanceRepository maintenanceRepository;

    public AssetMaintenanceController(InventoryService inventoryService,
                                       AssetMaintenanceRepository maintenanceRepository) {
        this.inventoryService = inventoryService;
        this.maintenanceRepository = maintenanceRepository;
    }

    @PostMapping("/{itemId}/maintenance")
    public ResponseEntity<AssetMaintenance> scheduleMaintenance(@PathVariable UUID itemId, @RequestBody MaintenanceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        AssetMaintenance maintenance = new AssetMaintenance();
        maintenance.setTenantId(tenantId);
        maintenance.setItemId(itemId);
        maintenance.setMaintenanceType(request.maintenanceType());
        maintenance.setTitle(request.title());
        maintenance.setDescription(request.description());
        maintenance.setScheduledFor(request.scheduledFor());
        maintenance.setVendorName(request.vendorName());
        maintenance.setVendorContact(request.vendorContact());
        maintenance.setStatus("SCHEDULED");

        AssetMaintenance saved = maintenanceRepository.save(maintenance);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/maintenance/{id}")
    public ResponseEntity<AssetMaintenance> updateMaintenance(@PathVariable UUID id, @RequestBody MaintenanceRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        AssetMaintenance maintenance = maintenanceRepository.findById(id)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Maintenance not found"));

        if (request.maintenanceType() != null) maintenance.setMaintenanceType(request.maintenanceType());
        if (request.title() != null) maintenance.setTitle(request.title());
        if (request.description() != null) maintenance.setDescription(request.description());
        if (request.scheduledFor() != null) maintenance.setScheduledFor(request.scheduledFor());
        if (request.vendorName() != null) maintenance.setVendorName(request.vendorName());
        if (request.vendorContact() != null) maintenance.setVendorContact(request.vendorContact());
        if (request.status() != null) maintenance.setStatus(request.status());
        if (request.cost() != null) maintenance.setCost(request.cost());

        if ("COMPLETED".equals(maintenance.getStatus())) {
            maintenance.setCompletedAt(LocalDateTime.now());
            maintenance.setNextMaintenanceDue(request.nextMaintenanceDue());
        }

        AssetMaintenance saved = maintenanceRepository.save(maintenance);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{itemId}/maintenance")
    public ResponseEntity<List<AssetMaintenance>> getMaintenanceHistory(@PathVariable UUID itemId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(maintenanceRepository.findByTenantIdAndItemId(tenantId, itemId));
    }

    @GetMapping("/{itemId}/tco")
    public ResponseEntity<Map<String, Object>> getTco(@PathVariable UUID itemId) {
        UUID tenantId = TenantContext.requireTenantId();
        InventoryItem item = inventoryService.findById(itemId);

        Double maintenanceCost = maintenanceRepository.getTotalMaintenanceCost(tenantId, itemId);
        if (maintenanceCost == null) maintenanceCost = 0.0;

        Double purchasePrice = item.getPurchasePrice() != null ? item.getPurchasePrice() :
                              item.getValeurUnitaire() != null ? item.getValeurUnitaire() : 0.0;

        Map<String, Object> tco = new LinkedHashMap<>();
        tco.put("itemId", itemId);
        tco.put("itemName", item.getNom());
        tco.put("purchasePrice", purchasePrice);
        tco.put("totalMaintenanceCost", maintenanceCost);
        tco.put("totalTco", purchasePrice + maintenanceCost);
        tco.put("totalCheckouts", item.getTotalCheckoutCount());
        tco.put("expectedLifespanMonths", item.getExpectedLifespanMonths());

        return ResponseEntity.ok(tco);
    }

    public record MaintenanceRequest(String maintenanceType, String title, String description,
                                      LocalDateTime scheduledFor, String vendorName, String vendorContact,
                                      Double cost, String status, LocalDateTime nextMaintenanceDue) {}
}
