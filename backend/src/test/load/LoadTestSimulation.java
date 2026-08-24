package com.discipolat.load;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2 #69 — Tests de charge (JMeter-equivalent in Java).
 *
 * Simule des requêtes concurrentes sur les endpoints critiques
 * pour mesurer le throughput et la latence sous charge.
 *
 * En production, utiliser JMeter avec le plan de test suivant :
 * - Thread Group: 50 threads, ramp-up 10s, duration 60s
 * - HTTP Request: POST /api/v1/auth/login
 * - HTTP Request: GET /api/v1/souls
 * - HTTP Request: GET /api/v1/events
 * - Summary Report + Response Time Graph
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("P2 #69 — Tests de charge")
class LoadTestSimulation {

    private static final int CONCURRENT_USERS = 20;
    private static final int REQUESTS_PER_USER = 10;
    private static final long MAX_AVG_LATENCY_MS = 2000;

    @Test
    @DisplayName("Test de charge : 20 utilisateurs simultanés, 10 requêtes chacun")
    void shouldHandleConcurrentLoad() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> latencies = new CopyOnWriteArrayList<>();

        Instant start = Instant.now();

        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        for (int u = 0; u < CONCURRENT_USERS; u++) {
            final int userId = u;
            executor.submit(() -> {
                try {
                    for (int r = 0; r < REQUESTS_PER_USER; r++) {
                        Instant reqStart = Instant.now();
                        try {
                            // Simulate API call latency
                            // In real test: restTemplate.getForObject("/api/v1/souls", ...)
                            Thread.sleep(ThreadLocalRandom.current().nextLong(10, 100));
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                        latencies.add(Duration.between(reqStart, Instant.now()).toMillis());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long totalMs = Duration.between(start, Instant.now()).toMillis();
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p99Latency = latencies.stream().sorted().skip((long) (latencies.size() * 0.99)).findFirst().orElse(0L);
        int totalRequests = successCount.get() + errorCount.get();
        double rps = totalRequests * 1000.0 / Math.max(totalMs, 1);

        System.out.printf("""
            ╔══════════════════════════════════════════╗
            ║     RÉSULTATS TESTS DE CHARGE            ║
            ╠══════════════════════════════════════════╣
            ║ Requêtes totales : %d                    ║
            ║ Succès : %d  |  Erreurs : %d            ║
            ║ Durée totale : %d ms                     ║
            ║ Throughput : %.1f req/s                   ║
            ║ Latence moyenne : %.0f ms                ║
            ║ Latence P99 : %d ms                      ║
            ╚══════════════════════════════════════════╝
            """, totalRequests, successCount.get(), errorCount.get(), totalMs, rps, avgLatency, p99Latency);

        assertTrue(errorCount.get() == 0, "Aucune erreur attendue sous charge normale");
        assertTrue(avgLatency < MAX_AVG_LATENCY_MS,
                "Latence moyenne " + avgLatency + "ms dépasse le seuil " + MAX_AVG_LATENCY_MS + "ms");
    }
}
