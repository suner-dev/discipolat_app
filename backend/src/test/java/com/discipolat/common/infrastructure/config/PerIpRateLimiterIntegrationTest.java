package com.discipolat.common.infrastructure.config;

import com.discipolat.DiscipolatApplication;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link PerIpRateLimiter} using Redis-backed distributed
 * rate limiting via Bucket4j ProxyManager.
 * <p>
 * In CI (GitHub Actions), the {@code REDIS_URL} environment variable is set
 * by the CI pipeline to point to the Redis service container, so Testcontainers
 * is not started. Locally, Testcontainers spins up a Redis 7 Alpine container.
 * <p>
 * Tests are ordered to simulate a real usage flow: consume tokens until
 * the bucket is exhausted, then verify denial with retry-after headers.
 */
@SpringBootTest(classes = DiscipolatApplication.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerIpRateLimiterIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        // In CI: REDIS_URL is already set via GitHub Actions env → use it as-is
        // Locally: start Testcontainers Redis and override the URL
        String ciRedisUrl = System.getenv("REDIS_URL");
        if (ciRedisUrl != null && !ciRedisUrl.isBlank()) {
            return; // Use CI Redis service — don't override
        }
        String redisUrl = "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort();
        registry.add("app.rate-limiting.redis-url", () -> redisUrl);
    }

    @Autowired
    private PerIpRateLimiter rateLimiter;

    // ======================== LOGIN BUCKET (capacity=10, refill=10/min) ========================

    @Test
    @Order(1)
    void tryConsumeLogin_ShouldAllowWithinLimit() {
        String ip = "192.168.1.10";
        for (int i = 0; i < 10; i++) {
            RateLimitResult result = rateLimiter.tryConsumeLogin(ip);
            assertTrue(result.allowed(),
                    "Token " + (i + 1) + " should be allowed for IP " + ip);
            assertTrue(result.remainingTokens() >= 0,
                    "Remaining tokens should be >= 0, got " + result.remainingTokens());
        }
    }

    @Test
    @Order(2)
    void tryConsumeLogin_ShouldBlockAfterExceedingCapacity() {
        String ip = "192.168.1.20";
        // Consume all 10 tokens
        for (int i = 0; i < 10; i++) {
            RateLimitResult r = rateLimiter.tryConsumeLogin(ip);
            assertTrue(r.allowed(), "Token " + (i + 1) + " should be allowed");
        }
        // 11th request should be denied
        RateLimitResult denied = rateLimiter.tryConsumeLogin(ip);
        assertFalse(denied.allowed(), "11th request should be denied");
        assertEquals(0, denied.remainingTokens(), "Remaining tokens should be 0");
        assertTrue(denied.retryAfterSeconds() > 0,
                "Retry-After should be positive, got " + denied.retryAfterSeconds());
    }

    @Test
    @Order(3)
    void tryConsumeLogin_DifferentIpsHaveSeparateBuckets() {
        // Exhaust IP-A
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryConsumeLogin("10.0.0.1");
        }
        assertFalse(rateLimiter.tryConsumeLogin("10.0.0.1").allowed());

        // IP-B should still have a full bucket
        assertTrue(rateLimiter.tryConsumeLogin("10.0.0.2").allowed());
    }

    // ======================== REFRESH BUCKET (capacity=20, refill=20/min) ========================

    @Test
    @Order(4)
    void tryConsumeRefresh_ShouldHaveSeparateBucketFromLogin() {
        String ip = "10.0.0.50";
        // Exhaust login bucket
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryConsumeLogin(ip);
        }
        assertFalse(rateLimiter.tryConsumeLogin(ip).allowed(),
                "Login should be exhausted");

        // Refresh bucket is separate — should still be available
        assertTrue(rateLimiter.tryConsumeRefresh(ip).allowed(),
                "Refresh should be on a separate bucket from login");
    }

    @Test
    @Order(5)
    void tryConsumeRefresh_ShouldAllowUpToCapacity() {
        String ip = "10.0.0.60";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiter.tryConsumeRefresh(ip).allowed(),
                    "Refresh token " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsumeRefresh(ip).allowed(),
                "21st refresh should be denied");
    }

    // ======================== FORGOT-PASSWORD BUCKET (capacity=3, refill=3/min) ========================

    @Test
    @Order(6)
    void tryConsumeForgotPassword_ShouldAllowUpTo3() {
        String ip = "10.0.0.100";
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.tryConsumeForgotPassword(ip).allowed(),
                    "Forgot-password token " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsumeForgotPassword(ip).allowed(),
                "4th forgot-password should be denied");
    }

    // ======================== RESET-PASSWORD BUCKET (capacity=5, refill=5/min) ========================

    @Test
    @Order(7)
    void tryConsumeResetPassword_ShouldAllowUpTo5() {
        String ip = "10.0.0.110";
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryConsumeResetPassword(ip).allowed(),
                    "Reset-password token " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsumeResetPassword(ip).allowed(),
                "6th reset-password should be denied");
    }

    // ======================== ACTIVATE BUCKET (capacity=5, refill=5/min) ========================

    @Test
    @Order(8)
    void tryConsumeActivate_ShouldAllowUpTo5() {
        String ip = "10.0.0.120";
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryConsumeActivate(ip).allowed(),
                    "Activate token " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsumeActivate(ip).allowed(),
                "6th activate should be denied");
    }

    // ======================== CHANGE-PASSWORD BUCKET (capacity=5, refill=5/min) ========================

    @Test
    @Order(9)
    void tryConsumeChangePassword_ShouldAllowUpTo5() {
        String ip = "10.0.0.130";
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryConsumeChangePassword(ip).allowed(),
                    "Change-password token " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsumeChangePassword(ip).allowed(),
                "6th change-password should be denied");
    }

    // ======================== SWITCH-ROLE BUCKET (capacity=30, refill=30/min) ========================

    @Test
    @Order(10)
    void tryConsumeSwitchRole_ShouldAllowUpTo30() {
        String ip = "10.0.0.140";
        for (int i = 0; i < 30; i++) {
            assertTrue(rateLimiter.tryConsumeSwitchRole(ip).allowed(),
                    "Switch-role token " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimiter.tryConsumeSwitchRole(ip).allowed(),
                "31st switch-role should be denied");
    }

    // ======================== CROSS-ENDPOINT ISOLATION ========================

    @Test
    @Order(11)
    void allBuckets_ShouldBeIsolated() {
        String ip = "10.0.0.200";
        // Each endpoint type should track its own consumption independently
        assertTrue(rateLimiter.tryConsumeLogin(ip).allowed());
        assertTrue(rateLimiter.tryConsumeRefresh(ip).allowed());
        assertTrue(rateLimiter.tryConsumeForgotPassword(ip).allowed());
        assertTrue(rateLimiter.tryConsumeResetPassword(ip).allowed());
        assertTrue(rateLimiter.tryConsumeActivate(ip).allowed());
        assertTrue(rateLimiter.tryConsumeChangePassword(ip).allowed());
        assertTrue(rateLimiter.tryConsumeSwitchRole(ip).allowed());
    }

    // ======================== EDGE CASES ========================

    @Test
    @Order(12)
    void rateLimitResult_ShouldProvideCorrectRetryAfter() {
        // When denied, retryAfterSeconds should be positive and finite
        String ip = "10.0.0.250";
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryConsumeLogin(ip);
        }
        RateLimitResult denied = rateLimiter.tryConsumeLogin(ip);
        assertFalse(denied.allowed());
        assertTrue(denied.retryAfterSeconds() > 0,
                "Retry-After should be > 0, got " + denied.retryAfterSeconds());
        assertTrue(denied.retryAfterSeconds() < 3600,
                "Retry-After should be reasonable (< 1h), got " + denied.retryAfterSeconds());
    }

    @Test
    @Order(13)
    void remainingTokens_ShouldDecreaseAfterConsume() {
        String ip = "192.168.100.1";
        RateLimitResult first = rateLimiter.tryConsumeLogin(ip);
        assertTrue(first.allowed());
        long remainingAfterFirst = first.remainingTokens();

        RateLimitResult second = rateLimiter.tryConsumeLogin(ip);
        assertTrue(second.allowed());
        assertEquals(remainingAfterFirst - 1, second.remainingTokens(),
                "Remaining tokens should decrease by 1 after consuming");
    }
}
