package com.discipolat.modules.authentication.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
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
    private final AuthorizationService authorizationService;
    private final AuthResponseFactory authResponseFactory;
    private final com.discipolat.modules.platform.domain.TenantRegistrationService tenantRegistrationService;

    public AuthController(
            AuthService authService,
            PerIpRateLimiter rateLimiter,
            AuthorizationService authorizationService,
            AuthResponseFactory authResponseFactory,
            com.discipolat.modules.platform.domain.TenantRegistrationService tenantRegistrationService
    ) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.authorizationService = authorizationService;
        this.authResponseFactory = authResponseFactory;
        this.tenantRegistrationService = tenantRegistrationService;
    }

    /**
     * Suivi d'une demande d'inscription (constat M2, contrat §3.3).
     *
     * <p>Endpoint PUBLIC : aucune authentification requise, donc trois
     * protections obligatoires —
     * <ol>
     *   <li><b>rate-limit par IP</b> (3 requêtes / 5 min), sinon la page devient
     *       un oracle permettant d'énumérer les adresses inscrites ;</li>
     *   <li><b>{@code Cache-Control: no-store}</b>, sinon un cache partagé
     *       (proxy, CDN) pourrait servir la réponse d'un utilisateur à un autre ;</li>
     *   <li><b>aucune fuite</b> : ni mot de passe, ni nom d'organisation, ni
     *       compte — uniquement le statut de la demande.</li>
     * </ol>
     */
    @PostMapping("/registration-status")
    public ResponseEntity<RegistrationStatusResponse> registrationStatus(
            @RequestBody RegistrationStatusRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeRegistrationStatus(clientIp);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .cacheControl(CacheControl.noStore())
                    .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                    .header(HEADER_RATE_LIMIT_REMAINING, "0")
                    .body(new RegistrationStatusResponse("NONE", null, null, false));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(tenantRegistrationService.registrationStatus(request.email()));
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

    /**
     * Demande publique d’une nouvelle organisation. Aucun tenant, utilisateur
     * ou rôle n’est créé avant l’approbation d’un Super Admin.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeRegister(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        // Récupérer l'invite code depuis les headers (optionnel)
        String inviteCode = httpRequest.getHeader("X-Invite-Code");

        // Consentements RGPD obligatoires (validés par @AssertTrue sur le DTO) :
        // la preuve (version + IP + user-agent) est conservée par le service.
        com.discipolat.modules.platform.domain.TenantRegistrationService.ConsentInfo consent =
                new com.discipolat.modules.platform.domain.TenantRegistrationService.ConsentInfo(
                        Boolean.TRUE.equals(request.consentCgu()),
                        Boolean.TRUE.equals(request.consentPrivacy()),
                        Boolean.TRUE.equals(request.consentArt9()),
                        request.legalVersion(),
                        clientIp,
                        httpRequest.getHeader("User-Agent"));

        // PORT Develop1 (§G3.1) — « s'inscrire AU NOM D'UNE église » : le lien web
        // /register?tenant=<slug> et le mobile (tenantId) créent directement un
        // compte MEMBRE rattaché au tenant demandé, avec email d'activation.
        // Sans rattachement, on reste sur le flux de main : demande d'organisation
        // approuvée par un Super Admin (aucune église créée automatiquement).
        if (request.tenantSlug() != null && !request.tenantSlug().isBlank()
                || request.tenantId() != null) {
            authService.registerInChurch(request.email(), request.password(), request.firstName(),
                    request.lastName(), request.phone(), request.tenantSlug(), request.tenantId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Account created. Check your email to activate it.",
                    "role", "MEMBRE"
            ));
        }

        authService.register(request.email(), request.password(), request.firstName(), request.lastName(),
                request.phone(), inviteCode, request.plan(), consent);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Demande d'église reçue. Elle sera examinée par un Super Admin.",
                "status", "PENDING_APPROVAL"
        ));
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
    public ResponseEntity<?> activateAccount(@Valid @RequestBody TokenRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeActivate(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.activateAccount(request.token());
        return ResponseEntity.ok(Map.of("message", "Account has been activated successfully"));
    }

    /** US-02: Resend activation email */
    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@Valid @RequestBody EmailRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeActivate(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.resendActivationEmail(request.email());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a new activation link has been sent."));
    }

    /** US-03: Request password reset */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody EmailRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeForgotPassword(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        String message = authService.generatePasswordResetToken(request.email());
        return ResponseEntity.ok(Map.of("message", message));
    }

    /** US-03: Reset password with token */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeResetPassword(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    /** Change password for authenticated user */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeChangePassword(clientIp);
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        authService.changePassword(request.currentPassword(), request.newPassword());
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
        // Construction factorisée dans AuthResponseFactory : la réponse est
        // rigoureusement identique à celle des connexions Google / Microsoft.
        return authResponseFactory.from(result);
    }
}
