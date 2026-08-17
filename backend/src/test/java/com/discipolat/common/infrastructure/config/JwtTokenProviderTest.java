package com.discipolat.common.infrastructure.config;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        // Build PEM-format private key (base64 of DER with PEM headers)
        String privDerB64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(pair.getPrivate().getEncoded());
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + privDerB64 + "\n-----END PRIVATE KEY-----";

        // Build PEM-format public key
        String pubDerB64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(pair.getPublic().getEncoded());
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + pubDerB64 + "\n-----END PUBLIC KEY-----";

        // Base64-encode the PEM content (simulating what setup-keys.sh does)
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKeyPem.getBytes(StandardCharsets.UTF_8));
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyPem.getBytes(StandardCharsets.UTF_8));

        jwtTokenProvider = new JwtTokenProvider(privateKeyBase64, publicKeyBase64, "", "");
    }

    @Test
    void generateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String token =        jwtTokenProvider.generateAccessToken(userId, "test@test.com", "FAISEUR", Set.of("FAISEUR"), false, null);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateAndValidateRefreshToken() {
        UUID userId = UUID.randomUUID();
        String token =        jwtTokenProvider.generateRefreshToken(userId, "test@test.com", "FAISEUR", Set.of("FAISEUR"), null);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void extractUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token =        jwtTokenProvider.generateAccessToken(userId, "test@test.com", "FAISEUR", Set.of("FAISEUR"), false, null);
        UUID extractedId = jwtTokenProvider.extractUserId(token);
        assertEquals(userId, extractedId);
    }

    @Test
    void invalidToken_ShouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }

    @Test
    void extractRoleFromToken() {
        UUID userId = UUID.randomUUID();
        String token =        jwtTokenProvider.generateAccessToken(userId, "test@test.com", "PASTEUR", Set.of("PASTEUR"), true, null);
        String role = jwtTokenProvider.extractRole(token);
        assertEquals("PASTEUR", role);
    }
}
