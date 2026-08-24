package com.discipolat.modules.usageAnalytics.domain;

import com.discipolat.modules.usageAnalytics.domain.UsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P3 #109 — Analytics d'usage self-hosted (alternative à Plausible).
 * Pages vues, temps moyen, utilisateurs actifs, top pages, funnel de navigation.
 */
@Service
@Transactional
public class UsageAnalyticsService {

    private final UsageEventRepository repository;

    public UsageAnalyticsService(UsageEventRepository repository) {
        this.repository = repository;
    }

    public void track(UUID tenantId, UUID userId, List<UsageEvent> events) {
        if (events == null) return;
        for (UsageEvent e : events) {
            e.setId(null);
            e.setTenantId(tenantId);
            if (e.getUserId() == null) e.setUserId(userId);
            e.setCreatedAt(LocalDateTime.now());
        }
        repository.saveAll(events);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(int days) {
        UUID tenantId = com.discipolat.common.multitenancy.TenantContext.getCurrentTenantId();
        LocalDateTime from = LocalDate.now().minusDays(Math.max(1, Math.min(days, 90))).atStartOfDay();
        List<UsageEvent> events =
                repository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(tenantId, from, LocalDateTime.now());

        long pageViews = events.stream().filter(e -> "PAGE_VIEW".equals(e.getAction())).count();
        Set<UUID> uniqueUsers = events.stream().map(UsageEvent::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        double avgDurationMs = events.stream().map(UsageEvent::getDurationMs)
                .filter(Objects::nonNull).mapToLong(Long::longValue).average().orElse(0.0);

        Map<String, Long> topPages = events.stream()
                .filter(e -> "PAGE_VIEW".equals(e.getAction()))
                .collect(Collectors.groupingBy(UsageEvent::getPage, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> daily = events.stream()
                .filter(e -> "PAGE_VIEW".equals(e.getAction()))
                .collect(Collectors.groupingBy(e -> e.getCreatedAt().toLocalDate().toString(),
                        TreeMap::new, Collectors.counting()));

        Map<String, Long> devices = events.stream()
                .collect(Collectors.groupingBy(e -> e.getDevice() == null ? "WEB" : e.getDevice(), Collectors.counting()));

        // Funnel de navigation : séquences de 3 pages les plus fréquentes
        Map<String, Long> funnels = new LinkedHashMap<>();
        List<String> sequence = new ArrayList<>();
        for (UsageEvent e : events) {
            if ("PAGE_VIEW".equals(e.getAction())) {
                sequence.add(e.getPage());
                if (sequence.size() > 3) sequence.remove(0);
                if (sequence.size() == 3) funnels.merge(String.join(" → ", sequence), 1L, Long::sum);
            }
        }
        Map<String, Long> topFunnels = funnels.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodeJours", days);
        result.put("totalEvenements", events.size());
        result.put("pagesVues", pageViews);
        result.put("utilisateursUniques", uniqueUsers.size());
        result.put("dureeMoyenneSec", Math.round(avgDurationMs / 100.0) / 10.0);
        result.put("topPages", topPages);
        result.put("vuesParJour", daily);
        result.put("parAppareil", devices);
        result.put("topFunnels", topFunnels);
        return result;
    }
}
