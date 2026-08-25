package com.discipolat.common.infrastructure.security;

import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

/**
 * Helper pour setup SecurityContext dans les tests unitaires.
 * Remplace le mock de SecurityUtils qui ne fonctionne pas pour les méthodes statiques.
 *
 * Usage :
 *   SecurityTestHelper.loginAs(userId);
 *   SecurityTestHelper.loginAs(userId, "ADMIN");
 *   SecurityTestHelper.logout();
 */
public final class SecurityTestHelper {

    private SecurityTestHelper() {}

    /**
     * Configure le SecurityContext avec un utilisateur authentifié.
     * Le principal est le UUID de l'utilisateur (comme dans le vrai filter JWT).
     */
    public static void loginAs(UUID userId) {
        loginAs(userId, "MEMBRE");
    }

    /**
     * Configure le SecurityContext avec un utilisateur authentifié et un rôle actif.
     */
    public static void loginAs(UUID userId, String role) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userId,           // principal = UUID
                "test-token",     // credentials = JWT token (pour getCurrentJwtToken)
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Nettoie le SecurityContext.
     */
    public static void logout() {
        SecurityContextHolder.clearContext();
    }
}
