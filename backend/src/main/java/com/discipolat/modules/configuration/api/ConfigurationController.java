package com.discipolat.modules.configuration.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.configuration.domain.ConfigurationResolver;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.audit.domain.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * G1.7 §53 — API de resolution de configuration heritee.
 * GET  /api/v1/config/resolved?unitId=&keys=
 * GET  /api/v1/config/resolved/origins?unitId=
 * PUT  /api/v1/config/source/{unitId}      (DEFAULT | INHERITED | OVERRIDDEN)
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigurationController {

    private final ConfigurationResolver resolver;
    private final OrganizationNodeRepository nodeRepository;
    private final AuditService auditService;
    private final HttpServletRequest httpServletRequest;

    public ConfigurationController(ConfigurationResolver resolver,
                                   OrganizationNodeRepository nodeRepository,
                                   AuditService auditService,
                                   HttpServletRequest httpServletRequest) {
        this.resolver = resolver;
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
        this.httpServletRequest = httpServletRequest;
    }

    private UUID tenantId() {
        return TenantContext.requireTenantId();
    }

    private UUID userId() {
        return TenantContext.getCurrentUserId();
    }

    /** Configuration resolue d'un noeud (toutes cles ou sous-ensemble). */
    @GetMapping("/resolved")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> resolved(
            @RequestParam UUID unitId,
            @RequestParam(required = false) String keys) {
        List<String> keyList = (keys == null || keys.isBlank())
                ? null
                : Arrays.stream(keys.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return ResponseEntity.ok(resolver.resolve(tenantId(), unitId, keyList));
    }

    /** Resolution avec origine (noeud fournisseur + heritage) pour le bandeau UI. */
    @GetMapping("/resolved/origins")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> resolvedWithOrigins(@RequestParam UUID unitId) {
        return ResponseEntity.ok(resolver.resolveWithOrigin(tenantId(), unitId));
    }

    /** Change la source de configuration d'un noeud (Personnaliser / Retablir heritage). */
    @PutMapping("/source/{unitId}")
    @PreAuthorize("@authz.can('ORG_NODE_UPDATE', 'TENANT', #unitId)")
    public ResponseEntity<Map<String, Object>> setConfigSource(
            @PathVariable UUID unitId,
            @RequestBody Map<String, String> body) {
        UUID tenantId = tenantId();
        UUID currentUserId = userId();
        String source = body.get("configSource");

        OrganizationNode node = nodeRepository.findById(unitId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Noeud introuvable ou hors tenant"));

        OrganizationNode.ConfigSource newSource;
        try {
            newSource = OrganizationNode.ConfigSource.valueOf(source == null ? "" : source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("configSource invalide : " + source + " (DEFAULT|INHERITED|OVERRIDDEN)");
        }

        OrganizationNode.ConfigSource old = node.getConfigSource();
        node.setConfigSource(newSource);
        nodeRepository.save(node);

        // Invalidation du cache de configuration (evenement config-changed G2.8).
        resolver.invalidate(tenantId, unitId);
        Map<String, Object> resolved = resolver.recalculateAndCache(tenantId, unitId);

        auditService.log(currentUserId, tenantId, "CONFIG_SOURCE_CHANGED",
                "ORGANIZATION_NODE", unitId, "SUCCESS",
                Map.of("oldSource", old != null ? old.name() : "DEFAULT", "newSource", newSource.name()),
                null, null, httpServletRequest);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unitId", unitId);
        result.put("configSource", newSource.name());
        result.put("resolvedConfig", resolved);
        return ResponseEntity.ok(result);
    }
}
