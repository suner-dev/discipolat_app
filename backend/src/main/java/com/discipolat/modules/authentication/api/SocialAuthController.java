package com.discipolat.modules.authentication.api;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
     * Google OAuth — valider le token Google et connecter/créer l'utilisateur.
     *
     * Le frontend utilise google.accounts.id.initialize() pour obtenir un
     * credential JWT, puis l'envoie ici.
     */
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Google credential is required"));
        }

        try {
            // En production : valider le token Google avec googleapis.com
            // En dev : traiter le credential comme un email direct
            String email = extractEmailFromGoogleToken(credential);
            String name = body.getOrDefault("name", email != null ? email.split("@")[0] : "User");
            String picture = body.getOrDefault("picture", "");

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid Google token"));
            }

            // Chercher ou créer l'utilisateur
            User user = userService.findByEmail(email).orElse(null);
            if (user == null) {
                // Créer un nouveau compte
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
     * Extraire l'email du token Google (simplifié pour le dev).
     * En production : utiliser Google's tokeninfo endpoint.
     */
    private String extractEmailFromGoogleToken(String credential) {
        // En dev, le credential est directement l'email
        if (credential.contains("@")) {
            return credential;
        }
        // En production, décoder le JWT Google et extraire l'email
        // googleapis.com/oauth2/v3/tokeninfo?id_token=...
        try {
            // TODO: implémenter la validation Google en production
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
