package com.discipolat.modules.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, UUID> {

    /** Anti-spam : la dernière demande récente pour un même (user, permission). */
    Optional<AccessRequest> findFirstByTenantIdAndUserIdAndPermissionKeyAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID tenantId, UUID userId, String permissionKey, Instant after);
}
