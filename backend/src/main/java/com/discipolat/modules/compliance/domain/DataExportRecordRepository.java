package com.discipolat.modules.compliance.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataExportRecordRepository extends JpaRepository<DataExportRecord, UUID> {
    List<DataExportRecord> findTop50ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<DataExportRecord> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);
}
