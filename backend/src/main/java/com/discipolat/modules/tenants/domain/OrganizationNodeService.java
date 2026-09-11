package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service pour la gestion de la hiérarchie d'organisation (églises, campus, départements, groupes)
 * Utilise materialized path (ltree) pour les requêtes hiérarchiques efficaces
 */
@Service
@Transactional
public class OrganizationNodeService {

    private final OrganizationNodeRepository nodeRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public OrganizationNodeService(OrganizationNodeRepository nodeRepository,
                                   AuditService auditService,
                                   EntityPropagationPublisher propagationPublisher) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getAllForTenant(UUID tenantId) {
        return nodeRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getByType(UUID tenantId, OrganizationNodeType type) {
        return nodeRepository.findByTenantIdAndType(tenantId, type);
    }

    @Transactional(readOnly = true)
    public List<OrganizationNode> getChildren(UUID parentId) {
        return nodeRepository.findByParentId(parentId);
    }

    @Transactional(readOnly = true)
    public Optional<OrganizationNode> getRoot(UUID tenantId) {
        return nodeRepository.findRootByTenantId(tenantId);
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

        // Use ltree query to find ancestors
        // path @> ancestor_path (ancestor path is prefix of node path)
        String nodePath = node.get().getPath();
        // We'll use a simple approach: find all nodes where path is prefix of nodePath
        List<OrganizationNode> allNodes = nodeRepository.findByTenantId(tenantId);
        return allNodes.stream()
                .filter(n -> nodePath.startsWith(n.getPath() + ".") || nodePath.equals(n.getPath()))
                .toList();
    }

    /**
     * Crée un nœud d'organisation (église, campus, département, etc.)
     */
    public OrganizationNode createNode(UUID tenantId, OrganizationNodeType type, String name, String code,
                                       UUID parentId, UUID responsibleId, UUID creatorId) {
        // Valider le parent si fourni
        UUID effectiveParentId = parentId;
        Integer level = 0;
        String path;

        if (effectiveParentId != null) {
            OrganizationNode parent = nodeRepository.findById(effectiveParentId)
                    .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", effectiveParentId));
            if (!parent.getTenantId().equals(tenantId)) {
                throw new BusinessRuleException("Le parent doit appartenir au même tenant", "PARENT_TENANT_MISMATCH");
            }
            level = parent.getLevel() + 1;
            path = parent.getPath() + "." + (code != null ? code : UUID.randomUUID().toString().substring(0, 8));
        } else {
            // Nœud racine
            level = 0;
            path = code != null ? code : "root";
        }

        // Vérifier unicité du code si fourni
        if (code != null && nodeRepository.findByTenantIdAndCode(tenantId, code).isPresent()) {
            throw new BusinessRuleException("Un nœud avec ce code existe déjà: " + code, "CODE_EXISTS");
        }

        OrganizationNode node = OrganizationNode.builder()
                .tenantId(tenantId)
                .parentId(effectiveParentId)
                .type(type)
                .name(name)
                .code(code)
                .status(OrganizationNodeStatus.ACTIVE)
                .path(path)
                .level(level)
                .responsibleId(responsibleId)
                .build();

        node = nodeRepository.save(node);

        auditService.log(
                creatorId,
                tenantId,
                "ORG_NODE_CREATED",
                "ORGANIZATION_NODE",
                node.getId(),
                "SUCCESS",
                Map.of("type", type.name(), "name", name, "parentId", effectiveParentId),
                null, null, null
        );

        propagationPublisher.publishCreated("ORGANIZATION_NODE", node.getId(),
                Map.of("tenantId", tenantId, "type", type.name(), "name", name),
                "Nœud créé: " + name + " (" + type.name() + ")");

        return node;
    }

    /**
     * Crée l'église racine pour un nouveau tenant
     */
    public OrganizationNode createRootChurch(UUID tenantId, String name, String code, UUID creatorId) {
        // Vérifier qu'il n'y a pas déjà d'église racine
        Optional<OrganizationNode> existingRoot = nodeRepository.findRootByTenantId(tenantId);
        if (existingRoot.isPresent()) {
            throw new BusinessRuleException("Une église racine existe déjà pour ce tenant", "ROOT_CHURCH_EXISTS");
        }

        return createNode(tenantId, OrganizationNodeType.ROOT_CHURCH, name, code, null, null, creatorId);
    }

    public OrganizationNode updateNode(UUID nodeId, String name, String code, UUID responsibleId,
                                       OrganizationNodeStatus status, UUID updaterId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        String oldName = node.getName();
        String oldCode = node.getCode();

        if (name != null) node.setName(name);
        if (code != null) {
            if (nodeRepository.findByTenantIdAndCode(node.getTenantId(), code)
                    .filter(n -> !n.getId().equals(nodeId)).isPresent()) {
                throw new BusinessRuleException("Un nœud avec ce code existe déjà: " + code, "CODE_EXISTS");
            }
            node.setCode(code);
        }
        if (responsibleId != null) node.setResponsibleId(responsibleId);
        if (status != null) node.setStatus(status);

        node = nodeRepository.save(node);

        auditService.log(
                updaterId,
                node.getTenantId(),
                "ORG_NODE_UPDATED",
                "ORGANIZATION_NODE",
                node.getId(),
                "SUCCESS",
                Map.of("oldName", oldName, "newName", name, "oldCode", oldCode, "newCode", code),
                null, null, null
        );

        return node;
    }

    /**
     * Déplace un nœud (et sa descendance) sous un nouveau parent
     */
    public OrganizationNode moveNode(UUID nodeId, UUID newParentId, UUID moverId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        OrganizationNode newParent = nodeRepository.findById(newParentId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", newParentId));

        if (!node.getTenantId().equals(newParent.getTenantId())) {
            throw new BusinessRuleException("Le nouveau parent doit appartenir au même tenant", "PARENT_TENANT_MISMATCH");
        }

        // Vérifier qu'on ne déplace pas un nœud sous son propre descendant
        List<OrganizationNode> descendants = getDescendants(node.getTenantId(), nodeId);
        if (descendants.stream().anyMatch(d -> d.getId().equals(newParentId))) {
            throw new BusinessRuleException("Impossible de déplacer un nœud sous son propre descendant", "CYCLIC_HIERARCHY");
        }

        String oldPath = node.getPath();
        String newPath = newParent.getPath() + "." + node.getPath().substring(node.getPath().lastIndexOf(".") + 1);
        int levelDiff = newParent.getLevel() + 1 - node.getLevel();

        // Mettre à jour le nœud et tous ses descendants (path et level)
        updateNodeAndDescendantsPath(node, newPath, levelDiff);

        auditService.log(
                moverId,
                node.getTenantId(),
                "ORG_NODE_MOVED",
                "ORGANIZATION_NODE",
                node.getId(),
                "SUCCESS",
                Map.of("oldPath", oldPath, "newPath", newPath, "newParentId", newParentId),
                null, null, null
        );

        return nodeRepository.findById(nodeId).orElseThrow();
    }

    private void updateNodeAndDescendantsPath(OrganizationNode node, String newPath, int levelDiff) {
        node.setPath(newPath);
        node.setLevel(node.getLevel() + levelDiff);
        nodeRepository.save(node);

        // Mettre à jour les descendants
        List<OrganizationNode> children = nodeRepository.findByParentId(node.getId());
        for (OrganizationNode child : children) {
            String childNewPath = newPath + "." + child.getPath().substring(child.getPath().lastIndexOf(".") + 1);
            updateNodeAndDescendantsPath(child, childNewPath, levelDiff);
        }
    }

    public void deleteNode(UUID nodeId, UUID deleterId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        // Vérifier qu'il n'y a pas d'enfants
        List<OrganizationNode> children = nodeRepository.findByParentId(nodeId);
        if (!children.isEmpty()) {
            throw new BusinessRuleException("Impossible de supprimer un nœud qui a des enfants", "NODE_HAS_CHILDREN");
        }

        nodeRepository.delete(node);

        auditService.log(
                deleterId,
                node.getTenantId(),
                "ORG_NODE_DELETED",
                "ORGANIZATION_NODE",
                nodeId,
                "SUCCESS",
                Map.of("name", node.getName(), "type", node.getType().name()),
                null, null, null
        );
    }

    @Transactional(readOnly = true)
    public long countByType(UUID tenantId, OrganizationNodeType type) {
        return nodeRepository.countByTenantIdAndType(tenantId, type);
    }
}