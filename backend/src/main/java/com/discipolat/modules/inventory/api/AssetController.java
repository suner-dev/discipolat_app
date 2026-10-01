package com.discipolat.modules.inventory.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.inventory.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Asset Controller (G3.5) — Checkout/return workflow and maintenance tracking.
 * G5.6 — photo de dommage terrain uploadée au retour d'actif (serveur, tenant-scopée).
 */
@RestController
@RequestMapping("/api/v1/assets")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
public class AssetController {

    private final InventoryService inventoryService;
    private final AssetCheckoutRepository checkoutRepository;
    private final AssetMaintenanceRepository maintenanceRepository;
    /** §G6.4 — cycle matériel : événements réels (outbox → notif/temps réel/historique). */
    private final com.discipolat.modules.core.service.OutboxPublisher outboxPublisher;

    @Value("${discipolat.file.storage.root:/var/discipolat/files}")
    private String storageRoot;

    public AssetController(InventoryService inventoryService,
                          AssetCheckoutRepository checkoutRepository,
                          AssetMaintenanceRepository maintenanceRepository,
                          com.discipolat.modules.core.service.OutboxPublisher outboxPublisher) {
        this.inventoryService = inventoryService;
        this.checkoutRepository = checkoutRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.outboxPublisher = outboxPublisher;
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
        publishAssetEvent("AssetCheckedOut", saved, itemId, userId);
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

        // §G6.4 — cycle matériel réel : un retour DOMMAGÉ ouvre AUTOMATIQUEMENT
        // un ticket de maintenance (et l'événement AssetDamaged alimente
        // notifications/temps réel/historique via l'outbox).
        if ("DAMAGED".equals(saved.getStatus())) {
            AssetMaintenance ticket = new AssetMaintenance();
            ticket.setTenantId(tenantId);
            ticket.setItemId(itemId);
            ticket.setMaintenanceType("REPARATION");
            ticket.setTitle("Dommage constaté au retour de prêt");
            ticket.setDescription(saved.getNotes() != null ? saved.getNotes()
                    : "Retourné endommagé — ticket créé automatiquement.");
            ticket.setScheduledFor(LocalDateTime.now());
            ticket.setStatus("SCHEDULED");
            maintenanceRepository.save(ticket);
            publishAssetEvent("AssetDamaged", saved, itemId, userId);
        } else {
            publishAssetEvent("AssetReturned", saved, itemId, userId);
        }
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

    // ==================== G5.6 — photo de dommage (mobile terrain) ====================

    /**
     * Attache la photo de dommage prise sur le terrain au prêt actif de l'objet.
     * Le chemin est généré côté serveur (tenant + id de prêt) : aucune traversée possible.
     */
    @PostMapping("/{itemId}/damage-photo")
    public ResponseEntity<Map<String, String>> uploadDamagePhoto(
            @PathVariable UUID itemId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UUID tenantId = TenantContext.requireTenantId();
        String contentType = file.getContentType();
        if (contentType == null || !List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Format d'image requis (JPEG/PNG/WebP)"));
        }
        AssetCheckout checkout = checkoutRepository
                .findFirstByTenantIdAndItemIdAndStatus(tenantId, itemId, "CHECKED_OUT")
                .orElseThrow(() -> new RuntimeException("No active checkout found for item"));

        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        Path dir = Path.of(storageRoot).toAbsolutePath().normalize().resolve("asset-damage").resolve(tenantId.toString());
        Files.createDirectories(dir);
        Path target = dir.resolve(checkout.getId() + ext);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        checkout.setDamagePhotoPath("asset-damage/" + tenantId + "/" + target.getFileName());
        checkoutRepository.save(checkout);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("photoPath", checkout.getDamagePhotoPath()));
    }

    /** Relit la photo de dommage du dernier prêt actif (affichable dans l'app mobile et le web). */
    @GetMapping("/{itemId}/damage-photo")
    public ResponseEntity<byte[]> getDamagePhoto(@PathVariable UUID itemId) throws IOException {
        UUID tenantId = TenantContext.requireTenantId();
        AssetCheckout checkout = checkoutRepository
                .findFirstByTenantIdAndItemIdAndStatus(tenantId, itemId, "CHECKED_OUT")
                .or(() -> checkoutRepository.findByTenantIdAndItemId(tenantId, itemId).stream()
                        .filter(c -> c.getDamagePhotoPath() != null)
                        .max(Comparator.comparing(c -> c.getCheckedOutAt() != null ? c.getCheckedOutAt() : LocalDateTime.MIN)))
                .orElseThrow(() -> new RuntimeException("No checkout found for item"));
        if (checkout.getDamagePhotoPath() == null) {
            return ResponseEntity.notFound().build();
        }
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        Path photo = root.resolve(checkout.getDamagePhotoPath()).normalize();
        if (!photo.startsWith(root) || !Files.exists(photo)) {
            return ResponseEntity.notFound().build();
        }
        MediaType media = checkout.getDamagePhotoPath().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(media).body(Files.readAllBytes(photo));
    }

    public record CheckoutRequest(UUID memberId, UUID spaceId, UUID eventId,
                                   LocalDateTime dueBackAt, String condition, String notes) {}

    public record ReturnRequest(String condition, String notes) {}

    /** §G6.4 — publie l'événement matériel (outbox → notif + temps réel + historique). */
    private void publishAssetEvent(String eventType, AssetCheckout checkout, UUID itemId, UUID actorId) {
        String assetName = "";
        try {
            assetName = inventoryService.findById(itemId).getNom();
        } catch (Exception ignore) {
            // nom non résolu : l'événement reste publié avec l'identifiant
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("assetId", itemId.toString());
        payload.put("assetName", assetName);
        payload.put("checkoutId", checkout.getId().toString());
        payload.put("status", checkout.getStatus());
        if (checkout.getMemberId() != null) payload.put("memberId", checkout.getMemberId().toString());
        if (checkout.getSpaceId() != null) payload.put("spaceId", checkout.getSpaceId().toString());
        payload.put("actorId", actorId.toString());
        outboxPublisher.publish(checkout.getTenantId(), "ASSET_CHECKOUT", checkout.getId(), eventType, payload);
    }
}
