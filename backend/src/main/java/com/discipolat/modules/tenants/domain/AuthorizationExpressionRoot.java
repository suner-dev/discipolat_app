package com.discipolat.modules.tenants.domain;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Custom security expression root that exposes AuthorizationService methods
 * for use in @PreAuthorize annotations.
 *
 * Usage:
 * @PreAuthorize("@authz.can('MEMBER_READ', 'DEPARTMENT', #deptId)")
 * @PreAuthorize("@authz.canView('MEMBER', #memberId)")
 * @PreAuthorize("@authz.canCreate('EVENT', 'CHURCH', #churchId)")
 */
public class AuthorizationExpressionRoot extends SecurityExpressionRoot
        implements MethodSecurityExpressionOperations {

    private final AuthorizationService authzService;
    private Object filterObject;
    private Object returnObject;
    private Object targetDomainObject;

    public AuthorizationExpressionRoot(Authentication authentication, AuthorizationService authzService) {
        super(authentication);
        this.authzService = authzService;
    }

    /**
     * Check permission with explicit scope type and scope ID.
     * Equivalent to authzService.can(userId, tenantId, permission, scopeType, scopeId)
     */
    public boolean can(String permissionKey, String scopeType, String scopeId) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();
        if (userId == null || tenantId == null) return false;
        
        try {
            MembershipScopeType scope = MembershipScopeType.valueOf(scopeType.toUpperCase());
            UUID scopeUuid = scopeId != null ? UUID.fromString(scopeId) : null;
            return authzService.can(userId, tenantId, permissionKey, scope, scopeUuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check permission with scope ID as UUID parameter.
     */
    public boolean can(String permissionKey, String scopeType, UUID scopeId) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();
        if (userId == null || tenantId == null) return false;
        
        try {
            MembershipScopeType scope = MembershipScopeType.valueOf(scopeType.toUpperCase());
            return authzService.can(userId, tenantId, permissionKey, scope, scopeId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check view permission for a resource.
     * Infers scope from resource type.
     */
    public boolean canView(String resourceType, String resourceId) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();
        if (userId == null || tenantId == null || resourceId == null) return false;
        
        try {
            return authzService.canView(userId, tenantId, resourceType, UUID.fromString(resourceId));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check create permission for a resource within a scope.
     */
    public boolean canCreate(String resourceType, String scopeType, String scopeId) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();
        if (userId == null || tenantId == null) return false;
        
        try {
            MembershipScopeType scope = MembershipScopeType.valueOf(scopeType.toUpperCase());
            UUID scopeUuid = scopeId != null ? UUID.fromString(scopeId) : null;
            return authzService.canCreate(userId, tenantId, resourceType, scope, scopeUuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check update permission for a resource.
     */
    public boolean canUpdate(String resourceType, String resourceId) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();
        if (userId == null || tenantId == null || resourceId == null) return false;
        
        try {
            return authzService.canUpdate(userId, tenantId, resourceType, UUID.fromString(resourceId));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check delete permission for a resource.
     */
    public boolean canDelete(String resourceType, String resourceId) {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();
        if (userId == null || tenantId == null || resourceId == null) return false;
        
        try {
            return authzService.canDelete(userId, tenantId, resourceType, UUID.fromString(resourceId));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if current user is platform super admin.
     */
    public boolean isPlatformSuperAdmin() {
        UUID userId = getCurrentUserId();
        if (userId == null) return false;
        return authzService.isPlatformSuperAdmin(userId);
    }

    /**
     * Check if user has role (legacy support - checks active role).
     */
    public boolean hasRole(String role) {
        // This checks the active role from JWT claims
        Authentication auth = getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.toUpperCase()));
    }

    /**
     * Check if user has any of the given roles.
     */
    public boolean hasAnyRole(String... roles) {
        Authentication auth = getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String authority = a.getAuthority().replace("ROLE_", "");
                    for (String r : roles) {
                        if (authority.equalsIgnoreCase(r)) return true;
                    }
                    return false;
                });
    }

    private UUID getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UUID) return (UUID) principal;
        if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private UUID getCurrentTenantId() {
        // Get from TenantContext
        return com.discipolat.common.multitenancy.TenantContext.getTenantId();
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return targetDomainObject;
    }
}