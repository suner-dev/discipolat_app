package com.discipolat.common.multitenancy;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Intercepts every request to extract the tenant ID from the JWT token
 * and set it in the TenantContext (ThreadLocal).
 *
 * This runs after JwtAuthenticationFilter but before the controller,
 * so the tenant context is available to all services and repositories.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public TenantInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if (token != null) {
            try {
                UUID tenantId = jwtTokenProvider.extractTenantId(token);
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }
            } catch (Exception ignored) {
                // Token may be invalid or missing tenant claim;
                // JwtAuthenticationFilter will handle auth errors
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
