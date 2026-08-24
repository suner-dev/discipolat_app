package com.discipolat.modules.executiveInsights.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutiveInsightRepository extends JpaRepository<ExecutiveInsight, UUID> {
    List<ExecutiveInsight> findByTenantIdAndIsDismissedFalseOrderByCreatedAtDesc(UUID tenantId);
    long countByTenantIdAndSeverity(UUID tenantId, ExecutiveInsight.Severity severity);
}
