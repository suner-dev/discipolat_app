package com.discipolat.modules.workflow.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.workflow.domain.WorkflowConfig;
import com.discipolat.modules.workflow.domain.WorkflowConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final WorkflowConfigRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Get all workflow configurations for the current tenant.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<WorkflowConfig> tenantConfigs = repository.findByTenantId(tenantId);
        if (tenantConfigs.isEmpty()) {
            tenantConfigs = seedDefaultConfigs(tenantId);
        }
        return ResponseEntity.ok(tenantConfigs.stream().map(this::toMap).collect(Collectors.toList()));
    }

    /**
     * Get a specific workflow configuration.
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String key) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdAndWorkflowKey(tenantId, key)
                .map(c -> ResponseEntity.ok(toMap(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a workflow configuration.
     */
    @PutMapping("/{key}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String key,
            @Valid @RequestBody WorkflowConfigUpdateRequest update) {
        UUID tenantId = TenantContext.getTenantId();
        WorkflowConfig config = repository.findByTenantIdAndWorkflowKey(tenantId, key).orElse(null);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        if (update.label() != null) config.setLabel(update.label());
        if (update.description() != null) config.setDescription(update.description());
        if (update.enabled() != null) config.setEnabled(update.enabled());
        if (update.rules() != null) config.setRules(serializeRules(update.rules()));
        config.setUpdatedAt(LocalDateTime.now());

        repository.save(config);
        auditService.logSimple("WORKFLOW_UPDATED", "WORKFLOW", null);
        return ResponseEntity.ok(toMap(config));
    }

    /**
     * Toggle a workflow on/off.
     */
    @PostMapping("/{key}/toggle")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable String key) {
        UUID tenantId = TenantContext.getTenantId();
        WorkflowConfig config = repository.findByTenantIdAndWorkflowKey(tenantId, key).orElse(null);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        config.setEnabled(!Boolean.TRUE.equals(config.getEnabled()));
        config.setUpdatedAt(LocalDateTime.now());

        repository.save(config);
        auditService.logSimple("WORKFLOW_TOGGLED", "WORKFLOW", null);
        return ResponseEntity.ok(toMap(config));
    }

    /**
     * Seed default workflow configurations for a tenant that has none yet.
     */
    @Transactional
    protected List<WorkflowConfig> seedDefaultConfigs(UUID tenantId) {
        List<WorkflowConfig> defaults = new ArrayList<>();
        defaults.add(makeConfig(tenantId, "ABSENCE_ESCALADE", "Escalade d'absentéisme",
                "Notifications automatiques when member is absent for X weeks",
                Map.of("semaines_faiseur", 3, "semaines_chef", 8, "semaines_pasteur", 12)));
        defaults.add(makeConfig(tenantId, "RAPPEL_ANNIVERSAIRE", "Rappels d'anniversaire",
                "Notifications on member birthday",
                Map.of("heure", "08:00", "canal", "PUSH")));
        defaults.add(makeConfig(tenantId, "SNAPSHOT_SCORE_SPIRITUEL", "Snapshot hebdomadaire score spirituel",
                "Weekly spiritual score snapshot for all souls",
                Map.of("jour", "SUNDAY", "heure", "22:00")));
        defaults.add(makeConfig(tenantId, "NOTIFICATION_ABSENCE", "Notification d'absence prolongée",
                "Alert when member hasn't been seen for X days",
                Map.of("jours_absence", 30, "canal", "EMAIL")));
        defaults.add(makeConfig(tenantId, "RAPPEL_RAPPORT_HEBDOMADAIRE", "Rappel de soumission de rapport",
                "Remind makers to submit weekly reports",
                Map.of("jour_rappel", "WEDNESDAY", "heure", "18:00", "canal", "PUSH")));
        return repository.saveAll(defaults);
    }

    private WorkflowConfig makeConfig(UUID tenantId, String key, String label, String description, Map<String, Object> rules) {
        WorkflowConfig config = new WorkflowConfig();
        config.setTenantId(tenantId);
        config.setWorkflowKey(key);
        config.setLabel(label);
        config.setDescription(description);
        config.setEnabled(true);
        config.setRules(serializeRules(rules));
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private Map<String, Object> toMap(WorkflowConfig c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", c.getWorkflowKey());
        map.put("label", c.getLabel());
        map.put("description", c.getDescription());
        map.put("enabled", c.getEnabled());
        map.put("rules", deserializeRules(c.getRules()));
        map.put("createdAt", c.getCreatedAt().toString());
        map.put("updatedAt", c.getUpdatedAt().toString());
        return map;
    }

    private String serializeRules(Map<String, Object> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeRules(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
