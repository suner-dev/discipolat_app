package com.discipolat.modules.inventory.domain;

import com.discipolat.common.infrastructure.qr.QrImageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * G5.6 — QR de terrain pour les actifs d'inventaire.
 *
 * Le QR contient un contenu signé par jeton opaque :
 *   {@code discipolat:asset:{itemId}:{qrToken}}
 * Le jeton est un UUID aléatoire généré à la première demande et stocké sur
 * l'objet — il est donc infalsifiable et impossible à deviner. La résolution
 * est toujours scopée au tenant courant (jamais de lookup global).
 */
@Service
public class AssetQrService {

    public static final String PREFIX = "discipolat:asset:";

    private final InventoryItemRepository itemRepository;
    private final QrImageService qrImageService;

    public AssetQrService(InventoryItemRepository itemRepository, QrImageService qrImageService) {
        this.itemRepository = itemRepository;
        this.qrImageService = qrImageService;
    }

    /** Garantit un jeton QR sur l'objet (génération paresseuse) et retourne le contenu complet. */
    public String ensureQrContent(InventoryItem item) {
        if (item.getQrToken() == null || item.getQrToken().isBlank()) {
            item.setQrToken(UUID.randomUUID().toString());
            itemRepository.save(item);
        }
        return PREFIX + item.getId() + ":" + item.getQrToken();
    }

    /** Encode un contenu en PNG QR (data URL base64), sans dépendance scanner mobile. */
    public String renderPngDataUrl(String content) throws IOException {
        return qrImageService.renderPngDataUrl(content);
    }

    /** Décode un QR depuis une photo de terrain (bytes PNG/JPEG). */
    public Optional<String> decodeQrImage(byte[] imageBytes) {
        return qrImageService.decodeQrImage(imageBytes);
    }

    /**
     * Résout un contenu (ou un jeton brut) scanné vers l'objet du tenant courant.
     * Rejette tout contenu dont l'itemId ne correspond pas au jeton (QR falsifié/recyclé).
     */
    public Optional<InventoryItem> resolve(UUID tenantId, String scanned) {
        if (scanned == null || scanned.isBlank()) {
            return Optional.empty();
        }
        String trimmed = scanned.trim();
        if (trimmed.startsWith(PREFIX)) {
            String body = trimmed.substring(PREFIX.length());
            String[] parts = body.split(":");
            if (parts.length != 2) {
                return Optional.empty();
            }
            final UUID expectedItemId;
            try {
                expectedItemId = UUID.fromString(parts[0]);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
            return itemRepository.findByTenantIdAndQrToken(tenantId, parts[1])
                    .filter(i -> i.getId().equals(expectedItemId));
        }
        // Tolère le jeton seul (saisie manuelle sur le terrain)
        return itemRepository.findByTenantIdAndQrToken(tenantId, trimmed);
    }
}
