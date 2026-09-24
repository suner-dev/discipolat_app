package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSubscriptionRepository extends TenantAwareRepository<TenantSubscription, UUID> {

    Optional<TenantSubscription> findByTenantId(UUID tenantId);

    List<TenantSubscription> findByStatusAndCurrentPeriodStartLessThanEqual(
            SubscriptionStatus status, java.time.Instant effectiveAt);

    @Query(value = "SELECT * FROM tenant_subscriptions WHERE tenant_id = :tenantId " +
            "ORDER BY CASE status WHEN 'ACTIVE' THEN 0 WHEN 'TRIAL' THEN 0 WHEN 'PAST_DUE' THEN 0 " +
            "WHEN 'CANCELED' THEN 1 WHEN 'PENDING_CHANGE' THEN 2 ELSE 3 END, " +
            "current_period_start DESC, created_at DESC LIMIT 1", nativeQuery = true)
    Optional<TenantSubscription> findCurrentByTenantId(@Param("tenantId") UUID tenantId);

    Optional<TenantSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<TenantSubscription> findByStripeCustomerId(String stripeCustomerId);

    long countByStatus(SubscriptionStatus status);

    Page<TenantSubscription> findByStatus(SubscriptionStatus status, Pageable pageable);
}