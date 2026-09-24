package com.discipolat.common.infrastructure.config;

import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantStatus;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.PermissionRepository;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SoulRepository soulRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private MemberDepartmentRepository memberDepartmentRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private OrganizationNodeRepository orgNodeRepository;
    @Mock private TenantSubscriptionRepository subscriptionRepository;
    @Mock private SaasPlanRepository planRepository;
    @Mock private TenantRepository tenantRepository;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(userRepository, passwordEncoder, soulRepository,
                departmentRepository, memberDepartmentRepository, roleRepository, permissionRepository,
                membershipRepository, orgNodeRepository, subscriptionRepository, planRepository, tenantRepository);
        when(roleRepository.count()).thenReturn(1L);
        when(userRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void productionDoesNotCreateDeterministicSuperAdminByDefault() {
        ReflectionTestUtils.setField(initializer, "environment", "prod");
        ReflectionTestUtils.setField(initializer, "seedDemoAccounts", false);
        ReflectionTestUtils.setField(initializer, "superadminBootstrapEnabled", false);

        initializer.run();

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void configuredProductionBootstrapCreatesOnlyANewAccount() {
        UUID tenantId = UUID.randomUUID();
        Role role = Role.builder().id(UUID.randomUUID()).key("PLATFORM_SUPER_ADMIN").build();
        when(roleRepository.findGlobalByKey("PLATFORM_SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(userRepository.findGlobalByEmail("admin@example.test")).thenReturn(Optional.empty());
        when(tenantRepository.findFirstByStatusOrderByCreatedAtAsc(TenantStatus.ACTIVE))
                .thenReturn(Optional.of(Tenant.builder().id(tenantId).build()));
        when(passwordEncoder.encode("strong-secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(initializer, "environment", "prod");
        ReflectionTestUtils.setField(initializer, "superadminBootstrapEnabled", true);
        ReflectionTestUtils.setField(initializer, "superadminEmail", "admin@example.test");
        ReflectionTestUtils.setField(initializer, "superadminSecret", "strong-secret");

        initializer.run();

        verify(passwordEncoder).encode("strong-secret");
        verify(membershipRepository).save(any());
    }

    @Test
    void existingBootstrapEmailIsNeverElevated() {
        User existing = User.builder().id(UUID.randomUUID()).email("admin@example.test").build();
        when(roleRepository.findGlobalByKey("PLATFORM_SUPER_ADMIN"))
                .thenReturn(Optional.of(Role.builder().id(UUID.randomUUID()).key("PLATFORM_SUPER_ADMIN").build()));
        when(userRepository.findGlobalByEmail("admin@example.test")).thenReturn(Optional.of(existing));
        ReflectionTestUtils.setField(initializer, "environment", "prod");
        ReflectionTestUtils.setField(initializer, "superadminBootstrapEnabled", true);
        ReflectionTestUtils.setField(initializer, "superadminEmail", "admin@example.test");
        ReflectionTestUtils.setField(initializer, "superadminSecret", "strong-secret");

        initializer.run();

        verify(passwordEncoder, never()).encode(any());
        verify(membershipRepository, never()).save(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
