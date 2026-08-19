package com.discipolat.modules.authentication.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.modules.authentication.domain.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final AuthService authService;
    private final PerIpRateLimiter rateLimiter;

    public AuthController(
            AuthService authService,
            PerIpRateLimiter rateLimiter
    ) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeLogin(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        AuthService.AuthResult result = authService.login(request.email(), request.password());
        AuthResponse response = toAuthResponse(result);
        return ResponseEntity.ok()
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeRefresh(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        AuthService.AuthResult result = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok()
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(toAuthResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        AuthService.AuthResult result = authService.getCurrentUser();
        return ResponseEntity.ok(toAuthResponse(result));
    }

    /**
     * Multi-role: Switch the active role for the current user.
     */
    @PostMapping("/switch-role")
    public ResponseEntity<?> switchRole(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeSwitchRole(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        String roleStr = body.get("role");
        if (roleStr == null || roleStr.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr);
        }

        UUID userId = authService.getCurrentUserId();
        AuthService.AuthResult result = authService.switchActiveRole(userId, newRole);
        return ResponseEntity.ok()
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(toAuthResponse(result));
    }

    /** US-02: Activate account with token */
    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeActivate(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.activateAccount(body.get("token"));
        return ResponseEntity.ok(Map.of("message", "Account has been activated successfully"));
    }

    /** US-02: Resend activation email */
    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeActivate(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.resendActivationEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "If the email exists, a new activation link has been sent."));
    }

    /** US-03: Request password reset */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeForgotPassword(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        String message = authService.generatePasswordResetToken(body.get("email"));
        return ResponseEntity.ok(Map.of("message", message));
    }

    /** US-03: Reset password with token */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeResetPassword(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    /** Change password for authenticated user */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeChangePassword(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.changePassword(body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been changed successfully"));
    }

    // ======================== HELPERS ========================

    /**
     * Build a 429 Too Many Requests response with rate limit headers.
     */
    private static ResponseEntity<Map<String, String>> rateLimitedResponse(RateLimitResult rl) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HEADER_RATE_LIMIT_REMAINING, "0")
                .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                .body(Map.of(
                        "error", "Too many requests. Please try again later.",
                        "retryAfter", rl.retryAfterSeconds() + " seconds"
                ));
    }

    private AuthResponse toAuthResponse(AuthService.AuthResult result) {
        List<String> roles = result.user().getRoles() != null
                ? result.user().getRoles().stream().map(Enum::name).collect(Collectors.toList())
                : List.of(result.user().getRole().name());
        String activeRole = result.activeRole() != null
                ? result.activeRole()
                : result.user().getRole().name();

        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                "Bearer",
                result.user().getId(),
                result.user().getEmail(),
                result.user().getRole().name(),
                roles,
                activeRole,
                result.user().isEstChefDeFamille(),
                result.user().getFirstName(),
                result.user().getLastName(),
                result.user().isTwoFactorEnabled()
        );
    }
}
