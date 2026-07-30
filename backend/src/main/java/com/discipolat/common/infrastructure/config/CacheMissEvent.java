package com.discipolat.common.infrastructure.config;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a cache lookup results in a miss.
 * Listened to by {@link CacheMissLogger} for DEBUG-level logging.
 */
public class CacheMissEvent extends ApplicationEvent {

    private final String cacheName;
    private final Object key;

    public CacheMissEvent(String cacheName, Object key) {
        super(cacheName + ":" + key);
        this.cacheName = cacheName;
        this.key = key;
    }

    public String getCacheName() {
        return cacheName;
    }

    public Object getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "CacheMissEvent{cache='" + cacheName + "', key=" + key + "}";
    }
}
