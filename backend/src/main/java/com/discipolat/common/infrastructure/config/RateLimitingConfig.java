package com.discipolat.common.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Rate limiting configuration is now managed by {@link PerIpRateLimiter}.
 * <p>
 * Per-IP buckets are created on-demand and stored in memory with automatic
 * cleanup of stale entries after 30 minutes of inactivity.
 * <p>
 * Limits are configured via {@code app.rate-limiting.*} properties in
 * {@code application.yml} and read directly by {@code PerIpRateLimiter}.
 *
 * @see PerIpRateLimiter
 */
@Configuration
public class RateLimitingConfig {
    // Configuration is now handled by PerIpRateLimiter.
    // This class remains as a placeholder for future rate limiting enhancements
    // (e.g., Redis-backed distributed rate limiting).
}

