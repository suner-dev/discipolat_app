package com.discipolat.modules.payments.payout;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * A3 (M8) — Primitives crypto partagées par les providers de décaissement.
 *
 * <p>La comparaison de signature est en temps constant
 * ({@link MessageDigest#isEqual}) : une comparaison lexicographique classique
 * est une oracle temporelle exploitable pour reconstituer une signature HMAC
 * octet par octet.</p>
 */
final class PayoutCrypto {

    private PayoutCrypto() {
    }

    /** HMAC-SHA256 en hexadécimal minuscule ; lève si le secret est vide (appelant responsable). */
    static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible sur cette JVM", e);
        }
    }

    /** Comparaison en temps constant de deux hexa (insensible à la casse). */
    static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
