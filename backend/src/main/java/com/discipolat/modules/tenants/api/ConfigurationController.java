package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.ConfigurationResolver;
import com.discipolat.modules.tenants.domain.OrganizationNode.ConfigSource;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API de résolution de configuration avec héritage (G1.7 §53).
 * GET /api/config/resolved?unitId=&keys=
 * PUT /api/config/source/{nodeId}
 * PUT /api/config/local/{nodeId}
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigurationController {

    private final ConfigurationResolver resolver;
    private final OrganizationNodeRepository orgNodeRepository;
    private final AuditService auditService;

    public ConfigurationController(ConfigurationResolver resolver,
                                   OrganizationNodeRepository orgNodeRepository,
                                   AuditService auditService) {
        this.resolver = resolver;
        this.orgNodeRepository = orgNodeRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        return TenantContext.requireTenantId();
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    /**
     * Résout la configuration effective pour un nœud (avec héritage).
     * 
     * @param unitId ID du nœud organisationnel
     * @param keys Clés à résoudre (optionnel, par défaut toutes)
     * @return Configuration résolue
     */
    @GetMapping("/resolved")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getResolvedConfig(
            @RequestParam UUID unitId,
            @RequestParam(required = false) List<String> keys) {

        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        // Vérifier que l'utilisateur a accès à ce nœud
        OrganizationNode node = orgNodeRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Nœud non trouvé: " + unitId));

        if (!node.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès cross-tenant interdit"));
        }

        Set<String> keySet = keys != null ? keys.stream().collect(Collectors.toSet()) : null;
        Map<String, Object> resolved = resolver.resolveConfig(tenantId, unitId, keySet);

        return ResponseEntity.ok(Map.of(
                "unitId", unitId,
                "config", resolved,
                "source", node.getConfigSource().name()
        ));
    }

    /**
     * Change la source de configuration d'un nœud (DEFAULT | INHERITED | OVERRIDDEN).
     */
    @PutMapping("/source/{nodeId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> setConfigSource(
            @PathVariable UUID nodeId,
            @RequestBody Map<String, String> request) {

        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        String sourceStr = request.get("source");
        if (sourceStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "source requis"));
        }

        OrganizationNode.ConfigSource source;
        try {
            source = OrganizationNode.ConfigSource.valueOf(sourceStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Source invalide: " + sourceStr));
        }

        resolver.setConfigSource(tenantId, nodeId, source, currentUserId);

        auditService.log(currentUserId, tenantId, "CONFIG_SOURCE_CHANGED", "ORG_NODE",
                nodeId, "SUCCESS", Map.of("source", source.name()), null, null, null);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "nodeId", nodeId,
                "source", source.name()
        ));
    }

    /**
     * Met à jour la configuration locale d'un nœud (metadata_json).
     * Ne prend effet que si config_source = OVERRIDDEN.
     */
    @PutMapping("/local/{nodeId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateLocalConfig(
            @PathVariable UUID nodeId,
            @RequestBody Map<String, Object> config) {

        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        resolver.updateLocalConfig(tenantId, nodeId, config, currentUserId);

        auditService.log(currentUserId, tenantId, "LOCAL_CONFIG_UPDATED", "ORG_NODE",
                nodeId, "SUCCESS", Map.of("keys", config.keySet()), null, null, null);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "nodeId", nodeId,
                "updatedKeys", config.keySet()
        ));
    }

    /**
     * Récupère l'état d'héritage pour l'UI (bandeau « Hérité de : [parent] »).
     */
    @GetMapping("/inheritance/{nodeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getInheritanceStatus(@PathVariable UUID nodeId) {
        UUID tenantId = getCurrentTenantId();

        OrganizationNode node = orgNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Nœud non trouvé: " + nodeId));

        if (!node.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès cross-tenant interdit"));
        }

        // Construire la chaîne d'héritage pour l'affichage
        List<Map<String, Object>> chain = new java.util.ArrayList<>();
        OrganizationNode current = node;
        
        while (current != null) {
            Map<String, Object> info = Map.of(
                    "id", current.getId().toString(),
                    "name", current.getName(),
                    "type", current.getType().name(),
                    "configSource", current.getConfigSource().name(),
                    "isCurrent", current.getId().equals(nodeId)
            );
            chain.add(info);
            
            if (current.getParentId() != null) {
                current = orgNodeRepository.findById(current.getParentId()).orElse(null);
            } else {
                break;
            }
        }

        // Inverser pour avoir racine → feuille
        java.util.Collections.reverse(chain);

        return ResponseEntity.ok(Map.of(
                "unitId", nodeId,
                "currentSource", node.getConfigSource().name(),
                "inheritanceChain", chain
        ));
    }
}