package com.discipolat.modules.authentication.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ActivationTokenRepository activationTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(userRepository, jwtTokenProvider, passwordEncoder, securityUtils,
                activationTokenRepository, passwordResetTokenRepository, emailService,
                "http://localhost:5173");

        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@discipolat.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .role(UserRole.FAISEUR)
                .estChefDeFamille(false)
                .statut(UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .twoFactorEnabled(false)
                .build();
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResult() {
        when(userRepository.findByEmail("test@discipolat.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString(), anySet(), anyBoolean(), any()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString(), anyString(), anySet(), any()))
                .thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthService.AuthResult result = authService.login("test@discipolat.com", "password123");

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(userId, result.user().getId());
        assertEquals("Test", result.user().getFirstName());
        // Verify failed login attempts reset on success (called at least once)
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowBadCredentialsException() {
        when(userRepository.findByEmail("test@discipolat.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThrows(BadCredentialsException.class, () ->
                authService.login("test@discipolat.com", "wrongpassword")
        );

        // Verify failed login attempts incremented
        verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 1));
    }

    @Test
    void login_WithInactiveUser_ShouldThrowBadCredentialsException() {
        testUser.setStatut(UserStatus.INACTIVE);
        when(userRepository.findByEmail("test@discipolat.com")).thenReturn(Optional.of(testUser));

        // Password is correct but account is inactive - should throw after password check
        // Since password matches, it won't increment failed attempts, but will throw for inactive
        assertThrows(BadCredentialsException.class, () ->
                authService.login("test@discipolat.com", "password123")
        );
    }

    @Test
    void login_ShouldLockAccountAfter5FailedAttempts() {
        testUser.setFailedLoginAttempts(4);
        when(userRepository.findByEmail("test@discipolat.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThrows(BadCredentialsException.class, () ->
                authService.login("test@discipolat.com", "wrongpassword")
        );

        // Verify account is now locked (5th failed attempt)
        verify(userRepository).save(argThat(u -> u.getFailedLoginAttempts() == 5));
    }

    @Test
    void login_ShouldRejectLockedAccount() {
        testUser.setAccountLockedUntil(java.time.Instant.now().plusSeconds(3600));
        when(userRepository.findByEmail("test@discipolat.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadCredentialsException.class, () ->
                authService.login("test@discipolat.com", "password123")
        );
    }

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewTokens() {
        String refreshToken = "valid-refresh-token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.extractUserId(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString(), anySet(), anyBoolean(), any()))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString(), anyString(), anySet(), any()))
                .thenReturn("new-refresh-token");

        AuthService.AuthResult result = authService.refreshToken(refreshToken);

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldThrowBadCredentialsException() {
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                authService.refreshToken("invalid-token")
        );
    }

    @Test
    void changePassword_WithCorrectCurrentPassword_ShouldSucceed() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() ->
                authService.changePassword("password123", "newPassword456")
        );

        verify(userRepository).save(argThat(u ->
                passwordEncoder.matches("newPassword456", u.getPasswordHash())
        ));
    }

    @Test
    void changePassword_WithWrongCurrentPassword_ShouldThrow() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(BadCredentialsException.class, () ->
                authService.changePassword("wrongPassword", "newPassword456")
        );
    }
}
