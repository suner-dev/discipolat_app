package com.discipolat.modules.inventory.domain;

import com.discipolat.common.infrastructure.qr.QrImageService;
import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G5.6 — QR de terrain inventaire : jeton infalsifiable, résolution scopée tenant,
 * aller-retour réel encode → decode (ZXing, sans mock du codec).
 */
@ExtendWith(MockitoExtension.class)
class AssetQrServiceTest {

    @Mock
    private InventoryItemRepository itemRepository;

    private AssetQrService service;
    private final QrImageService qrImageService = new QrImageService();

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AssetQrService(itemRepository, qrImageService);
        TenantContext.setTenantId(tenantA);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private InventoryItem item(UUID tenantId) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setTenantId(tenantId);
        item.setNom("Sono MB");
        item.setCategorie("TECHNIQUE");
        item.setStatut("DISPONIBLE");
        return item;
    }

    @Test
    void ensureQrContentGeneratesOpaqueTokenOnceAndPersists() throws Exception {
        InventoryItem item = item(tenantA);
        String content = service.ensureQrContent(item);

        assertThat(content).startsWith(AssetQrService.PREFIX).contains(item.getId().toString());
        assertThat(item.getQrToken()).isNotBlank();
        verify(itemRepository).save(item);

        // Deuxième appel : jeton stable, pas de nouvelle sauvegarde
        String again = service.ensureQrContent(item);
        assertThat(again).isEqualTo(content);
        verify(itemRepository).save(any()); // un seul save au total
    }

    @Test
    void resolveAcceptsSignedContentAndRejectsForgedItemId() {
        InventoryItem item = item(tenantA);
        item.setQrToken(UUID.randomUUID().toString());

        when(itemRepository.findByTenantIdAndQrToken(tenantA, item.getQrToken()))
                .thenReturn(Optional.of(item));

        Optional<InventoryItem> ok = service.resolve(tenantA,
                AssetQrService.PREFIX + item.getId() + ":" + item.getQrToken());
        assertThat(ok).contains(item);

        // Contenu avec un itemId different du jeton → falsifié, rejeté
        Optional<InventoryItem> forged = service.resolve(tenantA,
                AssetQrService.PREFIX + UUID.randomUUID() + ":" + item.getQrToken());
        assertThat(forged).isEmpty();
    }

    @Test
    void resolveIsTenantScoped() {
        InventoryItem item = item(tenantA);
        item.setQrToken(UUID.randomUUID().toString());
        String content = AssetQrService.PREFIX + item.getId() + ":" + item.getQrToken();

        // Le tenant B ne possède pas cet objet : lookup scopé → introuvable, jamais de lookup global
        when(itemRepository.findByTenantIdAndQrToken(eq(tenantB), any())).thenReturn(Optional.empty());
        assertThat(service.resolve(tenantB, content)).isEmpty();
        verify(itemRepository).findByTenantIdAndQrToken(tenantB, item.getQrToken());
    }

    @Test
    void resolveToleratesRawTokenManualEntry() {
        InventoryItem item = item(tenantA);
        item.setQrToken(UUID.randomUUID().toString());
        when(itemRepository.findByTenantIdAndQrToken(tenantA, item.getQrToken()))
                .thenReturn(Optional.of(item));

        assertThat(service.resolve(tenantA, item.getQrToken())).contains(item);
    }

    @Test
    void renderThenDecodeRoundTripRecoversSignedContent() throws Exception {
        InventoryItem item = item(tenantA);
        String content = service.ensureQrContent(item);

        String dataUrl = service.renderPngDataUrl(content);
        assertThat(dataUrl).startsWith("data:image/png;base64,");

        byte[] png = Base64.getDecoder().decode(dataUrl.substring("data:image/png;base64,".length()));
        Optional<String> decoded = service.decodeQrImage(png);
        assertThat(decoded).contains(content);

        // Et le contenu redécodé résout bien l'objet (lookup scopé tenant)
        when(itemRepository.findByTenantIdAndQrToken(tenantA, item.getQrToken()))
                .thenReturn(Optional.of(item));
        assertThat(service.resolve(tenantA, decoded.get())).contains(item);
    }

    @Test
    void decodeGarbageImageReturnsEmpty() {
        assertThat(service.decodeQrImage(new byte[]{0, 1, 2, 3})).isEmpty();
    }
}
