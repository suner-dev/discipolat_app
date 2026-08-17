package com.discipolat.modules.authentication.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final int PASSWORD_RESET_VALIDITY_MINUTES = 30;
    private static final int ACTIVATION_VALIDITY_HOURS = 48;

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final String frontendUrl;
    private final Set<String> blacklistedRefreshTokens = ConcurrentHashMap.newKeySet();

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder, SecurityUtils securityUtils,
                       ActivationTokenRepository activationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public record AuthResult(String accessToken, String refreshToken, User user, String activeRole) {
        public AuthResult(String accessToken, String refreshToken, User user) {
            this(accessToken, refreshToken, user, user.getActiveRole() != null ? user.getActiveRole().name() : user.getRole().name());
        }
    }

    // ======================== LOGIN ========================

    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // US-01: Account lockout after 5 failed attempts
        if (user.isAccountLocked()) {
            throw new BadCredentialsException("Account is temporarily locked. Please try again later.");
        }

        // US-02: Check if account is activated
        if (user.getStatut() == UserStatus.PENDING_ACTIVATION) {
            throw new BadCredentialsException("Account not activated. Please check your email for the activation link.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatut() == UserStatus.INACTIVE) {
            throw new BadCredentialsException("Account is inactive");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        // Initialize roles from existing role + estChefDeFamille flag
        Set<String> roleNames = new HashSet<>();
        roleNames.add(user.getRole().name());
        if (user.isEstChefDeFamille() && !roleNames.contains(UserRole.CHEF_DE_FAMILLE.name())) {
            roleNames.add(UserRole.CHEF_DE_FAMILLE.name());
        }
        // Ensure admin also has PASTEUR-level access
        if (user.getRole() == UserRole.ADMIN) {
            roleNames.add(UserRole.PASTEUR.name());
        }

        // Sync User entity roles set
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Set<UserRole> roles = roleNames.stream()
                    .map(UserRole::valueOf)
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        // Set active role to default (highest priority)
        if (user.getActiveRole() == null) {
            user.setActiveRole(getDefaultActiveRole(user));
        }
        userRepository.save(user);

        String activeRoleStr = user.getActiveRole().name();
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEstChefDeFamille(), user.getTenantId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.getTenantId());

        return new AuthResult(accessToken, refreshToken, user, activeRoleStr);
    }

    // ======================== ACTIVATION (US-02) ========================

    /**
     * Generate activation token and send welcome email
     */
    public void sendActivationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        String token = UUID.randomUUID().toString();
        ActivationToken activationToken = ActivationToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(Instant.now().plus(ACTIVATION_VALIDITY_HOURS, ChronoUnit.HOURS))
                .used(false)
                .build();
        activationTokenRepository.save(activationToken);

        String activationLink = frontendUrl + "/activate?token=" + token;
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), activationLink);
    }

    /**
     * Activate account using token
     */
    public void activateAccount(String token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired activation token"));

        if (activationToken.isUsed()) {
            throw new BadCredentialsException("Activation token has already been used");
        }

        if (activationToken.isExpired()) {
            throw new BadCredentialsException("Activation token has expired. Please contact an administrator.");
        }

        User user = userRepository.findById(activationToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", activationToken.getUserId()));

        user.setStatut(UserStatus.ACTIVE);
        userRepository.save(user);

        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);
    }

    /**
     * Resend activation email
     */
    public void resendActivationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("If the email exists, a new activation link has been sent."));

        if (user.getStatut() != UserStatus.PENDING_ACTIVATION) {
            throw new BadCredentialsException("Account is already activated");
        }

        // Invalidate old tokens
        activationTokenRepository.findByUserIdAndUsedFalse(user.getId())
                .ifPresent(oldToken -> {
                    oldToken.setUsed(true);
                    activationTokenRepository.save(oldToken);
                });

        sendActivationEmail(user.getId());
    }

    // ======================== PASSWORD RESET (US-03) ========================

    /**
     * Generate password reset token (valid 30 min)
     */
    public String generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "If the email exists, a reset link has been sent.";
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(Instant.now().plus(PASSWORD_RESET_VALIDITY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        return "If the email exists, a reset link has been sent.";
    }

    /**
     * Reset password using the token (US-03)
     */
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new BadCredentialsException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new BadCredentialsException("Reset token has expired. Please request a new password reset.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // ======================== TOKEN MANAGEMENT ========================

    public AuthResult refreshToken(String refreshToken) {
        if (blacklistedRefreshTokens.contains(refreshToken)) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        blacklistedRefreshTokens.add(refreshToken);

        String activeRoleStr = user.getActiveRole() != null ? user.getActiveRole().name() : user.getRole().name();
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEstChefDeFamille(), user.getTenantId());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.getTenantId());

        return new AuthResult(newAccessToken, newRefreshToken, user, activeRoleStr);
    }

    public void logout(String refreshToken) {
        try {
            if (jwtTokenProvider.validateToken(refreshToken)) {
                blacklistedRefreshTokens.add(refreshToken);
            }
        } catch (Exception ignored) {
        }
    }

    public AuthResult getCurrentUser() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
        return new AuthResult(null, null, user);
    }

    /**
     * Change password for authenticated user
     */
    public void changePassword(String currentPassword, String newPassword) {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ======================== ROLE SWITCHING ========================

    /**
     * Switch the active role for the current user.
     * Returns a new access token with the updated active role.
     */
    public AuthResult switchActiveRole(UUID userId, UserRole newActiveRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!user.getRoles().contains(newActiveRole)) {
            throw new BusinessRuleException("User does not have the role: " + newActiveRole,
                    "INVALID_ROLE");
        }

        user.setActiveRole(newActiveRole);
        userRepository.save(user);

        String activeRoleStr = newActiveRole.name();
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEstChefDeFamille(), user.getTenantId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.getTenantId());

        return new AuthResult(accessToken, refreshToken, user, activeRoleStr);
    }

    /**
     * Returns the default active role based on priority.
     * Priority: ADMIN > PASTEUR > RESPONSABLE > CHEF_DE_FAMILLE > FAISEUR > MEMBRE
     */
    /**
     * Returns the current authenticated user's ID
     */
    public UUID getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }

    private UserRole getDefaultActiveRole(User user) {
        List<UserRole> priority = List.of(
                UserRole.ADMIN,
                UserRole.PASTEUR,
                UserRole.RESPONSABLE,
                UserRole.CHEF_DE_FAMILLE,
                UserRole.FAISEUR,
                UserRole.MEMBRE
        );
        for (UserRole role : priority) {
            if (user.getRoles() != null && user.getRoles().contains(role)) {
                return role;
            }
        }
        return user.getRole();
    }
}
