package com.discipolat.modules.authentication.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.authentication.domain.AuthService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final AuthService authService;
    private final Bucket loginBucket;
    private final Bucket refreshBucket;
    private final Bucket forgotPasswordBucket;
    private final Bucket resetPasswordBucket;
    private final Bucket activateBucket;
    private final Bucket changePasswordBucket;
    private final Bucket switchRoleBucket;

    public AuthController(
            AuthService authService,
            @Qualifier("loginBucket") Bucket loginBucket,
            @Qualifier("refreshBucket") Bucket refreshBucket,
            @Qualifier("forgotPasswordBucket") Bucket forgotPasswordBucket,
            @Qualifier("resetPasswordBucket") Bucket resetPasswordBucket,
            @Qualifier("activateBucket") Bucket activateBucket,
            @Qualifier("changePasswordBucket") Bucket changePasswordBucket,
            @Qualifier("switchRoleBucket") Bucket switchRoleBucket
    ) {
        this.authService = authService;
        this.loginBucket = loginBucket;
        this.refreshBucket = refreshBucket;
        this.forgotPasswordBucket = forgotPasswordBucket;
        this.resetPasswordBucket = resetPasswordBucket;
        this.activateBucket = activateBucket;
        this.changePasswordBucket = changePasswordBucket;
        this.switchRoleBucket = switchRoleBucket;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!loginBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Too many login attempts. Please try again later."
                    ));
        }

        AuthService.AuthResult result = authService.login(request.email(), request.password());
        AuthResponse response = toAuthResponse(result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        if (!refreshBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many refresh requests. Please try again later."));
        }

        AuthService.AuthResult result = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(toAuthResponse(result));
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
    public ResponseEntity<?> switchRole(@RequestBody Map<String, String> body) {
        if (!switchRoleBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many role switch requests. Please slow down."));
        }

        String roleStr = body.get("role");
        if (roleStr == null || roleStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleStr));
        }

        UUID userId = authService.getCurrentUserId();
        AuthService.AuthResult result = authService.switchActiveRole(userId, newRole);
        return ResponseEntity.ok(toAuthResponse(result));
    }

    /** US-02: Activate account with token */
    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@RequestBody Map<String, String> body) {
        if (!activateBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many activation attempts. Please try again later."));
        }

        authService.activateAccount(body.get("token"));
        return ResponseEntity.ok(Map.of("message", "Account has been activated successfully"));
    }

    /** US-02: Resend activation email */
    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@RequestBody Map<String, String> body) {
        if (!activateBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many activation requests. Please try again later."));
        }

        authService.resendActivationEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "If the email exists, a new activation link has been sent."));
    }

    /** US-03: Request password reset */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        if (!forgotPasswordBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many password reset requests. Please try again later."));
        }

        String message = authService.generatePasswordResetToken(body.get("email"));
        return ResponseEntity.ok(Map.of("message", message));
    }

    /** US-03: Reset password with token */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        if (!resetPasswordBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many password reset attempts. Please try again later."));
        }

        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    /** Change password for authenticated user */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        if (!changePasswordBucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many password change attempts. Please try again later."));
        }

        authService.changePassword(body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been changed successfully"));
    }

    // ======================== HELPERS ========================

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
