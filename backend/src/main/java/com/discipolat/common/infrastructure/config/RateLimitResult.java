package com.discipolat.common.infrastructure.config;

/**
 * Result of a rate limit check, carrying information needed for
 * {@code X-RateLimit-Remaining} and {@code Retry-After} response headers.
 *
 * @param allowed          {@code true} if the request was allowed (token consumed)
 * @param remainingTokens  tokens remaining in the bucket after this request
 * @param retryAfterSeconds estimated seconds to wait before the bucket refills
 *                          enough for the next request; 0 if allowed
 */
public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds
) {

    /** Helper: create an "allowed" result with remaining tokens. */
    public static RateLimitResult allowed(long remainingTokens) {
        return new RateLimitResult(true, Math.max(remainingTokens, 0), 0);
    }

    /** Helper: create a "denied" result with the wait time in seconds. */
    public static RateLimitResult denied(long nanosToWait) {
        long seconds = nanosToWait > 0 ? (nanosToWait / 1_000_000_000) + 1 : 1;
        return new RateLimitResult(false, 0, seconds);
    }
}
