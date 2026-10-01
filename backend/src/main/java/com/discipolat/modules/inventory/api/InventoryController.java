package com.discipolat.modules.inventory.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.inventory.domain.AssetCheckout;
import com.discipolat.modules.inventory.domain.AssetCheckoutRepository;
import com.discipolat.modules.inventory.domain.AssetQrService;
import com.discipolat.modules.inventory.domain.InventoryItem;
import com.discipolat.modules.inventory.domain.InventoryItemRepository;
import com.discipolat.modules.inventory.domain.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryItemRepository itemRepository;
    private final AssetCheckoutRepository checkoutRepository;
    private final AssetQrService assetQrService;

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

    // ==================== G5.6 — QR de terrain (scan inventaire) ====================

    /** Génère (ou renvoie) le QR signé de l'objet : contenu = discipolat:asset:{id}:{jeton}. */
    @GetMapping("/{id}/qr-code")
    public ResponseEntity<Map<String, String>> qrCode(@PathVariable UUID id) throws IOException {
        UUID tenantId = TenantContext.requireTenantId();
        InventoryItem item = itemRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("InventoryItem", id));
        String content = assetQrService.ensureQrContent(item);
        return ResponseEntity.ok(Map.of(
                "itemId", id.toString(),
                "content", content,
                "qrPngDataUrl", assetQrService.renderPngDataUrl(content)));
    }

    /** Résout un contenu scanné (ou un jeton saisi manuellement) — scopé tenant. */
    @GetMapping("/qr/resolve")
    public ResponseEntity<Map<String, Object>> resolveQr(@RequestParam String content) {
        UUID tenantId = TenantContext.requireTenantId();
        return assetQrService.resolve(tenantId, content)
                .map(item -> ResponseEntity.ok(summarize(item)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Scan terrain : la photo du QR est remontée, décodée côté serveur (ZXing), puis résolue. */
    @PostMapping("/qr/scan")
    public ResponseEntity<Map<String, Object>> scanQr(@RequestParam("file") MultipartFile file) throws IOException {
        UUID tenantId = TenantContext.requireTenantId();
        Optional<String> decoded = assetQrService.decodeQrImage(file.getBytes());
        if (decoded.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun QR lisible sur la photo"));
        }
        return assetQrService.resolve(tenantId, decoded.get())
                .map(item -> ResponseEntity.ok(summarize(item)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Objet inconnu pour cette église", "scanned", decoded.get())));
    }

    private Map<String, Object> summarize(InventoryItem item) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("itemId", item.getId());
        summary.put("nom", item.getNom());
        summary.put("categorie", item.getCategorie());
        summary.put("statut", item.getStatut());
        summary.put("quantite", item.getQuantite());
        summary.put("quantiteDisponible", item.getQuantiteDisponible());
        summary.put("lieuStockage", item.getLieuStockage());
        summary.put("affecteAId", item.getAffecteAId());
        // Le jeton est visible uniquement dans le scope déjà autorisé (résolution tenant+rôles)
        summary.put("qrToken", item.getQrToken());
        checkoutRepository
                .findFirstByTenantIdAndItemIdAndStatus(item.getTenantId(), item.getId(), "CHECKED_OUT")
                .ifPresent(c -> {
                    summary.put("checkedOut", true);
                    summary.put("activeCheckoutId", c.getId());
                    summary.put("borrowedByMemberId", c.getMemberId());
                    summary.put("dueBackAt", c.getDueBackAt());
                });
        return summary;
    }
}
