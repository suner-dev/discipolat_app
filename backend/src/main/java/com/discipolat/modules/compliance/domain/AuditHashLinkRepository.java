package com.discipolat.modules.compliance.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditHashLinkRepository extends JpaRepository<AuditHashLink, UUID> {
    List<AuditHashLink> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);
    Optional<AuditHashLink> findFirstByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<AuditHashLink> findFirstByTenantIdOrderByCreatedAtAsc(UUID tenantId);
    boolean existsByAuditLogId(UUID auditLogId);
}
