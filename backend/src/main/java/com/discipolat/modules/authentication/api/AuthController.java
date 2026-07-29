package com.discipolat.modules.authentication.api;

import com.discipolat.modules.authentication.domain.AuthService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final Bucket rateLimiter;

    public AuthController(AuthService authService, Bucket rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!rateLimiter.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again later.");
        }

        AuthService.AuthResult result = authService.login(request.email(), request.password());
        AuthResponse response = new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                "Bearer",
                result.user().getId(),
                result.user().getEmail(),
                result.user().getRole().name(),
                result.user().isEstChefDeFamille(),
                result.user().getFirstName(),
                result.user().getLastName()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthService.AuthResult result = authService.refreshToken(request.refreshToken());
        AuthResponse response = new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                "Bearer",
                result.user().getId(),
                result.user().getEmail(),
                result.user().getRole().name(),
                result.user().isEstChefDeFamille(),
                result.user().getFirstName(),
                result.user().getLastName()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        AuthService.AuthResult result = authService.getCurrentUser();
        AuthResponse response = new AuthResponse(
                null,
                null,
                "Bearer",
                result.user().getId(),
                result.user().getEmail(),
                result.user().getRole().name(),
                result.user().isEstChefDeFamille(),
                result.user().getFirstName(),
                result.user().getLastName()
        );
        return ResponseEntity.ok(response);
    }

    /** US-02: Activate account with token */
    @PostMapping("/activate")
    public ResponseEntity<Map<String, String>> activateAccount(@RequestBody Map<String, String> body) {
        authService.activateAccount(body.get("token"));
        return ResponseEntity.ok(Map.of("message", "Account has been activated successfully"));
    }

    /** US-02: Resend activation email */
    @PostMapping("/resend-activation")
    public ResponseEntity<Map<String, String>> resendActivation(@RequestBody Map<String, String> body) {
        authService.resendActivationEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "If the email exists, a new activation link has been sent."));
    }

    /** US-03: Request password reset */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String message = authService.generatePasswordResetToken(body.get("email"));
        return ResponseEntity.ok(Map.of("message", message));
    }

    /** US-03: Reset password with token */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    /** Change password for authenticated user */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body) {
        authService.changePassword(body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been changed successfully"));
    }
}
