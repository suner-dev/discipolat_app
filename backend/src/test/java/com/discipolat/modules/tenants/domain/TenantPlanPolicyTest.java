package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantPlanPolicyTest {

    @Mock
    private TenantSubscriptionRepository subscriptionRepository;
    @Mock
    private SaasPlanRepository planRepository;

    private TenantPlanPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new TenantPlanPolicy(subscriptionRepository, planRepository, new ObjectMapper());
    }

    @Test
    void resolvesCaseInsensitiveAliasToCanonicalCatalogPlan() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).plan("pro").build();
        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId).planKey("pRo").status(SubscriptionStatus.ACTIVE).build();
        SaasPlan plan = SaasPlan.builder().key("GROWTH").isActive(true).status("ACTIVE")
                .seatsLimit(500).storageLimitMb(20000).aiCreditsLimit(2000)
                .limitsJson("{\"max_users\":500}").featuresJson("{\"ai\":true}").build();
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(planRepository.findById("GROWTH")).thenReturn(Optional.of(plan));

        TenantPlanPolicy.ResolvedPlan resolved = policy.resolve(tenant);

        assertThat(resolved.canonicalKey()).isEqualTo("GROWTH");
        assertThat(resolved.plan()).isSameAs(plan);
        assertThat(resolved.enforcementEnabled()).isTrue();
        assertThat(policy.limit(resolved, TenantPlanPolicy.Limit.USERS)).hasValue(500L);
    }

    @Test
    void subscriptionWithoutPlanKeyIsNotEnforced() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).plan("GROWTH").build();
        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId).planKey(" ").status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(subscription));

        assertThat(policy.resolve(tenant).enforcementEnabled()).isFalse();
    }

    @Test
    void invalidOrNegativeLimitsAreNotUnlimited() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).plan("growth").build();
        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId).planKey("growth").status(SubscriptionStatus.ACTIVE).build();
        SaasPlan plan = SaasPlan.builder().key("GROWTH").isActive(true).status("ACTIVE")
                .limitsJson("{\"max_users\":-1}").featuresJson("{}").build();
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(planRepository.findById("GROWTH")).thenReturn(Optional.of(plan));

        TenantPlanPolicy.ResolvedPlan resolved = policy.resolve(tenant);

        assertThat(resolved.limitsValid()).isFalse();
        assertThat(resolved.enforcementEnabled()).isFalse();
        assertThat(policy.limit(resolved, TenantPlanPolicy.Limit.USERS)).isEmpty();
    }

    @Test
    void inactiveSubscriptionIsNeverEnforced() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).plan("GROWTH").build();
        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId).planKey("GROWTH").status(SubscriptionStatus.CANCELED).build();
        SaasPlan plan = SaasPlan.builder().key("GROWTH").isActive(true).status("ACTIVE")
                .limitsJson("{\"max_users\":500}").featuresJson("{}").build();
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(planRepository.findById("GROWTH")).thenReturn(Optional.of(plan));

        assertThat(policy.resolve(tenant).enforcementEnabled()).isFalse();
    }
}
