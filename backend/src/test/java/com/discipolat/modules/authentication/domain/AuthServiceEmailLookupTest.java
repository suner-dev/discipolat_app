package com.discipolat.modules.authentication.domain;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.platform.domain.TenantRegistrationService;
import com.discipolat.modules.security.domain.RefreshTokenSessionService;
import com.discipolat.modules.security.domain.TokenRevocationService;
import com.discipolat.modules.tenants.domain.TenantStatusGuard;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat B4 — le login doit résoudre l'identité par email de façon
 * INSENSIBLE À LA CASSE, et jamais via un equality sensible à la casse
 * (index unique global {@code uk_users_email_lower}, migration V185).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceEmailLookupTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ActivationTokenRepository activationTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private TenantRegistrationService tenantRegistrationService;
    @Mock
    private TokenRevocationService tokenRevocationService;
    @Mock
    private RefreshTokenSessionService refreshTokenSessionService;
    @Mock
    private TenantStatusGuard tenantStatusGuard;

    @Test
    void loginIsCaseInsensitive() {
        // Le compte est stocke en minuscules ; l'utilisateur saisit une casse différente.
        when(userRepository.findByEmailIgnoreCase("Pasteur@Eglise.COM"))
                .thenReturn(Optional.of(activeUser("pasteur@eglise.com")));

        when(passwordEncoder.matches("MotDePasse1", "hash")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(UUID.class), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anySet(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(any(UUID.class), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anySet(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn("refresh");
        when(jwtTokenProvider.getTokenExpiration(anyString())).thenReturn(java.time.Instant.now());

        AuthService.AuthResult result = service().login("Pasteur@Eglise.COM", "MotDePasse1");

        assertThat(result.user().getEmail()).isEqualTo("pasteur@eglise.com");
        assertThat(result.accessToken()).isEqualTo("access");
        verify(userRepository).findByEmailIgnoreCase("Pasteur@Eglise.COM");
        // Le lookup sensible à la casse ne doit plus jamais être utilisé pour le login.
        verify(userRepository, never()).findFirstByEmail(anyString());
    }

    @Test
    void loginRejectsUnknownEmailWithoutLeakingAccountExistence() {
        when(userRepository.findByEmailIgnoreCase("inconnu@eglise.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().login("inconnu@eglise.com", "peu-importe"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginRejectsWrongPasswordForCaseVariantEmail() {
        when(userRepository.findByEmailIgnoreCase("PASTEUR@EGLISE.COM"))
                .thenReturn(Optional.of(activeUser("pasteur@eglise.com")));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service().login("PASTEUR@EGLISE.COM", "mauvais"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void resendActivationEmailIsCaseInsensitive() {
        UUID userId = UUID.randomUUID();
        User user = activeUser("pasteur@eglise.com");
        user.setId(userId);
        user.setStatut(UserStatus.PENDING_ACTIVATION);
        when(userRepository.findByEmailIgnoreCase("Pasteur@EGLISE.com")).thenReturn(Optional.of(user));
        when(activationTokenRepository.findByUserIdAndUsedFalse(userId)).thenReturn(Optional.empty());
        // sendActivationEmail(...) recharge l'utilisateur par son id
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activationTokenRepository.save(org.mockito.ArgumentMatchers.any(ActivationToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        service().resendActivationEmail("Pasteur@EGLISE.com");

        verify(userRepository, never()).findFirstByEmail(anyString());
        verify(activationTokenRepository).save(org.mockito.ArgumentMatchers.any(ActivationToken.class));
        verify(emailService).sendWelcomeEmail(org.mockito.ArgumentMatchers.eq("pasteur@eglise.com"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void passwordResetTokenLookupIsCaseInsensitive() {
        when(userRepository.findByEmailIgnoreCase("Pasteur@EGLISE.com"))
                .thenReturn(Optional.of(activeUser("pasteur@eglise.com")));
        when(passwordResetTokenRepository.save(org.mockito.ArgumentMatchers.any(PasswordResetToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        service().generatePasswordResetToken("Pasteur@EGLISE.com");

        verify(userRepository, never()).findFirstByEmail(anyString());
        verify(emailService).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq("pasteur@eglise.com"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void passwordResetDoesNotLeakExistenceForUnknownCaseVariantEmail() {
        when(userRepository.findByEmailIgnoreCase("inconnu@eglise.com")).thenReturn(Optional.empty());

        String message = service().generatePasswordResetToken("inconnu@eglise.com");

        assertThat(message).isEqualTo("If the email exists, a reset link has been sent.");
        verify(passwordResetTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void magicLinkResolvesUserCaseInsensitively() {
        User user = activeUser("pasteur@eglise.com");
        when(userRepository.findByEmailIgnoreCase("Pasteur@EGLISE.com"))
                .thenReturn(Optional.of(user));
        AuthService service = service();
        String token = service.generateMagicLink("Pasteur@EGLISE.com");

        User resolved = service.verifyMagicLink(token);

        assertThat(resolved.getId()).isEqualTo(user.getId());
        verify(userRepository, never()).findFirstByEmail(anyString());
    }

    private User activeUser(String email) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .firstName("Jean")
                .lastName("Dupont")
                .role(com.discipolat.common.domain.UserRole.PASTEUR)
                .statut(UserStatus.ACTIVE)
                .build();
        user.setRoles(Set.of(com.discipolat.common.domain.UserRole.PASTEUR));
        return user;
    }

    private AuthService service() {
        return new AuthService(userRepository, jwtTokenProvider, passwordEncoder, securityUtils,
                activationTokenRepository, passwordResetTokenRepository, emailService,
                tenantRegistrationService, tokenRevocationService, refreshTokenSessionService,
                tenantStatusGuard, "https://app.example.com");
    }
}
