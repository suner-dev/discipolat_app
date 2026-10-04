package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.JoinCodeService;
import com.discipolat.modules.tenants.domain.JoinMode;
import com.discipolat.modules.tenants.domain.TenantJoinCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-3) — gestion des codes de rejointure par
 * l'admin du tenant (église principale et sous-églises D3).
 * Réservé {@code TENANT_OWNER} / {@code TENANT_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/tenant/join-codes")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
public class TenantJoinCodeController {

    private final JoinCodeService joinCodeService;
    private final AuditService auditService;

    public TenantJoinCodeController(JoinCodeService joinCodeService, AuditService auditService) {
        this.joinCodeService = joinCodeService;
        this.auditService = auditService;
    }

    public record CreateJoinCodeRequest(UUID orgNodeId, String label, String joinMode) {
    }

    public record UpdateJoinCodeRequest(String label, String joinMode, Boolean isActive) {
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Map<String, Object>> body = joinCodeService.listForTenant(tenantId).stream()
                .map(this::toView)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateJoinCodeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getCurrentUserId();
        JoinMode mode = parseMode(request.joinMode());
        TenantJoinCode code = joinCodeService.generate(
                tenantId, request.orgNodeId(), normalizeLabel(request.label()), mode, userId);
        auditService.logSimple("JOIN_CODE_CREATED", "TENANT_JOIN_CODE", code.getId());
        return ResponseEntity.status(201).body(toView(code));
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<Map<String, Object>> rotate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getCurrentUserId();
        TenantJoinCode fresh = joinCodeService.rotate(id, tenantId, userId);
        auditService.logSimple("JOIN_CODE_ROTATED", "TENANT_JOIN_CODE", fresh.getId());
        return ResponseEntity.ok(toView(fresh));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody UpdateJoinCodeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantJoinCode updated = joinCodeService.update(id, tenantId,
                normalizeLabel(request.label()),
                request.joinMode() == null ? null : parseMode(request.joinMode()),
                request.isActive());
        auditService.logSimple("JOIN_CODE_UPDATED", "TENANT_JOIN_CODE", updated.getId());
        return ResponseEntity.ok(toView(updated));
    }

    /** Suppression douce : le code devient injoignable, l'historique reste. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        joinCodeService.deactivate(id, tenantId);
        auditService.logSimple("JOIN_CODE_DEACTIVATED", "TENANT_JOIN_CODE", id);
        return ResponseEntity.noContent().build();
    }

    private JoinMode parseMode(String raw) {
        if (raw == null || raw.isBlank()) return JoinMode.OPEN;
        try {
            return JoinMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("joinMode invalide : " + raw);
        }
    }

    private String normalizeLabel(String label) {
        return label == null || label.isBlank() ? null : label.trim();
    }

    private Map<String, Object> toView(TenantJoinCode code) {
        Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("id", code.getId());
        view.put("code", code.getCode());
        view.put("label", code.getLabel());
        view.put("joinMode", code.getJoinMode().name());
        view.put("isActive", code.isActive());
        view.put("orgNodeId", code.getOrgNodeId());
        view.put("createdAt", code.getCreatedAt());
        view.put("rotatedAt", code.getRotatedAt());
        return view;
    }
}
