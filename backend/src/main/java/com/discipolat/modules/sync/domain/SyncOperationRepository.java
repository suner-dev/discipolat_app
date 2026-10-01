package com.discipolat.modules.sync.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** §G5.7 — Repository d'idempotence (unique tenant+client_uuid). */
public interface SyncOperationRepository extends JpaRepository<SyncOperation, UUID> {

    Optional<SyncOperation> findByTenantIdAndClientUuid(UUID tenantId, String clientUuid);
}
