package com.discipolat.modules.authentication.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserIdentity;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.repository.UserIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Règles de décision de la connexion sociale.
 *
 * <p>Invariant central vérifié ici : <b>un credential externe n'authentifie que,
 * ne crée jamais de compte, et n'accorde jamais de rôle ni d'église.</b>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialIdentityServiceTest {

    @Mock private SocialIdentityVerifier verifier;
    @Mock private UserRepository userRepository;
    @Mock private UserIdentityRepository identityRepository;
    @Mock private AuthService authService;

    @InjectMocks private SocialIdentityService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private SocialIdentityVerifier.VerifiedIdentity verifiedIdentity;
    private AuthService.AuthResult session;

    @BeforeEach
    void setUp() {
        verifiedIdentity = new SocialIdentityVerifier.VerifiedIdentity(
                SocialProvider.GOOGLE, "google-sub-1", "Paul@exemple.com", true, "Paul Koffi", "https://img");
        session = new AuthService.AuthResult(
                "access", "refresh",
                User.builder().id(USER_ID).email("paul@exemple.com").role(UserRole.MEMBRE).build(),
                UserRole.MEMBRE.name());
        when(verifier.verify(eq(SocialProvider.GOOGLE), anyString())).thenReturn(verifiedIdentity);
        when(authService.issueSession(any(User.class))).thenReturn(session);
    }

    private static User activeUser() {
        return User.builder()
                .id(USER_ID)
                .tenantId(TENANT_ID)
                .email("paul@exemple.com")
                .role(UserRole.MEMBRE)
                .statut(UserStatus.ACTIVE)
                .passwordHash("$2a$10$hash")
                .build();
    }

    // ==================================================================
    // Connexion
    // ==================================================================

    @Test
    @DisplayName("Connexion : une identité déjà liée ouvre une session")
    void loginWithKnownIdentity() {
        User user = activeUser();
        UserIdentity identity = UserIdentity.builder()
                .userId(USER_ID).provider(SocialProvider.GOOGLE).subject("google-sub-1").build();
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.of(identity));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        SocialIdentityService.SocialLoginResult result =
                service.login(SocialProvider.GOOGLE, "credential");

        assertEquals(session, result.session());
        assertEquals(SocialProvider.GOOGLE, result.provider());
        verify(authService).issueSession(user);
        // Horodatage de dernier usage mis à jour (traçabilité).
        verify(identityRepository).save(identity);
        assertNotNull(identity.getLastLoginAt());
    }

    @Test
    @DisplayName("Connexion : email connu sans identité → rattachement automatique")
    void loginLinksIdentityOnFirstUse() {
        User user = activeUser();
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findGlobalByEmailIgnoreCase("Paul@exemple.com"))
                .thenReturn(Optional.of(user));
        when(identityRepository.existsByUserIdAndProvider(USER_ID, SocialProvider.GOOGLE))
                .thenReturn(false);

        service.login(SocialProvider.GOOGLE, "credential");

        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityRepository).save(saved.capture());
        assertEquals(USER_ID, saved.getValue().getUserId());
        assertEquals(SocialProvider.GOOGLE, saved.getValue().getProvider());
        assertEquals("google-sub-1", saved.getValue().getSubject());
        // L'email est mémorisé TEL QU'IL ÉTAIT vérifié, à titre de traçabilité.
        assertEquals("Paul@exemple.com", saved.getValue().getEmailAtLink());
    }

    @Test
    @DisplayName("Connexion : aucun compte pour cette adresse → 403, AUCUN compte créé")
    void loginRefusesUnknownAccount() {
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findGlobalByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        DomainException failure = assertThrows(DomainException.class,
                () -> service.login(SocialProvider.GOOGLE, "credential"));

        assertEquals("SOCIAL_ACCOUNT_NOT_LINKED", failure.getCode());
        // Le point central : aucun utilisateur n'est créé par un credential social.
        verify(userRepository, never()).save(any(User.class));
        verify(identityRepository, never()).save(any(UserIdentity.class));
    }

    @Test
    @DisplayName("Connexion : identité rattachée à un compte supprimé → erreur explicite, pas de recréation")
    void loginRefusesOrphanedIdentity() {
        UserIdentity identity = UserIdentity.builder()
                .userId(UUID.randomUUID()).provider(SocialProvider.GOOGLE).subject("google-sub-1").build();
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.of(identity));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        DomainException failure = assertThrows(DomainException.class,
                () -> service.login(SocialProvider.GOOGLE, "credential"));

        assertEquals("SOCIAL_IDENTITY_ORPHANED", failure.getCode());
    }

    @Test
    @DisplayName("Connexion : compte en attente d'activation, inactif ou verrouillé → refus motivé")
    void loginRefusesUnusableAccountStates() {
        when(identityRepository.findByProviderAndSubject(any(), anyString())).thenReturn(Optional.empty());

        User pending = activeUser();
        pending.setStatut(UserStatus.PENDING_ACTIVATION);
        assertCode("ACCOUNT_NOT_ACTIVATED", pending);

        User inactive = activeUser();
        inactive.setStatut(UserStatus.INACTIVE);
        assertCode("ACCOUNT_INACTIVE", inactive);

        User locked = activeUser();
        locked.setAccountLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
        assertCode("ACCOUNT_LOCKED", locked);
    }

    private void assertCode(String expectedCode, User user) {
        when(userRepository.findGlobalByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(identityRepository.existsByUserIdAndProvider(any(), any())).thenReturn(false);
        DomainException failure = assertThrows(DomainException.class,
                () -> service.login(SocialProvider.GOOGLE, "credential"));
        assertEquals(expectedCode, failure.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Connexion : un credential Google au même email mais avec un subject différent ne s'attache pas")
    void loginRefusesSecondIdentityOfSameProvider() {
        // Empêche qu'un compte Google homonyme (adresse réattribuée par Google)
        // puisse prendre la place d'une identité déjà liée au compte.
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findGlobalByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(activeUser()));
        when(identityRepository.existsByUserIdAndProvider(USER_ID, SocialProvider.GOOGLE))
                .thenReturn(true);

        DomainException failure = assertThrows(DomainException.class,
                () -> service.login(SocialProvider.GOOGLE, "credential"));

        assertEquals("SOCIAL_PROVIDER_ALREADY_LINKED", failure.getCode());
        verify(identityRepository, never()).save(any(UserIdentity.class));
    }

    // ==================================================================
    // Rattachement au compte connecté
    // ==================================================================

    @Test
    @DisplayName("Rattachement : identité cohérente → créée")
    void linkCreatesIdentity() {
        User user = activeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.empty());

        SocialIdentityService.SocialLinkResult result =
                service.linkToCurrentUser(USER_ID, SocialProvider.GOOGLE, "credential", true);

        assertTrue(result.created());
        verify(identityRepository).save(any(UserIdentity.class));
    }

    @Test
    @DisplayName("Rattachement : email vérifié différent du compte connecté → refus")
    void linkRefusesEmailMismatch() {
        // Un credential Google appartenant à quelqu'un d'autre ne peut pas
        // revendiquer un compte Discipolat, même si la session est valide.
        when(verifier.verify(eq(SocialProvider.GOOGLE), anyString())).thenReturn(
                new SocialIdentityVerifier.VerifiedIdentity(
                        SocialProvider.GOOGLE, "google-sub-1", "pirate@exemple.com", true, "Pirate", ""));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(identityRepository.findByProviderAndSubject(any(), anyString())).thenReturn(Optional.empty());

        DomainException failure = assertThrows(DomainException.class,
                () -> service.linkToCurrentUser(USER_ID, SocialProvider.GOOGLE, "credential", true));

        assertEquals("SOCIAL_EMAIL_MISMATCH", failure.getCode());
        verify(identityRepository, never()).save(any(UserIdentity.class));
    }

    @Test
    @DisplayName("Rattachement : identité déjà rattachée à un AUTRE compte → refus (pas de vol de compte)")
    void linkRefusesIdentityOwnedByAnotherAccount() {
        UserIdentity other = UserIdentity.builder()
                .userId(UUID.randomUUID()).provider(SocialProvider.GOOGLE).subject("google-sub-1").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.of(other));

        DomainException failure = assertThrows(DomainException.class,
                () -> service.linkToCurrentUser(USER_ID, SocialProvider.GOOGLE, "credential", true));

        assertEquals("SOCIAL_IDENTITY_ALREADY_LINKED", failure.getCode());
    }

    @Test
    @DisplayName("Rattachement : identité déjà liée au même compte → idempotent")
    void linkIsIdempotentForSameAccount() {
        UserIdentity mine = UserIdentity.builder()
                .userId(USER_ID).provider(SocialProvider.GOOGLE).subject("google-sub-1").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.of(mine));

        SocialIdentityService.SocialLinkResult result =
                service.linkToCurrentUser(USER_ID, SocialProvider.GOOGLE, "credential", true);

        assertFalse(result.created());
        verify(identityRepository, times(1)).save(mine);
    }

    @Test
    @DisplayName("Rattachement : refusé si la fonctionnalité est désactivée")
    void linkRefusedWhenDisabled() {
        DomainException failure = assertThrows(DomainException.class,
                () -> service.linkToCurrentUser(USER_ID, SocialProvider.GOOGLE, "credential", false));

        assertEquals("SOCIAL_LINKING_DISABLED", failure.getCode());
        verify(verifier, never()).verify(any(), anyString());
    }

    // ==================================================================
    // Acceptation d'invitation
    // ==================================================================

    @Test
    @DisplayName("Invitation : l'email vérifié doit être celui de l'invitation")
    void invitationRequiresMatchingEmail() {
        DomainException failure = assertThrows(DomainException.class,
                () -> service.verifyForInvitation(SocialProvider.GOOGLE, "credential", "autre@exemple.com"));

        assertEquals("SOCIAL_EMAIL_MISMATCH", failure.getCode());
    }

    @Test
    @DisplayName("Invitation : correspondance acceptée, la comparaison ignore la casse")
    void invitationAcceptsMatchingEmailIgnoringCase() {
        SocialIdentityVerifier.VerifiedIdentity identity =
                service.verifyForInvitation(SocialProvider.GOOGLE, "credential", "PAUL@EXEMPLE.COM");

        assertEquals("google-sub-1", identity.subject());
    }

    @Test
    @DisplayName("Identités du compte : lecture triée, jamais de données sensibles")
    void identitiesAreListedPerUser() {
        when(identityRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(
                UserIdentity.builder().userId(USER_ID).provider(SocialProvider.GOOGLE)
                        .subject("s1").emailAtLink("paul@exemple.com").build()));

        List<UserIdentity> identities = service.identitiesOf(USER_ID);

        assertEquals(1, identities.size());
        assertEquals(SocialProvider.GOOGLE, identities.get(0).getProvider());
    }
}
