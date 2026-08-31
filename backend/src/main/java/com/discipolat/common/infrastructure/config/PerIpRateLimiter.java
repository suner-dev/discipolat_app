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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter using Bucket4j.
 * <p>
 * When a {@link LettuceBasedProxyManager} bean is available (Redis running), distributed
 * rate limiting is used. When Redis is unavailable, falls back to an in-memory
 * ConcurrentHashMap of Bucket4j buckets.
 */
@Service
public class PerIpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(PerIpRateLimiter.class);

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

    @Value("${app.rate-limiting.demo-request-capacity:3}")
    private int demoRequestCapacity;
    @Value("${app.rate-limiting.demo-request-refill:3}")
    private int demoRequestRefill;
    @Value("${app.rate-limiting.demo-request-period-minutes:10}")
    private int demoRequestPeriodMinutes;

    private final MeterRegistry meterRegistry;
    private final boolean usingRedis;
    private final LettuceBasedProxyManager<byte[]> redisProxyManager;
    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    private Counter counterLoginTotal, counterRefreshTotal, counterForgotPasswordTotal;
    private Counter counterResetPasswordTotal, counterActivateTotal, counterChangePasswordTotal;
    private Counter counterSwitchRoleTotal;
    private Counter counterDemoRequestTotal;
    private Counter counterLoginDenied, counterRefreshDenied, counterForgotPasswordDenied;
    private Counter counterResetPasswordDenied, counterActivateDenied, counterChangePasswordDenied;
    private Counter counterSwitchRoleDenied;
    private Counter counterDemoRequestDenied;

    public PerIpRateLimiter(
            Optional<LettuceBasedProxyManager<byte[]>> redisProxyManager,
            MeterRegistry meterRegistry) {
        this.redisProxyManager = redisProxyManager.orElse(null);
        this.meterRegistry = meterRegistry;
        this.usingRedis = this.redisProxyManager != null;
    }

    @PostConstruct
    public void init() {
        registerMetrics();
        log.info("PerIpRateLimiter initialized — {} rate limiting",
                usingRedis ? "Redis-backed distributed" : "in-memory (no Redis)");
    }

    private void registerMetrics() {
        counterLoginTotal = buildCounter("login", "total");
        counterRefreshTotal = buildCounter("refresh", "total");
        counterForgotPasswordTotal = buildCounter("forgot_password", "total");
        counterResetPasswordTotal = buildCounter("reset_password", "total");
        counterActivateTotal = buildCounter("activate", "total");
        counterChangePasswordTotal = buildCounter("change_password", "total");
        counterSwitchRoleTotal = buildCounter("switch_role", "total");
        counterDemoRequestTotal = buildCounter("demo_request", "total");

        counterLoginDenied = buildCounter("login", "denied");
        counterRefreshDenied = buildCounter("refresh", "denied");
        counterForgotPasswordDenied = buildCounter("forgot_password", "denied");
        counterResetPasswordDenied = buildCounter("reset_password", "denied");
        counterActivateDenied = buildCounter("activate", "denied");
        counterChangePasswordDenied = buildCounter("change_password", "denied");
        counterSwitchRoleDenied = buildCounter("switch_role", "denied");
        counterDemoRequestDenied = buildCounter("demo_request", "denied");
    }

    private Counter buildCounter(String endpoint, String result) {
        return Counter.builder("rate_limiter_requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .description("Rate limit " + result + " checks for " + endpoint)
                .register(meterRegistry);
    }

    // ======================== PUBLIC API ========================

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

    /** Demandes de démonstration (endpoint public landing) — quota serré anti-spam. */
    public RateLimitResult tryConsumeDemoRequest(String ip) {
        return consume("demo_request", demoRequestCapacity, demoRequestRefill, demoRequestPeriodMinutes, ip,
                counterDemoRequestTotal, counterDemoRequestDenied);
    }

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

    private RateLimitResult consume(String endpoint, int capacity, int refillTokens, int periodMinutes,
                                      String ip, Counter counterTotal, Counter counterDenied) {
        counterTotal.increment();

        String bucketKey = endpoint + ":" + ip;
        Refill refill = Refill.greedy(refillTokens, Duration.ofMinutes(periodMinutes));
        Bandwidth limit = Bandwidth.classic(capacity, refill);

        try {
            Bucket bucket;
            if (usingRedis) {
                byte[] key = bucketKey.getBytes(StandardCharsets.UTF_8);
                bucket = redisProxyManager.builder()
                        .build(key, BucketConfiguration.builder().addLimit(limit).build());
            } else {
                bucket = localBuckets.computeIfAbsent(bucketKey,
                        k -> Bucket.builder().addLimit(limit).build());
            }

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                return RateLimitResult.allowed(probe.getRemainingTokens());
            }
            counterDenied.increment();
            return RateLimitResult.denied(probe.getNanosToWaitForRefill());
        } catch (Exception e) {
            log.warn("Rate limiting error for {}: {}", bucketKey, e.getMessage());
            return RateLimitResult.allowed(999);
        }
    }
}
