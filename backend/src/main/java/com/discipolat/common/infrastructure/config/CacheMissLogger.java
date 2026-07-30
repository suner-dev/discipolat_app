package com.discipolat.common.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link CacheMissEvent} and logs cache misses at DEBUG level.
 * Helps identify which cache keys are frequently missed for optimization.
 */
@Component
public class CacheMissLogger {

    private static final Logger log = LoggerFactory.getLogger(CacheMissLogger.class);

    @EventListener
    public void onCacheMiss(CacheMissEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("CACHE MISS [{}] key={}", event.getCacheName(), event.getKey());
        }
    }
}
