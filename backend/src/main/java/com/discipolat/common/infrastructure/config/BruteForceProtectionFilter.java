package com.discipolat.common.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Rate limiting on /auth/login to prevent brute-force attacks.
 * Uses a simple in-memory bucket: max 5 attempts per IP in a 15-minute window.
 * For production, replace with Redis-backed Bucket4j or similar.
 */
@Component
public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000; // 15 minutes

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.contains("/auth/login") && "POST".equalsIgnoreCase(request.getMethod())) {
            String ip = getClientIp(request);
            long now = System.currentTimeMillis();
            AttemptRecord record = attempts.compute(ip, (key, existing) -> {
                if (existing == null || now - existing.windowStart > WINDOW_MS) {
                    return new AttemptRecord(now);
                }
                return existing;
            });

            if (record.count.incrementAndGet() > MAX_ATTEMPTS) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Trop de tentatives. Réessayez dans 15 minutes.\",\"retryAfter\":" + ((WINDOW_MS - (now - record.windowStart)) / 1000) + "}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class AttemptRecord {
        final long windowStart;
        final AtomicInteger count;

        AttemptRecord(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }
    }
}
