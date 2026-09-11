package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Gestion complète de la hiérarchie organisationnelle
 * Sections 16-20, 25-26 du prompt maître
 * Types : REGION, CHURCH, SUB_CHURCH, CAMPUS, ASSEMBLY, DEPARTMENT, GROUP
 */
@RestController
@RequestMapping("/api/v1/admin/org")
public class OrganizationManagementController {

    private final OrganizationNodeService orgNodeService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public OrganizationManagementController(
            OrganizationNodeService orgNodeService,
            UserRepository userRepository,
            AuditService auditService) {
        this.orgNodeService = orgNodeService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant");
        return tenantId;
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    // ==================== ARBRE COMPLET ====================

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationTreeResponse> getTree() {
        UUID tenantId = getCurrentTenantId();
        Optional<OrganizationNode> root = orgNodeService.findRootByTenantId(tenantId);
        
        if (root.isEmpty()) {
            return ResponseEntity.ok(new OrganizationTreeResponse(
                    null, Collections.emptyList(), Collections.emptyMap()
            ));
        }

        List<OrganizationNode> allNodes = orgNodeService.findByTenantId(tenantId);
        Map<UUID, List<OrganizationNode>> childrenByParent = new HashMap<>();
        
        allNodes.forEach(node -> {
            UUID parentId = node.getParentId() != null ? node.getParentId() : root.get().getId();
            childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
        });

        return ResponseEntity.ok(new OrganizationTreeResponse(
                root.get(),
                allNodes,
                childrenByParent
        ));
    }

    // ==================== NOEUDS PAR TYPE ====================

    @GetMapping("/nodes/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNodeResponse>> getNodesByType(
            @PathVariable OrganizationNodeType type) {
        UUID tenantId = getCurrentTenantId();
        List<OrganizationNode> nodes = orgNodeService.findByTenantIdAndType(tenantId, type);
        
        return ResponseEntity.ok(nodes.stream()
                .map(this::toResponse)
                .toList());
    }

    // ==================== CRÉER UN NOEUD ====================

    @PostMapping("/nodes")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'REGION_ADMIN')")
    public ResponseEntity<OrganizationNodeResponse> createNode(@RequestBody CreateNodeRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        // Vérifier le parent si fourni
        OrganizationNode parent = null;
        String pathPrefix = "" + tenantId + ":";
        
        if (request.parentId() != null) {
            parent = orgNodeService.findById(request.parentId())
                    .orElseThrow(() -> new RuntimeException("Parent non trouvé"));
            
            if (!parent.getTenantId().equals(tenantId)) {
                throw new SecurityException("Le parent ne appartient pas à ce tenant");
            }
            pathPrefix = parent.getPath();
            
            // Vérifier permissions selon le type
            if (request.type() == OrganizationNodeType.REGION && 
                !hasRole(currentUserId, tenantId, "REGION_ADMIN")) {
                throw new SecurityException("Permission requise: REGION_ADMIN");
            }
        }

        // Générer un code unique
        String code = generateCode(request.type());
        String path = pathPrefix + code + ":";

        // Vérifier code unique
        if (orgNodeService.findByTenantIdAndCode(tenantId, code).isPresent()) {
            throw new RuntimeException("Ce code est déjà utilisé");
        }

        OrganizationNode node = OrganizationNode.builder()
                .tenantId(tenantId)
                .parentId(request.parentId())
                .name(request.name())
                .type(request.type())
                .code(code)
                .slug(generateSlug(request.name()))
                .path(path)
                .status(OrganizationNodeStatus.ACTIVE)
                .country(request.country())
                .city(request.city())
                .timezone(request.timezone() != null ? request.timezone() : 
                    parent != null ? parent.getTimezone() : "Africa/Douala")
                .metadata(request.metadata())
                .responsibleId(request.responsibleId())
                .build();

        node = orgNodeService.save(node);

        auditService.log(currentUserId, tenantId, "ORG_NODE_CREATED", "ORGANIZATION",
                node.getId(), "SUCCESS",
                Map.of(
                        "name", request.name(),
                        "type", request.type().name(),
                        "parentId", request.parentId() != null ? request.parentId().toString() : "ROOT",
                        "code", code
                ),
                null, null, null);

        return ResponseEntity.status(201).body(toResponse(node));
    }

