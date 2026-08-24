package com.discipolat.modules.usageAnalytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {
    List<UsageEvent> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID tenantId, LocalDateTime from, LocalDateTime to);
}
