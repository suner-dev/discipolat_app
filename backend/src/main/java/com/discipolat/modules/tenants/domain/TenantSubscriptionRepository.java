package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSubscriptionRepository extends TenantAwareRepository<TenantSubscription, UUID> {

    Optional<TenantSubscription> findByTenantId(UUID tenantId);

    Optional<TenantSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<TenantSubscription> findByStripeCustomerId(String stripeCustomerId);

    long countByStatus(SubscriptionStatus status);

    Page<TenantSubscription> findByStatus(SubscriptionStatus status, Pageable pageable);
}