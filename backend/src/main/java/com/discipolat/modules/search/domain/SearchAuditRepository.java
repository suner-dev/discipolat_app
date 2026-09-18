package com.discipolat.modules.search.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SearchAuditRepository extends JpaRepository<SearchAudit, UUID> {
    
    List<SearchAudit> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    List<SearchAudit> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    List<SearchAudit> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);
    
    List<SearchAudit> findByQueryTextContainingIgnoreCase(String queryText);
}