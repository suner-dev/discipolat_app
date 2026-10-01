package com.discipolat.modules.authentication.api;

import com.discipolat.common.domain.Payloads;
import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.authentication.config.SocialAuthProperties;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.authentication.domain.SocialIdentityService;
import com.discipolat.modules.authentication.domain.SocialProvider;
import com.discipolat.modules.authentication.domain.SocialIdentityVerifier;
import com.discipolat.modules.users.domain.UserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentification par identité externe (Google, Microsoft) et par lien magique.
 *
 * <p><b>Ce qui est conservé tel quel</b> (aucune suppression) :
 * {@code POST /api/v1/auth/google} et {@code POST /api/v1/auth/magic-link} +
 * {@code GET /api/v1/auth/magic-link/verify} répondent toujours, avec les mêmes
 * URLs et les mêmes formes de réponse, afin qu'aucun client existant ne casse.
 *
 * <p><b>Ce qui est corrigé</b> dans {@code /google} : la création de compte
 * supprimée. Elle construisait un {@code User} sans tenant alors que
 * {@code users.tenant_id} est NOT NULL, et ignorait l'approbation Super Admin.
 * L'endpoint délègue désormais à {@link SocialIdentityService}, qui
 * <b>n'authentifie que des comptes existants</b> et renvoie 403 sinon.
 *
 * <p><b>Nouveaux endpoints</b> :
 * <ul>
 *   <li>{@code GET /social/providers} — public. Le frontend s'en sert pour
 *       n'afficher un bouton que si le serveur l'accepte réellement : pas de
 *       bouton mort, pas d'appel qui se termine en 503.</li>
 *   <li>{@code POST /social/{provider}} — public, rate-limité. Connexion.</li>
 *   <li>{@code POST /social/link} — <b>authentifié</b>. Rattache une identité au
 *       compte connecté (Super Admin, comptes créés par mot de passe).</li>
 *   <li>{@code GET /social/identities} — <b>authentifié</b>. Identités du compte.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SocialAuthController {

    private static final Logger log = LoggerFactory.getLogger(SocialAuthController.class);
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final AuthService authService;
    private final SocialIdentityService socialIdentityService;
    private final SocialAuthProperties properties;
    private final AuthResponseFactory authResponseFactory;
    private final PerIpRateLimiter rateLimiter;

    public SocialAuthController(AuthService authService,
                                SocialIdentityService socialIdentityService,
                                SocialAuthProperties properties,
                                AuthResponseFactory authResponseFactory,
                                PerIpRateLimiter rateLimiter) {
        this.authService = authService;
        this.socialIdentityService = socialIdentityService;
        this.properties = properties;
        this.authResponseFactory = authResponseFactory;
        this.rateLimiter = rateLimiter;
    }

    // ==================================================================
    // Fournisseurs disponibles (public, mis en cache court)
    // ==================================================================

    /**
     * État réel des fournisseurs côté serveur.
     *
     * <p>Réponse sans secret : uniquement le nom du fournisseur et s'il est
     * actif. Le frontend masque un bouton non servi, au lieu d'afficher une
     * erreur au clic.
     */
    @GetMapping("/social/providers")
    public ResponseEntity<Map<String, Object>> providers() {
        List<Map<String, Object>> active = java.util.Arrays.stream(
                        SocialProvider.values())
                .filter(provider -> isActive(provider))
                .map(provider -> Map.<String, Object>of(
                        "provider", provider.wireName(),
                        "label", labelOf(provider)))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providers", active);
        body.put("accountLinkingEnabled", properties.isAllowAccountLinking());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    // ==================================================================
    // Connexion par identité externe
    // ==================================================================

    /**
     * Connexion (et première liaison) par Google ou Microsoft.
     *
     * <p>Refuse de créer un compte : un compte Discipolat existe toujours,
     * soit parce qu'il a été créé par une invitation, soit parce qu'un compte
     * antérieur porte la même adresse vérifiée.
     */
    @PostMapping("/social/{provider}")
    public ResponseEntity<?> socialLogin(@PathVariable String provider,
                                         @RequestBody(required = false) Map<String, String> body,
                                         HttpServletRequest httpRequest) {
        RateLimitResult rl = rateLimiter.tryConsumeSocialLogin(
                PerIpRateLimiter.extractClientIp(httpRequest));
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        SocialProviderRequest request = SocialProviderRequest.from(provider, body);
        SocialIdentityService.SocialLoginResult result =
                socialIdentityService.login(request.provider(), request.credential());

        AuthResponse response = authResponseFactory.from(result.session());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(response);
    }

    /**
     * Rattache une identité externe au compte <b>déjà connecté</b>.
     *
     * <p>Route protégée par Spring Security ({@code authenticated()}), et
     * refusée si l'email vérifié du credential diffère de celui du compte —
     * sans quoi un compte Google tiers pourrait s'approprier un compte
     * Discipolat.
     */
    @PostMapping("/social/link")
    public ResponseEntity<?> linkIdentity(@RequestBody(required = false) Map<String, String> body,
                                          HttpServletRequest httpRequest) {
        RateLimitResult rl = rateLimiter.tryConsumeSocialLink(
                PerIpRateLimiter.extractClientIp(httpRequest));
        if (!rl.allowed()) {
            return rateLimitedResponse(rl);
        }

        SocialProviderRequest request = SocialProviderRequest.from(
                body == null ? null : body.get("provider"), body);
        SocialIdentityService.SocialLinkResult result = socialIdentityService.linkToCurrentUser(
                SecurityUtils.getCurrentUserId(),
                request.provider(),
                request.credential(),
                properties.isAllowAccountLinking());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(Map.of(
                        "provider", result.provider().wireName(),
                        "linked", true,
                        "newlyLinked", result.created(),
                        "message", result.created()
                                ? "Identite rattachee a votre compte"
                                : "Cette identite etait deja rattachee a votre compte"));
    }

    /** Identités externes du compte connecté. */
    @GetMapping("/social/identities")
    public ResponseEntity<Map<String, Object>> identities() {
        List<Map<String, Object>> identities = socialIdentityService
                .identitiesOf(SecurityUtils.getCurrentUserId())
                .stream()
                .map(this::describe)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("identities", identities));
    }

    private Map<String, Object> describe(UserIdentity identity) {
        Map<String, Object> described = new LinkedHashMap<>();
        described.put("provider", identity.getProvider().wireName());
        described.put("label", labelOf(identity.getProvider()));
        described.put("emailAtLink", identity.getEmailAtLink());
        described.put("linkedAt", identity.getCreatedAt());
        described.put("lastUsedAt", identity.getLastLoginAt());
        return described;
    }

    // ==================================================================
    // Compatibilité : endpoint historique /auth/google
    // ==================================================================

    /**
     * Connexion Google historique — conservée pour ne casser aucun client.
     *
     * @deprecated Utiliser {@code POST /api/v1/auth/social/google} (charge utile
     *     {@code AuthResponse}, identique à {@code /auth/login}). Cette route est
     *     conservée avec son ancien format {@code {token, refreshToken, user}}
     *     et délègue désormais au même service : la création de compte
     *     automatique, impossible à cause de {@code users.tenant_id NOT NULL},
     *     a disparu.
     */
    @Deprecated
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody(required = false) Map<String, String> body,
                                                           HttpServletRequest httpRequest) {
        RateLimitResult rl = rateLimiter.tryConsumeSocialLogin(
                PerIpRateLimiter.extractClientIp(httpRequest));
        if (!rl.allowed()) {
            throw new com.discipolat.common.exception.DomainException(
                    "Trop de tentatives, réessayez plus tard", HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMITED");
        }

        SocialProviderRequest request = SocialProviderRequest.from("google", body);
        SocialIdentityService.SocialLoginResult result =
                socialIdentityService.login(request.provider(), request.credential());

        AuthService.AuthResult session = result.session();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(Payloads.of(
                        "token", session.accessToken(),
                        "refreshToken", session.refreshToken(),
                        "user", Payloads.of(
                                "id", session.user().getId().toString(),
                                "email", session.user().getEmail(),
                                "firstName", session.user().getFirstName(),
                                "lastName", session.user().getLastName(),
                                "role", session.user().getRole().name()
                        )
                ));
    }

    // ==================================================================
    // Magic link (inchangé)
    // ==================================================================

    /**
     * Magic Link — envoyer un lien de connexion par email.
     *
     * <p>L'utilisateur reçoit un email avec un lien unique.
     * Clique sur le lien → connecté sans mot de passe.
     */
    @PostMapping("/magic-link")
    public ResponseEntity<Map<String, String>> sendMagicLink(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        try {
            String magicToken = authService.generateMagicLink(email);
            authService.sendMagicLinkEmail(email, magicToken);
            return ResponseEntity.ok(Map.of(
                    "message", "Un lien de connexion a été envoyé à " + email,
                    "email", email
            ));
        } catch (Exception e) {
            log.error("Magic link failed for {}: {}", email, e.getMessage());
            // Ne pas révéler si l'email existe ou non (sécurité)
            return ResponseEntity.ok(Map.of(
                    "message", "Si cet email est enregistré, vous recevrez un lien de connexion.",
                    "email", email
            ));
        }
    }

    /**
     * Valider un Magic Link et connecter l'utilisateur.
     */
    @GetMapping("/magic-link/verify")
    public ResponseEntity<Map<String, Object>> verifyMagicLink(@RequestParam String token) {
        try {
            // verifyMagicLink renvoie le compte ; issueSession() applique la garde de
            // statut du tenant et la synchronisation des roles avant d'emettre la session.
            AuthService.AuthResult session = authService.issueSession(authService.verifyMagicLink(token));
            AuthResponse response = authResponseFactory.from(session);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Payloads.of(
                            "token", response.accessToken(),
                            "refreshToken", response.refreshToken(),
                            "user", Payloads.of(
                                    "id", response.userId().toString(),
                                    "email", response.email(),
                                    "firstName", response.firstName(),
                                    "lastName", response.lastName(),
                                    "role", response.role()
                            )
                    ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired magic link"));
        }
    }

    // ==================================================================
    // Interne
    // ==================================================================

    /**
     * Erreur d'un credential externe, traduite en réponse HTTP explicite.
     *
     * <p>Le client a besoin de distinguer « credential refusé » (il doit
     * recommencer), « fournisseur non configuré » (il doit masquer le bouton)
     * et « aucun compte pour cette adresse » (il doit proposer l'invitation).
     */
    @ExceptionHandler(SocialIdentityVerifier.SocialCredentialException.class)
    public ProblemDetail handleCredentialException(
            SocialIdentityVerifier.SocialCredentialException exception) {
        log.warn("Credential social refuse : {} ({})", exception.code(), exception.getMessage());
        // Même forme que les DomainException traitées par GlobalExceptionHandler
        // (RFC 7807 : code dans `title`) : le client(web et mobile) n'a ainsi
        // qu'un seul contrat d'erreur à lire sur ces endpoints.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.resolve(exception.httpStatus()),
                exception.getMessage());
        problem.setTitle(exception.code());
        problem.setType(URI.create("https://api.discipolat.com/errors/" + exception.code()));
        problem.setProperty("provider", "social");
        return problem;
    }

    private boolean isActive(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> properties.isGoogleActive();
            case MICROSOFT -> properties.isMicrosoftActive();
        };
    }

    private String labelOf(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> "Google";
            case MICROSOFT -> "Microsoft";
        };
    }

    private ResponseEntity<?> rateLimitedResponse(RateLimitResult rl) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .cacheControl(CacheControl.noStore())
                .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                .header(HEADER_RATE_LIMIT_REMAINING, "0")
                .body(Map.of(
                        "error", "Too many requests. Please try again later.",
                        "code", "RATE_LIMITED",
                        "retryAfter", rl.retryAfterSeconds() + " seconds"));
    }

    /**
     * Extraction et validation du fournisseur + du credential.
     *
     * <p>Un fournisseur inconnu est rejeté <b>avant</b> toute vérification de
     * jeton et sans révéler la liste des fournisseurs pris en charge.
     */
    record SocialProviderRequest(
            SocialProvider provider,
            String credential) {

        static SocialProviderRequest from(String providerName, Map<String, String> body) {
            String credential = body == null ? null : body.get("credential");
            SocialProvider provider;
            try {
                provider = SocialProvider.fromWireName(providerName);
            } catch (IllegalArgumentException unsupported) {
                throw SocialIdentityVerifier.SocialCredentialException.invalid(
                        "Fournisseur d'identite non pris en charge");
            }
            if (credential == null || credential.isBlank()) {
                throw SocialIdentityVerifier.SocialCredentialException.invalid("Credential absent");
            }
            return new SocialProviderRequest(provider, credential);
        }
    }
}
