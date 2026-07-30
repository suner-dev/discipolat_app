package com.discipolat.common.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Per-IP rate limiter using Bucket4j with a Redis-backed ProxyManager.
 * <p>
 * Each client IP gets its own set of Buckets (login, refresh, forgot-password, etc.)
 * stored in Redis via {@link LettuceBasedProxyManager}. This enables distributed
 * rate limiting across multiple application instances.
 * <p>
 * Bucket keys are automatically expired in Redis after a period of inactivity
 * (configured via {@code app.rate-limiting.redis-key-expire-minutes}).
 */
@Service
public class PerIpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(PerIpRateLimiter.class);

    // Redis key prefixes per bucket type
    private static final String KEY_PREFIX = "rl:";

    // ======================== BUCKET CONFIGURATION ========================

    @Value("${app.rate-limiting.login-capacity:10}")
    private int loginCapacity;

    @Value("${app.rate-limiting.login-refill:10}")
    private int loginRefill;

    @Value("${app.rate-limiting.login-period-minutes:1}")
    private int loginPeriodMinutes;

    @Value("${app.rate-limiting.refresh-capacity:20}")
    private int refreshCapacity;

    @Value("${app.rate-limiting.refresh-refill:20}")
    private int refreshRefill;

    @Value("${app.rate-limiting.refresh-period-minutes:1}")
    private int refreshPeriodMinutes;

    @Value("${app.rate-limiting.forgot-password-capacity:3}")
    private int forgotPasswordCapacity;

    @Value("${app.rate-limiting.forgot-password-refill:3}")
    private int forgotPasswordRefill;

    @Value("${app.rate-limiting.forgot-password-period-minutes:1}")
    private int forgotPasswordPeriodMinutes;

    @Value("${app.rate-limiting.reset-password-capacity:5}")
    private int resetPasswordCapacity;

    @Value("${app.rate-limiting.reset-password-refill:5}")
    private int resetPasswordRefill;

    @Value("${app.rate-limiting.reset-password-period-minutes:1}")
    private int resetPasswordPeriodMinutes;

    @Value("${app.rate-limiting.activate-capacity:5}")
    private int activateCapacity;

    @Value("${app.rate-limiting.activate-refill:5}")
    private int activateRefill;

    @Value("${app.rate-limiting.activate-period-minutes:1}")
    private int activatePeriodMinutes;

    @Value("${app.rate-limiting.change-password-capacity:5}")
    private int changePasswordCapacity;

    @Value("${app.rate-limiting.change-password-refill:5}")
    private int changePasswordRefill;

    @Value("${app.rate-limiting.change-password-period-minutes:1}")
    private int changePasswordPeriodMinutes;

    @Value("${app.rate-limiting.switch-role-capacity:30}")
    private int switchRoleCapacity;

    @Value("${app.rate-limiting.switch-role-refill:30}")
    private int switchRoleRefill;

    @Value("${app.rate-limiting.switch-role-period-minutes:1}")
    private int switchRolePeriodMinutes;

    // ======================== DEPENDENCIES ========================

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final MeterRegistry meterRegistry;

    // Counters: total requests checked
    private Counter counterLoginTotal;
    private Counter counterRefreshTotal;
    private Counter counterForgotPasswordTotal;
    private Counter counterResetPasswordTotal;
    private Counter counterActivateTotal;
    private Counter counterChangePasswordTotal;
    private Counter counterSwitchRoleTotal;

    // Counters: denied (429) requests
    private Counter counterLoginDenied;
    private Counter counterRefreshDenied;
    private Counter counterForgotPasswordDenied;
    private Counter counterResetPasswordDenied;
    private Counter counterActivateDenied;
    private Counter counterChangePasswordDenied;
    private Counter counterSwitchRoleDenied;

    public PerIpRateLimiter(LettuceBasedProxyManager<byte[]> proxyManager, MeterRegistry meterRegistry) {
        this.proxyManager = proxyManager;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        registerMetrics();
        log.info("PerIpRateLimiter initialized — Redis-backed distributed rate limiting");
    }

    // ======================== METRICS ========================

    private void registerMetrics() {
        // Counters — total requests (allowed + denied)
        counterLoginTotal = buildCounter("login", "total");
        counterRefreshTotal = buildCounter("refresh", "total");
        counterForgotPasswordTotal = buildCounter("forgot_password", "total");
        counterResetPasswordTotal = buildCounter("reset_password", "total");
        counterActivateTotal = buildCounter("activate", "total");
        counterChangePasswordTotal = buildCounter("change_password", "total");
        counterSwitchRoleTotal = buildCounter("switch_role", "total");

        // Counters — denied (429)
        counterLoginDenied = buildCounter("login", "denied");
        counterRefreshDenied = buildCounter("refresh", "denied");
        counterForgotPasswordDenied = buildCounter("forgot_password", "denied");
        counterResetPasswordDenied = buildCounter("reset_password", "denied");
        counterActivateDenied = buildCounter("activate", "denied");
        counterChangePasswordDenied = buildCounter("change_password", "denied");
        counterSwitchRoleDenied = buildCounter("switch_role", "denied");

        log.info("Rate limiter Prometheus metrics registered");
    }

    private Counter buildCounter(String endpoint, String result) {
        return Counter.builder("rate_limiter_requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .description("Rate limit " + result + " checks for " + endpoint)
                .register(meterRegistry);
    }

    // ======================== PUBLIC API ========================

    /**
     * Try to consume 1 token from the login bucket for the given IP.
     * The bucket is stored in Redis, shared across all instances.
     */
    public RateLimitResult tryConsumeLogin(String ip) {
        return consume("login", loginCapacity, loginRefill, loginPeriodMinutes, ip,
                counterLoginTotal, counterLoginDenied);
    }

    public RateLimitResult tryConsumeRefresh(String ip) {
        return consume("refresh", refreshCapacity, refreshRefill, refreshPeriodMinutes, ip,
                counterRefreshTotal, counterRefreshDenied);
    }

    public RateLimitResult tryConsumeForgotPassword(String ip) {
        return consume("forgot_password", forgotPasswordCapacity, forgotPasswordRefill, forgotPasswordPeriodMinutes, ip,
                counterForgotPasswordTotal, counterForgotPasswordDenied);
    }

    public RateLimitResult tryConsumeResetPassword(String ip) {
        return consume("reset_password", resetPasswordCapacity, resetPasswordRefill, resetPasswordPeriodMinutes, ip,
                counterResetPasswordTotal, counterResetPasswordDenied);
    }

    public RateLimitResult tryConsumeActivate(String ip) {
        return consume("activate", activateCapacity, activateRefill, activatePeriodMinutes, ip,
                counterActivateTotal, counterActivateDenied);
    }

    public RateLimitResult tryConsumeChangePassword(String ip) {
        return consume("change_password", changePasswordCapacity, changePasswordRefill, changePasswordPeriodMinutes, ip,
                counterChangePasswordTotal, counterChangePasswordDenied);
    }

    public RateLimitResult tryConsumeSwitchRole(String ip) {
        return consume("switch_role", switchRoleCapacity, switchRoleRefill, switchRolePeriodMinutes, ip,
                counterSwitchRoleTotal, counterSwitchRoleDenied);
    }

    /**
     * Extract the client IP from the HTTP request.
     */
    public static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    // ======================== INTERNAL ========================

    /**
     * Centralized consumption method.
     * Builds a Redis key from the endpoint type and IP, then uses the ProxyManager
     * to atomically retrieve-or-create the bucket in Redis and consume one token.
     */
    private RateLimitResult consume(String endpoint, int capacity, int refillTokens, int periodMinutes,
                                     String ip, Counter counterTotal, Counter counterDenied) {
        counterTotal.increment();

        byte[] redisKey = buildKey(endpoint, ip);
        BucketConfiguration config = buildConfig(capacity, refillTokens, periodMinutes);

        Bucket bucket = proxyManager.builder().build(redisKey, config);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitResult.allowed(probe.getRemainingTokens());
        }
        counterDenied.increment();
        return RateLimitResult.denied(probe.getNanosToWaitForRefill());
    }

    /**
     * Build a Redis key for the given endpoint and IP.
     * Format: {@code rl:{endpoint}:{ip}}
     */
    private static byte[] buildKey(String endpoint, String ip) {
        return (KEY_PREFIX + endpoint + ":" + ip).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Build a Bucket4j configuration from the given parameters.
     */
    private static BucketConfiguration buildConfig(int capacity, int refillTokens, int periodMinutes) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(refillTokens, Duration.ofMinutes(periodMinutes))))
                .build();
    }
}
