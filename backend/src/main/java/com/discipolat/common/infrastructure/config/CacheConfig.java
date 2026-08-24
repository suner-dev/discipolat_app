package com.discipolat.common.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configures the cache manager to publish {@link CacheMissEvent} on every
 * cache miss. The events are consumed by {@link CacheMissLogger} for
 * DEBUG-level logging.
 * <p>
 * Overrides {@link ConcurrentMapCacheManager#createConcurrentMapCache(String)}
 * to return a decorated cache that publishes an event when
 * {@link Cache#get(Object)} returns a miss.
 *
 * Fallback : ce CacheManager in-memory n'est créé QUE si aucun autre
 * CacheManager n'existe déjà (ex : {@link RedisCacheConfig} actif en prod).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Fallback in-memory — utilisé uniquement quand aucun CacheManager Redis
     * n'est disponible (profil test / environnement sans Redis).
     * Conditionné sur la présence d'une DataSource embarquée (H2) pour ne pas
     * entrer en conflit avec le RedisCacheManager de production.
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public ConcurrentMapCacheManager cacheManager(ApplicationEventPublisher eventPublisher) {
        return new ConcurrentMapCacheManager() {
            @Override
            protected Cache createConcurrentMapCache(String name) {
                Cache original = super.createConcurrentMapCache(name);
                return new PublishingCache(name, original, eventPublisher);
            }
        };
    }

    /**
     * Decorates a {@link Cache} instance to publish
     * {@link CacheMissEvent} on cache misses. Implements {@link Cache}
     * directly to avoid issues with {@code final} methods in
     * {@link ConcurrentMapCache}.
     */
    static class PublishingCache implements Cache {

        private final String name;
        private final Cache delegate;
        private final ApplicationEventPublisher eventPublisher;

        PublishingCache(String name, Cache delegate, ApplicationEventPublisher eventPublisher) {
            this.name = name;
            this.delegate = delegate;
            this.eventPublisher = eventPublisher;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper wrapper = delegate.get(key);
            if (wrapper == null) {
                eventPublisher.publishEvent(new CacheMissEvent(name, key));
            }
            return wrapper;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = delegate.get(key, type);
            if (value == null) {
                eventPublisher.publishEvent(new CacheMissEvent(name, key));
            }
            return value;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            T value = delegate.get(key, valueLoader);
            if (value == null) {
                eventPublisher.publishEvent(new CacheMissEvent(name, key));
            }
            return value;
        }

        @Override
        public void put(Object key, Object value) {
            delegate.put(key, value);
        }

        @Override
        public void evict(Object key) {
            delegate.evict(key);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }
}
