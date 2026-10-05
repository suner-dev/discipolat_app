package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §5.3 &amp; §5.4 — modules/thème par nœud
 * (D) et arbre/agrégats drill-down (E).
 *
 * <p><b>Garde d'accès (F10).</b> {@code @authz.isTenantAdmin()} pour la
 * lecture ; l'écriture de modules/thème est réservée à l'admin du nœud
 * (P2, délégation) — borné ici à l'admin tenant, l'admin délégué par
 * nœud est branché en {@code isNodeAdmin} (T-B16).
 */
@RestController
@RequestMapping("/api/v1/tenant/organization")
@PreAuthorize("@authz.isTenantAdmin()")
public class OrganizationNodeV3Controller {

    private final OrganizationNodeRepository nodeRepository;
    private final OrganizationLevelRepository levelRepository;
    private final UserRepository userRepository;
    private final NodeAggregateService aggregateService;
    private final OrganizationNodeFeatureService nodeFeatureService;
    private final ConfigurationResolver configurationResolver;
    private final ObjectMapper objectMapper;

    public OrganizationNodeV3Controller(OrganizationNodeRepository nodeRepository,
                                        OrganizationLevelRepository levelRepository,
                                        UserRepository userRepository,
                                        NodeAggregateService aggregateService,
                                        OrganizationNodeFeatureService nodeFeatureService,
                                        ConfigurationResolver configurationResolver,
                                        ObjectMapper objectMapper) {
        this.nodeRepository = nodeRepository;
        this.levelRepository = levelRepository;
        this.userRepository = userRepository;
        this.aggregateService = aggregateService;
        this.nodeFeatureService = nodeFeatureService;
        this.configurationResolver = configurationResolver;
        this.objectMapper = objectMapper;
    }

    public record ModulePayload(String code, Boolean enabled, Map<String, Object> configurationJson) {
    }

    public record SetModulesRequest(List<ModulePayload> modules) {
    }

    // ================= §5.4 — arbre & drill-down =================

    @GetMapping("/tree")
    public ResponseEntity<List<Map<String, Object>>> tree() {
        UUID tenantId = TenantContext.requireTenantId();
        List<OrganizationNode> nodes = nodeRepository.findByTenantId(tenantId);
        nodes = new ArrayList<>(nodes);
        nodes.sort(Comparator.comparingInt(OrganizationNode::getLevel)
                .thenComparing(OrganizationNode::getName, Comparator.nullsLast(Comparator.naturalOrder())));
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrganizationNode n : nodes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", n.getId());
            row.put("parentId", n.getParentId());
            row.put("name", n.getName());
            row.put("type", n.getType().name());
            row.put("levelId", n.getLevelId());
            row.put("levelName", levelName(n.getLevelId(), n.getType()));
            row.put("responsibleId", n.getResponsibleId());
            row.put("responsibleName", responsibleName(n.getResponsibleId()));
            aggregateService.latest(tenantId, n.getId());
            out.add(row);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/nodes/{nodeId}/aggregate")
    public ResponseEntity<Map<String, Object>> aggregate(@PathVariable UUID nodeId,
                                                         @RequestParam(defaultValue = "false") boolean refresh) {
        UUID tenantId = TenantContext.requireTenantId();
        OrganizationNode node = requireNode(tenantId, nodeId);
        NodeAggregateSnapshot snap = refresh
                ? aggregateService.snapshot(tenantId, nodeId)
                : aggregateService.latest(tenantId, nodeId);
        Map<String, Object> view = aggregateService.toView(snap,
                levelName(node.getLevelId(), node.getType()), responsibleName(node.getResponsibleId()));
        view.put("progression", aggregateService.series(tenantId, nodeId).stream()
                .map(s -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("snapshotAt", s.getSnapshotAt());
                    p.put("memberCount", s.getMemberCount());
                    p.put("churchCount", s.getChurchCount());
                    p.put("leaderCount", s.getLeaderCount());
                    return p;
                }).toList());
        return ResponseEntity.ok(view);
    }

    @GetMapping("/nodes/{nodeId}/children")
    public ResponseEntity<List<Map<String, Object>>> children(@PathVariable UUID nodeId) {
        UUID tenantId = TenantContext.requireTenantId();
        requireNode(tenantId, nodeId);
        return ResponseEntity.ok(aggregateService.childrenWithAggregate(tenantId, nodeId));
    }

