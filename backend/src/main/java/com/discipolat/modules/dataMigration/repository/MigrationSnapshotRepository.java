package com.discipolat.modules.dataMigration.repository;

import com.discipolat.modules.dataMigration.domain.MigrationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MigrationSnapshotRepository extends JpaRepository<MigrationSnapshot, UUID> {

    List<MigrationSnapshot> findByTenantIdAndJobIdOrderByIdDesc(UUID tenantId, UUID jobId);
}
