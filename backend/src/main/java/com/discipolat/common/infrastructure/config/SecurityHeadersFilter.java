package com.discipolat.common.infrastructure.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adds Content-Security-Policy and other security headers to all responses.
 * P2 — Security: CSP headers to protect against XSS attacks.
 */
@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Content Security Policy
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://accounts.google.com; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' http://localhost:* https:; " +
                "frame-src https://accounts.google.com; " +
                "object-src 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'; " +
                "frame-ancestors 'none'");

        // Other security headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(self), payment=()");

        chain.doFilter(request, response);
    }
}
