package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSubscriptionRepository extends TenantAwareRepository<TenantSubscription, UUID> {

    Optional<TenantSubscription> findFirstByTenantId(UUID tenantId);

    List<TenantSubscription> findByStatusAndCurrentPeriodStartLessThanEqual(
            SubscriptionStatus status, java.time.Instant effectiveAt);

    @Query(value = "SELECT * FROM tenant_subscriptions WHERE tenant_id = :tenantId " +
            "ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'TRIAL' THEN 0 WHEN 'PAST_DUE' THEN 0 " +
            "WHEN 'CANCELED' THEN 1 WHEN 'PENDING_CHANGE' THEN 2 ELSE 3 END, " +
            "current_period_start DESC, created_at DESC LIMIT 1", nativeQuery = true)
    Optional<TenantSubscription> findCurrentByTenantId(@Param("tenantId") UUID tenantId);

    Optional<TenantSubscription> findFirstByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<TenantSubscription> findFirstByStripeCustomerId(String stripeCustomerId);

    long countByStatus(SubscriptionStatus status);

    Page<TenantSubscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    @Query(value = """
        SELECT
            COUNT(*) as total,
            COUNT(*) FILTER (WHERE status = 'ACTIVE') as active,
            COUNT(*) FILTER (WHERE status = 'TRIAL') as trial,
            COUNT(*) FILTER (WHERE status = 'PAST_DUE') as past_due,
            COUNT(*) FILTER (WHERE status = 'CANCELED') as canceled,
            COUNT(*) FILTER (WHERE status = 'PENDING_CHANGE') as pending_change
        FROM tenant_subscriptions
        WHERE status IN ('ACTIVE', 'TRIAL', 'PAST_DUE', 'CANCELED', 'PENDING_CHANGE')
        """, nativeQuery = true)
    Map<String, Object> getDashboardStats();

    @Query(value = """
        SELECT
            plan_key,
            COUNT(*) as count
        FROM tenant_subscriptions
        WHERE status = 'ACTIVE'
        GROUP BY plan_key
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Map<String, Object>> getActiveSubscriptionsByPlan();

    @Query(value = """
        SELECT
            ts.tenant_id,
            ts.plan_key,
            ts.status,
            ts.current_period_end
        FROM tenant_subscriptions ts
        WHERE ts.status = 'PAST_DUE'
           OR ts.current_period_end < NOW() + INTERVAL '7 days'
        ORDER BY ts.current_period_end ASC
        LIMIT 100
        """, nativeQuery = true)
    List<Map<String, Object>> getSubscriptionsNeedingAttention();
}