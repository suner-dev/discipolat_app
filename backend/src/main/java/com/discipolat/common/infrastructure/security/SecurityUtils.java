package com.discipolat.common.infrastructure.security;

import com.discipolat.common.exception.UnauthorizedException;
import com.discipolat.common.multitenancy.TenantContext;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SecurityUtils {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityUtils(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

        /**
     * Returns the current authenticated user's ID.
     * Static so it can be used both via an injected bean
     * (`securityUtils.getCurrentUserId()`) and statically in controllers.
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new UnauthorizedException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID) {
            return (UUID) principal;
        }
        // Fallback: try to get JWT token and extract user ID
        if (principal instanceof String) {
            return UUID.fromString((String) principal);
        }
        throw new UnauthorizedException("Invalid authentication principal");
    }

    /**
     * Clés de rôle portées par l'authentification courante (autorité Spring
     * {@code ROLE_<clé>}), normalisées en MAJUSCULES. Vide hors requête
     * authentifiée.
     *
     * <p>PARAMÉTRISATION PAR ÉGLISE (Phase 1) : ce point d'accès isole la lecture
     * du {@code SecurityContextHolder} dans la couche infrastructure, pour que le
     * domaine ({@code AuthorizationService}) reste pur (règle R1 « domaine sans
     * infrastructure ») tout en pouvant reproduire à l'identique la décision
     * {@code hasAnyRole(...)} à partir des rôles actifs du jeton.
     */
    public static java.util.Set<String> getActiveAuthorityRoleKeys() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return java.util.Set.of();
        }
        java.util.Set<String> keys = new java.util.HashSet<>();
        authentication.getAuthorities().forEach(a -> {
            String granted = a.getAuthority();
            if (granted != null && granted.startsWith("ROLE_")) {
                // "ROLE_" compte 5 caractères — substring(4) laisserait un "_"
                // résiduel ("_PASTEUR"), et le pont d'équivalence ne résoudrait
                // aucun rôle (fail-closed silencieux).
                String key = granted.substring(5).trim().toUpperCase(java.util.Locale.ROOT);
                if (!key.isEmpty()) {
                    keys.add(key);
                }
            }
        });
        return keys;
    }

    /**
     * Returns the current tenant ID from the tenant context (set by the
     * TenantInterceptor from the JWT claim). Static for direct service use.
     */
    public static UUID getCurrentTenantId() {
        try {
            return TenantContext.requireTenantId();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Returns the active role from JWT claims (the role the user is currently
     * acting as). Falls back to the first authority if not found.
     */
    public String getCurrentUserRole() {
        // Try to get the active role from JWT token stored in security context
        String token = getCurrentJwtToken();
        if (token != null) {
            try {
                return jwtTokenProvider.extractActiveRole(token);
            } catch (Exception e) {
                // Log at debug level - this is a fallback path, not an error condition
                // logger.debug("Could not extract active role from JWT: {}", e.getMessage());
            }
        }

        // Fallback: first authority
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    /**
     * Checks if the ACTIVE role matches any of the given roles.
     * The active role represents the current workspace: access controls
     * must use this (not the full set of roles held).
     */
    public boolean hasActiveRole(String... roles) {
        String activeRole = getCurrentUserRole();
        if (activeRole == null) return false;
        for (String r : roles) {
            if (r.equals(activeRole)) return true;
        }
        return false;
    }

    /**
     * Super-user roles: full access to all workspaces.
     */
    public boolean isSuperUser() {
        return hasActiveRole("ADMIN", "PASTEUR");
    }

    /**
     * Returns ALL roles from JWT claims.
     */
    public List<String> getAllUserRoles() {
        String token = getCurrentJwtToken();
        if (token != null) {
            try {
                return jwtTokenProvider.extractRoles(token);
            } catch (Exception e) {
                // Log at debug level - this is a fallback path, not an error condition
                // logger.debug("Could not extract roles from JWT: {}", e.getMessage());
            }
        }
        String role = getCurrentUserRole();
        return role != null ? List.of(role) : List.of();
    }

    private String getCurrentJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof org.springframework.security.web.authentication.WebAuthenticationDetails) {
            return null;
        }
        if (authentication != null && authentication.getCredentials() instanceof String) {
            return (String) authentication.getCredentials();
        }
        return null;
    }
}
