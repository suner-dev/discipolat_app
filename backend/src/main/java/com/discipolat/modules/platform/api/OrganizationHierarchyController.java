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
 * API de gestion de la hiérarchie organisationnelle :
 * Églises, Campus, Sous-églises, Assemblées, Départements, Groupes
 */
@RestController
@RequestMapping("/api/v1/org")
public class OrganizationHierarchyController {

    private final OrganizationNodeService orgNodeService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public OrganizationHierarchyController(
            OrganizationNodeService orgNodeService,
            UserRepository userRepository,
            AuditService auditService) {
        this.orgNodeService = orgNodeService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        return TenantContext.requireTenantId();
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    // ==================== ARBRE COMPLET ====================

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationTree> getTree() {
        UUID tenantId = getCurrentTenantId();
        Optional<OrganizationNode> root = orgNodeService.findRootByTenantId(tenantId);
        
        if (root.isEmpty()) {
            return ResponseEntity.ok(new OrganizationTree(null, Collections.emptyList(), Collections.emptyMap()));
        }

        List<OrganizationNode> allNodes = orgNodeService.findByTenantId(tenantId);
        Map<UUID, List<OrganizationNode>> childrenByParent = new HashMap<>();
        
        allNodes.forEach(node -> {
            if (node.getParentId() != null) {
                childrenByParent.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
            }
        });

        return ResponseEntity.ok(new OrganizationTree(
                root.get(),
                allNodes,
                childrenByParent
        ));
    }

    // ==================== NOEUDS PAR TYPE ====================

    @GetMapping("/nodes/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNodeView>> getNodesByType(
            @PathVariable OrganizationNodeType type) {
        UUID tenantId = getCurrentTenantId();
        List<OrganizationNode> nodes = orgNodeService.findByTenantIdAndType(tenantId, type);
        
        return ResponseEntity.ok(nodes.stream().map(this::toNodeView).toList());
    }

    // ==================== DÉTAILS D'UN NOEUD ====================

    @GetMapping("/nodes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NodeDetails> getNode(@PathVariable UUID id) {
        OrganizationNode node = orgNodeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Noeud non trouvé"));
        
        List<OrganizationNode> children = orgNodeService.findByParentId(id);
        List<OrganizationNode> descendants = orgNodeService.findDescendants(
                getCurrentTenantId(), node.getPath());
        
        // Charger les informations du responsable
        Optional<com.discipolat.modules.users.domain.User> responsible = Optional.empty();
        if (node.getResponsibleId() != null) {
            responsible = userRepository.findById(node.getResponsibleId());
        }

        return ResponseEntity.ok(new NodeDetails(
                node,
                children,
                descendants.size(),
                responsible.map(u -> new NodeResponsibleInfo(
                        u.getId(), u.getFirstName() + " " + u.getLastName(), u.getEmail()
                )).orElse(null)
        ));
    }

    // ==================== CRÉER UN NOEUD ====================

    @PostMapping("/nodes")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN', 'PASTEUR')")
    public ResponseEntity<OrganizationNodeView> createNode(@RequestBody CreateNodeRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        // Vérifier le parent si fourni
        OrganizationNode parent = null;
        String pathPrefix = "" + tenantId + ":";
        
        if (request.parentId() != null) {
            parent = orgNodeService.findById(request.parentId())
                    .orElseThrow(() -> new RuntimeException("Parent non trouvé"));
            pathPrefix = parent.getPath();
        }

        // Générer un code unique
        String code = generateUniqueCode(request.type());
        String path = pathPrefix + code + ":";

        OrganizationNode node = OrganizationNode.builder()
                .tenantId(tenantId)
                .parentId(request.parentId())
                .name(request.name())
                .type(request.type())
                .code(code)
                .path(path)
                .status(OrganizationNodeStatus.ACTIVE)
                .responsibleId(request.responsibleId())
                .build();

        node = orgNodeService.save(node);

        auditService.log(currentUserId, tenantId, "ORG_NODE_CREATED", "ORGANIZATION",
                node.getId(), "SUCCESS",
                Map.of("name", request.name(), "type", request.type().name(),
                        "parentId", request.parentId() != null ? request.parentId().toString() : "ROOT"),
                null, null, null);

        return ResponseEntity.status(201).body(toNodeView(node));
    }

    // ==================== METTRE À JOUR UN NOEUD ====================

    @PutMapping("/nodes/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN', 'PASTEUR')")
    public ResponseEntity<OrganizationNodeView> updateNode(
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
        if (request.type() != null) node.setType(request.type());
        if (request.status() != null) node.setStatus(request.status());
        if (request.responsibleId() != null) node.setResponsibleId(request.responsibleId());

        node = orgNodeService.save(node);

        auditService.log(currentUserId, tenantId, "ORG_NODE_UPDATED", "ORGANIZATION",
                node.getId(), "SUCCESS",
                Map.of("name", request.name() != null ? request.name() : node.getName()),
                null, null, null);

        return ResponseEntity.ok(toNodeView(node));
    }

    // ==================== DÉPLACER UN NOEUD ====================

    @PostMapping("/nodes/{id}/move")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<OrganizationNodeView> moveNode(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> req) {
        
        OrganizationNode node = orgNodeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Noeud non trouvé"));
        
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Noeud ne appartient pas à ce tenant");
        }

        UUID newParentId = req.get("parentId");
        
        // Ne pas autoriser le déplacement de la racine
        if (node.getType() == OrganizationNodeType.ROOT_CHURCH) {
            throw new RuntimeException("Impossible de déplacer la racine");
        }

        // Vérifier le nouveau parent
        if (newParentId != null) {
            OrganizationNode newParent = orgNodeService.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("Nouveau parent non trouvé"));
            if (!newParent.getTenantId().equals(tenantId)) {
                throw new SecurityException("Nouveau parent ne appartient pas à ce tenant");
            }
        }

