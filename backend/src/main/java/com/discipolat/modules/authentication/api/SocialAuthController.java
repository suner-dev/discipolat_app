package com.discipolat.modules.authentication.api;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Authentification sociale — Google OAuth + Magic Link.
 *
 * Google OAuth : le frontend envoie le token Google, le backend le valide
 * et crée/connecte l'utilisateur.
 *
 * Magic Link : le backend envoie un lien par email, l'utilisateur clique
 * et est connecté sans mot de passe.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SocialAuthController {

    private static final Logger log = LoggerFactory.getLogger(SocialAuthController.class);

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.auth.google-client-id:}")
    private String googleClientId;

    public SocialAuthController(AuthService authService,
                                 UserService userService,
                                 JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Google OAuth — valider l'id_token Google côté serveur et connecter/créer
     * l'utilisateur.
     *
     * SÉCURITÉ : le credential (id_token JWT émis par Google Identity Services)
     * est TOUJOURS validé auprès de Google (tokeninfo) :
     *  - signature et expiration vérifiées par Google ;
     *  - `aud` doit correspondre au client-id configuré ;
     *  - `email_verified` doit être true.
     *
     * Aucun email nu n'est jamais accepté : sans configuration
     * (`app.auth.google-client-id` vide), l'endpoint répond 503 (désactivé).
     */
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> body) {
        if (googleClientId == null || googleClientId.isBlank()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Google sign-in is not configured on this server"));
        }
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Google credential is required"));
        }

        Map<String, Object> claims = verifyGoogleIdToken(credential);
        if (claims == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Google token"));
        }

        String email = (String) claims.get("email");
        String name = claims.get("name") != null ? claims.get("name").toString() : "";
        String picture = claims.get("picture") != null ? claims.get("picture").toString() : "";

        try {
            // Chercher ou créer l'utilisateur (rôle par défaut MEMBRE, mot de passe
            // aléatoire non communiqué : la connexion passe exclusivement par Google).
            User user = userService.findByEmail(email);
            if (user == null) {
                user = User.builder()
                        .email(email)
                        .firstName(name.contains(" ") ? name.split(" ")[0] : name)
                        .lastName(name.contains(" ") ? name.substring(name.indexOf(' ') + 1) : "")
                        .photoUrl(picture)
                        .role(com.discipolat.common.domain.UserRole.MEMBRE)
                        .build();
                user = userService.create(user, UUID.randomUUID().toString());
                log.info("New user created via Google OAuth: {}", email);
            }

            // Générer le token JWT
            String token = jwtTokenProvider.generateToken(user);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", user.getId().toString(),
                            "email", user.getEmail(),
                            "firstName", user.getFirstName(),
                            "lastName", user.getLastName(),
                            "role", user.getRole().name()
                    )
            ));

        } catch (Exception e) {
            log.error("Google OAuth failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Google authentication failed"));
        }
    }

    /**
     * Magic Link — envoyer un lien de connexion par email.
     *
     * L'utilisateur reçoit un email avec un lien unique.
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
            User user = authService.verifyMagicLink(token);
            String jwt = jwtTokenProvider.generateToken(user);
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "user", Map.of(
                            "id", user.getId().toString(),
                            "email", user.getEmail(),
                            "firstName", user.getFirstName(),
                            "lastName", user.getLastName(),
                            "role", user.getRole().name()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired magic link"));
        }
    }

    /**
     * Valide un id_token Google auprès du endpoint tokeninfo de Google.
     * Retourne les claims si le token est authentique, non expiré, émis pour
     * notre client-id et avec un email vérifié ; sinon null.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        try {
            RestTemplate rt = new RestTemplate();
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            Map<String, Object> claims = rt.getForObject(url, Map.class);
            if (claims == null) return null;

            // Audience : le token doit avoir été émis pour NOTRE application.
            if (!googleClientId.equals(claims.get("aud"))) {
                log.warn("Google token rejected: audience mismatch");
                return null;
            }
            // Email vérifié chez Google (sinon usurpation d'adresse possible).
            if (!"true".equals(String.valueOf(claims.get("email_verified")))) {
                log.warn("Google token rejected: email not verified");
                return null;
            }
            // Expiration (tokeninfo valide déjà la signature ; double contrôle).
            Object exp = claims.get("exp");
            if (exp instanceof String s && Long.parseLong(s) < System.currentTimeMillis() / 1000) {
                return null;
            }
            if (claims.get("email") == null) return null;
            return claims;
        } catch (Exception e) {
            log.warn("Google token validation failed: {}", e.getMessage());
            return null;
        }
    }
}
