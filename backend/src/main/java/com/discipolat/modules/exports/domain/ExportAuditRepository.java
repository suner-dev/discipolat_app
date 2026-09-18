package com.discipolat.modules.exports.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExportAuditRepository extends JpaRepository<ExportAudit, UUID> {
    
    List<ExportAudit> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    List<ExportAudit> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    List<ExportAudit> findByTenantIdAndExportTypeOrderByCreatedAtDesc(UUID tenantId, String exportType);
    
    List<ExportAudit> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);
}