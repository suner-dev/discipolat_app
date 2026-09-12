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
@RequestMapping("/api/v1/admin/roles")
public class RoleManagementController {

    private final RoleManagementService roleService;
    private final PermissionMatrixService permMatrixService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RoleManagementController(RoleManagementService roleService,
                                    PermissionMatrixService permMatrixService,
                                    UserRepository userRepository,
                                    AuditService auditService) {
        this.roleService = roleService;
        this.permMatrixService = permMatrixService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        return TenantContext.requireTenantId();
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    // ==================== ROLE CATALOG ====================

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Role>> getAllRoles() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(roleService.getAllRoles(tenantId));
    }

    @GetMapping("/system")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Role>> getSystemRoles() {
        return ResponseEntity.ok(roleService.getSystemRoles());
    }

    @GetMapping("/custom")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Role>> getCustomRoles() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(roleService.getCustomRoles(tenantId));
    }

    @GetMapping("/hierarchy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Role>> getRoleHierarchy() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(roleService.getRoleHierarchy(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Role> getRole(@PathVariable UUID id) {
        return roleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/key/{key}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Role> getRoleByKey(@PathVariable String key) {
        UUID tenantId = getCurrentTenantId();
        return roleService.findByKey(tenantId, key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Set<String>> getRolePermissions(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getRolePermissions(id));
    }

    // ==================== CREATE CUSTOM ROLE ====================

    @PostMapping
    @PreAuthorize("@authz.can('ROLE_CREATE', 'TENANT', null)")
    public ResponseEntity<Role> createRole(@RequestBody RoleManagementService.CreateRoleRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();
        Role role = roleService.createCustomRole(tenantId, request, currentUserId);
        return ResponseEntity.status(201).body(role);
    }

    // ==================== UPDATE ROLE ====================

    @PutMapping("/{id}")
    @PreAuthorize("@authz.can('ROLE_UPDATE', 'TENANT', null)")
    public ResponseEntity<Role> updateRole(@PathVariable UUID id, @RequestBody RoleManagementService.UpdateRoleRequest request) {
        UUID currentUserId = getCurrentUserId();
        Role role = roleService.updateRole(id, request, currentUserId);
        return ResponseEntity.ok(role);
    }

    // ==================== MANAGE ROLE PERMISSIONS ====================

    @PutMapping("/{id}/permissions")
    @PreAuthorize("@authz.can('PERMISSION_ASSIGN', 'TENANT', null)")
    public ResponseEntity<Role> assignPermissions(@PathVariable UUID id, @RequestBody Map<String, List<String>> req) {
        UUID currentUserId = getCurrentUserId();
        List<String> permissionKeys = req.get("permissionKeys");
        Role role = roleService.assignPermissions(id, permissionKeys, currentUserId);
        return ResponseEntity.ok(role);
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("@authz.can('PERMISSION_ASSIGN', 'TENANT', null)")
    public ResponseEntity<Role> addPermission(@PathVariable UUID id, @RequestBody Map<String, String> req) {
        UUID currentUserId = getCurrentUserId();
        String permissionKey = req.get("permissionKey");
        Role role = roleService.addPermission(id, permissionKey, currentUserId);
        return ResponseEntity.ok(role);
    }

    @DeleteMapping("/{id}/permissions/{permissionKey}")
    @PreAuthorize("@authz.can('PERMISSION_ASSIGN', 'TENANT', null)")
    public ResponseEntity<Role> removePermission(@PathVariable UUID id, @PathVariable String permissionKey) {
        UUID currentUserId = getCurrentUserId();
        Role role = roleService.removePermission(id, permissionKey, currentUserId);
        return ResponseEntity.ok(role);
    }

    // ==================== DELETE CUSTOM ROLE ====================

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can('ROLE_DELETE', 'TENANT', null)")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        UUID currentUserId = getCurrentUserId();
        roleService.deleteRole(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // ==================== VALIDATION ====================

    @GetMapping("/{id}/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoleManagementService.ValidationResult> validateRole(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.validateRolePermissions(id));
    }

    // ==================== ROLE ASSIGNMENT TO USERS ====================

    @PostMapping("/assign")
    @PreAuthorize("@authz.can('USER_MANAGE', 'TENANT', null)")
    public ResponseEntity<TenantMembership> assignRoleToUser(@RequestBody Map<String, Object> req) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();
        UUID userId = UUID.fromString((String) req.get("userId"));
        UUID roleId = UUID.fromString((String) req.get("roleId"));
        UUID scopeId = req.get("scopeId") != null ? UUID.fromString((String) req.get("scopeId")) : null;

        TenantMembership membership = roleService.assignRoleToUser(userId, tenantId, roleId, scopeId, currentUserId);
        return ResponseEntity.status(201).body(membership);
    }

    // ==================== PERMISSION CATALOG ====================

    @GetMapping("/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    @GetMapping("/permissions/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Permission>> getPermissionsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(roleService.getPermissionsByCategory(category));
    }

    @GetMapping("/permissions/scope/{scope}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Permission>> getPermissionsByScope(@PathVariable PermissionScope scope) {
        return ResponseEntity.ok(roleService.getPermissionsByScope(scope));
    }

    @GetMapping("/permissions/grouped/category")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, List<Permission>>> getPermissionsGroupedByCategory() {
        return ResponseEntity.ok(roleService.getPermissionsGroupedByCategory());
    }

    @GetMapping("/permissions/grouped/scope")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<PermissionScope, List<Permission>>> getPermissionsGroupedByScope() {
        return ResponseEntity.ok(roleService.getPermissionsGroupedByScope());
    }

    // ==================== PERMISSION MATRIX ====================

    @GetMapping("/matrix")
    @PreAuthorize("@authz.can('PERMISSION_READ', 'TENANT', null)")
    public ResponseEntity<PermissionMatrixService.PermissionMatrix> getFullMatrix() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(permMatrixService.getFullMatrix(tenantId));
    }

    @GetMapping("/matrix/scope/{scope}")
    @PreAuthorize("@authz.can('PERMISSION_READ', 'TENANT', null)")
    public ResponseEntity<PermissionMatrixService.PermissionMatrix> getMatrixByScope(@PathVariable PermissionScope scope) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(permMatrixService.getMatrixForScope(tenantId, scope));
    }

    @GetMapping("/matrix/user/{userId}")
    @PreAuthorize("@authz.can('USER_MANAGE', 'TENANT', null)")
    public ResponseEntity<PermissionMatrixService.UserPermissionMatrix> getUserMatrix(@PathVariable UUID userId) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(permMatrixService.getUserEffectivePermissions(userId, tenantId));
    }

    @GetMapping("/matrix/users")
    @PreAuthorize("@authz.can('USER_MANAGE', 'TENANT', null)")
    public ResponseEntity<List<PermissionMatrixService.UserPermissionSummary>> getAllUsersMatrix() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(permMatrixService.getAllUsersPermissions(tenantId));
    }

    @GetMapping("/matrix/node/{nodeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PermissionMatrixService.NodePermissionView> getNodeMatrix(@PathVariable UUID nodeId) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(permMatrixService.getNodePermissions(tenantId, nodeId));
    }

    @GetMapping("/matrix/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> checkPermission(@RequestParam UUID userId, @RequestParam String permissionKey, @RequestParam(required = false) UUID nodeId) {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(permMatrixService.userHasPermission(userId, tenantId, permissionKey, nodeId));
    }

    // ==================== AUDIT REPORT ====================

    @GetMapping("/audit")
    @PreAuthorize("@authz.can('AUDIT_READ', 'TENANT', null)")
    public ResponseEntity<PermissionMatrixService.PermissionAuditReport> getAuditReport() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(permMatrixService.generateAuditReport(tenantId));
    }
}