package com.discipolat.modules.authentication.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {
    Optional<ActivationToken> findByToken(String token);

    Optional<ActivationToken> findByUserIdAndUsedFalse(UUID userId);

    long deleteByExpiresAtBefore(Instant expiry);
}
