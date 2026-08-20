package com.discipolat.common.infrastructure.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis configuration for distributed rate limiting via Bucket4j ProxyManager.
 * <p>
 * Creates a {@link LettuceBasedProxyManager} that stores rate limit bucket state
 * in Redis, enabling multiple application instances to share rate limits.
 * <p>
 * Disabled when {@code app.rate-limiting.redis-enabled=false} (e.g. in test profiles
 * without a running Redis instance).
 */
@Configuration
@ConditionalOnProperty(name = "app.rate-limiting.redis-enabled", havingValue = "true", matchIfMissing = true)
public class RedisRateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterConfig.class);

    @Value("${app.rate-limiting.redis-url:redis://localhost:6379}")
    private String redisUrl;

    @Value("${app.rate-limiting.redis-key-expire-minutes:10}")
    private int keyExpireMinutes;

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient() {
        RedisURI uri = RedisURI.create(redisUrl);
        RedisClient client = RedisClient.create(uri);
        log.info("Redis client created for rate limiting: {}", uri);
        return client;
    }

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
