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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Constat B4 — unicité email globale + acceptation cross-tenant.
 *
 * <p>Prouve qu'une invitation dont l'email est déjà porté par un compte d'une
 * AUTRE église ne crée jamais de doublon dans {@code users} : seule une
 * {@code TenantMembership} est ajoutée (décision D3) et le drapeau
 * {@code crossTenantIdentity} vaut {@code true}.
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceCrossTenantTest {

    private static final String EMAIL = "pasteur@example.com";
    private static final String TOKEN = "cross-tenant-token";

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
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private com.discipolat.modules.authentication.domain.EmailService emailService;

    // ---------- 1. même tenant : réattribution simple, aucun doublon ----------

    @Test
    void reusesExistingAccountWhenItAlreadyBelongsToTheInvitedTenant() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = invitation(tenantId);
        User existing = existingUser(tenantId);
        givenPendingInvitation(invitation);
        givenRole("MEMBER");
        when(userRepository.findGlobalByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existing));
        when(membershipRepository.existsExactActiveMembership(
                existing.getId(), tenantId, idOfRole(), MembershipStatus.ACTIVE,
                MembershipScopeType.TENANT, null)).thenReturn(false);

        InvitationService.AcceptanceResult result = service().accept(TOKEN, null, null, null);

        assertThat(result.crossTenantIdentity()).isFalse();
        assertThat(result.alreadyMember()).isFalse();
        assertThat(result.userId()).isEqualTo(existing.getId());
        assertThat(result.tenantId()).isEqualTo(tenantId);
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- 2. autre tenant : membership ajoutée, AUCUN User créé ----------

    @Test
    void addsMembershipWithoutCreatingUserWhenAccountBelongsToAnotherTenant() {
        UUID invitedTenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        Invitation invitation = invitation(invitedTenantId);
        User foreign = existingUser(otherTenantId);
        givenPendingInvitation(invitation);
        givenRole("MEMBER");
        when(userRepository.findGlobalByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(foreign));
        when(membershipRepository.existsExactActiveMembership(
                foreign.getId(), invitedTenantId, idOfRole(), MembershipStatus.ACTIVE,
                MembershipScopeType.TENANT, null)).thenReturn(false);

        InvitationService.AcceptanceResult result = service().accept(TOKEN, null, null, null);

        assertThat(result.crossTenantIdentity()).isTrue();
        assertThat(result.userId()).isEqualTo(foreign.getId());
        assertThat(result.tenantId()).isEqualTo(invitedTenantId);

        ArgumentCaptor<TenantMembership> captor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(invitedTenantId);
        assertThat(captor.getValue().getUserId()).isEqualTo(foreign.getId());
        assertThat(captor.getValue().getScopeType()).isEqualTo(MembershipScopeType.TENANT);
    }

    @Test
    void neverCreatesUserRowInCrossTenantScenario() {
        UUID invitedTenantId = UUID.randomUUID();
        Invitation invitation = invitation(invitedTenantId);
        User foreign = existingUser(UUID.randomUUID());
        givenPendingInvitation(invitation);
        givenRole("MEMBER");
        when(userRepository.findGlobalByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(foreign));
        when(membershipRepository.existsExactActiveMembership(
                any(), any(), any(), any(), any(), any())).thenReturn(false);

        service().accept(TOKEN, "un-mot-de-passe-qui-ne-sert-a-rien", "Jean", "Dupont");

        verify(userRepository, times(0)).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crossTenantAcceptanceDoesNotRequireAPassword() {
        UUID invitedTenantId = UUID.randomUUID();
        Invitation invitation = invitation(invitedTenantId);
        givenPendingInvitation(invitation);
        givenRole("MEMBER");
        when(userRepository.findGlobalByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(existingUser(UUID.randomUUID())));
        when(membershipRepository.existsExactActiveMembership(
                any(), any(), any(), any(), any(), any())).thenReturn(false);

        // password == null : avant le correctif B4 cela levait INVITATION_PASSWORD_REQUIRED.
        InvitationService.AcceptanceResult result = service().accept(TOKEN, null, null, null);

        assertThat(result.crossTenantIdentity()).isTrue();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    // ---------- 3. nouvel email : création normale ----------

    @Test
    void createsSingleUserWhenEmailIsUnknownEverywhere() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = invitation(tenantId);
        givenPendingInvitation(invitation);
        givenRole("MEMBER");
        when(userRepository.findGlobalByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(membershipRepository.existsExactActiveMembership(
                any(), any(), any(), any(), any(), any())).thenReturn(false);

        InvitationService.AcceptanceResult result = service().accept(TOKEN, "password123", "Jean", "Dupont");

        assertThat(result.crossTenantIdentity()).isFalse();
        assertThat(result.alreadyMember()).isFalse();
        verify(userRepository, times(1)).save(any(User.class));
        verify(membershipRepository, times(1)).save(any(TenantMembership.class));
    }

    // ---------- 4. l'identité est résolue globalement ET sans distinction de casse ----------

    @Test
    void resolvesIdentityCaseInsensitivelyAcrossAllTenants() {
        UUID invitedTenantId = UUID.randomUUID();
        Invitation invitation = invitation(invitedTenantId);
        User storedWithOtherCase = existingUser(UUID.randomUUID());
        storedWithOtherCase.setEmail("Pasteur@Example.COM");
        givenPendingInvitation(invitation);
        givenRole("MEMBER");
        when(userRepository.findGlobalByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(storedWithOtherCase));
        when(membershipRepository.existsExactActiveMembership(
                any(), any(), any(), any(), any(), any())).thenReturn(false);

        InvitationService.AcceptanceResult result = service().accept(TOKEN, null, null, null);

        // Le repository global est interroge : la casse ne peut plus creer de doublon.
        verify(userRepository).findGlobalByEmailIgnoreCase(EMAIL);
        verify(userRepository, never()).findByTenantIdAndEmail(any(), anyString());
        assertThat(result.crossTenantIdentity()).isTrue();
    }

    @Test
    void marksInvitationAcceptedAndAuditsCrossTenantCase() {
        UUID invitedTenantId = UUID.randomUUID();
        Invitation invitation = invitation(invitedTenantId);
        User foreign = existingUser(UUID.randomUUID());
        givenPendingInvitation(invitation);
        givenRole("MEMBER");
        when(userRepository.findGlobalByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(foreign));
        when(membershipRepository.existsExactActiveMembership(
                any(), any(), any(), any(), any(), any())).thenReturn(false);

        service().accept(TOKEN, null, null, null);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedAt()).isNotNull();
        verify(auditService).logSimple("INVITATION_ACCEPTED", "INVITATION", invitation.getId());
        verify(auditService).logSimple("INVITATION_ACCEPTED_CROSS_TENANT", "USER", foreign.getId());
    }

    // ---------- helpers ----------

    private UUID idOfRole() {
        return roleRepository.findGlobalByKey("MEMBER").orElseThrow().getId();
    }

    private void givenPendingInvitation(Invitation invitation) {
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash(TOKEN)))
                .thenReturn(Optional.of(invitation));
    }

    private void givenRole(String key) {
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(null).key(key).build();
        when(roleRepository.findGlobalByKey(key)).thenReturn(Optional.of(role));
    }

    private User existingUser(UUID tenantId) {
        return User.builder().id(UUID.randomUUID()).tenantId(tenantId).email(EMAIL).build();
    }

    private Invitation invitation(UUID tenantId) {
        return Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email(EMAIL)
                .tokenHash(InvitationTokenHasher.hash(TOKEN))
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private InvitationService service() {
        return new InvitationService(invitationRepository, userRepository, membershipRepository,
                roleRepository, organizationNodeRepository, passwordEncoder, auditService,
                tenantRepository, emailService, "https://app.example.com");
    }
}
