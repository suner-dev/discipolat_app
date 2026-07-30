package com.discipolat.modules.admin.api;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/cache-stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCacheController {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public AdminCacheController(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        List<String> cacheNames = List.copyOf(cacheManager.getCacheNames());
        List<Map<String, Object>> cacheDetails = new LinkedList<>();
        long totalHits = 0;
        long totalMisses = 0;

        for (String name : cacheNames) {
            long hits = getCounterValue(name, "hit");
            long misses = getCounterValue(name, "miss");
            long total = hits + misses;
            double ratio = total > 0 ? (double) hits / total : 0.0;

            totalHits += hits;
            totalMisses += misses;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("cache", name);
            detail.put("hits", hits);
            detail.put("misses", misses);
            detail.put("totalRequests", total);
            detail.put("hitRatio", Math.round(ratio * 1000.0) / 10.0); // one decimal %
            detail.put("hitRatioFormatted", String.format("%.1f%%", ratio * 100));
            detail.put("missRatioFormatted", String.format("%.1f%%", (1 - ratio) * 100));
            detail.put("status", total == 0 ? "NO_DATA" : ratio > 0.8 ? "HEALTHY" : ratio > 0.5 ? "WARNING" : "CRITICAL");

            cacheDetails.add(detail);
        }

        long totalRequests = totalHits + totalMisses;
        double overallRatio = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("caches", cacheDetails);
        response.put("summary", Map.of(
                "totalCaches", cacheNames.size(),
                "totalHits", totalHits,
                "totalMisses", totalMisses,
                "totalRequests", totalRequests,
                "overallHitRatio", Math.round(overallRatio * 1000.0) / 10.0,
                "overallHitRatioFormatted", String.format("%.1f%%", overallRatio * 100)
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Query the Micrometer meter registry for the cache.gets counter with the
     * given cache name and result tag value (hit or miss).
     * Supports both Counter and FunctionCounter meter types.
     */
    private long getCounterValue(String cacheName, String result) {
        return meterRegistry.find("cache.gets")
                .tag("cache", cacheName)
                .tag("result", result)
                .meters()
                .stream()
                .mapToLong(m -> {
                    if (m instanceof io.micrometer.core.instrument.Counter c) return (long) c.count();
                    if (m instanceof io.micrometer.core.instrument.FunctionCounter fc) return (long) fc.count();
                    return 0L;
                })
                .sum();
    }
}
