package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.ForbiddenException;
import com.discipolat.common.exception.UnauthorizedException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized authorization service for multi-tenant scoped RBAC.
 * All permission checks go through this service.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final UserRepository userRepository;

    /**
     * Check if current user has a specific permission within a scope.
     * This is the main entry point for authorization checks.
     */
    public boolean can(String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.requireTenantId();
        return can(userId, tenantId, permissionKey, scopeType, scopeId);
    }

    /**
     * Check if a specific user has a permission within a scope.
     */
    @Transactional(readOnly = true)
    public boolean can(UUID userId, UUID tenantId, String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
        // Super-admin check (platform level)
        if (isPlatformSuperAdmin(userId)) {
            return true;
        }

        // Get user's memberships in this tenant
        List<TenantMembership> memberships = membershipRepository.findByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
        if (memberships.isEmpty()) {
            return false;
        }

        // Check each membership for the permission
        for (TenantMembership membership : memberships) {
            if (membershipMatchesScope(membership, scopeType, scopeId)) {
                if (roleHasPermission(membership.getRole(), permissionKey)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if user can view a resource (read access).
     */
    public boolean canView(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
        // Determine scope from resource
        MembershipScopeType scopeType = inferScopeFromResource(resourceType);
        UUID scopeId = inferScopeIdFromResource(resourceType, resourceId);
        String permissionKey = resourceType.toUpperCase() + "_READ";
        return can(userId, tenantId, permissionKey, scopeType, scopeId);
    }

    /**
     * Check if user can create a resource within a scope.
     */
    public boolean canCreate(UUID userId, UUID tenantId, String resourceType, MembershipScopeType scopeType, UUID scopeId) {
        String permissionKey = resourceType.toUpperCase() + "_CREATE";
        return can(userId, tenantId, permissionKey, scopeType, scopeId);
    }

    /**
     * Check if user can update a resource.
     */
    public boolean canUpdate(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
        MembershipScopeType scopeType = inferScopeFromResource(resourceType);
        UUID scopeId = inferScopeIdFromResource(resourceType, resourceId);
        String permissionKey = resourceType.toUpperCase() + "_UPDATE";
        return can(userId, tenantId, permissionKey, scopeType, scopeId);
    }

    /**
     * Check if user can delete a resource.
     */
    public boolean canDelete(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
        MembershipScopeType scopeType = inferScopeFromResource(resourceType);
        UUID scopeId = inferScopeIdFromResource(resourceType, resourceId);
        String permissionKey = resourceType.toUpperCase() + "_DELETE";
        return can(userId, tenantId, permissionKey, scopeType, scopeId);
    }

    /**
     * Require permission - throws ForbiddenException if not authorized.
     */
    public void require(String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
        if (!can(permissionKey, scopeType, scopeId)) {
            throw new ForbiddenException("Permission denied: " + permissionKey + " on " + scopeType + (scopeId != null ? ":" + scopeId : ""));
        }
    }

    /**
     * Require permission for current user - throws ForbiddenException if not authorized.
     */
    public void requireCurrentUser(String permissionKey, MembershipScopeType scopeType, UUID scopeId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.requireTenantId();
        if (!can(userId, tenantId, permissionKey, scopeType, scopeId)) {
            throw new ForbiddenException("Permission denied: " + permissionKey + " on " + scopeType + (scopeId != null ? ":" + scopeId : ""));
        }
    }

    /**
     * Get all permissions for current user in current tenant.
     */
    @Transactional(readOnly = true)
    public Set<String> getCurrentUserPermissions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.requireTenantId();
        return getUserPermissions(userId, tenantId);
    }

    /**
     * Get all permissions for a user in a tenant.
     */
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(UUID userId, UUID tenantId) {
        List<TenantMembership> memberships = membershipRepository.findByUserIdAndTenantIdAndStatusList(userId, tenantId, MembershipStatus.ACTIVE);
        Set<String> permissions = new HashSet<>();

        for (TenantMembership membership : memberships) {
            if (membership.getRole() != null) {
                Set<String> rolePerms = getRolePermissions(membership.getRole().getId());
                permissions.addAll(rolePerms);
            }
        }

        return permissions;
    }

    /**
     * Get effective permissions for a user within a specific scope.
     */
    @Transactional(readOnly = true)
    public Set<String> getUserPermissionsInScope(UUID userId, UUID tenantId, MembershipScopeType scopeType, UUID scopeId) {
        List<TenantMembership> memberships = membershipRepository.findByUserIdAndTenantIdAndStatusList(userId, tenantId, MembershipStatus.ACTIVE);
        Set<String> permissions = new HashSet<>();

        for (TenantMembership membership : memberships) {
            if (membershipMatchesScope(membership, scopeType, scopeId)) {
                if (membership.getRole() != null) {
                    Set<String> rolePerms = getRolePermissions(membership.getRole().getId());
                    permissions.addAll(rolePerms);
                }
            }
        }

        return permissions;
    }

    // ==================== PRIVATE HELPERS ====================

    public boolean isPlatformSuperAdmin(UUID userId) {
        return isPlatformSuperAdminInternal(userId);
    }

    private boolean isPlatformSuperAdminInternal(UUID userId) {
        // Check if user has PLATFORM_SUPER_ADMIN role in any tenant (global role)
        Optional<Role> superAdminRole = roleRepository.findByTenantIdIsNullAndKey("PLATFORM_SUPER_ADMIN");
        if (superAdminRole.isEmpty()) {
            return false;
        }
        return membershipRepository.existsByUserIdAndRoleIdAndStatus(userId, superAdminRole.get().getId(), MembershipStatus.ACTIVE);
    }

    private boolean membershipMatchesScope(TenantMembership membership, MembershipScopeType requiredScopeType, UUID requiredScopeId) {
        // TENANT scope matches everything within tenant
        if (membership.getScopeType() == MembershipScopeType.TENANT) {
            return true;
        }

        // Exact scope match
        if (membership.getScopeType() == requiredScopeType) {
            if (requiredScopeId == null || membership.getScopeId() == null) {
                return requiredScopeId == null && membership.getScopeId() == null;
            }
            return membership.getScopeId().equals(requiredScopeId);
        }

        // Hierarchy matching: parent scopes cover children
        // e.g., CHURCH scope covers SUB_CHURCH, CAMPUS, DEPARTMENT, FAMILY
        if (isParentScope(membership.getScopeType(), requiredScopeType)) {
            if (membership.getScopeId() != null && requiredScopeId != null) {
                return orgNodeRepository.isDescendantOf(requiredScopeId, membership.getScopeId());
            }
        }

        // ASSIGNED scope: user can only access specifically assigned resources
        if (membership.getScopeType() == MembershipScopeType.ASSIGNED) {
            return requiredScopeId != null && membership.getScopeId() != null
                    && membership.getScopeId().equals(requiredScopeId);
        }

        // OWN scope: only own resources
        if (membership.getScopeType() == MembershipScopeType.OWN) {
            UUID userId = SecurityUtils.getCurrentUserId();
            return requiredScopeId != null && requiredScopeId.equals(userId);
        }

        return false;
    }

    private boolean isParentScope(MembershipScopeType parent, MembershipScopeType child) {
        // Define hierarchy: TENANT > REGION > CHURCH > SUB_CHURCH/CAMPUS > DEPARTMENT > FAMILY > ASSIGNED/OWN
        return switch (parent) {
            case TENANT -> true; // TENANT covers all
            case REGION -> child == MembershipScopeType.CHURCH || child == MembershipScopeType.SUB_CHURCH
                    || child == MembershipScopeType.CAMPUS || child == MembershipScopeType.DEPARTMENT
                    || child == MembershipScopeType.FAMILY || child == MembershipScopeType.ASSIGNED;
            case CHURCH -> child == MembershipScopeType.SUB_CHURCH || child == MembershipScopeType.CAMPUS
                    || child == MembershipScopeType.DEPARTMENT || child == MembershipScopeType.FAMILY
                    || child == MembershipScopeType.ASSIGNED;
            case SUB_CHURCH, CAMPUS -> child == MembershipScopeType.DEPARTMENT || child == MembershipScopeType.FAMILY
                    || child == MembershipScopeType.ASSIGNED;
            case DEPARTMENT -> child == MembershipScopeType.FAMILY || child == MembershipScopeType.ASSIGNED;
            default -> false;
        };
    }

    private boolean roleHasPermission(Role role, String permissionKey) {
        if (role == null) return false;
        return role.getPermissions().stream()
                .anyMatch(p -> p.getKey().equalsIgnoreCase(permissionKey));
    }

    private Set<String> getRolePermissions(UUID roleId) {
        return permissionRepository.findByRoleId(roleId).stream()
                .map(Permission::getKey)
                .collect(Collectors.toSet());
    }

    private MembershipScopeType inferScopeFromResource(String resourceType) {
        return switch (resourceType.toUpperCase()) {
            case "TENANT", "USER", "ROLE", "PERMISSION", "SETTINGS", "BRANDING", "MODULES", "SUBSCRIPTION", "BILLING", "AUDIT" -> MembershipScopeType.TENANT;
            case "CHURCH", "CAMPUS", "SUB_CHURCH", "REGION" -> MembershipScopeType.CHURCH;
            case "DEPARTMENT" -> MembershipScopeType.DEPARTMENT;
            case "FAMILY", "GROUP" -> MembershipScopeType.FAMILY;
            case "MEMBER", "SOUL" -> MembershipScopeType.DEPARTMENT; // Members belong to departments
            case "REPORT" -> MembershipScopeType.DEPARTMENT; // Reports belong to department/family
            case "EVENT" -> MembershipScopeType.CHURCH;
            case "COURSE" -> MembershipScopeType.TENANT; // Courses can be tenant-global or local
            case "MESSAGE", "CONVERSATION" -> MembershipScopeType.TENANT;
            case "FILE" -> MembershipScopeType.TENANT;
            case "NOTIFICATION" -> MembershipScopeType.TENANT;
            case "FINANCE" -> MembershipScopeType.TENANT;
            default -> MembershipScopeType.TENANT;
        };
    }

    private UUID inferScopeIdFromResource(String resourceType, UUID resourceId) {
        // For resources that are organization nodes themselves, return their ID
        if (Set.of("CHURCH", "CAMPUS", "SUB_CHURCH", "REGION", "DEPARTMENT", "FAMILY", "GROUP").contains(resourceType.toUpperCase())) {
            return resourceId;
        }

        // For other resources, we'd need to look up their organization node
        // This is a simplified version - in practice, you'd query the resource's org node
        return null;
    }
}