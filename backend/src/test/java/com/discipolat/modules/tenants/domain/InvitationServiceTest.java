package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantMembershipRepository membershipRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private OrganizationNodeRepository organizationNodeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    @Test
    void createsTenantScopedUserWhenSameEmailOnlyExistsInAnotherTenant() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = invitation(tenantId, InvitationStatus.PENDING, Instant.now().plusSeconds(3600));
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(null).key("MEMBER").build();
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash("token"))).thenReturn(Optional.of(invitation));
        when(roleRepository.findGlobalByKey("MEMBER")).thenReturn(Optional.of(role));
        when(userRepository.findByTenantIdAndEmail(tenantId, "invitee@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(membershipRepository.existsExactActiveMembership(
                any(), any(), any(), any(), any(), any())).thenReturn(false);

        InvitationService.AcceptanceResult result = service().accept(
                "token", "password123", "Jean", " Dupont");

        assertThat(result.alreadyMember()).isFalse();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedAt()).isNotNull();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(userCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        ArgumentCaptor<TenantMembership> membershipCaptor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getRoleLegacy()).isEqualTo("MEMBER");
        assertThat(membershipCaptor.getValue().getScopeType()).isEqualTo(MembershipScopeType.TENANT);
        assertThat(membershipCaptor.getValue().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void rejectsExpiredInvitationAndDoesNotCreateUser() {
        Invitation invitation = invitation(UUID.randomUUID(), InvitationStatus.PENDING, Instant.now().minusSeconds(1));
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash("token"))).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service().accept("token", "password123", "Jean", " Dupont"))
                .isInstanceOf(com.discipolat.common.exception.DomainException.class)
                .hasMessage("Invitation expirée");

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(TenantMembership.class));
    }

    @Test
    void rejectsAlreadyUsedInvitationWithoutCreatingAnotherAccount() {
        Invitation invitation = invitation(UUID.randomUUID(), InvitationStatus.ACCEPTED, Instant.now().plusSeconds(3600));
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash("token"))).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service().accept("token", "password123", "Jean", " Dupont"))
                .isInstanceOf(com.discipolat.common.exception.DomainException.class)
                .hasMessage("Invitation expirée ou déjà utilisée");

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(TenantMembership.class));
    }

    @Test
    void acceptsExistingUserFromInvitedTenantWithoutChangingCredentials() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = invitation(tenantId, InvitationStatus.PENDING, Instant.now().plusSeconds(3600));
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(null).key("MEMBER").build();
        User existing = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .email(invitation.getEmail()).build();
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash("token"))).thenReturn(Optional.of(invitation));
        when(roleRepository.findGlobalByKey("MEMBER")).thenReturn(Optional.of(role));
        when(userRepository.findByTenantIdAndEmail(tenantId, invitation.getEmail())).thenReturn(Optional.of(existing));
        when(membershipRepository.existsExactActiveMembership(
                existing.getId(),
                tenantId,
                role.getId(),
                MembershipStatus.ACTIVE,
                MembershipScopeType.TENANT,
                null)).thenReturn(true);

        InvitationService.AcceptanceResult result = service().accept("token", null, null, null);

        assertThat(result.userId()).isEqualTo(existing.getId());
        assertThat(result.alreadyMember()).isTrue();
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void createsRequestedRoleWhenOnlyAnotherTenantMembershipExists() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = invitation(tenantId, InvitationStatus.PENDING, Instant.now().plusSeconds(3600));
        invitation.setRole("PASTEUR");
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(null).key("PASTEUR").build();
        User existing = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .email(invitation.getEmail()).build();
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash("token"))).thenReturn(Optional.of(invitation));
        when(roleRepository.findGlobalByKey("PASTEUR")).thenReturn(Optional.of(role));
        when(userRepository.findByTenantIdAndEmail(tenantId, invitation.getEmail())).thenReturn(Optional.of(existing));
        when(membershipRepository.existsExactActiveMembership(
                existing.getId(), tenantId, role.getId(), MembershipStatus.ACTIVE,
                MembershipScopeType.TENANT, null)).thenReturn(false);

        InvitationService.AcceptanceResult result = service().accept("token", null, null, null);

        ArgumentCaptor<TenantMembership> captor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(result.alreadyMember()).isFalse();
        assertThat(captor.getValue().getRole().getId()).isEqualTo(role.getId());
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimit() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = invitation(tenantId, InvitationStatus.PENDING, Instant.now().plusSeconds(3600));
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(null).key("MEMBER").build();
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash("token"))).thenReturn(Optional.of(invitation));
        when(roleRepository.findGlobalByKey("MEMBER")).thenReturn(Optional.of(role));
        when(userRepository.findByTenantIdAndEmail(tenantId, invitation.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().accept("token", "a".repeat(73), "Jean", " Dupont"))
                .isInstanceOf(com.discipolat.common.exception.DomainException.class)
                .hasMessage("Le mot de passe est trop long");

        verify(userRepository, never()).save(any(User.class));
    }

    private InvitationService service() {
        return new InvitationService(invitationRepository, userRepository, membershipRepository,
                roleRepository, organizationNodeRepository, passwordEncoder, auditService);
    }

    private Invitation invitation(UUID tenantId, InvitationStatus status, Instant expiresAt) {
        return Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("invitee@example.com")
                .tokenHash(InvitationTokenHasher.hash("token"))
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(status)
                .expiresAt(expiresAt)
                .build();
    }
}
