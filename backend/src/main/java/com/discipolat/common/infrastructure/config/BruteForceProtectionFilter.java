package com.discipolat.common.infrastructure.config;

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
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Brute-force protection on /auth/login.
 *
 * <p>Counts only <strong>failed</strong> login attempts per IP (HTTP 401/403),
 * not successful ones. After MAX_ATTEMPTS failed attempts within a 15-minute
 * window, the IP is temporarily blocked with HTTP 429. A successful login
 * (HTTP 2xx) resets the counter immediately, so legitimate users who mistype
 * their password a few times are never permanently locked out.
 *
 * <p>This complements {@link AuthService}'s per-account lockout and
 * {@link PerIpRateLimiter}'s Redis-based rate limiting.
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

            // --- Check if IP is already blocked ---
            AttemptRecord existing = attempts.get(ip);
            if (existing != null && now - existing.windowStart < WINDOW_MS
                    && existing.count.get() >= MAX_ATTEMPTS) {
                long remainingSec = (WINDOW_MS - (now - existing.windowStart)) / 1000;
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Trop de tentatives. Résseyez dans " + remainingSec + " secondes.\",\"retryAfter\":" + remainingSec + "}");
                return;
            }

            // --- Wrap response so we can inspect the final HTTP status ---
            ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
            try {
                filterChain.doFilter(request, wrapped);
            } finally {
                int status = wrapped.getStatus();
                if (status == 200 || status == 201) {
                    // Successful login → reset the counter for this IP
                    attempts.remove(ip);
                } else if (status == 401 || status == 403) {
                    // Failed login → increment the counter
                    AttemptRecord record = attempts.compute(ip, (key, rec) -> {
                        if (rec == null || now - rec.windowStart > WINDOW_MS) {
                            return new AttemptRecord(now);
                        }
                        return rec;
                    });
                    record.count.incrementAndGet();
                }
                wrapped.copyBodyToResponse();
            }
        } else {
            filterChain.doFilter(request, response);
        }
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
