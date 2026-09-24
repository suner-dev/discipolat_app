package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAdminAuthorizationTest {

    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private AuthorizationService authorizationService;

    @Test
    void acceptsAdminMembershipOnlyInTenantScope() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantMembership owner = TenantMembership.builder()
                .tenantId(tenantId).userId(userId)
                .role(Role.builder().key("TENANT_OWNER").build())
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                userId, tenantId, MembershipStatus.ACTIVE)).thenReturn(List.of(owner));

        assertThat(authorizationService.isTenantAdmin(userId, tenantId)).isTrue();
        assertThat(authorizationService.isTenantAdmin(UUID.randomUUID(), tenantId)).isFalse();
    }

    @Test
    void refusesAdminRoleOutsideTenantScope() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantMembership departmentAdmin = TenantMembership.builder()
                .tenantId(tenantId).userId(userId)
                .role(Role.builder().key("TENANT_ADMIN").build())
                .scopeType(MembershipScopeType.DEPARTMENT)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                userId, tenantId, MembershipStatus.ACTIVE)).thenReturn(List.of(departmentAdmin));

        assertThat(authorizationService.isTenantAdmin(userId, tenantId)).isFalse();
    }
}
