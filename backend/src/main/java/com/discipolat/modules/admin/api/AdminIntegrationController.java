package com.discipolat.modules.admin.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/admin/integrations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIntegrationController {

    private static final Set<String> VALID_CATEGORIES = Set.of("smtp", "storage", "jwt", "rate-limiting");

    private final ConcurrentHashMap<String, Map<String, Object>> configs = new ConcurrentHashMap<>();

    public AdminIntegrationController() {
        configs.put("smtp", Map.of(
                "host", "smtp.example.com",
                "port", 587,
                "username", "",
                "password", "",
                "fromAddress", "noreply@discipolat.com",
                "fromName", "Discipolat",
                "tls", true,
                "enabled", false
        ));
        configs.put("storage", Map.of(
                "provider", "MINIO",
                "bucket", "discipolat-files",
                "region", "eu-west-1",
                "accessKey", "",
                "secretKey", "",
                "endpoint", "",
                "enabled", false
        ));
        configs.put("jwt", Map.of(
                "accessTokenTtlMinutes", 15,
                "refreshTokenTtlDays", 7,
                "algorithm", "RS256",
                "enabled", true
        ));
        configs.put("rate-limiting", Map.of(
                "enabled", true,
                "requestsPerMinute", 60,
                "burstSize", 10,
                "blockDurationMinutes", 30
        ));
    }

    @GetMapping("/{category}")
    public ResponseEntity<?> getConfig(@PathVariable String category) {
        String key = normalizeCategory(category);
        if (key == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown category: " + category));
        }
        return ResponseEntity.ok(configs.getOrDefault(key, Map.of()));
    }

    @PutMapping("/{category}")
    public ResponseEntity<?> saveConfig(@PathVariable String category, @RequestBody Map<String, Object> body) {
        String key = normalizeCategory(category);
        if (key == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown category: " + category));
        }
        configs.put(key, body);
        return ResponseEntity.ok(Map.of("success", true, "category", key));
    }

    @PostMapping("/{category}/test")
    public ResponseEntity<?> testConnection(@PathVariable String category) {
        String key = normalizeCategory(category);
        if (key == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown category: " + category));
        }

        return switch (key) {
            case "smtp" -> testSmtp();
            case "storage" -> testStorage();
            case "jwt" -> testJwt();
            case "rate-limiting" -> testRateLimiting();
            default -> ResponseEntity.badRequest().body(Map.of("error", "Unknown category: " + category));
        };
    }

    private String normalizeCategory(String category) {
        if (VALID_CATEGORIES.contains(category)) return category;
        if ("rateLimiting".equals(category)) return "rate-limiting";
        return null;
    }

    private ResponseEntity<?> testSmtp() {
        Map<String, Object> cfg = configs.get("smtp");
        if (cfg == null || cfg.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No SMTP configuration found"));
        }
        String host = (String) cfg.getOrDefault("host", "");
        int port = cfg.containsKey("port") ? ((Number) cfg.get("port")).intValue() : 587;
        if (host.isBlank()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "SMTP host is empty"));
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            return ResponseEntity.ok(Map.of("success", true, "message", "Connection to " + host + ":" + port + " successful"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Connection failed: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> testStorage() {
        Map<String, Object> cfg = configs.get("storage");
        if (cfg == null || cfg.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No storage configuration found"));
        }
        String provider = (String) cfg.getOrDefault("provider", "");
        String bucket = (String) cfg.getOrDefault("bucket", "");
        String endpoint = (String) cfg.getOrDefault("endpoint", "");
        if (bucket.isBlank()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Bucket name is empty"));
        }
        if (!endpoint.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(endpoint);
                try (Socket socket = new Socket()) {
                    int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(uri.getScheme()) ? 443 : 80);
                    socket.connect(new InetSocketAddress(uri.getHost(), port), 5000);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Bucket '" + bucket + "' endpoint reachable (" + provider + ")"));
                }
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Endpoint unreachable: " + e.getMessage()));
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Storage configured for " + provider + " / " + bucket + " (no endpoint to test)"));
    }

    private ResponseEntity<?> testJwt() {
        Map<String, Object> cfg = configs.get("jwt");
        if (cfg == null || cfg.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "No JWT configuration found"));
        }
        String algorithm = (String) cfg.getOrDefault("algorithm", "RS256");
        try {
            KeyPairGenerator gen;
            switch (algorithm) {
                case "RS256" -> {
                    gen = KeyPairGenerator.getInstance("RSA");
                    gen.initialize(new RSAKeyGenParameterSpec(2048, java.security.spec.RSAKeyGenParameterSpec.F4));
                    KeyPair kp = gen.generateKeyPair();
                    RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
                    RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Key pair generated successfully (" + algorithm + ")",
                            "keySize", pub.getModulus().bitLength(),
                            "publicKeyFingerprint", Base64.getEncoder().encodeToString(
                                    java.security.MessageDigest.getInstance("SHA-256")
                                            .digest(pub.getEncoded())).substring(0, 16)
                    ));
                }
                case "ES256" -> {
                    gen = KeyPairGenerator.getInstance("EC");
                    gen.initialize(new ECGenParameterSpec("secp256r1"));
                    KeyPair kp = gen.generateKeyPair();
                    ECPublicKey pub = (ECPublicKey) kp.getPublic();
                    ECPrivateKey priv = (ECPrivateKey) kp.getPrivate();
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Key pair generated successfully (" + algorithm + ")",
                            "curve", "P-256",
                            "publicKeyFingerprint", Base64.getEncoder().encodeToString(
                                    java.security.MessageDigest.getInstance("SHA-256")
                                            .digest(pub.getEncoded())).substring(0, 16)
                    ));
                }
                case "HS256" -> {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Symmetric algorithm " + algorithm + " — no key pair to verify (uses shared secret)"
                    ));
                }
                default -> {
                    return ResponseEntity.ok(Map.of("success", false, "message", "Unsupported algorithm: " + algorithm));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Key generation failed: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> testRateLimiting() {
        Map<String, Object> cfg = configs.get("rate-limiting");
        if (cfg == null || cfg.isEmpty()) {
            cfg = Map.of("enabled", false, "requestsPerMinute", 0, "burstSize", 0, "blockDurationMinutes", 0);
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rate limiting is " + (Boolean.TRUE.equals(cfg.get("enabled")) ? "enabled" : "disabled"),
                "config", cfg
        ));
    }

    private record TestResult(boolean success, String message) {}
}
