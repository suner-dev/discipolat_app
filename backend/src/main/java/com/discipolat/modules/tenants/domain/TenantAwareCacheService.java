package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRedisManager;
import com.discipolat.common.multitenancy.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tenant-aware distributed cache service using Redis.
 * <p>
 * Provides caching for common tenant-scoped queries (users, souls, events, etc.)
 * with automatic tenant isolation via TenantAwareRedisManager.
 * <p>
 * Cache keys are prefixed with tenant ID to ensure complete isolation between tenants.
 */
@Service
public class TenantAwareCacheService {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareCacheService.class);

    private static final String CACHE_PREFIX = "tenant:cache:";
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes

    private final ObjectProvider<TenantAwareRedisManager> redisManagerProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean usingRedis = false;

    public TenantAwareCacheService(ObjectProvider<TenantAwareRedisManager> redisManagerProvider) {
        this.redisManagerProvider = redisManagerProvider;
    }

    @PostConstruct
    public void init() {
        TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
        this.usingRedis = redisManager != null;
        if (usingRedis) {
            log.info("TenantAwareCacheService initialized with Redis backend");
        } else {
            log.warn("TenantAwareCacheService initialized WITHOUT Redis - caching disabled");
        }
    }

    // ======================== GENERIC CACHE OPERATIONS ========================

    /**
     * Get a cached value for the current tenant.
     */
    public <T> Optional<T> get(String cacheKey, TypeReference<T> typeRef) {
        if (!usingRedis) {
            return Optional.empty();
        }
        return getForTenant(TenantContext.getCurrentTenantId(), cacheKey, typeRef);
    }

    /**
     * Get a cached value for a specific tenant.
     */
    public <T> Optional<T> getForTenant(UUID tenantId, String cacheKey, TypeReference<T> typeRef) {
        if (!usingRedis || tenantId == null) {
            return Optional.empty();
        }
        try {
            TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
            if (redisManager == null) {
                return Optional.empty();
            }
            Object value = redisManager.getValue(CACHE_PREFIX + tenantId + ":" + cacheKey);
            if (value != null) {
                return Optional.of(objectMapper.convertValue(value, typeRef));
            }
        } catch (Exception e) {
            log.debug("Cache get failed for {}:{}", tenantId, cacheKey, e);
        }
        return Optional.empty();
    }

    /**
     * Put a value in cache for the current tenant.
     */
    public void put(String cacheKey, Object value) {
        putForTenant(TenantContext.getCurrentTenantId(), cacheKey, value, DEFAULT_TTL_SECONDS);
    }

    /**
     * Put a value in cache for the current tenant with custom TTL.
     */
    public void put(String cacheKey, Object value, long ttlSeconds) {
        putForTenant(TenantContext.getCurrentTenantId(), cacheKey, value, ttlSeconds);
    }

    /**
     * Put a value in cache for a specific tenant.
     */
    public void putForTenant(UUID tenantId, String cacheKey, Object value, long ttlSeconds) {
        if (!usingRedis || tenantId == null || value == null) {
            return;
        }
        try {
            TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
            if (redisManager != null) {
                redisManager.setValue(CACHE_PREFIX + tenantId + ":" + cacheKey, value, ttlSeconds,
                        java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.debug("Cache put failed for {}:{}", tenantId, cacheKey, e);
        }
    }

    /**
     * Invalidate a cache entry for the current tenant.
     */
    public void evict(String cacheKey) {
        evictForTenant(TenantContext.getCurrentTenantId(), cacheKey);
    }

    /**
     * Invalidate a cache entry for a specific tenant.
     */
    public void evictForTenant(UUID tenantId, String cacheKey) {
        if (!usingRedis || tenantId == null) {
            return;
        }
        try {
            TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
            if (redisManager != null) {
                redisManager.delete(CACHE_PREFIX + tenantId + ":" + cacheKey);
            }
        } catch (Exception e) {
            log.debug("Cache evict failed for {}:{}", tenantId, cacheKey, e);
        }
    }

    /**
     * Invalidate all cache entries for a specific tenant (pattern-based).
     */
    public void evictAllForTenant(UUID tenantId) {
        if (!usingRedis || tenantId == null) {
            return;
        }
        try {
            TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
            if (redisManager != null) {
                // Note: This is a simplified approach. In production, consider using
                // Redis SCAN with pattern matching for better performance.
                // For now, we just log - a proper implementation would use SCAN.
                log.info("Cache invalidation requested for all keys of tenant: {}", tenantId);
            }
        } catch (Exception e) {
            log.debug("Cache evict all failed for tenant: {}", tenantId, e);
        }
    }

    // ======================== PREDEFINED CACHE KEYS ========================

    public static final String USERS_COUNT = "users:count";
    public static final String USERS_ACTIVE_COUNT = "users:active:count";
    public static final String SOULS_COUNT = "souls:count";
    public static final String EVENTS_UPCOMING = "events:upcoming";
    public static final String EVENTS_COUNT = "events:count";
    public static final String PAYMENTS_PENDING_COUNT = "payments:pending:count";
    public static final String PAYMENTS_CONFIRMED_TOTAL = "payments:confirmed:total";
    public static final String REPORTS_PENDING_COUNT = "reports:pending:count";
    public static final String DASHBOARD_KPI = "dashboard:kpi";
    public static final String DASHBOARD_TRENDS = "dashboard:trends";
    public static final String TENANT_SUBSCRIPTION = "subscription:current";
    public static final String TENANT_FEATURES = "features:enabled";

    // ======================== CONVENIENCE METHODS ========================

    /**
     * Get cached user count for current tenant.
     */
    public Optional<Long> getUsersCount() {
        return get(USERS_COUNT, new TypeReference<>() {});
    }

    /**
     * Cache user count for current tenant.
     */
    public void putUsersCount(long count) {
        put(USERS_COUNT, count);
    }

    /**
     * Get cached active users count for current tenant.
     */
    public Optional<Long> getActiveUsersCount() {
        return get(USERS_ACTIVE_COUNT, new TypeReference<>() {});
    }

    /**
     * Cache active users count for current tenant.
     */
    public void putActiveUsersCount(long count) {
        put(USERS_ACTIVE_COUNT, count);
    }

    /**
     * Get cached souls count for current tenant.
     */
    public Optional<Long> getSoulsCount() {
        return get(SOULS_COUNT, new TypeReference<>() {});
    }

    /**
     * Cache souls count for current tenant.
     */
    public void putSoulsCount(long count) {
        put(SOULS_COUNT, count);
    }

    /**
     * Get cached upcoming events for current tenant.
     */
    public Optional<List<Map<String, Object>>> getUpcomingEvents() {
        return get(EVENTS_UPCOMING, new TypeReference<>() {});
    }

    /**
     * Cache upcoming events for current tenant.
     */
    public void putUpcomingEvents(List<Map<String, Object>> events) {
        put(EVENTS_UPCOMING, events, 60); // Shorter TTL for events
    }

    /**
     * Get cached dashboard KPIs for current tenant.
     */
    public Optional<Map<String, Object>> getDashboardKpi() {
        return get(DASHBOARD_KPI, new TypeReference<>() {});
    }

    /**
     * Cache dashboard KPIs for current tenant.
     */
    public void putDashboardKpi(Map<String, Object> kpi) {
        put(DASHBOARD_KPI, kpi, 60); // 1 minute TTL for real-time feel
    }

    /**
     * Get cached dashboard trends for current tenant.
     */
    public Optional<List<Map<String, Object>>> getDashboardTrends() {
        return get(DASHBOARD_TRENDS, new TypeReference<>() {});
    }

    /**
     * Cache dashboard trends for current tenant.
     */
    public void putDashboardTrends(List<Map<String, Object>> trends) {
        put(DASHBOARD_TRENDS, trends, 300); // 5 minutes TTL
    }

    /**
     * Get cached current subscription for current tenant.
     */
    public Optional<Map<String, Object>> getCurrentSubscription() {
        return get(TENANT_SUBSCRIPTION, new TypeReference<>() {});
    }

    /**
     * Cache current subscription for current tenant.
     */
    public void putCurrentSubscription(Map<String, Object> subscription) {
        put(TENANT_SUBSCRIPTION, subscription, 300);
    }

    /**
     * Get cached enabled features for current tenant.
     */
    public Optional<Map<String, Boolean>> getEnabledFeatures() {
        return get(TENANT_FEATURES, new TypeReference<>() {});
    }

    /**
     * Cache enabled features for current tenant.
     */
    public void putEnabledFeatures(Map<String, Boolean> features) {
        put(TENANT_FEATURES, features, 300);
    }

    /**
     * Invalidate all dashboard-related caches for current tenant.
     */
    public void invalidateDashboardCaches() {
        evict(DASHBOARD_KPI);
        evict(DASHBOARD_TRENDS);
        evict(USERS_COUNT);
        evict(USERS_ACTIVE_COUNT);
        evict(SOULS_COUNT);
        evict(EVENTS_COUNT);
        evict(EVENTS_UPCOMING);
        evict(PAYMENTS_PENDING_COUNT);
        evict(PAYMENTS_CONFIRMED_TOTAL);
        evict(REPORTS_PENDING_COUNT);
    }

    /**
     * Invalidate all caches for a tenant (e.g., on plan change).
     */
    public void invalidateAllTenantCaches(UUID tenantId) {
        evictAllForTenant(tenantId);
    }
}