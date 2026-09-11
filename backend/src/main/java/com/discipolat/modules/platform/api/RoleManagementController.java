package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Gestion des rôles et permissions (Section 21-24 du prompt)
 * RBAC + Scope complet
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
public class RoleManagementController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final TenantMembershipRepository membershipRepository;
    private final AuditService auditService;

    public RoleManagementController(
            RoleService roleService,
            PermissionService permissionService,
            TenantMembershipRepository membershipRepository,
            AuditService auditService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.membershipRepository = membershipRepository;
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

    // ==================== LISTER LES RÔLES ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<RoleDetailResponse>> listRoles() {
        UUID tenantId = getCurrentTenantId();
        List<Role> roles = roleService.getAllRolesForTenant(tenantId);
        
        List<RoleDetailResponse> result = new ArrayList<>();
        for (Role role : roles) {
            List<Permission> permissions = new ArrayList<>();
            if (role.getPermissions() != null) {
                permissions = new ArrayList<>(role.getPermissions());
            }
            
            // Compter les utilisateurs avec ce rôle
            long userCount = membershipRepository.countByTenantIdAndStatus(tenantId, 
                    MembershipStatus.ACTIVE);
            
            result.add(new RoleDetailResponse(
                    role.getId(),
                    role.getTenantId(),
                    role.getKey(),
                    role.getLabel(),
                    role.getDescription(),
                    role.getSystem(),
                    role.getPriority(),
                    permissions.stream().map(Permission::getKey).toList(),
                    userCount
            ));
        }

        return ResponseEntity.ok(result);
    }

    // ==================== CRÉER UN RÔLE ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER')")
    public ResponseEntity<RoleDetailResponse> createRole(@RequestBody CreateRoleRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        // Vérifier unicité
        if (roleService.getRoles(tenantId).stream()
                .anyMatch(r -> r.getKey().equalsIgnoreCase(request.key()))) {
            throw new RuntimeException("Un rôle avec cette clé existe déjà");
        }

        Role role = roleService.createRole(tenantId, request.key(), request.label(),
                request.description(), currentUserId);

        // Assigner les permissions si fournies
        if (request.permissionKeys() != null && !request.permissionKeys().isEmpty()) {
            Set<UUID> permissionIds = new HashSet<>();
            for (String permKey : request.permissionKeys()) {
                Optional<Permission> perm = permissionService.getByKey(permKey);
                if (perm.isPresent()) {
                    permissionIds.add(perm.get().getId());
                }
            }
            if (!permissionIds.isEmpty()) {
                roleService.assignPermissions(role.getId(), permissionIds, currentUserId);
            }
        }

        auditService.log(currentUserId, tenantId, "ROLE_CREATED", "TENANT",
                role.getId(), "SUCCESS",
                Map.of("key", request.key(), "label", request.label()),
                null, null, null);

        return ResponseEntity.status(201).body(toRoleDetailResponse(role));
    }

    // ==================== MODIFIER UN RÔLE ====================

    @PutMapping("/{roleId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<RoleDetailResponse> updateRole(
            @PathVariable UUID roleId,
            @RequestBody UpdateRoleRequest request) {
        
        UUID currentUserId = getCurrentUserId();
        Role role = roleService.updateRole(roleId, request.label(), request.description(),
                request.priority(), currentUserId);

        // Mettre à jour les permissions si fournies
        if (request.permissionKeys() != null) {
            Set<UUID> permissionIds = new HashSet<>();
            for (String permKey : request.permissionKeys()) {
                Optional<Permission> perm = permissionService.getByKey(permKey);
                if (perm.isPresent()) {
                    permissionIds.add(perm.get().getId());
                }
            }
            roleService.assignPermissions(roleId, permissionIds, currentUserId);
        }

        auditService.log(currentUserId, role.getTenantId(), "ROLE_UPDATED", "TENANT",
                roleId, "SUCCESS",
                Map.of("key", role.getKey()),
                null, null, null);

        return ResponseEntity.ok(toRoleDetailResponse(role));
    }

    // ==================== SUPPRIMER UN RÔLE ====================

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        UUID currentUserId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        roleService.deleteRole(roleId, currentUserId);

        auditService.log(currentUserId, tenantId, "ROLE_DELETED", "TENANT",
                roleId, "SUCCESS", Map.of(), null, null, null);

        return ResponseEntity.noContent().build();
    }

    // ==================== LISTER LES PERMISSIONS ====================

    @GetMapping("/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PermissionResponse>> listPermissions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) PermissionScope scope) {
        
        UUID tenantId = getCurrentTenantId();
        List<Permission> permissions;

        if (scope != null) {
            permissions = permissionService.getPermissionsByScope(scope);
        } else if (category != null) {
            permissions = permissionService.getPermissionsByCategory(category);
        } else {
            permissions = permissionService.getTenantPermissions(tenantId);
        }

        List<PermissionResponse> result = permissions.stream()
                .map(p -> new PermissionResponse(
                        p.getId(),
                        p.getKey(),
                        p.getLabel(),
                        p.getDescription(),
                        p.getScope(),
                        p.getCategory(),
                        p.getSystem(),
                        p.getTenantId()
                )).toList();

        return ResponseEntity.ok(result);
    }

    // ==================== CRÉER UNE PERMISSION ====================

    @PostMapping("/permissions")
    @PreAuthorize("hasAnyRole('TENANT_OWNER')")
    public ResponseEntity<PermissionResponse> createPermission(@RequestBody CreatePermissionRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        Permission permission = permissionService.createPermission(
                tenantId,
                request.key(),
                request.label(),
                request.description(),
                request.scope(),
                request.category(),
                currentUserId
        );

        auditService.log(currentUserId, tenantId, "PERMISSION_CREATED", "TENANT",
                permission.getId(), "SUCCESS",
                Map.of("key", request.key(), "scope", request.scope().name()),
                null, null, null);

        return ResponseEntity.status(201).body(new PermissionResponse(
                permission.getId(),
                permission.getKey(),
                permission.getLabel(),
                permission.getDescription(),
                permission.getScope(),
                permission.getCategory(),
                permission.getSystem(),
                permission.getTenantId()
        ));
    }

    // ==================== LISTER LES RÔLES PAR UTILISATEUR ====================

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<UserRolesResponse> getRolesForUser(@PathVariable UUID userId) {
        UUID tenantId = getCurrentTenantId();

        List<TenantMembership> memberships = membershipRepository
                .findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);

        List<RoleInfo> roles = new ArrayList<>();
        for (TenantMembership m : memberships) {
            if (m.getTenantId().equals(tenantId)) {
                Optional<Role> role = roleService.getRoles(tenantId).stream()
                        .filter(r -> r.getKey().equals(m.getRole()))
                        .findFirst();
                
                roles.add(new RoleInfo(
                        m.getRole(),
                        m.getStatus().name(),
                        role.flatMap(r -> Optional.of(r.getLabel())).orElse(m.getRole()),
                        m.getJoinedAt()
                ));
            }
        }

        return ResponseEntity.ok(new UserRolesResponse(userId, roles));
    }

    // ==================== DTOs ====================

    public record RoleDetailResponse(
            UUID id, UUID tenantId, String key, String label, String description,
            boolean isSystem, int priority, List<String> permissionKeys, long userCount
    ) {}

    public record PermissionResponse(
            UUID id, String key, String label, String description,
            PermissionScope scope, String category, boolean isSystem, UUID tenantId
    ) {}

    public record UserRolesResponse(UUID userId, List<RoleInfo> roles) {}
    public record RoleInfo(String role, String status, String label, java.time.Instant joinedAt) {}

    public record CreateRoleRequest(
            String key, String label, String description, List<String> permissionKeys
    ) {}

    public record UpdateRoleRequest(
            String label, String description, Integer priority, List<String> permissionKeys
    ) {}

    public record CreatePermissionRequest(
            String key, String label, String description,
            PermissionScope scope, String category
    ) {}
}
