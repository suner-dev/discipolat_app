package com.discipolat.modules.workflow.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin API for configuring workflow rules.
 * Churches can customize escalation thresholds, notification templates,
 * and enable/disable individual workflow automations.
 */
@RestController
@RequestMapping("/api/v1/workflows")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
@RequiredArgsConstructor
public class WorkflowConfigController {

    private final AuditService auditService;

    // In-memory store; in production: database table workflow_configs
    private static final Map<UUID, List<Map<String, Object>>> configs = new LinkedHashMap<>();

    /**
     * Get all workflow configurations for the current tenant.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configs.getOrDefault(tenantId, getDefaultConfigs()));
    }

    /**
     * Get a specific workflow configuration.
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String key) {
        UUID tenantId = TenantContext.getTenantId();
        List<Map<String, Object>> tenantConfigs = configs.getOrDefault(tenantId, getDefaultConfigs());
        return tenantConfigs.stream()
                .filter(c -> key.equals(c.get("key")))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a workflow configuration.
     */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String key,
            @RequestBody Map<String, Object> update) {
        UUID tenantId = TenantContext.getTenantId();
        List<Map<String, Object>> tenantConfigs = configs.computeIfAbsent(tenantId, k -> getDefaultConfigs());

        for (Map<String, Object> config : tenantConfigs) {
            if (key.equals(config.get("key"))) {
                config.putAll(update);
                config.put("updatedAt", LocalDateTime.now().toString());
                auditService.logSimple("WORKFLOW_UPDATED", "WORKFLOW", null);
                return ResponseEntity.ok(config);
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle a workflow on/off.
     */
    @PostMapping("/{key}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable String key) {
        UUID tenantId = TenantContext.getTenantId();
        List<Map<String, Object>> tenantConfigs = configs.computeIfAbsent(tenantId, k -> getDefaultConfigs());

        for (Map<String, Object> config : tenantConfigs) {
            if (key.equals(config.get("key"))) {
                boolean enabled = Boolean.TRUE.equals(config.get("enabled"));
                config.put("enabled", !enabled);
                config.put("updatedAt", LocalDateTime.now().toString());
                auditService.logSimple("WORKFLOW_TOGGLED", "WORKFLOW", null);
                return ResponseEntity.ok(config);
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Default workflow configurations (same as the hardcoded ones in WorkflowService).
     */
    private List<Map<String, Object>> getDefaultConfigs() {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        list.add(makeConfig("ABSENCE_ESCALADE", "Escalade d'absentéisme",
                "Notifications automatiques when member is absent for X weeks",
                new java.util.LinkedHashMap<>(Map.of("semaines_faiseur", 3, "semaines_chef", 8, "semaines_pasteur", 12))));
        list.add(makeConfig("RAPPEL_ANNIVERSAIRE", "Rappels d'anniversaire",
                "Notifications on member birthday",
                new java.util.LinkedHashMap<>(Map.of("heure", "08:00", "canal", "PUSH"))));
        list.add(makeConfig("SNAPSHOT_SCORE_SPIRITUEL", "Snapshot hebdomadaire score spirituel",
                "Weekly spiritual score snapshot for all souls",
                new java.util.LinkedHashMap<>(Map.of("jour", "SUNDAY", "heure", "22:00"))));
        list.add(makeConfig("NOTIFICATION_ABSENCE", "Notification d'absence prolongée",
                "Alert when member hasn't been seen for X days",
                new java.util.LinkedHashMap<>(Map.of("jours_absence", 30, "canal", "EMAIL"))));
        list.add(makeConfig("RAPPEL_RAPPORT_HEBDOMADAIRE", "Rappel de soumission de rapport",
                "Remind makers to submit weekly reports",
                new java.util.LinkedHashMap<>(Map.of("jour_rappel", "WEDNESDAY", "heure", "18:00", "canal", "PUSH"))));
        return list;
    }

    private Map<String, Object> makeConfig(String key, String label, String description, Map<String, Object> rules) {
        Map<String, Object> config = new java.util.LinkedHashMap<>();
        config.put("key", key);
        config.put("label", label);
        config.put("description", description);
        config.put("enabled", true);
        config.put("rules", rules);
        config.put("createdAt", LocalDateTime.now().toString());
        return config;
    }
}
