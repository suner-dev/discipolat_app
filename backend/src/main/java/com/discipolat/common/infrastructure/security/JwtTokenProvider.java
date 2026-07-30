package com.discipolat.common.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final long ACCESS_TOKEN_VALIDITY_MINUTES = 15;
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;
    private static final Pattern PEM_KEY_PATTERN = Pattern.compile("-+BEGIN\\s+.*KEY-+\\s*([\\s\\S]*?)\\-+END\\s+.*KEY-+");

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtTokenProvider(
            @Value("${app.jwt.private-key:}") String privateKeyBase64,
            @Value("${app.jwt.public-key:}") String publicKeyBase64,
            @Value("${app.jwt.private-key-path:}") String privateKeyPath,
            @Value("${app.jwt.public-key-path:}") String publicKeyPath
    ) {
        try {
            String privKeyContent = readKey(privateKeyBase64, privateKeyPath);
            String pubKeyContent = readKey(publicKeyBase64, publicKeyPath);

            byte[] privateKeyBytes = decodePem(privKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = keyFactory.generatePrivate(keySpec);

            if (pubKeyContent != null && !pubKeyContent.isBlank()) {
                byte[] publicKeyBytes = decodePem(pubKeyContent);
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                this.publicKey = keyFactory.generatePublic(publicKeySpec);
            } else {
                this.publicKey = null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT keys", e);
        }
    }

    private String readKey(String base64Content, String filePath) {
        if (filePath != null && !filePath.isBlank()) {
            try {
                String pemContent = Files.readString(Path.of(filePath));
                log.info("Loaded key from file: {}", filePath);
                return pemContent;
            } catch (Exception e) {
                log.warn("Could not read key file '{}', falling back to env var", filePath);
            }
        }
        if (base64Content != null && !base64Content.isBlank()) {
            return new String(Base64.getDecoder().decode(base64Content));
        }
        throw new IllegalArgumentException("No JWT key provided (neither base64 env var nor file path)");
    }

    private byte[] decodePem(String pemContent) {
        Matcher matcher = PEM_KEY_PATTERN.matcher(pemContent);
        if (matcher.find()) {
            String base64 = matcher.group(1).replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        }
        // Already raw base64 (no PEM headers)
        return Base64.getDecoder().decode(pemContent.replaceAll("\\s", ""));
    }

    public String generateAccessToken(UUID userId, String email, String activeRole, java.util.Set<String> roles, boolean estChefDeFamille) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", activeRole);
        claims.put("roles", roles);
        claims.put("activeRole", activeRole);
        claims.put("estChefDeFamille", estChefDeFamille);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(ACCESS_TOKEN_VALIDITY_MINUTES))))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String email, String activeRole, java.util.Set<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", activeRole);
        claims.put("roles", roles);
        claims.put("activeRole", activeRole);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_VALIDITY_DAYS))))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public java.util.List<String> extractRoles(String token) {
        return getClaims(token).get("roles", java.util.List.class);
    }

    public String extractActiveRole(String token) {
        String activeRole = getClaims(token).get("activeRole", String.class);
        return activeRole != null ? activeRole : getClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getPublicKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private PublicKey getPublicKey() {
        if (this.publicKey != null) {
            return this.publicKey;
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            java.security.interfaces.RSAPrivateKey rsaPrivateKey = (java.security.interfaces.RSAPrivateKey) privateKey;
            java.security.spec.RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(
                    rsaPrivateKey.getModulus(),
                    java.math.BigInteger.valueOf(65537)
            );
            return keyFactory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive public key from private key", e);
        }
    }
}