    // ================= §5.3 — modules & thème par nœud =================

    @GetMapping("/nodes/{nodeId}/features")
    public ResponseEntity<List<Map<String, Object>>> features(@PathVariable UUID nodeId) {
        UUID tenantId = TenantContext.requireTenantId();
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrganizationNodeFeature f : nodeFeatureService.listForNode(tenantId, nodeId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("moduleCode", f.getModuleCode());
            m.put("enabled", f.getEnabled());
            m.put("configurationJson", f.getConfigurationJson());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @PutMapping("/nodes/{nodeId}/features")
    public ResponseEntity<List<Map<String, Object>>> setFeatures(@PathVariable UUID nodeId,
                                                                 @RequestBody SetModulesRequest req) {
        UUID tenantId = TenantContext.requireTenantId();
        List<OrganizationNodeFeatureService.ModuleSelection> selections = new ArrayList<>();
        if (req.modules() != null) {
            for (ModulePayload p : req.modules()) {
                selections.add(new OrganizationNodeFeatureService.ModuleSelection(
                        p.code(), p.enabled(), p.configurationJson()));
            }
        }
        List<OrganizationNodeFeature> saved = nodeFeatureService.setModules(tenantId, nodeId, selections);
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrganizationNodeFeature f : saved) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("moduleCode", f.getModuleCode());
            m.put("enabled", f.getEnabled());
            m.put("configurationJson", f.getConfigurationJson());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/nodes/{nodeId}/theme")
    public ResponseEntity<Map<String, Object>> theme(@PathVariable UUID nodeId) {
        UUID tenantId = TenantContext.requireTenantId();
        OrganizationNode node = requireNode(tenantId, nodeId);
        Map<String, Object> body = new LinkedHashMap<>();
        if (node.getThemeJson() != null && !node.getThemeJson().isBlank()) {
            body.put("source", "OVERRIDDEN");
            body.put("theme", readMap(node.getThemeJson()));
        } else {
            // Indépendant par défaut (V3-D) : pas de thème local → résolution
            // de confort via le moteur d'héritage (jamais une contrainte).
            body.put("source", "DEFAULT");
            body.put("theme", configurationResolver.resolveConfig(tenantId, nodeId,
                    Set.of("branding", "colors", "fonts")));
        }
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/nodes/{nodeId}/theme")
    public ResponseEntity<Map<String, Object>> patchTheme(@PathVariable UUID nodeId,
                                                          @RequestBody Map<String, Object> theme) {
        UUID tenantId = TenantContext.requireTenantId();
        OrganizationNode node = requireNode(tenantId, nodeId);
        try {
            node.setThemeJson(objectMapper.writeValueAsString(theme == null ? Map.of() : theme));
        } catch (Exception e) {
            node.setThemeJson("{}");
        }
        node.setConfigSource(OrganizationNode.ConfigSource.OVERRIDDEN);
        nodeRepository.save(node);
        configurationResolver.invalidateResolvedConfig(tenantId, nodeId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source", "OVERRIDDEN");
        body.put("theme", theme == null ? Map.of() : theme);
        return ResponseEntity.ok(body);
    }

    // ================= helpers =================

    private OrganizationNode requireNode(UUID tenantId, UUID nodeId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException("OrganizationNode", nodeId));
        if (!node.getTenantId().equals(tenantId)) {
            throw new com.discipolat.common.domain.EntityNotFoundException("OrganizationNode", nodeId);
        }
        return node;
    }

    /** Libellé du niveau custom, sinon repli sur le type sémantique (rétrocompat). */
    private String levelName(UUID levelId, OrganizationNodeType type) {
        if (levelId != null) {
            Optional<OrganizationLevel> level = levelRepository.findById(levelId);
            if (level.isPresent()) return level.get().getName();
        }
        return type == null ? null : humanize(type.name());
    }

    private String responsibleName(UUID responsibleId) {
        if (responsibleId == null) return null;
        return userRepository.findById(responsibleId)
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim()).orElse(null);
    }

    private Map<String, Object> readMap(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = objectMapper.readValue(json, Map.class);
            return m;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String humanize(String enumName) {
        String s = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
