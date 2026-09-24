package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.ai.domain.AiUsageRepository;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.messages.domain.ConversationMessageRepository;
import com.discipolat.modules.trainings.domain.CourseRepository;
import com.discipolat.modules.users.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private FileEntityRepository fileRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ConversationMessageRepository messageRepository;
    @Mock private AiUsageRepository aiUsageRepository;
    @Mock private TenantPlanPolicy planPolicy;
    @Mock private TenantUsageSnapshotService usageSnapshotService;

    private QuotaService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service = new QuotaService(tenantRepository, userRepository, organizationNodeRepository,
                fileRepository, courseRepository, messageRepository, aiUsageRepository,
                planPolicy, usageSnapshotService, new ObjectMapper());
        tenantId = UUID.randomUUID();
        when(tenantRepository.findByIdForUpdate(tenantId))
                .thenReturn(java.util.Optional.of(Tenant.builder().id(tenantId).plan("GROWTH").build()));
    }

    @Test
    void writeChecksFailClosedWhenSubscriptionOrCatalogIsInvalid() {
        SaasPlan plan = SaasPlan.builder().key("GROWTH").isActive(true).build();
        when(planPolicy.resolve(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TenantPlanPolicy.ResolvedPlan("GROWTH", "GROWTH", plan,
                        Map.of("max_users", 10), false, true, true, null));

        assertThatThrownBy(() -> service.checkCanCreateUser(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("QUOTA_CONFIGURATION_INVALID");
    }

    @Test
    void organizationChecksUseCatalogOrganizationLimits() {
        SaasPlan plan = SaasPlan.builder().key("GROWTH").isActive(true).build();
        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId).planKey("GROWTH").status(com.discipolat.modules.tenants.enums.SubscriptionStatus.ACTIVE)
                .build();
        when(planPolicy.resolve(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TenantPlanPolicy.ResolvedPlan("GROWTH", "GROWTH", plan,
                        Map.of("max_churches", 1), true, true, true, subscription));
        when(planPolicy.isEnabledSubscription(com.discipolat.modules.tenants.enums.SubscriptionStatus.ACTIVE))
                .thenReturn(true);
        when(organizationNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH))
                .thenReturn(1L);
        when(organizationNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.checkCanCreateChurch(tenantId, OrganizationNodeType.ROOT_CHURCH))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("QUOTA_EXCEEDED_CHURCHES");
    }

    @Test
    void missingResourceLimitFailsClosed() {
        SaasPlan plan = SaasPlan.builder().key("GROWTH").isActive(true).build();
        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId).planKey("GROWTH").status(com.discipolat.modules.tenants.enums.SubscriptionStatus.ACTIVE)
                .build();
        when(planPolicy.resolve(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TenantPlanPolicy.ResolvedPlan("GROWTH", "GROWTH", plan,
                        Map.of("max_users", 10), true, true, true, subscription));
        when(planPolicy.isEnabledSubscription(com.discipolat.modules.tenants.enums.SubscriptionStatus.ACTIVE))
                .thenReturn(true);
        when(planPolicy.limit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(TenantPlanPolicy.Limit.USERS)))
                .thenReturn(java.util.OptionalLong.empty());

        assertThatThrownBy(() -> service.checkCanCreateUser(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("QUOTA_CONFIGURATION_INVALID");
    }
}
