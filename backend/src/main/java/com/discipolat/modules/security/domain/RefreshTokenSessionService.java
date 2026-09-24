package com.discipolat.modules.security.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenSessionService {

    private final RefreshTokenSessionRepository repository;

    public RefreshTokenSessionService(RefreshTokenSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void register(String token, UUID userId, UUID familyId, Instant expiresAt) {
        repository.save(RefreshTokenSession.builder()
                .userId(userId)
                .familyId(familyId)
                .tokenHash(hash(token))
                .expiresAt(expiresAt)
                .build());
    }

    @Transactional
    public ConsumptionResult consume(String token, UUID userId, UUID familyId) {
        RefreshTokenSession session = repository.findByTokenHashForUpdate(hash(token))
                .orElse(null);
        if (session == null || !userId.equals(session.getUserId()) || !familyId.equals(session.getFamilyId())) {
            return ConsumptionResult.NOT_FOUND;
        }

        if (session.getUsedAt() != null) {
            revokeFamily(familyId);
            return ConsumptionResult.REUSE;
        }
        if (session.getRevokedAt() != null) {
            return ConsumptionResult.REVOKED;
        }
        if (!session.getExpiresAt().isAfter(Instant.now())) {
            session.setRevokedAt(Instant.now());
            repository.save(session);
            return ConsumptionResult.EXPIRED;
        }

        session.setUsedAt(Instant.now());
        repository.save(session);
        return ConsumptionResult.ROTATED;
    }

    @Transactional
    public void revokeByToken(String token) {
        repository.findByTokenHashForUpdate(hash(token))
                .ifPresent(session -> revokeFamily(session.getFamilyId()));
    }

    private void revokeFamily(UUID familyId) {
        repository.findByFamilyId(familyId).forEach(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
                repository.save(session);
            }
        });
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public enum ConsumptionResult {
        ROTATED,
        NOT_FOUND,
        REUSE,
        REVOKED,
        EXPIRED
    }
}
