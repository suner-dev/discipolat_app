package com.discipolat.modules.engagementAnalytics.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class EngagementAnalyticsService {

    private final EngagementAnalyticsRepository analyticsRepo;
    private final PlatformFeatureFlagService featureFlagService;

    public EngagementAnalyticsService(EngagementAnalyticsRepository analyticsRepo,
                                      PlatformFeatureFlagService featureFlagService) {
        this.analyticsRepo = analyticsRepo;
        this.featureFlagService = featureFlagService;
    }

    public List<EngagementAnalytics> listAll() {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.ANALYTICS_ENABLED);
        return analyticsRepo.findByTenantIdOrderByRecordedAtDesc(TenantContext.getCurrentTenantId());
    }

    public List<EngagementAnalytics> listByCategory(String category) {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.ANALYTICS_ENABLED);
        return analyticsRepo.findByTenantIdAndMetricCategoryOrderByRecordedAtDesc(TenantContext.getCurrentTenantId(), category);
    }

    public EngagementAnalytics record(EngagementAnalytics analytics) {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.ANALYTICS_ENABLED);
        analytics.setTenantId(TenantContext.getCurrentTenantId());
        return analyticsRepo.save(analytics);
    }

    public Map<String, Object> getDashboard() {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.ANALYTICS_ENABLED);
        UUID tenantId = TenantContext.getCurrentTenantId();
        var all = analyticsRepo.findByTenantIdOrderByRecordedAtDesc(tenantId);
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalMetrics", all.size());
        dashboard.put("byCategory", all.stream().collect(java.util.stream.Collectors.groupingBy(EngagementAnalytics::getMetricCategory)));
        dashboard.put("topGrowing", all.stream().sorted((a, b) -> Double.compare(
            b.getChangePercentage() != null ? b.getChangePercentage() : 0.0,
            a.getChangePercentage() != null ? a.getChangePercentage() : 0.0
        )).limit(5).toList());
        dashboard.put("topDeclining", all.stream().sorted((a, b) -> Double.compare(
            a.getChangePercentage() != null ? a.getChangePercentage() : 0.0,
            b.getChangePercentage() != null ? b.getChangePercentage() : 0.0
        )).limit(5).toList());
        return dashboard;
    }
}
