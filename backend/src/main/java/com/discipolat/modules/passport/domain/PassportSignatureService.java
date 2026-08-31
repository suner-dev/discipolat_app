package com.discipolat.modules.passport.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Signature cryptographique des passeports spirituels (RSA-SHA256).
 *
 * Réutilise la clé RSA de la plateforme (mêmes propriétés que le provider JWT :
 * `app.jwt.private-key*` / `app.jwt.public-key*`). Le contenu signé lie le code,
 * le membre, le statut et l'empreinte des entrées : toute falsification locale
 * (modification offline ou DB directe) invalide la vérification.
 */
@Component
public class PassportSignatureService {

    private static final Logger log = LoggerFactory.getLogger(PassportSignatureService.class);
    private static final Pattern PEM_KEY_PATTERN = Pattern.compile("-+BEGIN\\s+.*KEY-+\\s*([\\s\\S]*?)\\-+END\\s+.*KEY-+");
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public PassportSignatureService(
            @Value("${app.jwt.private-key:}") String privateKeyBase64,
            @Value("${app.jwt.public-key:}") String publicKeyBase64,
            @Value("${app.jwt.private-key-path:}") String privateKeyPath,
            @Value("${app.jwt.public-key-path:}") String publicKeyPath
    ) {
        PrivateKey priv = null;
        PublicKey pub = null;
        try {
            String privPem = readKey(privateKeyBase64, privateKeyPath);
            if (privPem != null && !privPem.isBlank()) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                priv = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodePem(privPem)));
            }
            String pubPem = readKey(publicKeyBase64, publicKeyPath);
            if (pubPem != null && !pubPem.isBlank()) {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                pub = keyFactory.generatePublic(new X509EncodedKeySpec(decodePem(pubPem)));
            }
        } catch (Exception e) {
            log.warn("[Passport] Clés de signature indisponibles, la vérification cryptographique sera dégradée : {}", e.getMessage());
        }
        this.privateKey = priv;
        this.publicKey = pub;
    }

    /** true si la plateforme peut signer (clé privée chargée). */
    public boolean isSigningConfigured() {
        return privateKey != null;
    }

    /** true si la plateforme peut vérifier (clé publique chargée). */
    public boolean isVerificationConfigured() {
        return publicKey != null;
    }

    /** Empreinte SHA-256 hexadécimale du contenu canonique. */
    public String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Signe le payload canonique (RSA-SHA256, base64). */
    public String sign(String payload) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de signer le passeport", e);
        }
    }

    /** Vérifie la signature RSA du payload. */
    public boolean verify(String payload, String signatureBase64) {
        if (!isVerificationConfigured() || payload == null || signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    private String readKey(String base64Content, String filePath) {
        if (filePath != null && !filePath.isBlank()) {
            try {
                return Files.readString(Path.of(filePath));
            } catch (Exception e) {
                log.debug("Fichier de clé '{}' illisible, essai de la variable d'environnement", filePath);
            }
        }
        if (base64Content != null && !base64Content.isBlank()) {
            return new String(Base64.getDecoder().decode(base64Content));
        }
        return null;
    }

    private byte[] decodePem(String pemContent) {
        Matcher matcher = PEM_KEY_PATTERN.matcher(pemContent);
        String body = matcher.find() ? matcher.group(1) : pemContent;
        return Base64.getMimeDecoder().decode(body.replaceAll("\\s", ""));
    }
}
