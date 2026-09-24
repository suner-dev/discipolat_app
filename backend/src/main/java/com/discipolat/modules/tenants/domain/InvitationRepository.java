package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.id = :id")
    Optional<Invitation> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.tokenHash = :tokenHash")
    Optional<Invitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<Invitation> findByEmailAndTenantIdAndStatus(String email, UUID tenantId, InvitationStatus status);

    Optional<Invitation> findByTenantIdAndEmailAndStatus(UUID tenantId, String email, InvitationStatus status);

    List<Invitation> findByTenantIdAndStatusIn(UUID tenantId, List<InvitationStatus> statuses);

    List<Invitation> findByTenantId(UUID tenantId);

    Optional<Invitation> findByEmailAndTenantId(String email, UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, InvitationStatus status);

    List<Invitation> findByExpiresAtAfter(Instant date);

    List<Invitation> findByTenantIdAndExpiresAtBefore(UUID tenantId, Instant date);
}
