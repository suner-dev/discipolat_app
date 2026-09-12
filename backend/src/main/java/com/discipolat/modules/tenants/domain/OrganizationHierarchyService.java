package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service complet pour la gestion de la hiérarchie d'organisation.
 * Inclut CRUD, move, copy, bulk operations, et utilitaires de hiérarchie.
 */
@Service
@Transactional
public class OrganizationHierarchyService {

    private final OrganizationNodeRepository nodeRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public OrganizationHierarchyService(OrganizationNodeRepository nodeRepository,
                                        AuditService auditService,
                                        EntityPropagationPublisher propagationPublisher,
                                        TenantMembershipRepository membershipRepository,
                                        UserRepository userRepository) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    // ==================== READ OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<OrganizationNode> getTree(UUID tenantId) {
        return nodeRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<OrganizationNode> getRoot(UUID tenantId) {
        return nodeRepository.findRootByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getByType(UUID tenantId, OrganizationNodeType type) {
        return nodeRepository.findByTenantIdAndType(tenantId, type);
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getChildren(UUID tenantId, UUID parentId) {
        return nodeRepository.findByParentId(parentId);
    }

    @Transactional(readOnly = true)
    public OrganizationTreeView getTreeView(UUID tenantId) {
        Optional<OrganizationNode> root = getRoot(tenantId);
        if (root.isEmpty()) {
            return new OrganizationTreeView(null, List.of(), Map.of());
        }
        List<OrganizationNode> allNodes = nodeRepository.findByTenantId(tenantId);
        Map<UUID, List<OrganizationNode>> childrenByParent = allNodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(OrganizationNode::getParentId));
        return new OrganizationTreeView(root.get(), allNodes, childrenByParent);
    }

    @Transactional(readOnly = true)
    public Optional<OrganizationNode> findById(UUID id) {
        return nodeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public NodeDetails getNodeDetails(UUID tenantId, UUID nodeId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));
        if (!node.getTenantId().equals(tenantId)) {
            throw new BusinessRuleException("Nœud n'appartient pas à ce tenant", "TENANT_MISMATCH");
        }

        List<OrganizationNode> children = nodeRepository.findByParentId(nodeId);
        List<OrganizationNode> descendants = getDescendants(tenantId, nodeId);
        Optional<User> responsible = node.getResponsibleId() != null ?
                userRepository.findById(node.getResponsibleId()) : Optional.empty();

        return new NodeDetails(node, children, descendants.size(),
                responsible.map(u -> new ResponsibleInfo(u.getId(), u.getFirstName() + " " + u.getLastName(), u.getEmail())).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getDescendants(UUID tenantId, UUID ancestorId) {
        Optional<OrganizationNode> ancestor = nodeRepository.findById(ancestorId);
        if (ancestor.isEmpty()) return List.of();
        return nodeRepository.findDescendants(tenantId, ancestor.get().getPath() + ".*");
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getAncestors(UUID tenantId, UUID nodeId) {
        Optional<OrganizationNode> node = nodeRepository.findById(nodeId);
        if (node.isEmpty()) return List.of();

        String nodePath = node.get().getPath();
        return nodeRepository.findByTenantId(tenantId).stream()
                .filter(n -> nodePath.startsWith(n.getPath() + ".") || nodePath.equals(n.getPath()))
                .sorted(Comparator.comparingInt(OrganizationNode::getLevel))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getSiblings(UUID tenantId, UUID nodeId) {
        Optional<OrganizationNode> node = nodeRepository.findById(nodeId);
        if (node.isEmpty() || node.get().getParentId() == null) return List.of();
        return nodeRepository.findByParentId(node.get().getParentId()).stream()
                .filter(n -> !n.getId().equals(nodeId))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countByType(UUID tenantId, OrganizationNodeType type) {
        return nodeRepository.countByTenantIdAndType(tenantId, type);
    }

    @Transactional(readOnly = true)
    public Map<OrganizationNodeType, Long> getStatsByType(UUID tenantId) {
        return Arrays.stream(OrganizationNodeType.values())
                .collect(Collectors.toMap(
                        t -> t,
                        t -> nodeRepository.countByTenantIdAndType(tenantId, t)
                ));
    }

    // ==================== CREATE OPERATIONS ====================

    public OrganizationNode createNode(UUID tenantId, CreateNodeRequest request, UUID creatorId) {
        // Valider le parent
        UUID parentId = request.parentId();
        OrganizationNode parent = null;
        String path;
        int level;

        if (parentId != null) {
            parent = nodeRepository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", parentId));
            if (!parent.getTenantId().equals(tenantId)) {
                throw new BusinessRuleException("Le parent doit appartenir au même tenant", "PARENT_TENANT_MISMATCH");
            }
            level = parent.getLevel() + 1;
            path = parent.getPath() + "." + (request.code() != null ? request.code() : generateCode(request.type()));
        } else {
            // Nœud racine - vérifier unicité
            if (nodeRepository.findRootByTenantId(tenantId).isPresent()) {
                throw new BusinessRuleException("Une racine existe déjà pour ce tenant", "ROOT_EXISTS");
            }
            level = 0;
            path = request.code() != null ? request.code() : "ROOT_" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Vérifier unicité du code
        if (request.code() != null && nodeRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new BusinessRuleException("Un nœud avec ce code existe déjà: " + request.code(), "CODE_EXISTS");
        }

        // Valider responsable
        if (request.responsibleId() != null) {
            User user = userRepository.findById(request.responsibleId())
                    .orElseThrow(() -> new EntityNotFoundException("User", request.responsibleId()));
            if (!user.getTenantId().equals(tenantId)) {
                throw new BusinessRuleException("Le responsable doit appartenir au même tenant", "RESPONSIBLE_TENANT_MISMATCH");
            }
        }

        OrganizationNode node = OrganizationNode.builder()
                .tenantId(tenantId)
                .parentId(parentId)
                .type(request.type())
                .name(request.name())
                .code(request.code() != null ? request.code() : generateCode(request.type()))
                .status(OrganizationNodeStatus.ACTIVE)
                .path(path)
                .level(level)
                .timezone(request.timezone())
                .country(request.country())
                .city(request.city())
                .metadataJson(request.metadata() != null ? toJson(request.metadata()) : null)
                .responsibleId(request.responsibleId())
                .build();

        node = nodeRepository.save(node);

        auditAndPropagate(creatorId, tenantId, "ORG_NODE_CREATED", node,
                Map.of("type", request.type().name(), "name", request.name(), "parentId", parentId != null ? parentId.toString() : "ROOT"));

        return node;
    }

    public OrganizationNode createRootChurch(UUID tenantId, String name, String code, UUID creatorId) {
        if (nodeRepository.findRootByTenantId(tenantId).isPresent()) {
            throw new BusinessRuleException("Une église racine existe déjà pour ce tenant", "ROOT_CHURCH_EXISTS");
        }
        return createNode(tenantId, new CreateNodeRequest(name, OrganizationNodeType.ROOT_CHURCH, null, null, code, null, null, null, null), creatorId);
    }

    // ==================== UPDATE OPERATIONS ====================

    public OrganizationNode updateNode(UUID nodeId, UpdateNodeRequest request, UUID updaterId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        String oldName = node.getName();
        String oldCode = node.getCode();
        UUID oldParentId = node.getParentId();
        UUID oldResponsibleId = node.getResponsibleId();
        OrganizationNodeStatus oldStatus = node.getStatus();

        if (request.name() != null) node.setName(request.name());
        if (request.code() != null) {
            if (nodeRepository.findByTenantIdAndCode(node.getTenantId(), request.code())
                    .filter(n -> !n.getId().equals(nodeId)).isPresent()) {
                throw new BusinessRuleException("Un nœud avec ce code existe déjà: " + request.code(), "CODE_EXISTS");
            }
            node.setCode(request.code());
        }
        if (request.responsibleId() != null) {
            User user = userRepository.findById(request.responsibleId())
                    .orElseThrow(() -> new EntityNotFoundException("User", request.responsibleId()));
            if (!user.getTenantId().equals(node.getTenantId())) {
                throw new BusinessRuleException("Le responsable doit appartenir au même tenant", "RESPONSIBLE_TENANT_MISMATCH");
            }
            node.setResponsibleId(request.responsibleId());
        }
        if (request.status() != null) node.setStatus(request.status());
        if (request.timezone() != null) node.setTimezone(request.timezone());
        if (request.country() != null) node.setCountry(request.country());
        if (request.city() != null) node.setCity(request.city());
        if (request.metadata() != null) node.setMetadataJson(toJson(request.metadata()));

        node = nodeRepository.save(node);

        auditService.log(updaterId, node.getTenantId(), "ORG_NODE_UPDATED", "ORGANIZATION_NODE",
                node.getId(), "SUCCESS",
                Map.of(
                        "oldName", oldName, "newName", request.name(),
                        "oldCode", oldCode, "newCode", request.code(),
                        "oldResponsibleId", oldResponsibleId, "newResponsibleId", request.responsibleId(),
                        "oldStatus", oldStatus, "newStatus", request.status()
                ), null, null, null);

        return node;
    }

    // ==================== MOVE OPERATIONS ====================

    public OrganizationNode moveNode(UUID nodeId, UUID newParentId, UUID moverId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        if (node.getType() == OrganizationNodeType.ROOT_CHURCH) {
            throw new BusinessRuleException("Impossible de déplacer la racine", "CANNOT_MOVE_ROOT");
        }

        OrganizationNode newParent = nodeRepository.findById(newParentId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", newParentId));

        if (!node.getTenantId().equals(newParent.getTenantId())) {
            throw new BusinessRuleException("Le nouveau parent doit appartenir au même tenant", "PARENT_TENANT_MISMATCH");
        }

        // Vérifier cycle
        List<OrganizationNode> descendants = getDescendants(node.getTenantId(), nodeId);
        if (descendants.stream().anyMatch(d -> d.getId().equals(newParentId))) {
            throw new BusinessRuleException("Impossible de déplacer un nœud sous son propre descendant", "CYCLIC_HIERARCHY");
        }

        String oldPath = node.getPath();
        UUID oldParentId = node.getParentId();

        String newPath = newParent.getPath() + "." + node.getPath().substring(node.getPath().lastIndexOf(".") + 1);
        int levelDiff = newParent.getLevel() + 1 - node.getLevel();

        node.setParentId(newParentId);
        updateNodeAndDescendantsPath(node, newPath, levelDiff);

        auditService.log(moverId, node.getTenantId(), "ORG_NODE_MOVED", "ORGANIZATION_NODE",
                node.getId(), "SUCCESS",
                Map.of("oldPath", oldPath, "newPath", newPath, "oldParentId", oldParentId, "newParentId", newParentId),
                null, null, null);

        return nodeRepository.findById(nodeId).orElseThrow();
    }

    // Move multiple nodes at once (bulk move)
    @Transactional
    public List<OrganizationNode> moveNodes(List<UUID> nodeIds, UUID newParentId, UUID moverId) {
        return nodeIds.stream()
                .map(id -> moveNode(id, newParentId, moverId))
                .toList();
    }

    private void updateNodeAndDescendantsPath(OrganizationNode node, String newPath, int levelDiff) {
        node.setPath(newPath);
        node.setLevel(node.getLevel() + levelDiff);
        nodeRepository.save(node);

        List<OrganizationNode> children = nodeRepository.findByParentId(node.getId());
        for (OrganizationNode child : children) {
            String childNewPath = newPath + "." + child.getPath().substring(child.getPath().lastIndexOf(".") + 1);
            updateNodeAndDescendantsPath(child, childNewPath, levelDiff);
        }
    }

    // ==================== COPY OPERATIONS ====================

    @Transactional
    public OrganizationNode copyNode(UUID nodeId, UUID newParentId, UUID copierId, boolean includeDescendants) {
        OrganizationNode source = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        OrganizationNode newParent = null;
        if (newParentId != null) {
            newParent = nodeRepository.findById(newParentId)
                    .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", newParentId));
            if (!source.getTenantId().equals(newParent.getTenantId())) {
                throw new BusinessRuleException("Le parent doit appartenir au même tenant", "PARENT_TENANT_MISMATCH");
            }
        }

        OrganizationNode copy = copyNodeRecursive(source, newParent, copierId, includeDescendants, new HashMap<>());

        auditService.log(copierId, source.getTenantId(), "ORG_NODE_COPIED", "ORGANIZATION_NODE",
                copy.getId(), "SUCCESS",
                Map.of("sourceId", source.getId().toString(), "includeDescendants", includeDescendants),
                null, null, null);

        return copy;
    }

    private OrganizationNode copyNodeRecursive(OrganizationNode source, OrganizationNode newParent,
                                               UUID copierId, boolean includeDescendants,
                                               Map<UUID, UUID> idMapping) {
        String path = (newParent != null ? newParent.getPath() : source.getTenantId().toString()) + "."
                + (source.getCode() != null ? source.getCode() + "_copy" : UUID.randomUUID().toString().substring(0, 8));
        int level = (newParent != null ? newParent.getLevel() + 1 : 0);

        OrganizationNode copy = OrganizationNode.builder()
                .tenantId(source.getTenantId())
                .parentId(newParent != null ? newParent.getId() : null)
                .type(source.getType())
                .name(source.getName() + " (copie)")
                .code(source.getCode() != null ? source.getCode() + "_copy_" + UUID.randomUUID().toString().substring(0, 6) : null)
                .status(OrganizationNodeStatus.ACTIVE)
                .path(path)
                .level(level)
                .timezone(source.getTimezone())
                .country(source.getCountry())
                .city(source.getCity())
                .metadataJson(source.getMetadataJson())
                .responsibleId(null) // Responsable non copié par défaut
                .build();

        copy = nodeRepository.save(copy);
        idMapping.put(source.getId(), copy.getId());

        if (includeDescendants) {
            List<OrganizationNode> children = nodeRepository.findByParentId(source.getId());
            for (OrganizationNode child : children) {
                copyNodeRecursive(child, copy, copierId, true, idMapping);
            }
        }

        return copy;
    }

    // ==================== DELETE OPERATIONS ====================

    public void deleteNode(UUID nodeId, UUID deleterId, boolean forceCascade) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        if (node.getType() == OrganizationNodeType.ROOT_CHURCH) {
            throw new BusinessRuleException("Impossible de supprimer la racine", "CANNOT_DELETE_ROOT");
        }

        List<OrganizationNode> children = nodeRepository.findByParentId(nodeId);
        if (!children.isEmpty() && !forceCascade) {
            throw new BusinessRuleException("Le nœud a des enfants. Utilisez forceCascade=true pour suppression en cascade", "NODE_HAS_CHILDREN");
        }

        if (forceCascade) {
            deleteSubtree(node, deleterId);
        } else {
            nodeRepository.delete(node);
        }

        auditService.log(deleterId, node.getTenantId(), "ORG_NODE_DELETED", "ORGANIZATION_NODE",
                nodeId, "SUCCESS",
                Map.of("name", node.getName(), "type", node.getType().name(), "forceCascade", forceCascade),
                null, null, null);
    }

    private void deleteSubtree(OrganizationNode node, UUID deleterId) {
        List<OrganizationNode> children = nodeRepository.findByParentId(node.getId());
        for (OrganizationNode child : children) {
            deleteSubtree(child, deleterId);
        }
        nodeRepository.delete(node);
    }

    // ==================== BULK OPERATIONS ====================

    @Transactional
    public BulkOperationResult bulkCreate(UUID tenantId, List<CreateNodeRequest> requests, UUID creatorId) {
        List<OrganizationNode> created = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            try {
                created.add(createNode(tenantId, requests.get(i), creatorId));
            } catch (Exception e) {
                errors.add("Index " + i + ": " + e.getMessage());
            }
        }

        return new BulkOperationResult(created.size(), errors);
    }

    @Transactional
    public BulkOperationResult bulkDelete(List<UUID> nodeIds, UUID deleterId, boolean forceCascade) {
        int deleted = 0;
        List<String> errors = new ArrayList<>();

        for (UUID id : nodeIds) {
            try {
                deleteNode(id, deleterId, forceCascade);
                deleted++;
            } catch (Exception e) {
                errors.add(id + ": " + e.getMessage());
            }
        }

        return new BulkOperationResult(deleted, errors);
    }

    @Transactional
    public BulkOperationResult bulkUpdateStatus(List<UUID> nodeIds, OrganizationNodeStatus status, UUID updaterId) {
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (UUID id : nodeIds) {
            try {
                OrganizationNode node = nodeRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", id));
                node.setStatus(status);
                nodeRepository.save(node);
                updated++;
            } catch (Exception e) {
                errors.add(id + ": " + e.getMessage());
            }
        }

        return new BulkOperationResult(updated, errors);
    }

    // ==================== RESPONSIBLE ASSIGNMENT ====================

    public NodeDetails assignResponsible(UUID nodeId, UUID responsibleId, UUID assignerId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        if (responsibleId != null) {
            User user = userRepository.findById(responsibleId)
                    .orElseThrow(() -> new EntityNotFoundException("User", responsibleId));
            if (!user.getTenantId().equals(node.getTenantId())) {
                throw new BusinessRuleException("Le responsable doit appartenir au même tenant", "RESPONSIBLE_TENANT_MISMATCH");
            }
        }

        UUID oldResponsibleId = node.getResponsibleId();
        node.setResponsibleId(responsibleId);
        nodeRepository.save(node);

        auditService.log(assignerId, node.getTenantId(), "NODE_RESPONSIBLE_SET", "ORGANIZATION_NODE",
                node.getId(), "SUCCESS",
                Map.of("oldResponsibleId", oldResponsibleId, "newResponsibleId", responsibleId),
                null, null, null);

        return getNodeDetails(node.getTenantId(), nodeId);
    }

    // ==================== CONFIGURATION INHERITANCE ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getEffectiveConfig(UUID nodeId) {
        Optional<OrganizationNode> node = nodeRepository.findById(nodeId);
        if (node.isEmpty()) return Map.of();

        Map<String, Object> effective = new HashMap<>();
        List<OrganizationNode> ancestors = getAncestors(node.get().getTenantId(), nodeId);

        // Merge configs from root to node (child overrides parent)
        for (OrganizationNode ancestor : ancestors) {
            if (ancestor.getMetadataJson() != null) {
                Map<String, Object> ancestorConfig = fromJson(ancestor.getMetadataJson());
                for (Map.Entry<String, Object> entry : ancestorConfig.entrySet()) {
                    if (!effective.containsKey(entry.getKey())) {
                        effective.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        // Apply node's own config
        if (node.get().getMetadataJson() != null) {
            effective.putAll(fromJson(node.get().getMetadataJson()));
        }

        return effective;
    }

    // ==================== PERMISSIONS BY NODE ====================

    @Transactional(readOnly = true)
    public List<TenantMembership> getMembersWithAccess(UUID tenantId, UUID nodeId) {
        // Get all memberships that have scope covering this node
        List<TenantMembership> allMemberships = membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
        UUID ancestorPath = nodeId;

        return allMemberships.stream()
                .filter(m -> {
                    if (m.getScopeType() == MembershipScopeType.TENANT) return true;
                    if (m.getScopeId() == null) return false;
                    return isDescendantOf(ancestorPath, m.getScopeId());
                })
                .toList();
    }

    private boolean isDescendantOf(UUID nodeId, UUID ancestorId) {
        Optional<OrganizationNode> node = nodeRepository.findById(nodeId);
        Optional<OrganizationNode> ancestor = nodeRepository.findById(ancestorId);
        if (node.isEmpty() || ancestor.isEmpty()) return false;
        return node.get().getPath().startsWith(ancestor.get().getPath());
    }

    // ==================== HELPERS ====================

    private void auditAndPropagate(UUID actorId, UUID tenantId, String action, OrganizationNode node, Map<String, Object> metadata) {
        auditService.log(actorId, tenantId, action, "ORGANIZATION_NODE", node.getId(), "SUCCESS", metadata, null, null, null);
        propagationPublisher.publishCreated("ORGANIZATION_NODE", node.getId(),
                Map.of("tenantId", tenantId, "type", node.getType().name(), "name", node.getName()),
                action + ": " + node.getName());
    }

    private String generateCode(OrganizationNodeType type) {
        String base = type.name() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // Note: In production, check uniqueness in a loop
        return base;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ==================== RECORDS ====================

    public record CreateNodeRequest(
            String name,
            OrganizationNodeType type,
            UUID parentId,
            UUID responsibleId,
            String code,
            String timezone,
            String country,
            String city,
            Map<String, Object> metadata
    ) {}

    public record UpdateNodeRequest(
            String name,
            String code,
            UUID responsibleId,
            OrganizationNodeStatus status,
            String timezone,
            String country,
            String city,
            Map<String, Object> metadata
    ) {}

    public record OrganizationTreeView(
            OrganizationNode root,
            List<OrganizationNode> allNodes,
            Map<UUID, List<OrganizationNode>> childrenByParent
    ) {}

    public record NodeDetails(
            OrganizationNode node,
            List<OrganizationNode> children,
            int descendantCount,
            ResponsibleInfo responsible
    ) {}

    public record ResponsibleInfo(
            UUID id, String fullName, String email
    ) {}

    public record BulkOperationResult(
            int successCount,
            List<String> errors
    ) {}
}