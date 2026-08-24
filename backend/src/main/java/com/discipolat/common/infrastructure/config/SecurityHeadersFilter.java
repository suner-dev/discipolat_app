package com.discipolat.common.infrastructure.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * P2 #66 — Content Security Policy (CSP) complet
 * Ajoute les en-têtes de sécurité HTTP pour protéger contre XSS, clickjacking, etc.
 */
@Component
@Order(2)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            // P2 #66 — CSP complet
            httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "img-src 'self' data: https: blob:; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "connect-src 'self' https://api.openai.com wss: ws:; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'");

            // P2 #66 — X-Content-Type-Options
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");

            // P2 #66 — X-Frame-Options
            httpResponse.setHeader("X-Frame-Options", "DENY");

            // P2 #66 — X-XSS-Protection
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

            // P2 #66 — Referrer Policy
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // P2 #66 — Permissions Policy
            httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(self), geolocation=(), payment=()");

            // P2 #66 — Strict-Transport-Security
            httpResponse.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");

            // P2 #66 — X-Permitted-Cross-Domain-Policies
            httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        }

        chain.doFilter(request, response);
    }
}
