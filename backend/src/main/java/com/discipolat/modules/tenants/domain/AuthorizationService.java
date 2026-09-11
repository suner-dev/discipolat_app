package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AUTHORIZATION SERVICE - Vérification Centralisée d'Autorisation
 * 
 * RÈGLE D'OR (Section 78 du prompt) :
 * Chaque requête métier doit être validée :
 * WHO ARE YOU? + WHICH TENANT? + WHICH MEMBERSHIP? + WHICH ROLE?
 * + WHICH PERMISSION? + WHICH SCOPE? + WHICH RESOURCE?
 * + DO YOU OWN / CONTROL THIS RESOURCE?
 * 
 * Si une seule condition échoue : ACCESS DENIED
 */
@Service
public class AuthorizationService {

    private final TenantMembershipRepository membershipRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public AuthorizationService(
            TenantMembershipRepository membershipRepository,
            PermissionRepository permissionRepository,
            RoleRepository roleRepository) {
        this.membershipRepository = membershipRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    // ==================== VÉRIFICATION DE BASE ====================

    /**
     * Vérifie si l'utilisateur courant a l'authentification requise
     */
    public boolean isAuthenticated() {
        UUID userId = getCurrentUserId();
        return userId != null && membershipRepository.existsByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
    }

    /**
     * Obtient l'ID utilisateur courant depuis le contexte
     */
    private UUID getCurrentUserId() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.discipolat.modules.users.domain.User user) {
                return user.getId();
            }
        } catch (Exception e) {
            // Fallback si pas dans SecurityContext
        }
        return null;
    }

    // ==================== VÉRIFICATION TENANT ====================

    /**
     * Vérifie que l'utilisateur appartient au tenant donné
     */
    public boolean hasTenantAccess(UUID userId, UUID tenantId) {
        return membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
    }

    /**
     * Vérifie que l'utilisateur a le rôle requis dans le tenant
     */
    public boolean hasRole(UUID userId, UUID tenantId, String requiredRole) {
        Optional<TenantMembership> membership = membershipRepository
                .findByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
        
        if (membership.isEmpty()) return false;
        
        // Vérifier rôle direct
        if (membership.get().getRole().equalsIgnoreCase(requiredRole)) {
            return true;
        }
        
        // Vérifier dans les permissions du rôle
        Set<String> userRoles = getAllUserRolesInTenant(userId, tenantId);
        return userRoles.contains(requiredRole.toUpperCase());
    }

    /**
     * Vérifie si l'utilisateur a l'un des rôles donnés
     */
    public boolean hasAnyRole(UUID userId, UUID tenantId, String... requiredRoles) {
        Set<String> userRoles = getAllUserRolesInTenant(userId, tenantId);
        for (String role : requiredRoles) {
            if (userRoles.contains(role.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si l'utilisateur a TOUS les rôles donnés
     */
    public boolean hasAllRoles(UUID userId, UUID tenantId, String... requiredRoles) {
        Set<String> userRoles = getAllUserRolesInTenant(userId, tenantId);
        for (String role : requiredRoles) {
            if (!userRoles.contains(role.toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    // ==================== VÉRIFICATION PERMISSIONS ====================

    /**
     * Vérifie si l'utilisateur a la permission donnée (scoped ou globale)
     */
    public boolean hasPermission(UUID userId, UUID tenantId, String permissionKey) {
        Set<String> permissions = getAllPermissionsForUserInTenant(userId, tenantId);
        return permissions.contains(permissionKey.toUpperCase());
    }

    /**
     * Vérifie si l'utilisateur a la permission avec scope spécifique
     */
    public boolean hasPermissionInScope(
            UUID userId, 
            UUID tenantId, 
            String permissionKey, 
            PermissionScope scope, 
            UUID scopeId) {
        
        // Vérifier permission globale d'abord
        if (hasPermission(userId, tenantId, permissionKey)) {
            return true;
        }
        
        // Vérifier permission scoped
        Set<String> scopedPermissions = getScopedPermissionsForUser(
                userId, tenantId, permissionKey, scope, scopeId);
        return !scopedPermissions.isEmpty();
    }

    /**
     * Vérifie si l'utilisateur peut voir la ressource
     */
    public boolean canView(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
        // Déterminer le scope de la ressource
        PermissionScope scope = getResourceScope(resourceType, resourceId);
        String viewPermission = resourceType.toUpperCase() + "_VIEW";
        
        if (scope == PermissionScope.PLATFORM) {
            return hasPermission(userId, tenantId, viewPermission);
        }
        
        // Pour les ressources tenant-scoped, vérifier l'accès tenant + permission
        if (!hasTenantAccess(userId, tenantId)) {
            return false;
        }
        
        return hasPermissionInScope(userId, tenantId, viewPermission, scope, resourceId);
    }

    /**
     * Vérifie si l'utilisateur peut créer dans le scope donné
     */
    public boolean canCreate(UUID userId, UUID tenantId, String resourceType, UUID scopeId) {
        if (!hasTenantAccess(userId, tenantId)) {
            return false;
        }
        
        String createPermission = resourceType.toUpperCase() + "_CREATE";
        PermissionScope scope = getResourceScope(resourceType, scopeId);
        
        return hasPermissionInScope(userId, tenantId, createPermission, scope, scopeId);
    }

    /**
     * Vérifie si l'utilisateur peut modifier la ressource
     */
    public boolean canUpdate(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
        if (!hasTenantAccess(userId, tenantId)) {
            return false;
        }
        
        String updatePermission = resourceType.toUpperCase() + "_UPDATE";
        PermissionScope scope = getResourceScope(resourceType, resourceId);
        
        return hasPermissionInScope(userId, tenantId, updatePermission, scope, resourceId);
    }

    /**
     * Vérifie si l'utilisateur peut supprimer la ressource
     */
    public boolean canDelete(UUID userId, UUID tenantId, String resourceType, UUID resourceId) {
        if (!hasTenantAccess(userId, tenantId)) {
            return false;
        }
        
        String deletePermission = resourceType.toUpperCase() + "_DELETE";
        PermissionScope scope = getResourceScope(resourceType, resourceId);
        
        return hasPermissionInScope(userId, tenantId, deletePermission, scope, resourceId);
    }

    // ==================== VÉRIFICATION SCOPE HIÉRARCHIQUE ====================

    /**
     * Vérifie si l'utilisateur a accès au scope hiérarchique (église, département, etc.)
     */
    public boolean hasOrganizationalScope(
            UUID userId, 
            UUID tenantId, 
            OrganizationNodeType nodeType, 
            UUID nodeId) {
        
        if (!hasTenantAccess(userId, tenantId)) {
            return false;
        }

        Optional<OrganizationNode> node = getOrganizationNode(nodeId);
        if (node.isEmpty() || !node.get().getTenantId().equals(tenantId)) {
            return false;
        }

        // Vérifier si l'utilisateur a un scope qui inclut ce node
        Set<UUID> userScopes = getUserOrganizationScopes(userId, tenantId);
        
        // L'utilisateur a accès si :
        // 1. Il a le scope exact
        if (userScopes.contains(nodeId)) {
            return true;
        }
        
        // 2. Il a un scope parent qui contient ce node
        for (UUID userScopeId : userScopes) {
            Optional<OrganizationNode> userScopeNode = getOrganizationNode(userScopeId);
            if (userScopeNode.isPresent() && isAncestorOrSelf(userScopeNode.get(), node.get())) {
                return true;
            }
        }
        
        // 3. Son rôle a accès global au type de node
        String viewPermission = nodeType.name() + "_VIEW";
        return hasPermission(userId, tenantId, viewPermission);
    }

    // ==================== SERVICES PRIVÉS ====================

    private Set<String> getAllUserRolesInTenant(UUID userId, UUID tenantId) {
        Set<String> roles = new HashSet<>();
        
        List<TenantMembership> memberships = membershipRepository
                .findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
        
        for (TenantMembership m : memberships) {
            if (m.getTenantId().equals(tenantId)) {
                roles.add(m.getRole());
                
                // Ajouter les permissions du rôle
                Optional<Role> role = roleRepository.findByTenantIdAndKey(tenantId, m.getRole());
                if (role.isPresent()) {
                    // Dans un système complet, on aurait aussi les rôles système
                    role.get().getPermissions().forEach(p -> 
                        roles.addAll(getPermissionKeys(p)));
                }
            }
        }
        
        return roles;
    }

    private Set<String> getAllPermissionsForUserInTenant(UUID userId, UUID tenantId) {
        Set<String> permissions = new HashSet<>();
        
        List<TenantMembership> memberships = membershipRepository
                .findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
        
        for (TenantMembership m : memberships) {
            if (m.getTenantId().equals(tenantId)) {
                Optional<Role> role = roleRepository.findByTenantIdAndKey(tenantId, m.getRole());
                if (role.isPresent()) {
                    role.get().getPermissions().forEach(p -> permissions.add(p.getKey()));
                }
            }
        }
        
        return permissions;
    }

    private Set<String> getScopedPermissionsForUser(
            UUID userId, UUID tenantId, String permissionKey, 
            PermissionScope scope, UUID scopeId) {
        
        Set<String> result = new HashSet<>();
        
        List<TenantMembership> memberships = membershipRepository
                .findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
        
        for (TenantMembership m : memberships) {
            if (m.getTenantId().equals(tenantId)) {
                Optional<Role> role = roleRepository.findByTenantIdAndKey(tenantId, m.getRole());
                if (role.isPresent()) {
                    for (Permission perm : role.get().getPermissions()) {
                        if (perm.getKey().equalsIgnoreCase(permissionKey)) {
                            if (perm.getScope() == scope || perm.getScope() == PermissionScope.PLATFORM) {
                                if (perm.getScopeId() == null || perm.getScopeId().equals(scopeId)) {
                                    result.add(perm.getKey());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }

    private Set<UUID> getUserOrganizationScopes(UUID userId, UUID tenantId) {
        Set<UUID> scopes = new HashSet<>();
        
        List<TenantMembership> memberships = membershipRepository
                .findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
        
        for (TenantMembership m : memberships) {
            if (m.getTenantId().equals(tenantId)) {
                // Dans un système complet, on aurait une table MembershipScope
                // Pour l'instant, on utilise le role comme scope
                scopes.add(m.getTenantId());
            }
        }
        
        return scopes;
    }

    private PermissionScope getResourceScope(String resourceType, UUID resourceId) {
        // Mapping des types de ressources vers leurs scopes
        // Dans un système complet, chaque ressource aurait son propre scope
        switch (resourceType.toUpperCase()) {
            case "TENANT":
            case "USER":
            case "SETTINGS":
                return PermissionScope.TENANT;
            case "CHURCH":
            case "EGLISE":
                return PermissionScope.CHURCH;
            case "SUB_CHURCH":
                return PermissionScope.SUB_CHURCH;
            case "CAMPUS":
                return PermissionScope.CAMPUS;
            case "DEPARTMENT":
            case "DEPARTEMENT":
                return PermissionScope.DEPARTMENT;
            case "FAMILY":
            case "GROUPE":
            case "GROUPS":
                return PermissionScope.FAMILY;
            case "REGION":
                return PermissionScope.REGION;
            case "MEMBER":
                return PermissionScope.OWN;
            default:
                return PermissionScope.TENANT;
        }
    }

    private Optional<OrganizationNode> getOrganizationNode(UUID nodeId) {
        // Dans un système complet, injecter OrganizationNodeRepository
        return Optional.empty();
    }

    private boolean isAncestorOrSelf(OrganizationNode ancestor, OrganizationNode node) {
        if (ancestor.getId().equals(node.getId())) {
            return true;
        }
        // Vérifier si le path de node commence par le path de ancestor
        return node.getPath().startsWith(ancestor.getPath());
    }

    private Set<String> getPermissionKeys(Permission perm) {
        Set<String> keys = new HashSet<>();
        keys.add(perm.getKey());
        // Ajouter les permissions parentes si hiérarchie
        return keys;
    }

    // ==================== VERIFICATIONS RAPIDES ====================

    /**
     * Vérification rapide : utilisateur authentifié + tenant valide
     */
    public void requireAuthenticated() {
        if (!isAuthenticated()) {
            throw new SecurityException("Authentification requise");
        }
    }

    /**
     * Vérification rapide : accès tenant obligatoire
     */
    public void requireTenantAccess(UUID tenantId) {
        UUID userId = getCurrentUserId();
        if (userId == null || !hasTenantAccess(userId, tenantId)) {
            throw new SecurityException("Accès au tenant refusé");
        }
    }

    /**
     * Vérification rapide : rôle requis
     */
    public void requireRole(UUID tenantId, String... requiredRoles) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("Authentification requise");
        }
        
        boolean hasRole = false;
        for (String role : requiredRoles) {
            if (hasRole(userId, tenantId, role)) {
                hasRole = true;
                break;
            }
        }
        
        if (!hasRole) {
            throw new SecurityException("Rôle requis: " + String.join(", ", requiredRoles));
        }
    }

    /**
     * Vérification rapide : permission requise
     */
    public void requirePermission(UUID tenantId, String permissionKey) {
        UUID userId = getCurrentUserId();
        if (userId == null || !hasPermission(userId, tenantId, permissionKey)) {
            throw new SecurityException("Permission requise: " + permissionKey);
        }
    }
}
