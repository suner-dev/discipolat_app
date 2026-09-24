package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.Invitation;
import com.discipolat.modules.tenants.domain.InvitationRepository;
import com.discipolat.modules.tenants.domain.InvitationService;
import com.discipolat.modules.tenants.domain.InvitationStatus;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    @Mock InvitationRepository invitationRepository;
    @Mock InvitationService invitationService;
    @Mock UserRepository userRepository;
    @Mock TenantMembershipRepository membershipRepository;
    @Mock RoleRepository roleRepository;
    @Mock OrganizationNodeRepository organizationNodeRepository;
    @Mock TenantRepository tenantRepository;
    @Mock AuthorizationService authorizationService;
    @Mock AuditService auditService;
    @Mock EmailService emailService;
    @Mock PerIpRateLimiter rateLimiter;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID(), null, List.of()));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void validationIsTenantScopedAndNeverCacheable() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("invitee@example.com")
                .tokenHash("hash")
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Tenant tenant = Tenant.builder().id(tenantId).name("Église Bethel").build();
        User user = User.builder().id(userId).tenantId(tenantId).email(invitation.getEmail()).build();
        when(invitationService.validate("secret-token")).thenReturn(invitation);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantIdAndEmail(tenantId, invitation.getEmail()))
                .thenReturn(Optional.of(user));

        var response = controller().validateInvitation("secret-token");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getBody()).containsEntry("accountExists", true);
        assertThat(response.getBody()).containsEntry("tenantName", "Église Bethel");
        assertThat(response.getBody()).doesNotContainKey("token");
        assertThat(response.getBody()).doesNotContainKey("tokenHash");
    }

    @Test
    void resendRotatesOnlyTheHashAndAttemptsEmailDelivery() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = pendingInvitation(tenantId);
        String previousHash = invitation.getTokenHash();
        Tenant tenant = Tenant.builder().id(tenantId).name("Église Bethel").build();
        when(invitationRepository.findByIdForUpdate(invitation.getId())).thenReturn(Optional.of(invitation));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        TenantContext.setTenantId(tenantId);

        var response = controller().resendInvitation(invitation.getId());

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        verify(emailService).send(anyString(), anyString(), anyString());
        assertThat(response.getBody()).containsEntry("emailSent", true);
        assertThat(response.getBody().get("invitationToken")).asString().hasSize(32);
        assertThat(captor.getValue().getTokenHash()).hasSize(64);
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(previousHash);
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void cancelUsesTheLockedInvitationAndPersistsCanceledState() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = pendingInvitation(tenantId);
        when(invitationRepository.findByIdForUpdate(invitation.getId())).thenReturn(Optional.of(invitation));
        TenantContext.setTenantId(tenantId);

        var response = controller().cancelInvitation(invitation.getId());

        verify(invitationRepository).save(invitation);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELED);
    }

    private Invitation pendingInvitation(UUID tenantId) {
        return Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("invitee@example.com")
                .tokenHash("old-hash")
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private InvitationController controller() {
        return new InvitationController(
                invitationRepository,
                invitationService,
                userRepository,
                membershipRepository,
                roleRepository,
                organizationNodeRepository,
                tenantRepository,
                authorizationService,
                auditService,
                emailService,
                rateLimiter,
                "https://app.example.com"
        );
    }
}
