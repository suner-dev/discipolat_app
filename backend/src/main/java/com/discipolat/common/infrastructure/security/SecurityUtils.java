package com.discipolat.common.infrastructure.security;

import com.discipolat.common.exception.UnauthorizedException;
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

    public UUID getCurrentUserId() {
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
            } catch (Exception ignored) {}
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
     * Returns ALL roles from JWT claims.
     */
    public List<String> getAllUserRoles() {
        String token = getCurrentJwtToken();
        if (token != null) {
            try {
                return jwtTokenProvider.extractRoles(token);
            } catch (Exception ignored) {}
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
