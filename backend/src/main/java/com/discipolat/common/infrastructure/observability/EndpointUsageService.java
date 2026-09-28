package com.discipolat.common.infrastructure.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Compteur d'usage des endpoints HTTP en mémoire (sans coût par requête sur la
 * base), vidangé périodiquement dans {@code endpoint_usage_daily}.
 *
 * <p>Objectif : objectiver le code mort — un endpoint jamais appelé depuis N
 * jours est un candidat sérieux à la retraite, contrairement aux intuitions.</p>
 */
@Service
public class EndpointUsageService {

    private static final Logger log = LoggerFactory.getLogger(EndpointUsageService.class);

    /** clé = "METHOD route-pattern" */
    private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> errorCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDate> bucketDay = new ConcurrentHashMap<>();

    private final EndpointUsageDailyRepository repository;

    public EndpointUsageService(EndpointUsageDailyRepository repository) {
        this.repository = repository;
    }

    public void record(String method, String route, boolean error) {
        String key = method + " " + route;
        counters.computeIfAbsent(key, k -> new LongAdder()).increment();
        if (error) {
            errorCounters.computeIfAbsent(key, k -> new LongAdder()).increment();
        }
        Instant now = Instant.now();
        lastSeen.merge(key, now, (a, b) -> b.isAfter(a) ? b : a);
        bucketDay.putIfAbsent(key, LocalDate.now());
    }

    /** Vu depuis le démarrage du processus (cheap, pour le rapport temps réel). */
    public Map<String, Long> liveCounters() {
        Map<String, Long> snapshot = new HashMap<>();
        counters.forEach((k, v) -> snapshot.put(k, v.sum()));
        return snapshot;
    }

    /**
     * Vidange planifiée des compteurs vers la base (toutes les 5 minutes).
     * Les compteurs sont swapped (copie + remise à zéro) pour éviter de
     * perdre des appels pendant l'écriture.
     */
    @Scheduled(fixedDelay = 300_000L, initialDelay = 60_000L)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void flush() {
        if (counters.isEmpty()) return;
        Map<String, Long> batch = new HashMap<>();
        counters.forEach((k, v) -> batch.put(k, v.sumThenReset()));
        Map<String, Long> errors = new HashMap<>();
        errorCounters.forEach((k, v) -> errors.put(k, v.sumThenReset()));

        int written = 0;
        for (Map.Entry<String, Long> entry : batch.entrySet()) {
            long calls = entry.getValue();
            if (calls <= 0) {
                continue;
            }
            String key = entry.getKey();
            int space = key.indexOf(' ');
            String method = space > 0 ? key.substring(0, space) : key;
            String route = space > 0 ? key.substring(space + 1) : "/";
            LocalDate day = bucketDay.getOrDefault(key, LocalDate.now());
            Instant seen = lastSeen.getOrDefault(key, Instant.now());
            try {
                repository.upsertAdd(day, method, truncate(route), calls,
                        errors.getOrDefault(key, 0L), seen);
                written++;
            } catch (RuntimeException e) {
                log.warn("[EndpointUsage] flush partiellement échoué: {}", e.getMessage());
            }
        }
        bucketDay.clear();
        if (written > 0) {
            log.debug("[EndpointUsage] {} routes vidangées", written);
        }
    }

    private String truncate(String route) {
        return route.length() > 255 ? route.substring(0, 255) : route;
    }
}
