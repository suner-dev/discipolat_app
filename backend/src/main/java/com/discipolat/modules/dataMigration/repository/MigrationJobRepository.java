package com.discipolat.modules.dataMigration.repository;

import com.discipolat.modules.dataMigration.domain.MigrationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MigrationJobRepository extends JpaRepository<MigrationJob, UUID> {

    List<MigrationJob> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<MigrationJob> findByTenantIdAndModuleCodeOrderByCreatedAtDesc(UUID tenantId, String moduleCode);

    MigrationJob findByIdAndTenantId(UUID id, UUID tenantId);
}
