package com.discipolat.modules.dataMigration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataMigrationJobRepository extends JpaRepository<DataMigrationJob, UUID> {
    List<DataMigrationJob> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