    // ==================== METTRE À JOUR UN NOEUD ====================

    @PutMapping("/nodes/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'CHURCH_ADMIN', " +
            "'SUB_CHURCH_ADMIN', 'CAMPUS_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<OrganizationNodeResponse> updateNode(
            @PathVariable UUID id,
            @RequestBody UpdateNodeRequest request) {
        
        OrganizationNode node = orgNodeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Noeud non trouvé"));
        
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Noeud ne appartient pas à ce tenant");
        }

        if (request.name() != null) node.setName(request.name());
        if (request.slug() != null) node.setSlug(request.slug());
        if (request.type() != null) node.setType(request.type());
        if (request.code() != null) node.setCode(request.code());
        if (request.status() != null) node.setStatus(request.status());
        if (request.country() != null) node.setCountry(request.country());
        if (request.city() != null) node.setCity(request.city());
        if (request.timezone() != null) node.setTimezone(request.timezone());
        if (request.metadata() != null) node.setMetadata(request.metadata());
        if (request.responsibleId() != null) node.setResponsibleId(request.responsibleId());

        node = orgNodeService.save(node);

        auditService.log(currentUserId, tenantId, "ORG_NODE_UPDATED", "ORGANIZATION",
                node.getId(), "SUCCESS",
                Map.of("name", node.getName(), "type", node.getType().name()),
                null, null, null);

