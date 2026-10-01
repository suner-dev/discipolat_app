package com.discipolat.modules.sync.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** §G5.7 — File de conflits LWW à réconcilier par le responsable. */
public interface SyncConflictRepository extends JpaRepository<SyncConflict, UUID> {

    List<SyncConflict> findByTenantIdAndResolvedFalseOrderByCreatedAtDesc(UUID tenantId);
}
