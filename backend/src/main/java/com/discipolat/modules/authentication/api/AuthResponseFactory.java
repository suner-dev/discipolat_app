package com.discipolat.modules.authentication.api;

import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Construction de la réponse d'authentification, <b>partagée par tous les
 * chemins</b> (mot de passe, Google, Microsoft, changement de rôle).
 *
 * <p>Pourquoi l'extraire : la réponse incluait les rôles plateforme
 * ({@code platformRoles}, {@code platformSuperAdmin}), utilisés par le frontend pour
 * basculer vers l'espace Super Admin. Dupliquer cette construction dans le
 * contrôleur social aurait garanti qu'un jour les deux réponses divergent — et
 * l'utilisateur verrait « Super Admin » disparaître sur connexion Google. Une
 * seule source, donc un comportement identique par construction.
 */
@Component
public class AuthResponseFactory {

    private final AuthorizationService authorizationService;

    public AuthResponseFactory(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /** Construit la réponse standard à partir du résultat d'une session. */
    public AuthResponse from(AuthService.AuthResult result) {
        List<String> roles = result.user().getRoles() != null
                ? result.user().getRoles().stream().map(Enum::name).collect(Collectors.toList())
                : List.of(result.user().getRole().name());
        String activeRole = result.activeRole() != null
                ? result.activeRole()
                : result.user().getRole().name();

        List<String> platformRoles = List.copyOf(authorizationService.getPlatformRoleKeys(result.user().getId()));

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
                result.user().isTwoFactorEnabled(),
                platformRoles,
                platformRoles.contains("PLATFORM_SUPER_ADMIN")
        );
    }
}
