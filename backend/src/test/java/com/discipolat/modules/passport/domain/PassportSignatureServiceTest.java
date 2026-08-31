package com.discipolat.modules.passport.domain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PassportSignatureService — signature RSA des passeports")
class PassportSignatureServiceTest {

    private static PassportSignatureService service;
    private static KeyPair keyPair;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        String privPem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        String pubPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        // Le service attend la clé en base64(PEM) via la variable d'environnement
        String privB64 = Base64.getEncoder().encodeToString(privPem.getBytes(StandardCharsets.UTF_8));
        String pubB64 = Base64.getEncoder().encodeToString(pubPem.getBytes(StandardCharsets.UTF_8));

        service = new PassportSignatureService(privB64, pubB64, "", "");
    }

    private static String toPem(String type, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }

    @Test
    @DisplayName("Les clés sont chargées depuis la configuration")
    void keysConfigured() {
        assertTrue(service.isSigningConfigured());
        assertTrue(service.isVerificationConfigured());
    }

    @Test
    @DisplayName("sha256Hex — empreinte stable et sensible au contenu")
    void sha256() {
        String h1 = service.sha256Hex("DP-AAAA|member|ACTIVE|");
        String h2 = service.sha256Hex("DP-AAAA|member|ACTIVE|");
        String h3 = service.sha256Hex("DP-AAAA|member|REVOKED|");
        assertEquals(64, h1.length());
        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
    }

    @Test
    @DisplayName("sign/verify — une signature valide se vérifie")
    void signAndVerify() {
        String payload = "DP-1234-5678-9ABC|111e4567-e89b-12d3-a456-426614174000|ACTIVE|abc";
        String signature = service.sign(payload);
        assertNotNull(signature);
        assertTrue(service.verify(payload, signature));
    }

    @Test
    @DisplayName("verify — un payload modifié invalide la signature (anti-falsification)")
    void tamperedPayloadRejected() {
        String payload = "DP-1234-5678-9ABC|member|ACTIVE|hash1";
        String signature = service.sign(payload);
        assertFalse(service.verify("DP-1234-5678-9ABC|member|REVOKED|hash1", signature));
        assertFalse(service.verify(payload + "x", signature));
    }

    @Test
    @DisplayName("verify — signature absente ou invalide rejetée sans exception")
    void invalidInputsRejected() {
        assertFalse(service.verify("payload", null));
        assertFalse(service.verify("payload", ""));
        assertFalse(service.verify("payload", "not-base64!!!"));
        assertFalse(service.verify("payload", "AAAA"));
    }

    @Test
    @DisplayName("Sans clé publique configurée, la vérification échoue proprement")
    void unconfiguredVerificationFails() {
        PassportSignatureService noKeys = new PassportSignatureService("", "", "", "");
        assertFalse(noKeys.isSigningConfigured());
        assertFalse(noKeys.isVerificationConfigured());
        assertFalse(noKeys.verify("payload", "whatever"));
    }
}
