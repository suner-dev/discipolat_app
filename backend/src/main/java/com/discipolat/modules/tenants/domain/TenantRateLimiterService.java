package com.discipolat.modules.tenants.domain;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.multitenancy.TenantAwareRedisManager;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tenant-aware rate limiter using Bucket4j.
 * <p>
 * Provides per-tenant rate limiting for API endpoints, ensuring fair usage
 * across thousands of tenants. When Redis is available, uses distributed
 * rate limiting. Falls back to in-memory buckets.
 * <p>
 * Rate limits are configurable per endpoint type and can be overridden
 * per tenant plan (e.g., higher limits for premium plans).
 */
@Service
public class TenantRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(TenantRateLimiterService.class);

    // Default rate limits (can be overridden per tenant plan)
    @Value("${app.rate-limiting.tenant.api-capacity:1000}")
    private int defaultApiCapacity;
    @Value("${app.rate-limiting.tenant.api-refill:1000}")
    private int defaultApiRefill;
    @Value("${app.rate-limiting.tenant.api-period-minutes:1}")
    private int defaultApiPeriodMinutes;

    @Value("${app.rate-limiting.tenant.auth-capacity:50}")
    private int defaultAuthCapacity;
    @Value("${app.rate-limiting.tenant.auth-refill:50}")
    private int defaultAuthRefill;
    @Value("${app.rate-limiting.tenant.auth-period-minutes:1}")
    private int defaultAuthPeriodMinutes;

    @Value("${app.rate-limiting.tenant.webhook-capacity:200}")
    private int defaultWebhookCapacity;
    @Value("${app.rate-limiting.tenant.webhook-refill:200}")
    private int defaultWebhookRefill;
    @Value("${app.rate-limiting.tenant.webhook-period-minutes:1}")
    private int defaultWebhookPeriodMinutes;

    @Value("${app.rate-limiting.tenant.ussd-capacity:30}")
    private int defaultUssdCapacity;
    @Value("${app.rate-limiting.tenant.ussd-refill:30}")
    private int defaultUssdRefill;
    @Value("${app.rate-limiting.tenant.ussd-period-minutes:1}")
    private int defaultUssdPeriodMinutes;

    @Value("${app.rate-limiting.tenant.ai-capacity:100}")
    private int defaultAiCapacity;
    @Value("${app.rate-limiting.tenant.ai-refill:100}")
    private int defaultAiRefill;
    @Value("${app.rate-limiting.tenant.ai-period-minutes:1}")
    private int defaultAiPeriodMinutes;

    private final MeterRegistry meterRegistry;
    private final ObjectProvider<LettuceBasedProxyManager<byte[]>> redisProxyManagerProvider;
    private final ObjectProvider<TenantAwareRedisManager> redisManagerProvider;
    private final TenantService tenantService;
    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();
    private volatile boolean usingRedis = false;

    private Counter counterApiTotal, counterApiDenied;
    private Counter counterAuthTotal, counterAuthDenied;
    private Counter counterWebhookTotal, counterWebhookDenied;
    private Counter counterUssdTotal, counterUssdDenied;
    private Counter counterAiTotal, counterAiDenied;

    public TenantRateLimiterService(
            MeterRegistry meterRegistry,
            ObjectProvider<LettuceBasedProxyManager<byte[]>> redisProxyManagerProvider,
            ObjectProvider<TenantAwareRedisManager> redisManagerProvider,
            TenantService tenantService) {
        this.meterRegistry = meterRegistry;
        this.redisProxyManagerProvider = redisProxyManagerProvider;
        this.redisManagerProvider = redisManagerProvider;
        this.tenantService = tenantService;
    }

    @PostConstruct
    public void init() {
        LettuceBasedProxyManager<byte[]> proxyManager = redisProxyManagerProvider.getIfAvailable();
        this.usingRedis = proxyManager != null;
        registerMetrics();
        log.info("TenantRateLimiterService initialized — {} rate limiting",
                usingRedis ? "Redis-backed distributed" : "in-memory (no Redis)");
    }

    private void registerMetrics() {
        counterApiTotal = buildCounter("api", "total");
        counterAuthTotal = buildCounter("auth", "total");
        counterWebhookTotal = buildCounter("webhook", "total");
        counterUssdTotal = buildCounter("ussd", "total");
        counterAiTotal = buildCounter("ai", "total");

        counterApiDenied = buildCounter("api", "denied");
        counterAuthDenied = buildCounter("auth", "denied");
        counterWebhookDenied = buildCounter("webhook", "denied");
        counterUssdDenied = buildCounter("ussd", "denied");
        counterAiDenied = buildCounter("ai", "denied");
    }

    private Counter buildCounter(String endpoint, String result) {
        return Counter.builder("tenant_rate_limiter_requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .description("Tenant rate limit " + result + " checks for " + endpoint)
                .register(meterRegistry);
    }

    // ======================== PUBLIC API ========================

    /**
     * Check rate limit for general API calls per tenant.
     */
    public RateLimitResult tryConsumeApi(UUID tenantId) {
        TenantRateLimitConfig config = getConfigForTenant(tenantId, "api");
        return consume("api", config, tenantId, counterApiTotal, counterApiDenied);
    }

    /**
     * Check rate limit for authentication endpoints per tenant.
     */
    public RateLimitResult tryConsumeAuth(UUID tenantId) {
        TenantRateLimitConfig config = getConfigForTenant(tenantId, "auth");
        return consume("auth", config, tenantId, counterAuthTotal, counterAuthDenied);
    }

    /**
     * Check rate limit for webhook endpoints per tenant.
     */
    public RateLimitResult tryConsumeWebhook(UUID tenantId) {
        TenantRateLimitConfig config = getConfigForTenant(tenantId, "webhook");
        return consume("webhook", config, tenantId, counterWebhookTotal, counterWebhookDenied);
    }

    /**
     * Check rate limit for USSD endpoints per tenant.
     */
    public RateLimitResult tryConsumeUssd(UUID tenantId) {
        TenantRateLimitConfig config = getConfigForTenant(tenantId, "ussd");
        return consume("ussd", config, tenantId, counterUssdTotal, counterUssdDenied);
    }

    /**
     * Check rate limit for AI endpoints per tenant.
     */
    public RateLimitResult tryConsumeAi(UUID tenantId) {
        TenantRateLimitConfig config = getConfigForTenant(tenantId, "ai");
        return consume("ai", config, tenantId, counterAiTotal, counterAiDenied);
    }

    /**
     * Check rate limit for a specific tenant and endpoint with custom config.
     */
    public RateLimitResult tryConsume(UUID tenantId, String endpoint, TenantRateLimitConfig config) {
        Counter totalCounter = buildCounter(endpoint, "total");
        Counter deniedCounter = buildCounter(endpoint, "denied");
        return consume(endpoint, config, tenantId, totalCounter, deniedCounter);
    }

    // ======================== INTERNAL ========================

    private TenantRateLimitConfig getConfigForTenant(UUID tenantId, String endpoint) {
        try {
            Tenant tenant = tenantService.getTenant(tenantId);
            if (tenant != null && tenant.getPlan() != null) {
                return getConfigForPlan(tenant.getPlan(), endpoint);
            }
        } catch (Exception e) {
            log.debug("Could not fetch tenant plan for rate limiting, using defaults: {}", e.getMessage());
        }
        return getDefaultConfig(endpoint);
    }

    private TenantRateLimitConfig getConfigForPlan(String plan, String endpoint) {
        // Plan-based rate limits
        return switch (plan.toUpperCase()) {
            case "ENTERPRISE" -> getEnterpriseConfig(endpoint);
            case "PROFESSIONAL" -> getProfessionalConfig(endpoint);
            case "GROWTH" -> getGrowthConfig(endpoint);
            case "DISCOVERY" -> getDiscoveryConfig(endpoint);
            default -> getDefaultConfig(endpoint);
        };
    }

    private TenantRateLimitConfig getEnterpriseConfig(String endpoint) {
        return switch (endpoint) {
            case "api" -> new TenantRateLimitConfig(10000, 10000, 1);
            case "auth" -> new TenantRateLimitConfig(500, 500, 1);
            case "webhook" -> new TenantRateLimitConfig(2000, 2000, 1);
            case "ussd" -> new TenantRateLimitConfig(200, 200, 1);
            case "ai" -> new TenantRateLimitConfig(1000, 1000, 1);
            default -> getDefaultConfig(endpoint);
        };
    }

    private TenantRateLimitConfig getProfessionalConfig(String endpoint) {
        return switch (endpoint) {
            case "api" -> new TenantRateLimitConfig(5000, 5000, 1);
            case "auth" -> new TenantRateLimitConfig(200, 200, 1);
            case "webhook" -> new TenantRateLimitConfig(1000, 1000, 1);
            case "ussd" -> new TenantRateLimitConfig(100, 100, 1);
            case "ai" -> new TenantRateLimitConfig(500, 500, 1);
            default -> getDefaultConfig(endpoint);
        };
    }

    private TenantRateLimitConfig getGrowthConfig(String endpoint) {
        return switch (endpoint) {
            case "api" -> new TenantRateLimitConfig(2000, 2000, 1);
            case "auth" -> new TenantRateLimitConfig(100, 100, 1);
            case "webhook" -> new TenantRateLimitConfig(500, 500, 1);
            case "ussd" -> new TenantRateLimitConfig(50, 50, 1);
            case "ai" -> new TenantRateLimitConfig(200, 200, 1);
            default -> getDefaultConfig(endpoint);
        };
    }

    private TenantRateLimitConfig getDiscoveryConfig(String endpoint) {
        return switch (endpoint) {
            case "api" -> new TenantRateLimitConfig(500, 500, 1);
            case "auth" -> new TenantRateLimitConfig(30, 30, 1);
            case "webhook" -> new TenantRateLimitConfig(100, 100, 1);
            case "ussd" -> new TenantRateLimitConfig(20, 20, 1);
            case "ai" -> new TenantRateLimitConfig(50, 50, 1);
            default -> getDefaultConfig(endpoint);
        };
    }

    private TenantRateLimitConfig getDefaultConfig(String endpoint) {
        return switch (endpoint) {
            case "api" -> new TenantRateLimitConfig(defaultApiCapacity, defaultApiRefill, defaultApiPeriodMinutes);
            case "auth" -> new TenantRateLimitConfig(defaultAuthCapacity, defaultAuthRefill, defaultAuthPeriodMinutes);
            case "webhook" -> new TenantRateLimitConfig(defaultWebhookCapacity, defaultWebhookRefill, defaultWebhookPeriodMinutes);
            case "ussd" -> new TenantRateLimitConfig(defaultUssdCapacity, defaultUssdRefill, defaultUssdPeriodMinutes);
            case "ai" -> new TenantRateLimitConfig(defaultAiCapacity, defaultAiRefill, defaultAiPeriodMinutes);
            default -> new TenantRateLimitConfig(1000, 1000, 1);
        };
    }

    private RateLimitResult consume(String endpoint, TenantRateLimitConfig config, UUID tenantId,
                                    Counter counterTotal, Counter counterDenied) {
        counterTotal.increment();

        String bucketKey = "tenant:" + tenantId + ":" + endpoint;
        Refill refill = Refill.greedy(config.refillTokens(), Duration.ofMinutes(config.periodMinutes()));
        Bandwidth limit = Bandwidth.classic(config.capacity(), refill);

        try {
            Bucket bucket;
            if (usingRedis) {
                LettuceBasedProxyManager<byte[]> proxyManager = redisProxyManagerProvider.getIfAvailable();
                if (proxyManager != null) {
                    byte[] key = bucketKey.getBytes(StandardCharsets.UTF_8);
                    bucket = proxyManager.builder()
                            .build(key, BucketConfiguration.builder().addLimit(limit).build());
                } else {
                    bucket = getLocalBucket(bucketKey, limit);
                }
            } else {
                bucket = getLocalBucket(bucketKey, limit);
            }

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                return RateLimitResult.allowed(probe.getRemainingTokens());
            }
            counterDenied.increment();
            return RateLimitResult.denied(probe.getNanosToWaitForRefill());
        } catch (Exception e) {
            log.warn("Tenant rate limiting error for {}: {}", bucketKey, e.getMessage());
            return RateLimitResult.allowed(999);
        }
    }

    private Bucket getLocalBucket(String bucketKey, Bandwidth limit) {
        return localBuckets.computeIfAbsent(bucketKey,
                k -> Bucket.builder().addLimit(limit).build());
    }

    /**
     * Configuration for tenant rate limits.
     */
    public record TenantRateLimitConfig(
            int capacity,
            int refillTokens,
            int periodMinutes
    ) {}

    /**
     * Result of a rate limit check.
     */
    public record RateLimitResult(
            boolean allowed,
            long remainingTokens,
            long retryAfterNanos
    ) {
        public static RateLimitResult allowed(long remainingTokens) {
            return new RateLimitResult(true, remainingTokens, 0);
        }

        public static RateLimitResult denied(long retryAfterNanos) {
            return new RateLimitResult(false, 0, retryAfterNanos);
        }
    }
}