package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/org")
public class OrganizationHierarchyController {

    private final OrganizationHierarchyService hierarchyService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public OrganizationHierarchyController(
            OrganizationHierarchyService hierarchyService,
            UserRepository userRepository,
            AuditService auditService) {
        this.hierarchyService = hierarchyService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        return TenantContext.requireTenantId();
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    // ==================== TREE VIEWS ====================

    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationHierarchyService.OrganizationTreeView> getTree() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getTreeView(tenantId));
    }

    @GetMapping("/tree/flat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNode>> getFlatTree() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getTree(tenantId));
    }

    @GetMapping("/root")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationNode> getRoot() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getRoot(tenantId).orElse(null));
    }

    // ==================== NODES BY TYPE ====================

    @GetMapping("/nodes/type/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNode>> getNodesByType(@PathVariable OrganizationNodeType type) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getByType(tenantId, type));
    }

    @GetMapping("/nodes/parent/{parentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNode>> getChildren(@PathVariable UUID parentId) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getChildren(tenantId, parentId));
    }

    // ==================== NODE DETAILS ====================

    @GetMapping("/nodes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationHierarchyService.NodeDetails> getNode(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getNodeDetails(tenantId, id));
    }

    @GetMapping("/nodes/{id}/descendants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNode>> getDescendants(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getDescendants(tenantId, id));
    }

    @GetMapping("/nodes/{id}/ancestors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNode>> getAncestors(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getAncestors(tenantId, id));
    }

    @GetMapping("/nodes/{id}/siblings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrganizationNode>> getSiblings(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getSiblings(tenantId, id));
    }

    @GetMapping("/nodes/{id}/effective-config")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getEffectiveConfig(@PathVariable UUID id) {
        return ResponseEntity.ok(hierarchyService.getEffectiveConfig(id));
    }

    @GetMapping("/nodes/{id}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TenantMembership>> getMembersWithAccess(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getMembersWithAccess(tenantId, id));
    }

    // ==================== CREATE NODE ====================

    @PostMapping("/nodes")
    @PreAuthorize("@authz.can('ORG_NODE_CREATE', 'TENANT', null)")
    public ResponseEntity<OrganizationNode> createNode(@RequestBody OrganizationHierarchyService.CreateNodeRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();
        OrganizationNode node = hierarchyService.createNode(tenantId, request, currentUserId);
        return ResponseEntity.status(201).body(node);
    }

    @PostMapping("/root-church")
    @PreAuthorize("@authz.can('CHURCH_CREATE', 'TENANT', null)")
    public ResponseEntity<OrganizationNode> createRootChurch(@RequestBody Map<String, String> request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();
        String name = request.get("name");
        String code = request.get("code");
        OrganizationNode node = hierarchyService.createRootChurch(tenantId, name, code, currentUserId);
        return ResponseEntity.status(201).body(node);
    }

    // ==================== UPDATE NODE ====================

    @PutMapping("/nodes/{id}")
    @PreAuthorize("@authz.can('ORG_NODE_UPDATE', 'TENANT', #id)")
    public ResponseEntity<OrganizationNode> updateNode(@PathVariable UUID id, @RequestBody OrganizationHierarchyService.UpdateNodeRequest request) {
        UUID currentUserId = getCurrentUserId();
        OrganizationNode node = hierarchyService.updateNode(id, request, currentUserId);
        return ResponseEntity.ok(node);
    }

    // ==================== MOVE NODE ====================

    @PostMapping("/nodes/{id}/move")
    @PreAuthorize("@authz.can('ORG_NODE_MOVE', 'TENANT', #id)")
    public ResponseEntity<OrganizationNode> moveNode(@PathVariable UUID id, @RequestBody Map<String, UUID> req) {
        UUID currentUserId = getCurrentUserId();
        UUID newParentId = req.get("parentId");
        OrganizationNode node = hierarchyService.moveNode(id, newParentId, currentUserId);
        return ResponseEntity.ok(node);
    }

    @PostMapping("/nodes/bulk-move")
    @PreAuthorize("@authz.can('ORG_NODE_MOVE', 'TENANT', null)")
    public ResponseEntity<List<OrganizationNode>> bulkMoveNodes(@RequestBody Map<String, Object> req) {
        UUID currentUserId = getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<UUID> nodeIds = (List<UUID>) req.get("nodeIds");
        UUID newParentId = (UUID) req.get("parentId");
        List<OrganizationNode> nodes = hierarchyService.moveNodes(nodeIds, newParentId, currentUserId);
        return ResponseEntity.ok(nodes);
    }

    // ==================== COPY NODE ====================

    @PostMapping("/nodes/{id}/copy")
    @PreAuthorize("@authz.can('ORG_NODE_CREATE', 'TENANT', null)")
    public ResponseEntity<OrganizationNode> copyNode(@PathVariable UUID id, @RequestBody Map<String, Object> req) {
        UUID currentUserId = getCurrentUserId();
        UUID newParentId = (UUID) req.get("parentId");
        boolean includeDescendants = Boolean.TRUE.equals(req.get("includeDescendants"));
        OrganizationNode node = hierarchyService.copyNode(id, newParentId, currentUserId, includeDescendants);
        return ResponseEntity.status(201).body(node);
    }

    // ==================== DELETE NODE ====================

    @DeleteMapping("/nodes/{id}")
    @PreAuthorize("@authz.can('ORG_NODE_DELETE', 'TENANT', #id)")
    public ResponseEntity<Void> deleteNode(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean forceCascade) {
        UUID currentUserId = getCurrentUserId();
        hierarchyService.deleteNode(id, currentUserId, forceCascade);
        return ResponseEntity.noContent().build();
    }

    // ==================== BULK OPERATIONS ====================

    @PostMapping("/nodes/bulk-create")
    @PreAuthorize("@authz.can('ORG_NODE_CREATE', 'TENANT', null)")
    public ResponseEntity<OrganizationHierarchyService.BulkOperationResult> bulkCreate(@RequestBody Map<String, Object> req) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<OrganizationHierarchyService.CreateNodeRequest> requests = (List<OrganizationHierarchyService.CreateNodeRequest>) req.get("requests");
        OrganizationHierarchyService.BulkOperationResult result = hierarchyService.bulkCreate(tenantId, requests, currentUserId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/nodes/bulk-delete")
    @PreAuthorize("@authz.can('ORG_NODE_DELETE', 'TENANT', null)")
    public ResponseEntity<OrganizationHierarchyService.BulkOperationResult> bulkDelete(@RequestBody Map<String, Object> req) {
        UUID currentUserId = getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<UUID> nodeIds = (List<UUID>) req.get("nodeIds");
        boolean forceCascade = Boolean.TRUE.equals(req.get("forceCascade"));
        OrganizationHierarchyService.BulkOperationResult result = hierarchyService.bulkDelete(nodeIds, currentUserId, forceCascade);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/nodes/bulk-status")
    @PreAuthorize("@authz.can('ORG_NODE_UPDATE', 'TENANT', null)")
    public ResponseEntity<OrganizationHierarchyService.BulkOperationResult> bulkUpdateStatus(@RequestBody Map<String, Object> req) {
        UUID currentUserId = getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<UUID> nodeIds = (List<UUID>) req.get("nodeIds");
        OrganizationNodeStatus status = OrganizationNodeStatus.valueOf((String) req.get("status"));
        OrganizationHierarchyService.BulkOperationResult result = hierarchyService.bulkUpdateStatus(nodeIds, status, currentUserId);
        return ResponseEntity.ok(result);
    }

    // ==================== RESPONSIBLE ASSIGNMENT ====================

    @PutMapping("/nodes/{id}/responsible")
    @PreAuthorize("@authz.can('ORG_NODE_UPDATE', 'TENANT', #id)")
    public ResponseEntity<OrganizationHierarchyService.NodeDetails> assignResponsible(@PathVariable UUID id, @RequestBody Map<String, UUID> req) {
        UUID currentUserId = getCurrentUserId();
        UUID responsibleId = req.get("responsibleId");
        OrganizationHierarchyService.NodeDetails details = hierarchyService.assignResponsible(id, responsibleId, currentUserId);
        return ResponseEntity.ok(details);
    }

    // ==================== STATS ====================

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<OrganizationNodeType, Long>> getStats() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.getStatsByType(tenantId));
    }

    @GetMapping("/stats/count/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getCountByType(@PathVariable OrganizationNodeType type) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(hierarchyService.countByType(tenantId, type));
    }
}