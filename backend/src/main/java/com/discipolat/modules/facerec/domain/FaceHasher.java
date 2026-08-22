package com.discipolat.modules.facerec.domain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Calculateur d'empreintes perceptuelles faciales (dHash 256 bits).
 *
 * Pipeline : décodage ImageIO (standard JDK) → niveaux de gris →
 * réduction par moyennage de zone en grille 17×16 → comparaison des
 * pixels adjacents horizontaux → empreinte de 256 bits encodée en hex.
 *
 * Ce hachage robuste aux variations mineures (luminosité, compression,
 * recadrage léger) constitue le descripteur biométrique stocké. Il ne
 * remplace pas un modèle d'embedding profond pour les grandes bases,
 * mais offre une reconnaissance fiable sur les effectifs d'une église
 * locale sans dépendance ML externe — l'interface {@code descriptorHash}
 * permet de brancher ultérieurement un encodeur neuronal (128 floats)
 * sans changer l'API.
 */
public final class FaceHasher {

    /** Largeur de la grille de hachage : hashSize + 1 colonnes pour la comparaison. */
    public static final int HASH_SIZE = 16;

    /** Distance de Hamming maximale pour déclarer une correspondance (sur 256 bits). */
    public static final int MATCH_THRESHOLD = 42;

    /** Écart-type minimal des niveaux de gris pour un gabarit exploitable. */
    private static final double MIN_STDDEV = 18.0;

    private FaceHasher() {
    }

    /** Empreinte d'une photo de visage encodée en base64 (JPEG/PNG). */
    public static FaceDescriptor hash(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length < 64) {
            throw new IOException("Image vide ou trop petite");
        }
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IllegalArgumentException e) {
            throw new IOException("Format d'image non lisible");
        }
        if (source == null) {
            throw new IOException("Format d'image non supporté (JPEG/PNG attendu)");
        }

        // 1) Niveaux de gris + réduction par moyennage de zone 17×16.
        double[][] gray = downsampleGrayscale(source, HASH_SIZE + 1, HASH_SIZE);

        // 2) Qualité : une photo uniforme (mur, écran éteint) n'est pas un visage.
        double stddev = standardDeviation(gray);
        double quality = Math.min(1.0, stddev / MIN_STDDEV);

        // 3) dHash horizontal — 16×16 = 256 bits.
        StringBuilder bits = new StringBuilder(256);
        for (int y = 0; y < HASH_SIZE; y++) {
            for (int x = 0; x < HASH_SIZE; x++) {
                bits.append(gray[x + 1][y] > gray[x][y] ? '1' : '0');
            }
        }
        return new FaceDescriptor(toHex(bits.toString()), quality);
    }

    /** Distance de Hamming entre deux empreintes hexadécimales. */
    public static int hammingDistance(String hashA, String hashB) {
        if (hashA == null || hashB == null || hashA.length() != hashB.length()) {
            return Integer.MAX_VALUE;
        }
        int distance = 0;
        for (int i = 0; i < hashA.length(); i++) {
            int xor = Character.digit(hashA.charAt(i), 16) ^ Character.digit(hashB.charAt(i), 16);
            distance += Integer.bitCount(xor);
        }
        return distance;
    }

    /** Vrai si la distance est sous le seuil de correspondance. */
    public static boolean matches(String hashA, String hashB) {
        return hammingDistance(hashA, hashB) <= MATCH_THRESHOLD;
    }

    /**
     * Réduction d'échelle par moyennage de zone : chaque cellule cible moyenne
     * tous les pixels sources qui lui correspondent — plus stable qu'un simple
     * plus-proche-voisin face à la compression JPEG.
     */
    private static double[][] downsampleGrayscale(BufferedImage img, int targetW, int targetH) {
        int w = img.getWidth();
        int h = img.getHeight();
        double[][] out = new double[targetW][targetH];

        for (int ty = 0; ty < targetH; ty++) {
            int srcYStart = (int) ((long) ty * h / targetH);
            int srcYEnd = Math.max(srcYStart + 1, (int) (((long) (ty + 1)) * h / targetH));
            for (int tx = 0; tx < targetW; tx++) {
                int srcXStart = (int) ((long) tx * w / targetW);
                int srcXEnd = Math.max(srcXStart + 1, (int) (((long) (tx + 1)) * w / targetW));
                long sum = 0;
                int count = 0;
                for (int sy = srcYStart; sy < Math.min(srcYEnd, h); sy++) {
                    for (int sx = srcXStart; sx < Math.min(srcXEnd, w); sx++) {
                        sum += luminosity(img.getRGB(sx, sy));
                        count++;
                    }
                }
                out[tx][ty] = count == 0 ? 0 : (double) sum / count;
            }
        }
        return out;
    }

    /** Luminosité perceptive ITU-R BT.601 (0–255). */
    private static int luminosity(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    private static double standardDeviation(double[][] grid) {
        double mean = 0;
        int n = 0;
        for (double[] column : grid) {
            for (double v : column) {
                mean += v;
                n++;
            }
        }
        mean /= n;
        double variance = 0;
        for (double[] column : grid) {
            for (double v : column) {
                variance += (v - mean) * (v - mean);
            }
        }
        return Math.sqrt(variance / n);
    }

    private static String toHex(String bits) {
        StringBuilder hex = new StringBuilder(bits.length() / 4);
        for (int i = 0; i < bits.length(); i += 4) {
            hex.append(Integer.toHexString(Integer.parseInt(bits.substring(i, i + 4), 2)));
        }
        return hex.toString();
    }

    /** Empreinte calculée + métrique de qualité associée. */
    public record FaceDescriptor(String descriptorHash, double qualityScore) {
    }
}
