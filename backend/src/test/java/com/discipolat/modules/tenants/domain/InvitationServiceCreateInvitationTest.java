package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat B2, §4 A3.5 — extraction de la création d'invitation de
 * {@code InvitationController} vers {@code InvitationService.createInvitation},
 * SANS régression de comportement (le contrôleur délègue désormais au service
 * et n'a plus qu'à traduire la réponse).
 *
 * <p>Le service est aussi appelé par l'action d'onboarding {@code ROLES} : une
 * seule implémentation, donc aucune divergence entre l'écran d'invitations et
 * le parcours d'onboarding.
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceCreateInvitationTest {

    private static final String FRONTEND_URL = "https://app.example.com";

    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.modules.people.service.PeopleService peopleService;
    @Mock private com.discipolat.modules.people.repository.PersonRepository personRepository;

    @Mock private TenantRepository tenantRepository;
    @Mock private EmailService emailService;

    private InvitationService service;
    private UUID tenantId;
    private UUID inviterId;

    @BeforeEach
    void setUp() {
        service = new InvitationService(invitationRepository, userRepository, membershipRepository,
                roleRepository, organizationNodeRepository, passwordEncoder, auditService,
                tenantRepository, emailService, peopleService, personRepository, FRONTEND_URL);
        tenantId = UUID.randomUUID();
        inviterId = UUID.randomUUID();
    }

    // ---------- cas nominal ----------

    @Test
    @DisplayName("Email inconnu : invitation créée, token.hashe, email tenté, audit")
    void createsInvitationForUnknownEmail() {
        givenRole("PASTEUR");
        when(userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "pasteur@eglise.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com"))
                .thenReturn(Optional.empty());
        when(invitationRepository.findByTenantIdAndEmailAndStatus(
                tenantId, "pasteur@eglise.com", InvitationStatus.PENDING)).thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> {
            Invitation saved = i.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        InvitationService.InvitationCreationResult result = service.createInvitation(
                tenantId, inviterId, "Pasteur@Eglise.com", "pasteur",
                MembershipScopeType.TENANT, null, null);

        assertThat(result.kind()).isEqualTo(InvitationService.InvitationCreationKind.INVITATION);
        assertThat(result.email()).isEqualTo("pasteur@eglise.com");
        assertThat(result.role()).isEqualTo("PASTEUR");
        assertThat(result.invitationToken()).hasSize(32);
        assertThat(result.invitationLink()).startsWith(FRONTEND_URL + "/accept-invitation?token=");
        assertThat(result.requiresTenantSwitch()).isFalse();
        assertThat(result.invitationId()).isNotNull();

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        // Le token n'est JAMAIS stocké en clair, seulement son hash.
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(result.invitationToken());
        assertThat(captor.getValue().getInviterId()).isEqualTo(inviterId);
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(auditService).logSimple("INVITATION_CREATED", "INVITATION", result.invitationId());
    }

    @Test
    @DisplayName("SMTP cassé : l'invitation est créée et emailSent=false, aucune exception (D10)")
    void emailFailureNeverBreaksTheBusinessFlow() {
        givenRole("PASTEUR");
        when(userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "pasteur@eglise.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com"))
                .thenReturn(Optional.empty());
        when(invitationRepository.findByTenantIdAndEmailAndStatus(
                any(), anyString(), any())).thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> {
            Invitation saved = i.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP indisponible"))
                .when(emailService).send(anyString(), anyString(), anyString());

        InvitationService.InvitationCreationResult result = service.createInvitation(
                tenantId, inviterId, "pasteur@eglise.com", "PASTEUR",
                MembershipScopeType.TENANT, null, null);

        assertThat(result.emailSent()).isFalse();
        assertThat(result.invitationId()).isNotNull();
    }

    // ---------- compte existant ----------

    @Test
    @DisplayName("Compte existant du même tenant : membership directe, aucune invitation")
    void addsMembershipDirectlyForExistingSameTenantUser() {
        Role role = givenRole("PASTEUR");
        User existing = existingUser(tenantId);
        when(userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "pasteur@eglise.com"))
                .thenReturn(Optional.of(existing));
        when(membershipRepository.existsByUserIdAndTenantIdAndStatus(
                existing.getId(), tenantId, MembershipStatus.ACTIVE)).thenReturn(false);

        InvitationService.InvitationCreationResult result = service.createInvitation(
                tenantId, inviterId, "pasteur@eglise.com", "PASTEUR",
                MembershipScopeType.TENANT, null, null);

        assertThat(result.kind()).isEqualTo(InvitationService.InvitationCreationKind.DIRECT_MEMBERSHIP);
        assertThat(result.invitedUserId()).isEqualTo(existing.getId());
        assertThat(result.crossTenantIdentity()).isFalse();
        assertThat(result.requiresTenantSwitch()).isFalse();
        assertThat(result.invitationId()).isNull();

        ArgumentCaptor<TenantMembership> captor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getRole().getId()).isEqualTo(role.getId());
        assertThat(captor.getValue().getInvitedBy()).isEqualTo(inviterId);
        verify(invitationRepository, never()).save(any(Invitation.class));
        verify(auditService).logSimple("USER_INVITED_EXISTING", "USER", existing.getId());
    }

    @Test
    @DisplayName("Compte déjà membre : refus 400 sans rien écrire")
    void refusesWhenUserIsAlreadyMember() {
        givenRole("PASTEUR");
        User existing = existingUser(tenantId);
        when(userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "pasteur@eglise.com"))
                .thenReturn(Optional.of(existing));
        when(membershipRepository.existsByUserIdAndTenantIdAndStatus(
                existing.getId(), tenantId, MembershipStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "pasteur@eglise.com", "PASTEUR",
                MembershipScopeType.TENANT, null, null))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(problem.getTitle()).isEqualTo("INVITATION_ALREADY_MEMBER");
                });

        verify(membershipRepository, never()).save(any());
        verify(invitationRepository, never()).save(any());
    }

    // ---------- cross-tenant (décision D3) ----------

    @Test
    @DisplayName("Compte d'une AUTRE église : invitation classique + requiresTenantSwitch, aucune membership")
    void createsClassicInvitationForForeignTenantIdentity() {
        givenRole("PASTEUR");
        User foreign = existingUser(UUID.randomUUID());
        when(userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "pasteur@eglise.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com"))
                .thenReturn(Optional.of(foreign));
        when(invitationRepository.findByTenantIdAndEmailAndStatus(
                any(), anyString(), any())).thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> {
            Invitation saved = i.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        InvitationService.InvitationCreationResult result = service.createInvitation(
                tenantId, inviterId, "pasteur@eglise.com", "PASTEUR",
                MembershipScopeType.TENANT, null, null);

        assertThat(result.kind()).isEqualTo(InvitationService.InvitationCreationKind.CROSS_TENANT_INVITATION);
        assertThat(result.requiresTenantSwitch()).isTrue();
        assertThat(result.invitedUserId()).isNull();
        // Jamais de membership cross-tenant ajoutée par cet écran.
        verify(membershipRepository, never()).save(any(TenantMembership.class));
        verify(auditService).logSimple("INVITATION_CREATED_CROSS_TENANT", "INVITATION", result.invitationId());
    }

    // ---------- refus ----------

    @Test
    @DisplayName("Rôle inconnu : 400 INVITATION_ROLE_INVALID")
    void refusesUnknownRole() {
        when(roleRepository.findByTenantIdAndKey(tenantId, "INCONNU")).thenReturn(Optional.empty());
        when(roleRepository.findByTenantIdIsNullAndKey("INCONNU")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "a@b.com", "INCONNU", MembershipScopeType.TENANT, null, null))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("INVITATION_ROLE_INVALID"));
    }

    @Test
    @DisplayName("Rôle plateforme : refus (INVITATION_PLATFORM_ROLE_FORBIDDEN)")
    void refusesPlatformRole() {
        Role platformRole = Role.builder().id(UUID.randomUUID()).tenantId(null).key("PLATFORM_SUPERADMIN").build();
        when(roleRepository.findByTenantIdAndKey(tenantId, "PLATFORM_SUPERADMIN"))
                .thenReturn(Optional.of(platformRole));

        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "a@b.com", "PLATFORM_SUPERADMIN", MembershipScopeType.TENANT, null, null))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("INVITATION_PLATFORM_ROLE_FORBIDDEN"));
    }

    @Test
    @DisplayName("Nœud organisationnel d'un autre tenant : refus")
    void refusesNodeOfAnotherTenant() {
        givenRole("PASTEUR");
        UUID foreignNode = UUID.randomUUID();
        OrganizationNode node = OrganizationNode.builder()
                .id(foreignNode).tenantId(UUID.randomUUID()).name("Nœud").code("NOEUD").build();
        when(organizationNodeRepository.findById(foreignNode)).thenReturn(Optional.of(node));

        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "a@b.com", "PASTEUR",
                MembershipScopeType.CHURCH, foreignNode, foreignNode))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("INVITATION_NODE_INVALID"));
    }

    @Test
    @DisplayName("Scope tenant avec ressource : refus (INVITATION_SCOPE_INVALID)")
    void refusesTenantScopeWithResource() {
        givenRole("PASTEUR");

        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "a@b.com", "PASTEUR",
                MembershipScopeType.TENANT, UUID.randomUUID(), null))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("INVITATION_SCOPE_INVALID"));
    }

    @Test
    @DisplayName("Email ou rôle vide : refus")
    void refusesBlankInput() {
        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "  ", "PASTEUR", MembershipScopeType.TENANT, null, null))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "a@b.com", null, MembershipScopeType.TENANT, null, null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Invitation en attente déjà présente : refus idempotent")
    void refusesWhenPendingInvitationAlreadyExists() {
        givenRole("PASTEUR");
        Invitation pending = Invitation.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .email("pasteur@eglise.com").status(InvitationStatus.PENDING).build();
        when(userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, "pasteur@eglise.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com"))
                .thenReturn(Optional.empty());
        when(invitationRepository.findByTenantIdAndEmailAndStatus(
                tenantId, "pasteur@eglise.com", InvitationStatus.PENDING)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.createInvitation(
                tenantId, inviterId, "pasteur@eglise.com", "PASTEUR",
                MembershipScopeType.TENANT, null, null))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("INVITATION_ALREADY_PENDING"));
        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    // ---------- helpers ----------

    private Role givenRole(String key) {
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(tenantId).key(key).build();
        when(roleRepository.findByTenantIdAndKey(tenantId, key)).thenReturn(Optional.of(role));
        return role;
    }

    private User existingUser(UUID owner) {
        return User.builder().id(UUID.randomUUID()).tenantId(owner).email("pasteur@eglise.com").build();
    }
}
