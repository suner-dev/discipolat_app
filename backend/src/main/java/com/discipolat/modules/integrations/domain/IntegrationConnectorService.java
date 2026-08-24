package com.discipolat.modules.integrations.domain;

import com.discipolat.common.infrastructure.security.CryptoService;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.webhooks.domain.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Connecteurs tiers natifs (feature #3) :
 * - Zapier / Make : transfert des événements métier vers un webhook sortant
 * - Google Calendar / Outlook : import iCal → événements
 * - QuickBooks / Xero : export des transactions financières (payload JSON standard)
 */
@Service
@Transactional
public class IntegrationConnectorService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationConnectorService.class);
    private static final Pattern ICAL_EVENT = Pattern.compile(
            "BEGIN:VEVENT(.*?)END:VEVENT", Pattern.DOTALL);
    private static final Pattern ICAL_SUMMARY = Pattern.compile("SUMMARY[^:]*:(.*)");
    private static final Pattern ICAL_DTSTART = Pattern.compile("DTSTART[^:]*:(.*)");

    private final IntegrationConfigRepository repository;
    private final CryptoService cryptoService;
    private final WebhookService webhookService;
    private final RestClient restClient;

    public IntegrationConnectorService(IntegrationConfigRepository repository,
                                       CryptoService cryptoService,
                                       WebhookService webhookService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.webhookService = webhookService;
        this.restClient = RestClient.create();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID tenantId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (IntegrationConfig.Connector c : IntegrationConfig.Connector.values()) {
            var cfg = repository.findByTenantIdAndConnector(tenantId, c).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("connector", c.name());
            m.put("enabled", cfg != null && cfg.isEnabled());
            m.put("configured", cfg != null);
            m.put("endpointUrl", cfg == null ? null : cfg.getEndpointUrl());
            m.put("icalUrl", cfg == null ? null : cfg.getIcalUrl());
            m.put("hasApiKey", cfg != null && cfg.getApiKeyEncrypted() != null);
            m.put("lastSyncAt", cfg == null ? null : cfg.getLastSyncAt());
            m.put("lastSyncStatus", cfg == null ? null : cfg.getLastSyncStatus());
            result.add(m);
            seen.add(c.name());
        }
        return result;
    }

    public Map<String, Object> save(UUID tenantId, String connectorName, boolean enabled,
                                    String endpointUrl, String apiKey, String icalUrl) {
        IntegrationConfig.Connector connector = IntegrationConfig.Connector.valueOf(connectorName.toUpperCase(Locale.ROOT));
        IntegrationConfig cfg = repository.findByTenantIdAndConnector(tenantId, connector)
                .orElseGet(() -> {
                    IntegrationConfig c = new IntegrationConfig();
                    c.setTenantId(tenantId);

                    c.setConnector(connector);
                    return c;
                });
        cfg.setEnabled(enabled);
        if (endpointUrl != null) cfg.setEndpointUrl(endpointUrl);
        if (icalUrl != null) cfg.setIcalUrl(icalUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            cfg.setApiKeyEncrypted(cryptoService.encrypt(apiKey));
        }
        repository.save(cfg);
        return Map.of("connector", connector.name(), "enabled", cfg.isEnabled());
    }

    /** Teste le connecteur : ping du webhook ou fetch iCal. */
    public Map<String, Object> test(UUID tenantId, String connectorName) {
        IntegrationConfig.Connector connector = IntegrationConfig.Connector.valueOf(connectorName.toUpperCase(Locale.ROOT));
        IntegrationConfig cfg = repository.findByTenantIdAndConnector(tenantId, connector)
                .orElseThrow(() -> new IllegalStateException("Connecteur non configuré"));
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            switch (connector) {
                case ZAPIER, MAKE -> {
                    int notified = webhookService.fire("integration.test",
                            Map.of("connector", connector.name(), "message", "Test depuis Discipolat"));
                    result.put("success", true);
                    result.put("detail", notified + " webhook(s) notifié(s)");
                }
                case GOOGLE_CALENDAR, OUTLOOK_CALENDAR -> {
                    String ics = restClient.get().uri(cfg.getIcalUrl()).retrieve().body(String.class);
                    int events = countIcalEvents(ics);
                    result.put("success", true);
                    result.put("detail", events + " événement(s) détecté(s) dans l'agenda");
                }
                default -> {
                    // QUICKBOOKS/XERO — ping de l'endpoint configuré
                    if (cfg.getEndpointUrl() == null || cfg.getEndpointUrl().isBlank()) {
                        result.put("success", false);
                        result.put("error", "URL d'endpoint manquante");
                        return result;
                    }
                    restClient.get().uri(cfg.getEndpointUrl()).retrieve().toEntity(String.class);
                    result.put("success", true);
                    result.put("detail", "Endpoint joignable");
                }
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Synchronise les événements d'un agenda externe (iCal) et les diffuse
     * en interne via les webhooks (les modules consommateurs créent les Event).
     */
    public Map<String, Object> syncCalendar(UUID tenantId, String connectorName) {
        IntegrationConfig.Connector connector = IntegrationConfig.Connector.valueOf(connectorName.toUpperCase(Locale.ROOT));
        IntegrationConfig cfg = repository.findByTenantIdAndConnector(tenantId, connector)
                .orElseThrow(() -> new IllegalStateException("Connecteur non configuré"));
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String ics = restClient.get().uri(cfg.getIcalUrl()).retrieve().body(String.class);
            List<Map<String, Object>> events = parseIcalEvents(ics);
            for (Map<String, Object> ev : events) {
                webhookService.fire("EXTERNAL_CALENDAR.EVENT", ev);
            }
            cfg.setLastSyncAt(LocalDateTime.now());
            cfg.setLastSyncStatus("SUCCESS");
            repository.save(cfg);
            result.put("success", true);
            result.put("syncedEvents", events.size());
        } catch (Exception e) {
            cfg.setLastSyncAt(LocalDateTime.now());
            cfg.setLastSyncStatus("FAILED");
            repository.save(cfg);
            log.warn("Sync calendrier échouée : {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /** Exporte les transactions financières vers QuickBooks/Xero. */
    public Map<String, Object> pushFinanceExport(UUID tenantId, List<Map<String, Object>> transactions) {
        var quickbooks = repository.findByTenantIdAndConnector(tenantId, IntegrationConfig.Connector.QUICKBOOKS);
        var xero = repository.findByTenantIdAndConnector(tenantId, IntegrationConfig.Connector.XERO);
        Map<String, Object> result = new LinkedHashMap<>();
        int pushed = 0;
        for (var targetOpt : List.of(quickbooks, xero)) {
            if (targetOpt.isEmpty() || !targetOpt.get().isEnabled()) continue;
            IntegrationConfig target = targetOpt.get();
            try {
                String apiKey = cryptoService.decrypt(target.getApiKeyEncrypted());
                restClient.post()
                        .uri(target.getEndpointUrl())
                        .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                        .header("X-Tenant-Id", tenantId.toString())
                        .body(transactions)
                        .retrieve()
                        .toBodilessEntity();
                pushed += transactions.size();
                target.setLastSyncAt(LocalDateTime.now());
                target.setLastSyncStatus("SUCCESS");
                repository.save(target);
            } catch (Exception e) {
                target.setLastSyncStatus("FAILED");
                repository.save(target);
                result.put(target.getConnector().name() + "_error", e.getMessage());
            }
        }
        result.put("pushedTransactions", pushed);
        return result;
    }

    // ── Parsing iCal minimal ─────────────────────────────────────

    private int countIcalEvents(String ics) {
        if (ics == null) return 0;
        Matcher matcher = ICAL_EVENT.matcher(ics);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private List<Map<String, Object>> parseIcalEvents(String ics) {
        List<Map<String, Object>> events = new ArrayList<>();
        if (ics == null) return events;
        Matcher matcher = ICAL_EVENT.matcher(ics);
        while (matcher.find()) {
            String block = matcher.group(1);
            Map<String, Object> ev = new HashMap<>();
            extractField(block, ICAL_SUMMARY).ifPresent(v -> ev.put("titre", v.trim()));
            extractField(block, ICAL_DTSTART).map(this::parseIcalDate).ifPresent(v -> ev.put("dateDebut", v));
            if (!ev.isEmpty()) events.add(ev);
        }
        return events;
    }

    private Optional<String> extractField(String block, Pattern pattern) {
        Matcher m = pattern.matcher(block);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private String parseIcalDate(String raw) {
        // Format iCal : 20260824T180000Z
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() < 12) return raw;
        return "%s-%s-%sT%s:%s:%s".formatted(
                digits.substring(0, 4), digits.substring(4, 6), digits.substring(6, 8),
                digits.substring(8, 10), digits.substring(10, 12),
                digits.length() >= 14 ? digits.substring(12, 14) : "00");
    }
}
