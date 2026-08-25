package com.discipolat.modules.facerec;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.facerec.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionServiceTest {

    @Mock private FaceTemplateRepository repository;
    @Mock private SecurityUtils securityUtils;

    private FaceRecognitionService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FaceRecognitionService(repository, securityUtils);
        TenantContext.setTenantId(tenantId);
        lenient().when(repository.save(any(FaceTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Fixtures ─────────────────────────────────────────────────────

    private static byte[] faceImage(long seed) throws IOException {
        BufferedImage img = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        java.util.Random rnd = new java.util.Random(seed);

        int cx = 105 + rnd.nextInt(35);
        int cy = 105 + rnd.nextInt(30);
        int rx = 46 + rnd.nextInt(28);
        int ry = 66 + rnd.nextInt(34);

        Color bg = switch (rnd.nextInt(4)) {
            case 0 -> new Color(60, 80, 120);
            case 1 -> new Color(120, 110, 60);
            case 2 -> new Color(90, 130, 90);
            default -> new Color(140, 70, 70);
        };
        g.setColor(bg);
        g.fillRect(0, 0, 240, 240);

        if (rnd.nextBoolean()) {
            g.setColor(new Color(35, 25, 20));
            g.fillOval(cx - rx - 9, cy - ry - 10, 2 * rx + 18, ry);
        }

        g.setColor(new Color(220, 190, 160));
        g.fillOval(cx - rx, cy - ry, 2 * rx, 2 * ry);

        g.setColor(Color.DARK_GRAY);
        int eyeRx = Math.max(7, rx / 5);
        int eyeRy = Math.max(5, ry / 10);
        g.fillOval(cx - rx / 2 + rnd.nextInt(4), cy - ry / 3, eyeRx, eyeRy);
        g.fillOval(cx + rx / 2 - rnd.nextInt(4), cy - ry / 3, eyeRx, eyeRy);
        g.drawArc(cx - rx / 2, cy + ry / 5, rx, Math.max(14, ry / 3), 10, -180);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static FaceTemplate template(String hash, String name) {
        return FaceTemplate.builder()
                .id(UUID.randomUUID()).tenantId(UUID.randomUUID())
                .userId(UUID.randomUUID()).displayName(name)
                .descriptorHash(hash).qualityScore(1).active(true)
                .build();
    }

    // ── Enrôlement ───────────────────────────────────────────────────

    @Test
    void enrôleUnVisageAvecEmpreinteEtQualite() throws IOException {
        FaceTemplate saved = service.enroll(UUID.randomUUID(), null, "Jean Dupont", faceImage(11));

        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getDisplayName()).isEqualTo("Jean Dupont");
        assertThat(saved.getDescriptorHash()).hasSize(64);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void réenrôlementMetÀJourLeGabaritExistant() throws IOException {
        UUID userId = UUID.randomUUID();
        FaceTemplate existant = FaceTemplate.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).userId(userId)
                .displayName("Ancien").descriptorHash("00".repeat(32)).qualityScore(1).active(true)
                .build();
        when(repository.findByTenantIdAndUserId(tenantId, userId)).thenReturn(Optional.of(existant));

        FaceTemplate maj = service.enroll(userId, null, "Nouveau nom", faceImage(12));

        assertThat(maj.getId()).isEqualTo(existant.getId());
        assertThat(maj.getDisplayName()).isEqualTo("Nouveau nom");
        assertThat(maj.getDescriptorHash()).hasSize(64).isNotEqualTo("00".repeat(32));
    }

    @Test
    void photoSansContrasteRejetée() throws IOException {
        BufferedImage flat = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flat.createGraphics();
        g.setColor(new Color(90, 90, 90));
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(flat, "png", out);

        assertThatThrownBy(() -> service.enroll(UUID.randomUUID(), null, "Mur", out.toByteArray()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inexploitable");
    }

    @Test
    void nomObligatoire() throws IOException {
        assertThatThrownBy(() -> service.enroll(UUID.randomUUID(), null, "  ", faceImage(13)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userIdAbsentRepliSurUtilisateurCourant() throws IOException {
        UUID courant = UUID.randomUUID();
        SecurityTestHelper.loginAs(courant);

        FaceTemplate saved = service.enroll(null, null, "Auto-enrôlement", faceImage(14));

        assertThat(saved.getUserId()).isEqualTo(courant);
    }

    // ── Identification ───────────────────────────────────────────────

    @Test
    void visageEnrôléEstReconnuAvecConfianceÉlevée() throws IOException {
        byte[] reference = faceImage(21);
        String hash = FaceHasher.hash(reference).descriptorHash();

        when(repository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(
                        template(hash, "Jean"),
                        template(FaceHasher.hash(faceImage(555)).descriptorHash(), "Marie")));

        var result = service.identify(faceImage(21)); // même visage que la référence

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getDisplayName()).isEqualTo("Jean");
        assertThat(result.confidence()).isGreaterThan(0.7);
    }

    @Test
    void visageInconnuNonMatché() throws IOException {
        when(repository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(template(FaceHasher.hash(faceImage(777)).descriptorHash(), "Autre")));

        var result = service.identify(faceImage(888));

        assertThat(result.matched()).isFalse();
        assertThat(result.template()).isNull();
    }

    @Test
    void aucunGabaritEnrôléRésultatVide() throws IOException {
        when(repository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of());

        var result = service.identify(faceImage(31));

        assertThat(result.matched()).isFalse();
        assertThat(result.message()).contains("Aucun gabarit");
    }

    // ── Administration ───────────────────────────────────────────────

    @Test
    void désactivationEffaceLeGabaritRGPD() {
        FaceTemplate t = template("ab".repeat(32), "À effacer");
        when(repository.findById(t.getId())).thenReturn(Optional.of(t));

        service.deactivate(t.getId());

        assertThat(t.isActive()).isFalse();
    }

    @Test
    void désactivationInexistante404() {
        when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void statsCompteLesGabaritsActifs() {
        when(repository.countByTenantIdAndActiveTrue(tenantId)).thenReturn(7L);

        assertThat(service.stats()).containsEntry("enrolledFaces", 7L);
    }
}
