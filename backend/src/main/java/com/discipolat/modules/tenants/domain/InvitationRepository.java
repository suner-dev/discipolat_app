package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByInvitationToken(String token);

    Optional<Invitation> findByEmailAndTenantIdAndStatus(String email, UUID tenantId, InvitationStatus status);

    List<Invitation> findByTenantIdAndStatusIn(UUID tenantId, List<InvitationStatus> statuses);

    List<Invitation> findByTenantId(UUID tenantId);

    Optional<Invitation> findByEmailAndTenantId(String email, UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, InvitationStatus status);

    List<Invitation> findByExpiresAtAfter(Instant date);

    List<Invitation> findByTenantIdAndExpiresAtBefore(UUID tenantId, Instant date);
}
