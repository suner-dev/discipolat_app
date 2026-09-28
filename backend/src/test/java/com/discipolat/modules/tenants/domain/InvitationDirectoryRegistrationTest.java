package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.people.service.PeopleService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Constat M4 — l'acceptation d'une invitation doit inscrire la personne au
 * <b>répertoire</b> de l'église, exactement une fois, sans jamais créer de
 * doublon.
 */
@ExtendWith(MockitoExtension.class)
class InvitationDirectoryRegistrationTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private TenantRepository tenantRepository;
    @Mock private EmailService emailService;
    @Mock private PeopleService peopleService;
    @Mock private PersonRepository personRepository;

    private InvitationService service;
    private UUID tenantId;
    private static final String TOKEN = "directory-token";

    @BeforeEach
    void setUp() {
        service = new InvitationService(invitationRepository, userRepository, membershipRepository,
                roleRepository, organizationNodeRepository, passwordEncoder, auditService,
                tenantRepository, emailService, peopleService, personRepository, "https://app.example.com");
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Une personne est créée au répertoire avec la source INVITATION")
    void registersPersonInDirectory() {
        givenPendingInvitation();
        givenNewUser();

        service.accept(TOKEN, "password123", "Jean", "Dupont");

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(peopleService).registerPerson(eq(tenantId), captor.capture(),
                eq("INVITATION"), any(UUID.class));
        Person person = captor.getValue();
        assertThat(person.getFirstName()).isEqualTo("Jean");
        assertThat(person.getLastName()).isEqualTo("Dupont");
        assertThat(person.getEmailNormalized()).isEqualTo("jean@eglise.com");
    }

    @Test
    @DisplayName("Aucune écriture si la personne existe déjà (pas de doublon)")
    void doesNotDuplicateAnExistingPerson() {
        givenPendingInvitation();
        givenNewUser();
        when(personRepository.findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(
                tenantId, "jean@eglise.com"))
                .thenReturn(Optional.of(Person.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .firstName("Jean").lastName("Dupont").build()));

        service.accept(TOKEN, "password123", "Jean", "Dupont");

        verifyNoInteractions(peopleService);
    }

    @Test
    @DisplayName("Le prénom est déduit de l'email quand l'invitation n'en fournit pas")
    void derivesFirstNameFromEmailWhenAbsent() {
        givenPendingInvitation("jean.bernard@eglise.com");
        givenNewUser("jean.bernard@eglise.com");
        when(personRepository.findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(
                tenantId, "jean.bernard@eglise.com"))
                .thenReturn(Optional.empty());

        service.accept(TOKEN, "password123", null, null);

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(peopleService).registerPerson(eq(tenantId), captor.capture(), eq("INVITATION"), any(UUID.class));
        assertThat(captor.getValue().getFirstName()).isEqualTo("jean.bernard");
    }

    @Test
    @DisplayName("Un échec du répertoire n'annule PAS l'acceptation")
    void directoryFailureNeverBreaksAcceptance() {
        givenPendingInvitation();
        givenNewUser();
        when(personRepository.findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(any(), anyString()))
                .thenReturn(Optional.empty());
        doThrow(new IllegalStateException("répertoire indisponible"))
                .when(peopleService).registerPerson(any(), any(Person.class), anyString(), any());

        InvitationService.AcceptanceResult result = service.accept(TOKEN, "password123", "Jean", "Dupont");

        // Le compte et la membership restent valides.
        assertThat(result.userId()).isNotNull();
        assertThat(result.tenantId()).isEqualTo(tenantId);
        verify(membershipRepository).save(any(TenantMembership.class));
    }

    @Test
    @DisplayName("Un compte cross-tenant est aussi inscrit au répertoire de l'église qui l'invite")
    void registersCrossTenantIdentityInInvitingDirectory() {
        givenPendingInvitation();
        // Compte existant dans une AUTRE église : aucun doublon `users`.
        User foreign = User.builder().id(UUID.randomUUID()).tenantId(UUID.randomUUID())
                .email("jean@eglise.com").firstName("Jean").lastName("Dupont").build();
        when(userRepository.findGlobalByEmailIgnoreCase("jean@eglise.com")).thenReturn(Optional.of(foreign));
        when(membershipRepository.existsExactActiveMembership(any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(personRepository.findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(
                tenantId, "jean@eglise.com")).thenReturn(Optional.empty());

        service.accept(TOKEN, null, null, null);

        verify(peopleService).registerPerson(eq(tenantId), any(Person.class), eq("INVITATION"), any(UUID.class));
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- helpers ----------

    private void givenPendingInvitation() {
        givenPendingInvitation("jean@eglise.com");
    }

    private void givenPendingInvitation(String email) {
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email(email)
                .tokenHash(InvitationTokenHasher.hash(TOKEN))
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(invitationRepository.findByTokenHashForUpdate(InvitationTokenHasher.hash(TOKEN)))
                .thenReturn(Optional.of(invitation));
        Role role = Role.builder().id(UUID.randomUUID()).tenantId(null).key("MEMBER").build();
        when(roleRepository.findGlobalByKey("MEMBER")).thenReturn(Optional.of(role));
    }

    private void givenNewUser() {
        givenNewUser("jean@eglise.com");
    }

    private void givenNewUser(String email) {
        when(userRepository.findGlobalByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(membershipRepository.existsExactActiveMembership(any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
    }
}
