package com.discipolat.modules.facerec;

import com.discipolat.modules.facerec.domain.FaceHasher;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaceHasherTest {

    /**
     * Génère un « visage » synthétique déterministe par graine : la géométrie
     * (position, proportions, palette, cheveux) varie fortement avec le seed,
     * la luminosité (brightnessShift) ne fait que décaler les couleurs pour
     * simuler une même personne photographiée sous un autre éclairage.
     */
    private static byte[] syntheticFace(long seed, int brightnessShift) throws IOException {
        BufferedImage img = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Random rnd = new Random(seed);

        int b = brightnessShift;
        // Fond : teinte et motif propres à la graine
        Color bg = switch (rnd.nextInt(4)) {
            case 0 -> new Color(60 + b, 80 + b, 120 + b);
            case 1 -> new Color(120 + b, 110 + b, 60 + b);
            case 2 -> new Color(90 + b, 130 + b, 90 + b);
            default -> new Color(140 + b, 70 + b, 70 + b);
        };
        g.setColor(bg);
        g.fillRect(0, 0, 320, 320);
        if (rnd.nextBoolean()) { // bandes de fond pour certains profils
            g.setColor(bg.darker());
            for (int i = 0; i < 5; i++) {
                g.fillRect(rnd.nextInt(280), 0, 8, 320);
            }
        }

        // Géométrie du visage fortement variable
        int cx = 140 + rnd.nextInt(50);
        int cy = 140 + rnd.nextInt(40);
        int rx = 62 + rnd.nextInt(38);
        int ry = 88 + rnd.nextInt(44);

        // Cheveux éventuels
        if (rnd.nextBoolean()) {
            g.setColor(new Color(35 + b, 25 + b, 20 + b));
            g.fillOval(cx - rx - 12, cy - ry - 14, 2 * rx + 24, ry);
        }

        // Ovale du visage
        g.setColor(new Color(215 + b, 180 + b, 150 + b));
        g.fillOval(cx - rx, cy - ry, 2 * rx, 2 * ry);

        // Yeux proportionnés au visage
        g.setColor(Color.DARK_GRAY);
        int eyeRy = Math.max(6, ry / 10);
        int eyeRx = Math.max(9, rx / 5);
        int eyeY = cy - ry / 3;
        g.fillOval(cx - rx / 2 - eyeRx / 2 + rnd.nextInt(5), eyeY, eyeRx, eyeRy);
        g.fillOval(cx + rx / 2 - eyeRx / 2 - rnd.nextInt(5), eyeY, eyeRx, eyeRy);

        // Bouche proportionnée
        int mouthW = rx;
        g.drawArc(cx - mouthW / 2, cy + ry / 5, mouthW, Math.max(18, ry / 3),
                10 + rnd.nextInt(30), -180);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void empreinteDe64CaracteresHexadecimauxAvecBonneQualite() throws IOException {
        FaceHasher.FaceDescriptor d = FaceHasher.hash(syntheticFace(1, 0));

        assertThat(d.descriptorHash()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(d.qualityScore()).isGreaterThan(0.5);
    }

    @Test
    void memeImageEmpreinteIdentique() throws IOException {
        byte[] image = syntheticFace(42, 0);

        assertThat(FaceHasher.hammingDistance(
                FaceHasher.hash(image).descriptorHash(),
                FaceHasher.hash(image).descriptorHash())).isZero();
    }

    @Test
    void variationLumiereDistanceFaibleDoncCorrespondance() throws IOException {
        String ref = FaceHasher.hash(syntheticFace(7, 0)).descriptorHash();
        String probe = FaceHasher.hash(syntheticFace(7, 18)).descriptorHash();

        assertThat(FaceHasher.hammingDistance(ref, probe)).isLessThanOrEqualTo(FaceHasher.MATCH_THRESHOLD);
        assertThat(FaceHasher.matches(ref, probe)).isTrue();
    }

    @Test
    void visagesDifferentsRejetes() throws IOException {
        String a = FaceHasher.hash(syntheticFace(100, 0)).descriptorHash();
        String b = FaceHasher.hash(syntheticFace(999, 0)).descriptorHash();

        assertThat(a).isNotEqualTo(b);
        assertThat(FaceHasher.matches(a, b))
                .as("deux visages distincts doivent différer au-delà du seuil")
                .isFalse();
    }

    @Test
    void photoUniformeSignaleeFaibleQualite() throws IOException {
        BufferedImage flat = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flat.createGraphics();
        g.setColor(new Color(150, 150, 150));
        g.fillRect(0, 0, 200, 200);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(flat, "png", out);

        assertThat(FaceHasher.hash(out.toByteArray()).qualityScore()).isLessThan(0.25);
    }

    @Test
    void octetsInvalidesRejetes() {
        assertThatThrownBy(() -> FaceHasher.hash("pas une image".getBytes()))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> FaceHasher.hash(null)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> FaceHasher.hash(new byte[10])).isInstanceOf(IOException.class);
    }

    @Test
    void distanceMaximaleSurEntreesIncompatibles() {
        assertThat(FaceHasher.hammingDistance(null, "abc")).isEqualTo(Integer.MAX_VALUE);
        assertThat(FaceHasher.hammingDistance("abc", "abcd")).isEqualTo(Integer.MAX_VALUE);
        assertThat(FaceHasher.matches("00", null)).isFalse();
    }
}
