package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.multitenancy.TenantAwareRedisManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlatformFeatureFlagService {

    public static final String AI_ENABLED = "aiEnabled";
    public static final String MOBILE_MONEY_ENABLED = "mobileMoneyEnabled";
    public static final String WHATSAPP_ENABLED = "whatsappEnabled";
    public static final String ANALYTICS_ENABLED = "analyticsEnabled";
    public static final String DOCS_ENABLED = "docsEnabled";

    private static final int MAX_CACHE_ENTRIES = 32;
    private static final long CACHE_TTL_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final String REDIS_KEY_PREFIX = "platform:feature-flags:";
    private static final long REDIS_TTL_SECONDS = 300; // 5 minutes

    private static final Map<String, Boolean> DEFAULTS = Map.of(
            AI_ENABLED, true,
            MOBILE_MONEY_ENABLED, true,
            WHATSAPP_ENABLED, true,
            ANALYTICS_ENABLED, true,
            DOCS_ENABLED, true
    );
    private static final Map<String, FeatureDefinition> FEATURES = Map.of(
            AI_ENABLED, new FeatureDefinition("FEATURE_DISABLED_AI",
                    "La fonctionnalité IA est désactivée par l'administrateur de la plateforme."),
            MOBILE_MONEY_ENABLED, new FeatureDefinition("FEATURE_DISABLED_MOBILE_MONEY",
                    "Le service de paiement Mobile Money est désactivé par l'administrateur de la plateforme."),
            WHATSAPP_ENABLED, new FeatureDefinition("FEATURE_DISABLED_WHATSAPP",
                    "La messagerie WhatsApp est désactivée par l'administrateur de la plateforme."),
            ANALYTICS_ENABLED, new FeatureDefinition("FEATURE_DISABLED_ANALYTICS",
                    "Les analytics d'usage sont désactivés par l'administrateur de la plateforme."),
            DOCS_ENABLED, new FeatureDefinition("FEATURE_DISABLED_DOCS",
                    "La documentation de l'API est désactivée par l'administrateur de la plateforme.")
    );

    private final PlatformFeatureFlagRepository repository;
    private final ObjectProvider<TenantAwareRedisManager> redisManagerProvider;
    private final Map<String, CachedFlag> localCache = new LinkedHashMap<>(16, 0.75f, true);
    private final Object cacheLock = new Object();
    private long cacheVersion;

    public List<PlatformFeatureFlag> getAll() {
        return repository.findAllByOrderByCategoryAscKeyAsc();
    }

    public Map<String, Boolean> getAllAsMap() {
        return repository.findAllByOrderByCategoryAscKeyAsc().stream()
                .collect(Collectors.toMap(PlatformFeatureFlag::getKey, PlatformFeatureFlag::isEnabled));
    }

    public Optional<PlatformFeatureFlag> getByKey(String key) {
        return repository.findByKey(key);
    }

    public boolean isEnabled(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Feature flag key is required");
        }

        // Try Redis first (distributed cache)
        Boolean redisValue = getFromRedis(key);
        if (redisValue != null) {
            // Update local cache
            updateLocalCache(key, redisValue);
            return redisValue;
        }

        // Fallback to local cache
        CachedFlag cached;
        long version;
        synchronized (cacheLock) {
            cached = localCache.get(key);
            if (cached != null && !cached.isExpired()) {
                return cached.enabled();
            }
            if (cached != null) {
                localCache.remove(key);
            }
            version = cacheVersion;
        }

        // Fetch from database
        boolean enabled = repository.findByKey(key)
                .map(PlatformFeatureFlag::isEnabled)
                .orElseGet(() -> DEFAULTS.getOrDefault(key, false));

        // Store in Redis and local cache
        putToRedis(key, enabled);
        synchronized (cacheLock) {
            if (version == cacheVersion) {
                if (localCache.size() >= MAX_CACHE_ENTRIES) {
                    localCache.remove(localCache.keySet().iterator().next());
                }
                localCache.put(key, new CachedFlag(enabled, System.nanoTime() + CACHE_TTL_NANOS));
            }
        }
        return enabled;
    }

    private Boolean getFromRedis(String key) {
        TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
        if (redisManager == null) {
            return null;
        }
        try {
            Object value = redisManager.getValue(REDIS_KEY_PREFIX + key);
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof String str) {
                return Boolean.parseBoolean(str);
            }
        } catch (Exception e) {
            // Redis unavailable, fallback to local cache
        }
        return null;
    }

    private void putToRedis(String key, boolean value) {
        TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
        if (redisManager == null) {
            return;
        }
        try {
            redisManager.setValue(REDIS_KEY_PREFIX + key, value, REDIS_TTL_SECONDS,
                    java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis unavailable, continue with local cache only
        }
    }

    private void removeFromRedis(String key) {
        TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
        if (redisManager == null) {
            return;
        }
        try {
            redisManager.delete(REDIS_KEY_PREFIX + key);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void updateLocalCache(String key, boolean value) {
        synchronized (cacheLock) {
            localCache.put(key, new CachedFlag(value, System.nanoTime() + CACHE_TTL_NANOS));
        }
    }

    public void requireEnabled(String key) {
        FeatureDefinition feature = FEATURES.get(key);
        if (feature == null) {
            throw new IllegalArgumentException("Unknown platform feature flag: " + key);
        }
        if (!isEnabled(key)) {
            throw new BusinessRuleException(feature.message(), feature.code());
        }
    }

    public void invalidateCache(String key) {
        removeFromRedis(key);
        synchronized (cacheLock) {
            cacheVersion++;
            localCache.remove(key);
        }
    }

    public void invalidateCache() {
        // Clear all feature flags from Redis
        TenantAwareRedisManager redisManager = redisManagerProvider.getIfAvailable();
        if (redisManager != null) {
            try {
                // Delete all keys with our prefix
                for (String key : DEFAULTS.keySet()) {
                    removeFromRedis(key);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        synchronized (cacheLock) {
            cacheVersion++;
            localCache.clear();
        }
    }

    public PlatformFeatureFlag createOrUpdate(String key, String name, String description, boolean enabled, String category) {
        PlatformFeatureFlag flag = repository.findByKey(key).orElse(PlatformFeatureFlag.builder().key(key).build());
        flag.setName(name);
        flag.setDescription(description);
        flag.setEnabled(enabled);
        flag.setCategory(category);
        PlatformFeatureFlag saved = repository.save(flag);
        invalidateCache(key);
        return saved;
    }

    public PlatformFeatureFlag toggle(String key, boolean enabled) {
        PlatformFeatureFlag flag = repository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found: " + key));
        flag.setEnabled(enabled);
        PlatformFeatureFlag saved = repository.save(flag);
        invalidateCache(key);
        return saved;
    }

    public void delete(String key) {
        repository.findByKey(key).ifPresent(flag -> {
            repository.delete(flag);
            invalidateCache(key);
        });
    }

    public void seedDefaults() {
        var defaults = List.of(
                new DefaultFlag(AI_ENABLED, "Intelligence Artificielle", "Fonctionnalités IA (sermons, conseils, etc.)", true, "AI"),
                new DefaultFlag(MOBILE_MONEY_ENABLED, "Mobile Money", "Paiements Mobile Money (Orange Money, MTN MoMo, etc.)", true, "PAYMENTS"),
                new DefaultFlag(WHATSAPP_ENABLED, "WhatsApp Business", "Notifications et communication via WhatsApp", true, "COMMUNICATION"),
                new DefaultFlag(ANALYTICS_ENABLED, "Analytics Avancés", "Tableaux de bord et rapports avancés", true, "ANALYTICS"),
                new DefaultFlag(DOCS_ENABLED, "Documentation", "Accès à la documentation intégrée", true, "CORE")
        );

        for (var d : defaults) {
            if (repository.findByKey(d.key).isEmpty()) {
                createOrUpdate(d.key, d.name, d.description, d.enabled, d.category);
            }
        }
    }

    private record CachedFlag(boolean enabled, long expiresAtNanos) {
        private boolean isExpired() {
            return System.nanoTime() - expiresAtNanos >= 0;
        }
    }

    private record FeatureDefinition(String code, String message) {}

    private record DefaultFlag(String key, String name, String description, boolean enabled, String category) {}
}
