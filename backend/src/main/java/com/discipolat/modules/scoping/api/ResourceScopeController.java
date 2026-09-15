package com.discipolat.modules.scoping.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.scoping.domain.ResourceScope;
import com.discipolat.modules.scoping.domain.ResourceScopeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * G1.8 §54 — API pour le selecteur de portee et le filtre GLOBAL/LOCAL.
 * GET /api/v1/scoping/scopes       : valeurs pour le selecteur UI (badge globe / pin)
 * GET /api/v1/scoping/visible-units : unites visibles pour l'utilisateur courant
 */
@RestController
@RequestMapping("/api/v1/scoping")
@PreAuthorize("isAuthenticated()")
public class ResourceScopeController {

    private final ResourceScopeService scopeService;

    public ResourceScopeController(ResourceScopeService scopeService) {
        this.scopeService = scopeService;
    }

    @GetMapping("/scopes")
    public ResponseEntity<List<Map<String, Object>>> scopes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ResourceScope scope : ResourceScope.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", scope.name());
            m.put("label", switch (scope) {
                case TENANT_GLOBAL -> "Global (toute l'eglise)";
                case ORGANIZATION_LOCAL -> "Local (unite et descendants)";
                case UNIT_LOCAL -> "Local strict (unite seule)";
            });
            m.put("badge", scope.isGlobal() ? "GLOBAL" : "LOCAL");
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/visible-units")
    public ResponseEntity<Map<String, Object>> visibleUnits(@RequestParam(required = false) UUID viewerUnitId) {
        UUID tenantId = TenantContext.requireTenantId();
        Set<UUID> visible = scopeService.visibleUnitIds(tenantId, viewerUnitId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantWide", visible == null);
        result.put("unitIds", visible == null ? List.of() : visible);
        return ResponseEntity.ok(result);
    }
}
