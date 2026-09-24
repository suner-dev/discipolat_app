package com.discipolat.modules.platform.domain;

import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.OrganizationNodeStatus;
import com.discipolat.modules.tenants.domain.OrganizationNodeType;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRegistrationServiceTest {

    @Mock
    private TenantRegistrationRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantService tenantService;
    @Mock
    private OrganizationNodeService organizationNodeService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TenantMembershipRepository membershipRepository;
    @Mock
    private AuditService auditService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicSubmissionDoesNotCreateTenantOrUser() {
        when(requestRepository.findByEmail("demandeur@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(requestRepository.save(any(TenantRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TenantRegistrationService service = service();

        TenantRegistrationRequest request = service.submit(
                "demandeur@example.com", "password123", "Jean", "Test", "0700000000");

        assertThat(request.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_APPROVAL);
        assertThat(request.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(request.getPasswordHash()).isNotEqualTo("password123");
        verify(tenantService, never()).create(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void approvalCreatesOwnerAndTenantMembership() {
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SecurityTestHelper.loginAs(UUID.randomUUID());
        TenantRegistrationRequest request = TenantRegistrationRequest.builder()
                .id(requestId).email("owner@example.com").passwordHash("hashed-password")
                .firstName("Owner").lastName("Test").organizationName("Owner Test")
                .slug("owner-test").plan("free").country("CM").currency("XAF")
                .timezone("Africa/Douala").locale("fr")
                .status(TenantRegistrationStatus.PENDING_APPROVAL).build();
        TenantResponse tenant = new TenantResponse(tenantId, "Owner Test", "owner-test",
                com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE, "free", "CM", "XAF",
                "Africa/Douala", "fr", null, null, null, null, Instant.now(), Instant.now());
        OrganizationNode church = OrganizationNode.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .name("Owner Test").type(OrganizationNodeType.ROOT_CHURCH).code("ROOT")
                .path("ROOT").level(0).status(OrganizationNodeStatus.ACTIVE).build();
        User owner = User.builder().id(userId).email("owner@example.com").tenantId(tenantId).build();
        Role pasteur = Role.builder().id(UUID.randomUUID()).key("PASTEUR").build();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(tenantService.create(any())).thenReturn(tenant);
        when(organizationNodeService.createRootChurch(any(), any(), any(), any())).thenReturn(church);
        when(userRepository.findGlobalByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(roleRepository.findGlobalByKey("PASTEUR")).thenReturn(Optional.of(pasteur));

        TenantRegistrationService.ApprovalResult result = service().approve(requestId, "approved");

        assertThat(result.tenant()).isEqualTo(tenant);
        assertThat(result.owner()).isEqualTo(owner);
        assertThat(request.getStatus()).isEqualTo(TenantRegistrationStatus.APPROVED);
        verify(membershipRepository).save(any());
        verify(requestRepository).save(request);
        verify(auditService).log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private TenantRegistrationService service() {
        return new TenantRegistrationService(requestRepository, userRepository, passwordEncoder,
                tenantService, organizationNodeService, roleRepository, membershipRepository, auditService);
    }
}
