package com.discipolat.common.infrastructure.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis configuration for distributed rate limiting via Bucket4j ProxyManager.
 * <p>
 * Creates a {@link LettuceBasedProxyManager} that stores rate limit bucket state
 * in Redis, enabling multiple application instances to share rate limits.
 * <p>
 * Bucket keys are automatically expired after a period of inactivity,
 * preventing stale key accumulation in Redis.
 */
@Configuration
public class RedisRateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterConfig.class);

    @Value("${app.rate-limiting.redis-url:redis://localhost:6379}")
    private String redisUrl;

    @Value("${app.rate-limiting.redis-key-expire-minutes:10}")
    private int keyExpireMinutes;

    /**
     * Creates a RedisClient connected to the configured Redis instance.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient() {
        RedisURI uri = RedisURI.create(redisUrl);
        RedisClient client = RedisClient.create(uri);
        log.info("Redis client created for rate limiting: {}", uri);
        return client;
    }

    /**
     * Creates a Bucket4j ProxyManager backed by Redis via Lettuce.
     * <p>
     * The ProxyManager handles:
     * <ul>
     *   <li>Atomic compare-and-swap operations on Redis keys</li>
     *   <li>Automatic bucket creation on first access</li>
     *   <li>Key expiration when buckets are idle (prevents memory leaks)</li>
     * </ul>
     */
    @Bean(destroyMethod = "")
    public LettuceBasedProxyManager<byte[]> lettuceProxyManager(RedisClient redisClient) {
        LettuceBasedProxyManager<byte[]> manager = LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofMinutes(keyExpireMinutes)
                        )
                )
                .build();
        log.info("LettuceBasedProxyManager created — key expire after {} min of inactivity", keyExpireMinutes);
        return manager;
    }
}
