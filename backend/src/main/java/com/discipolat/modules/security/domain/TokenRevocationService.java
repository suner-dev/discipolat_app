package com.discipolat.modules.security.domain;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class TokenRevocationService {

    private final RevokedTokenRepository repository;

    public TokenRevocationService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) return false;
        return repository.existsByTokenHashAndExpiresAtAfter(hash(token), Instant.now());
    }

    @Transactional
    public void revoke(String token, String tokenType, Instant expiresAt, String reason) {
        if (token == null || token.isBlank() || expiresAt == null || !expiresAt.isAfter(Instant.now())) return;
        String hash = hash(token);
        if (repository.findByTokenHash(hash).isPresent()) return;
        repository.save(RevokedToken.builder()
                .tokenHash(hash)
                .tokenType(tokenType)
                .expiresAt(expiresAt)
                .revokedAt(Instant.now())
                .reason(reason)
                .build());
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
