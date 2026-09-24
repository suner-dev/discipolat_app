package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
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
    private final Map<String, CachedFlag> cache = new LinkedHashMap<>(16, 0.75f, true);
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

        CachedFlag cached;
        long version;
        synchronized (cacheLock) {
            cached = cache.get(key);
            if (cached != null && !cached.isExpired()) {
                return cached.enabled();
            }
            if (cached != null) {
                cache.remove(key);
            }
            version = cacheVersion;
        }

        boolean enabled = repository.findByKey(key)
                .map(PlatformFeatureFlag::isEnabled)
                .orElseGet(() -> DEFAULTS.getOrDefault(key, false));
        synchronized (cacheLock) {
            if (version == cacheVersion) {
                if (cache.size() >= MAX_CACHE_ENTRIES) {
                    cache.remove(cache.keySet().iterator().next());
                }
                cache.put(key, new CachedFlag(enabled, System.nanoTime() + CACHE_TTL_NANOS));
            }
        }
        return enabled;
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
        synchronized (cacheLock) {
            cacheVersion++;
            cache.remove(key);
        }
    }

    public void invalidateCache() {
        synchronized (cacheLock) {
            cacheVersion++;
            cache.clear();
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