        return ResponseEntity.ok(toResponse(node));
    }

    // ==================== DÉPLACER UN NOEUD ====================

    @PostMapping("/nodes/{id}/move")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<OrganizationNodeResponse> moveNode(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> request) {
        
        OrganizationNode node = orgNodeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Noeud non trouvé"));
        
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Noeud ne appartient pas à ce tenant");
        }

        if (node.getType() == OrganizationNodeType.ROOT_CHURCH) {
            throw new RuntimeException("Impossible de déplacer la racine");
        }

        UUID newParentId = request.get("parentId");
        
        if (newParentId != null) {
            OrganizationNode newParent = orgNodeService.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("Nouveau parent non trouvé"));
            
            if (!newParent.getTenantId().equals(tenantId)) {
                throw new SecurityException("Nouveau parent ne appartient pas à ce tenant");
            }
            
            // Vérifier pas de cycle
            List<OrganizationNode> descendants = orgNodeService.getDescendants(tenantId, newParentId);
            if (descendants.stream().anyMatch(d -> d.getId().equals(id))) {
                throw new RuntimeException("Cycle détecté: impossible de déplacer un parent sous son enfant");
            }
        }

        node.setParentId(newParentId);
        String newPathPrefix = (newParentId != null) 
            ? orgNodeService.findById(newParentId).get().getPath() 
            : "" + tenantId + ":";
        node.setPath(newPathPrefix + node.getCode() + ":");

        node = orgNodeService.save(node);

        auditService.log(currentUserId, tenantId, "ORG_NODE_MOVED", "ORGANIZATION",
                node.getId(), "SUCCESS",
                Map.of("newParentId", newParentId != null ? newParentId.toString() : "ROOT"),
                null, null, null);

        return ResponseEntity.ok(toResponse(node));
    }

    // ==================== SUPPRIMER UN NOEUD ====================

    @DeleteMapping("/nodes/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteNode(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        OrganizationNode node = orgNodeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Noeud non trouvé"));

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Noeud ne appartient pas à ce tenant");
        }

        if (node.getType() == OrganizationNodeType.ROOT_CHURCH) {
            throw new RuntimeException("Impossible de supprimer la racine de l'organisation");
        }

        // Vérifier qu'il n'y a pas d'enfants
        List<OrganizationNode> children = orgNodeService.findByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("Impossible de supprimer un noeud qui a des enfants (" 
                + children.size() + " enfants)");
        }

        String nodeName = node.getName();
        OrganizationNodeType nodeType = node.getType();

        orgNodeService.delete(id);

        auditService.log(currentUserId, tenantId, "ORG_NODE_DELETED", "ORGANIZATION",
                id, "SUCCESS",
                Map.of("name", nodeName, "type", nodeType.name()),
                null, null, null);

        return ResponseEntity.noContent().build();
    }

    // ==================== AFFECTER RESPONSABLE ====================

    @PutMapping("/nodes/{id}/responsible")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<NodeResponsibleResponse> setResponsible(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> request) {
        
        OrganizationNode node = orgNodeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Noeud non trouvé"));
        
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Noeud ne appartient pas à ce tenant");
        }

        UUID responsibleId = request.get("responsibleId");
        
        if (responsibleId != null) {
            var user = userRepository.findById(responsibleId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            if (!user.getTenantId().equals(tenantId)) {
                throw new SecurityException("L'utilisateur ne appartient pas à ce tenant");
            }
        }

        node.setResponsibleId(responsibleId);
        node = orgNodeService.save(node);

        auditService.log(currentUserId, tenantId, "NODE_RESPONSIBLE_SET", "ORGANIZATION",
                node.getId(), "SUCCESS",
                Map.of("responsibleId", responsibleId != null ? responsibleId.toString() : "null"),
                null, null, null);

        Optional<com.discipolat.modules.users.domain.User> resp = Optional.empty();
        if (node.getResponsibleId() != null) {
            resp = userRepository.findById(node.getResponsibleId());
        }

        return ResponseEntity.ok(new NodeResponsibleResponse(
                node.getId(),
                resp.map(u -> new ResponsibleInfo(
                        u.getId(),
                        u.getFirstName() + " " + u.getLastName(),
                        u.getEmail()
                )).orElse(null)
        ));
    }

    // ==================== STATS ====================

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStats() {
        UUID tenantId = getCurrentTenantId();
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("regions", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.REGION));
        stats.put("churches", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH));
        stats.put("subChurches", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH));
        stats.put("campuses", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS));
        stats.put("assemblies", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.ASSEMBLY));
        stats.put("departments", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT));
        stats.put("groups", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.GROUP));
        stats.put("totalNodes", orgNodeService.countByTenantId(tenantId));

        return ResponseEntity.ok(stats);
    }

    // ==================== MAPPERS ====================

    private OrganizationNodeResponse toResponse(OrganizationNode node) {
        return new OrganizationNodeResponse(
                node.getId(),
                node.getName(),
                node.getSlug(),
                node.getType(),
                node.getCode(),
                node.getPath(),
                node.getStatus(),
                node.getTenantId(),
                node.getParentId(),
                node.getResponsibleId(),
                node.getCountry(),
                node.getCity(),
                node.getTimezone(),
                node.getMetadata(),
                node.getLevel(),
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }

    private boolean hasRole(UUID userId, UUID tenantId, String role) {
        return orgNodeService.getMembershipsByUserId(userId).stream()
                .anyMatch(m -> m.getTenantId().equals(tenantId) && m.getRole().equalsIgnoreCase(role));
    }

    private String generateCode(OrganizationNodeType type) {
        return type.name() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .substring(0, Math.min(name.length(), 50));
    }

    // ==================== RECORDS ====================

    public record OrganizationTreeResponse(
            OrganizationNodeResponse root,
            List<OrganizationNodeResponse> allNodes,
            Map<UUID, List<OrganizationNodeResponse>> childrenByParent
    ) {}

    public record OrganizationNodeResponse(
            UUID id, String name, String slug, OrganizationNodeType type, String code,
            String path, OrganizationNodeStatus status, UUID tenantId,
            UUID parentId, UUID responsibleId, String country, String city,
            String timezone, Map<String, Object> metadata, Integer level,
            Instant createdAt, Instant updatedAt
    ) {}

    public record CreateNodeRequest(
            String name, OrganizationNodeType type, UUID parentId,
            String country, String city, String timezone,
            Map<String, Object> metadata, UUID responsibleId
    ) {}

    public record UpdateNodeRequest(
            String name, String slug, OrganizationNodeType type, String code,
            OrganizationNodeStatus status, String country, String city,
            String timezone, Map<String, Object> metadata, UUID responsibleId
    ) {}

    public record NodeResponsibleResponse(
            UUID nodeId, ResponsibleInfo responsible
    ) {}

    public record ResponsibleInfo(UUID id, String fullName, String email) {}
}
