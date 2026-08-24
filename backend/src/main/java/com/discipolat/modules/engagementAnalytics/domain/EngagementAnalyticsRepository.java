package com.discipolat.modules.engagementAnalytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EngagementAnalyticsRepository extends JpaRepository<EngagementAnalytics, UUID> {
    List<EngagementAnalytics> findByTenantIdOrderByRecordedAtDesc(UUID tenantId);
    List<EngagementAnalytics> findByTenantIdAndMetricCategoryOrderByRecordedAtDesc(UUID tenantId, String category);
}
