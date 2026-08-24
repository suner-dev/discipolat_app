package com.discipolat.modules.integrations.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.integrations.domain.IntegrationConnectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API des connecteurs tiers (feature #3).
 * Complète /admin/integrations (config système) avec les connecteurs
 * métier par église : Zapier, Make, agendas, comptabilité.
 */
@RestController
@RequestMapping("/api/v1/connectors")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class ConnectorController {

    private final IntegrationConnectorService service;

    public ConnectorController(IntegrationConnectorService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(service.list(TenantContext.requireTenantId()));
    }

    public record SaveConnectorRequest(
            String connector,
            boolean enabled,
            String endpointUrl,
            String apiKey,
            String icalUrl) {}

    @PutMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody SaveConnectorRequest request) {
        return ResponseEntity.ok(service.save(TenantContext.requireTenantId(),
                request.connector(), request.enabled(), request.endpointUrl(),
                request.apiKey(), request.icalUrl()));
    }

    @PostMapping("/{connector}/test")
    public ResponseEntity<Map<String, Object>> test(@PathVariable String connector) {
        return ResponseEntity.ok(service.test(TenantContext.requireTenantId(), connector));
    }

    @PostMapping("/{connector}/sync")
    public ResponseEntity<Map<String, Object>> sync(@PathVariable String connector) {
        return ResponseEntity.ok(service.syncCalendar(TenantContext.requireTenantId(), connector));
    }

    /** Export des transactions financières vers QuickBooks/Xero. */
    @PostMapping("/finance-export")
    public ResponseEntity<Map<String, Object>> financeExport(@RequestBody List<Map<String, Object>> transactions) {
        return ResponseEntity.ok(service.pushFinanceExport(TenantContext.requireTenantId(), transactions));
    }
}
