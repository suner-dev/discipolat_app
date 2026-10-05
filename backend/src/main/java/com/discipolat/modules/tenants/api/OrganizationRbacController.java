package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.MemberRoleAssignment;
import com.discipolat.modules.tenants.domain.MemberRoleAssignmentService;
import com.discipolat.modules.tenants.domain.RoleTitle;
import com.discipolat.modules.tenants.domain.RoleTitleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §5.2 — intitulés de rôles (B) et
 * affiliations membre×rôle×nœud (C).
 *
 * <p><b>Garde d'accès (F10).</b> Lecture/écriture scopées tenant via
 * {@code @authz.isTenantAdmin()} lisant {@code tenant_memberships} —
 * JAMAIS le claim {@code role} du JWT.
 */
@RestController
@RequestMapping("/api/v1/tenant")
@PreAuthorize("@authz.isTenantAdmin()")
public class OrganizationRbacController {

    private final RoleTitleService titleService;
    private final MemberRoleAssignmentService assignmentService;

    public OrganizationRbacController(RoleTitleService titleService,
                                      MemberRoleAssignmentService assignmentService) {
        this.titleService = titleService;
        this.assignmentService = assignmentService;
    }

    public record UpsertTitleRequest(UUID nodeId, String label, String labelPlural) {
    }

    public record CreateAssignmentRequest(UUID roleId, UUID nodeId) {
    }

    // ---------- Intitulés (B) ----------

    @GetMapping("/roles/{roleId}/titles")
    public ResponseEntity<List<Map<String, Object>>> titles(@PathVariable UUID roleId,
                                                            @RequestParam(required = false) UUID nodeId) {
        UUID tenantId = TenantContext.requireTenantId();
        List<Map<String, Object>> out = titleService.listForRole(tenantId, roleId).stream()
                .map(t -> toTitleMap(t, tenantId, roleId))
                .toList();
        // Ajout du libellé RÉSOLU pour le nœud demandé (fallback tenant → global).
        Map<String, Object> resolved = new LinkedHashMap<>();
        resolved.put("resolved", true);
        resolved.put("nodeId", nodeId);
        resolved.put("effectiveLabel", titleService.getEffectiveLabel(tenantId, roleId, nodeId));
        java.util.List<Map<String, Object>> withResolved = new java.util.ArrayList<>(out);
        withResolved.add(resolved);
        return ResponseEntity.ok(withResolved);
    }

    @PutMapping("/roles/{roleId}/titles")
    public ResponseEntity<Map<String, Object>> upsertTitle(@PathVariable UUID roleId,
                                                           @RequestBody UpsertTitleRequest req) {
        UUID tenantId = TenantContext.requireTenantId();
        RoleTitle title = titleService.upsert(tenantId, roleId, req.nodeId(), req.label(), req.labelPlural());
        return ResponseEntity.ok(toTitleMap(title, tenantId, roleId));
    }

    // ---------- Assignations (C) ----------

    @GetMapping("/members/{userId}/assignments")
    public ResponseEntity<List<Map<String, Object>>> assignments(@PathVariable UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(assignmentService.listForUser(tenantId, userId).stream()
                .map(this::toAssignmentMap).toList());
    }

    @PostMapping("/members/{userId}/assignments")
    public ResponseEntity<Map<String, Object>> assign(@PathVariable UUID userId,
                                                      @RequestBody CreateAssignmentRequest req) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        MemberRoleAssignment a = assignmentService.assign(tenantId, userId, req.roleId(), req.nodeId(), actor);
        return ResponseEntity.status(201).body(toAssignmentMap(a));
    }

    @DeleteMapping("/members/{userId}/assignments/{assignmentId}")
    public ResponseEntity<Void> endAssignment(@PathVariable UUID userId, @PathVariable UUID assignmentId) {
        UUID tenantId = TenantContext.requireTenantId();
        assignmentService.end(tenantId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    // ---------- mapping ----------

    private Map<String, Object> toTitleMap(RoleTitle t, UUID tenantId, UUID roleId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("tenantId", tenantId);
        m.put("roleId", roleId);
        m.put("nodeId", t.getNodeId());
        m.put("label", t.getLabel());
        m.put("labelPlural", t.getLabelPlural());
        return m;
    }

    private Map<String, Object> toAssignmentMap(MemberRoleAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("tenantId", a.getTenantId());
        m.put("userId", a.getUserId());
        m.put("roleId", a.getRoleId());
        m.put("nodeId", a.getNodeId());
        m.put("status", a.getStatus() == null ? null : a.getStatus().name());
        m.put("assignedAt", a.getAssignedAt());
        m.put("endedAt", a.getEndedAt());
        return m;
    }
}
