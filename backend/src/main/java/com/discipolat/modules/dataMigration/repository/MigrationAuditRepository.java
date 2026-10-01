package com.discipolat.modules.dataMigration.repository;

import com.discipolat.modules.dataMigration.domain.MigrationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface MigrationAuditRepository extends JpaRepository<MigrationAudit, UUID> {

    List<MigrationAudit> findByTenantIdAndJobId(UUID tenantId, UUID jobId);

    List<MigrationAudit> findByTenantIdAndModuleCodeOrderByCreatedAtDesc(UUID tenantId, String moduleCode);

    /** Replay idempotent : sources déjà traitées (MIGRATED/MERGED) pour un module. */
    Set<MigrationAudit> findByTenantIdAndSourceTableAndStatusIn(
            UUID tenantId, String sourceTable, List<MigrationAudit.RowStatus> statuses);

    boolean existsByTenantIdAndSourceTableAndSourceIdAndStatusIn(
            UUID tenantId, String sourceTable, String sourceId, List<MigrationAudit.RowStatus> statuses);
}
