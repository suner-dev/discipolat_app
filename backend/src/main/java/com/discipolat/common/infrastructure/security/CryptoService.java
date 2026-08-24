package com.discipolat.common.infrastructure.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement AES-256-GCM au repos pour les données sensibles
 * (tokens API tiers, notes pastorales, prières privées…).
 * Format sérialisé : base64(iv[12] || ciphertext||tag[128]).
 */
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public CryptoService(String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalArgumentException("La clé de chiffrement doit faire 32 octets (AES-256), reçue : " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /** Chiffre une chaîne ; retourne null si l'entrée est null/vide. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv).put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Échec du chiffrement", e);
        }
    }

    /** Déchiffre une chaîne produite par encrypt() ; retourne null si l'entrée est null/vide. */
    public String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du déchiffrement", e);
        }
    }
}
