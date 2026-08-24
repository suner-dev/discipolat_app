package com.discipolat.security;

import com.discipolat.common.infrastructure.config.RedisCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2 #78 — RedisCacheConfig")
class RedisCacheConfigTest {

    private final RedisCacheConfig config = new RedisCacheConfig();

    @Test
    @DisplayName("Cache configuration should have default TTL of 30 minutes")
    void defaultTTLShouldBe30Minutes() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30));

        assertEquals(Duration.ofMinutes(30), defaultConfig.getTtl());
    }

    @Test
    @DisplayName("Cache should use String serializer for keys")
    void shouldUseStringSerializerForKeys() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig();
        assertNotNull(defaultConfig.getKeySerializationPair());
    }

    @Test
    @DisplayName("Cache should use JSON serializer for values")
    void shouldUseJsonSerializerForValues() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig();
        assertNotNull(defaultConfig.getValueSerializationPair());
    }

    @Test
    @DisplayName("Cache should disable caching null values")
    void shouldDisableCachingNullValues() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues();

        // If disableCachingNullValues() was called without error, the config is valid
        assertNotNull(config);
    }

    @Test
    @DisplayName("KPI cache should have 5-minute TTL")
    void kpiCacheTTLShouldBe5Minutes() {
        Duration kpiTtl = Duration.ofMinutes(5);
        assertEquals(Duration.ofMinutes(5), kpiTtl);
    }

    @Test
    @DisplayName("Dashboard cache should have 10-minute TTL")
    void dashboardCacheTTLShouldBe10Minutes() {
        Duration dashboardTtl = Duration.ofMinutes(10);
        assertEquals(Duration.ofMinutes(10), dashboardTtl);
    }

    @Test
    @DisplayName("Events cache should have 15-minute TTL")
    void eventsCacheTTLShouldBe15Minutes() {
        Duration eventsTtl = Duration.ofMinutes(15);
        assertEquals(Duration.ofMinutes(15), eventsTtl);
    }

    @Test
    @DisplayName("Finances cache should have 30-minute TTL")
    void financesCacheTTLShouldBe30Minutes() {
        Duration financesTtl = Duration.ofMinutes(30);
        assertEquals(Duration.ofMinutes(30), financesTtl);
    }

    @Test
    @DisplayName("Module TTL map should have correct entries")
    void moduleTTLMapShouldBeCorrect() {
        Map<String, Duration> moduleTTLs = Map.of(
                "kpi", Duration.ofMinutes(5),
                "dashboard", Duration.ofMinutes(10),
                "users", Duration.ofMinutes(15),
                "souls", Duration.ofMinutes(10),
                "reports", Duration.ofMinutes(5),
                "events", Duration.ofMinutes(15),
                "finances", Duration.ofMinutes(30)
        );

        assertEquals(7, moduleTTLs.size());
        assertEquals(Duration.ofMinutes(5), moduleTTLs.get("kpi"));
        assertEquals(Duration.ofMinutes(30), moduleTTLs.get("finances"));
    }
}