        node.setParentId(newParentId);
        
        // Mettre à jour le path
        String newPathPrefix = (newParentId != null) 
            ? orgNodeService.findById(newParentId).get().getPath() 
            : "" + tenantId + ":";
        node.setPath(newPathPrefix + node.getCode() + ":");

        node = orgNodeService.save(node);

        auditService.log(currentUserId, tenantId, "ORG_NODE_MOVED", "ORGANIZATION",
                node.getId(), "SUCCESS",
                Map.of("newParentId", newParentId != null ? newParentId.toString() : "ROOT"),
                null, null, null);

        return ResponseEntity.ok(toNodeView(node));
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
            throw new RuntimeException("Impossible de supprimer la racine");
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

    // ==================== AFFECTER DES UTILISATEURS ====================

    @PutMapping("/nodes/{id}/responsible")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<NodeDetails> setResponsible(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> req) {
        
        OrganizationNode node = orgNodeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Noeud non trouvé"));
        
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Noeud ne appartient pas à ce tenant");
        }

        UUID responsibleId = req.get("responsibleId");
        
        if (responsibleId != null) {
            // Vérifier que l'utilisateur existe et appartient au tenant
            com.discipolat.modules.users.domain.User user = userRepository.findById(responsibleId)
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

        Optional<com.discipolat.modules.users.domain.User> responsible = Optional.empty();
        if (node.getResponsibleId() != null) {
            responsible = userRepository.findById(node.getResponsibleId());
        }

        return ResponseEntity.ok(new NodeDetails(
                node,
                orgNodeService.findByParentId(id),
                orgNodeService.findDescendants(tenantId, node.getPath()).size(),
                responsible.map(u -> new NodeResponsibleInfo(
                        u.getId(), u.getFirstName() + " " + u.getLastName(), u.getEmail()
                )).orElse(null)
        ));
    }

    // ==================== STATS PAR TYPE ====================

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getStats() {
        UUID tenantId = getCurrentTenantId();
        
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("eglises", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH));
        stats.put("campuses", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS));
        stats.put("sous_eglises", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH));
        stats.put("assemblies", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.ASSEMBLY));
        stats.put("departements", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT));
        stats.put("groupes", orgNodeService.countByTenantIdAndType(tenantId, OrganizationNodeType.GROUP));

        return ResponseEntity.ok(stats);
    }

    // ==================== MAPPERS ====================

    private OrganizationNodeView toNodeView(OrganizationNode node) {
        return new OrganizationNodeView(
                node.getId(), node.getName(), node.getType(), node.getCode(),
                node.getPath(), node.getStatus(), node.getTenantId(),
                node.getParentId(), node.getResponsibleId(), node.getCreatedAt()
        );
    }

    private String generateUniqueCode(OrganizationNodeType type) {
        String base = type.name() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // Vérifier et regénérer si collision (peu probable mais sécurité)
        while (orgNodeService.findByTenantIdAndCode(getCurrentTenantId(), base).isPresent()) {
            base = type.name() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return base;
    }

    // ==================== RECORDS ====================

    public record OrganizationTree(
            OrganizationNode root,
            List<OrganizationNode> allNodes,
            Map<UUID, List<OrganizationNode>> childrenByParent
    ) {}

    public record NodeDetails(
            OrganizationNode node,
            List<OrganizationNode> children,
            int descendantCount,
            NodeResponsibleInfo responsible
    ) {}

    public record NodeResponsibleInfo(
            UUID id, String fullName, String email
    ) {}

    public record OrganizationNodeView(
            UUID id, String name, OrganizationNodeType type, String code,
            String path, OrganizationNodeStatus status, UUID tenantId,
            UUID parentId, UUID responsibleId, Instant createdAt
    ) {}

    public record CreateNodeRequest(
            String name, OrganizationNodeType type, UUID parentId, UUID responsibleId
    ) {}

    public record UpdateNodeRequest(
            String name, OrganizationNodeType type,
            OrganizationNodeStatus status, UUID responsibleId
    ) {}
}
