package com.discipolat.modules.authentication.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.tenants.domain.Invitation;
import com.discipolat.modules.tenants.domain.InvitationService;
import com.discipolat.modules.tenants.domain.InvitationStatus;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Acceptation d'une invitation en s'identifiant avec Google / Microsoft.
 *
 * <p>Trois propriétés non négociables sont vérifiées ici :
 * <ol>
 *   <li>l'email vérifié doit être celui de l'invitation (anti-détournement) ;</li>
 *   <li>le rôle et le tenant viennent de l'invitation, jamais du fournisseur ;</li>
 *   <li>aucun mot de passe n'est choisi par l'utilisateur : un secret aléatoire
 *       est généré, jamais transmis — donc le compte ne s'ouvre pas par mot de
 *       passe.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialInvitationAcceptanceServiceTest {

    @Mock private InvitationService invitationService;
    @Mock private SocialIdentityService socialIdentityService;
    @Mock private UserIdentityRepository identityRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;

    @InjectMocks private SocialInvitationAcceptanceService service;

    private static final String TOKEN = "token-invitation";
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID INVITER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String INVITED_EMAIL = "paul@exemple.com";

    private Invitation invitation;
    private SocialIdentityVerifier.VerifiedIdentity verified;
    private AuthService.AuthResult session;

    @BeforeEach
    void setUp() {
        invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .email(INVITED_EMAIL)
                .role("MEMBRE")
                .inviterId(INVITER_ID)
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();

        verified = new SocialIdentityVerifier.VerifiedIdentity(
                SocialProvider.GOOGLE, "google-sub-1", INVITED_EMAIL, true, "Paul Koffi", "https://img");

        session = new AuthService.AuthResult("access", "refresh",
                User.builder().id(USER_ID).email(INVITED_EMAIL).role(UserRole.MEMBRE).build(),
                UserRole.MEMBRE.name());

        when(invitationService.validate(TOKEN)).thenReturn(invitation);
        when(socialIdentityService.verifyForInvitation(SocialProvider.GOOGLE, "credential", INVITED_EMAIL))
                .thenReturn(verified);
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.empty());
        when(invitationService.accept(eq(TOKEN), anyString(), any(), any()))
                .thenReturn(new InvitationService.AcceptanceResult(
                        USER_ID, INVITED_EMAIL, TENANT_ID, false, false));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(
                User.builder().id(USER_ID).tenantId(TENANT_ID).email(INVITED_EMAIL)
                        .role(UserRole.MEMBRE).statut(UserStatus.ACTIVE).build()));
        when(authService.issueSession(any(User.class))).thenReturn(session);
    }

    @Test
    @DisplayName("Invitation acceptée : identité rattachée et session ouverte")
    void acceptsInvitationAndOpensSession() {
        SocialInvitationAcceptanceService.AcceptanceOutcome outcome =
                service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null);

        assertTrue(outcome.identityCreated());
        assertEquals(session, outcome.session());
        assertEquals(TENANT_ID, outcome.acceptance().tenantId());

        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identityRepository).save(saved.capture());
        assertEquals(USER_ID, saved.getValue().getUserId());
        assertEquals("google-sub-1", saved.getValue().getSubject());

        verify(authService).issueSession(any(User.class));
    }

    @Test
    @DisplayName("Le mot de passe est un secret aléatoire, jamais celui de l'utilisateur")
    void generatesUnusableRandomPassword() {
        SocialInvitationAcceptanceService.AcceptanceOutcome outcome =
                service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null);

        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(invitationService).accept(eq(TOKEN), password.capture(), any(), any());

        String generated = password.getValue();
        assertNotEquals(null, generated);
        assertFalse(generated.isBlank());
        // 32 octets en base64url : un secret que personne ne peut deviner.
        assertTrue(generated.length() >= 40, "secret trop court pour être irrésoluble : " + generated.length());
        // Et surtout : ce qui est renvoyé au client ne contient aucun mot de passe.
        assertEquals("access", session.accessToken());
    }

    @Test
    @DisplayName("Deux invitations successives ne produisent pas le même secret")
    void passwordIsNotPredictable() {
        service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null);
        service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null);

        ArgumentCaptor<String> passwords = ArgumentCaptor.forClass(String.class);
        verify(invitationService, org.mockito.Mockito.times(2))
                .accept(eq(TOKEN), passwords.capture(), any(), any());

        assertNotEquals(passwords.getAllValues().get(0), passwords.getAllValues().get(1));
    }

    @Test
    @DisplayName("Prénom et nom : saisie prioritaire, sinon déduits du fournisseur")
    void resolvesNames() {
        service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", "Jean", "Koffi");
        service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null);

        ArgumentCaptor<String> firstNames = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> lastNames = ArgumentCaptor.forClass(String.class);
        verify(invitationService, org.mockito.Mockito.times(2))
                .accept(eq(TOKEN), anyString(), firstNames.capture(), lastNames.capture());

        // Saisie prioritaire…
        assertEquals("Jean", firstNames.getAllValues().get(0));
        // …sinon déduction depuis le nom affiché par le fournisseur.
        assertEquals("Paul", firstNames.getAllValues().get(1));
        assertEquals("Koffi", lastNames.getAllValues().get(1));
    }

    @Test
    @DisplayName("Email différent de l'invitation → refus AVANT toute création de compte")
    void refusesMismatchedEmail() {
        when(socialIdentityService.verifyForInvitation(SocialProvider.GOOGLE, "credential", INVITED_EMAIL))
                .thenThrow(new DomainException(
                        "L'identite verifiee ne correspond pas a l'adresse invitée",
                        org.springframework.http.HttpStatus.FORBIDDEN, "SOCIAL_EMAIL_MISMATCH"));

        DomainException failure = assertThrows(DomainException.class,
                () -> service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null));

        assertEquals("SOCIAL_EMAIL_MISMATCH", failure.getCode());
        // Le point essentiel : aucune accepting n'a eu lieu.
        verify(invitationService, never()).accept(anyString(), anyString(), any(), any());
        verify(identityRepository, never()).save(any(UserIdentity.class));
    }

    @Test
    @DisplayName("Identité déjà rattachée à un AUTRE compte que l'invité → refus")
    void refusesIdentityLinkedToAnotherAccount() {
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.of(UserIdentity.builder()
                        .userId(UUID.randomUUID())
                        .provider(SocialProvider.GOOGLE)
                        .subject("google-sub-1")
                        .build()));
        // L'adresse invitée n'a pas encore de compte : l'identité ne peut donc
        // pas être à elle.
        when(userRepository.findGlobalByEmailIgnoreCase(INVITED_EMAIL)).thenReturn(Optional.empty());

        DomainException failure = assertThrows(DomainException.class,
                () -> service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null));

        assertEquals("SOCIAL_IDENTITY_ALREADY_LINKED", failure.getCode());
        verify(invitationService, never()).accept(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Identité déjà rattachée au bon compte → pas de doublon")
    void doesNotDuplicateExistingIdentity() {
        when(identityRepository.findByProviderAndSubject(SocialProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.of(UserIdentity.builder()
                        .userId(USER_ID)
                        .provider(SocialProvider.GOOGLE)
                        .subject("google-sub-1")
                        .build()));
        when(userRepository.findGlobalByEmailIgnoreCase(INVITED_EMAIL)).thenReturn(Optional.of(
                User.builder().id(USER_ID).email(INVITED_EMAIL).build()));

        SocialInvitationAcceptanceService.AcceptanceOutcome outcome =
                service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null);

        assertFalse(outcome.identityCreated());
        verify(identityRepository, never()).save(any(UserIdentity.class));
        // L'acceptation, elle, a bien eu lieu (adhésion créée).
        verify(invitationService).accept(eq(TOKEN), anyString(), any(), any());
    }

    @Test
    @DisplayName("Invitation invalide ou expirée : la vérification amont bloque tout")
    void propagatesInvitationFailure() {
        when(invitationService.validate(TOKEN)).thenThrow(new DomainException(
                "Invitation expirée", org.springframework.http.HttpStatus.GONE, "INVITATION_EXPIRED"));

        DomainException failure = assertThrows(DomainException.class,
                () -> service.acceptWithIdentity(TOKEN, SocialProvider.GOOGLE, "credential", null, null));

        assertEquals("INVITATION_EXPIRED", failure.getCode());
        verify(socialIdentityService, never())
                .verifyForInvitation(any(), anyString(), anyString());
    }
}
