package com.discipolat.modules.security.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    Optional<RevokedToken> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBefore(Instant expiry);
}
