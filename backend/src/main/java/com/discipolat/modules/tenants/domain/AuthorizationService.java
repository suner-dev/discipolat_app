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
    private final PermissionRepository permissionRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final UserRepository userRepository;
    private final MemberRoleAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;

    private static final Set<String> TENANT_ADMIN_ROLE_KEYS = Set.of(
            "ADMIN", "PASTEUR", "TENANT_OWNER", "TENANT_ADMIN"
    );

    /**
     * Clé du rôle propriétaire. Volontairement <b>absente</b> de
     * {@link #TENANT_ADMIN_ROLE_KEYS} côté « propriétaire » : elle y figure pour
     * l'héritage des droits d'administration, mais les opérations de propriété
     * (transfert, délégation) exigent <b>exactement</b> cette clé.
     */
    private static final String TENANT_OWNER_ROLE_KEY = "TENANT_OWNER";

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
        List<TenantMembership> memberships = membershipRepository.findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
        if (memberships.isEmpty()) {
            // PARAMÉTRISATION PAR ÉGLISE (Phase 1) — équivalence STRICTE avec
            // `hasAnyRole(...)` : le garde legacy n'exigeait AUCUNE appartenance,
            // le pont d'équivalence doit donc rester consultable ici aussi, sous
            // peine de RÉTRÉCIR l'accès d'un porteur de jeton valide. Sans
            // appartenance ni autorité active, la décision reste `false`
            // (fail-closed) — le comportement des endpoint `isAuthenticated()`
            // et `@authz.can` sans matrice dédiée n'est pas élargi.
            return grantsViaActiveAuthorities(permissionKey, tenantId, scopeType, scopeId);
        }

        // Check each membership for the permission
        for (TenantMembership membership : memberships) {
            if (membershipMatchesScope(membership, scopeType, scopeId)) {
                if (roleHasPermission(membership.getRole(), permissionKey)) {
                    return true;
                }
            }
        }

        // SPEC_ORGANISATION_MODULABLE_V3 §C / T-B12 — affiliation multi-nœuds.
        // Une permission est AUSSI accordée si le membre porte un rôle-capacité
        // la contenant sur le nœud cible OU un ancêtre de celui-ci (descendance
        // par path). Purement ADDITIVE : découplée de l'appartenance, et sans
        // jamais dépendre d'un intitulé (V3-B). La capacité = `role.permissions`.
        if (hasAssignmentPermission(userId, tenantId, permissionKey, scopeId)) {
            return true;
        }

        // PARAMÉTRISATION PAR ÉGLISE (Phase 1) — pont d'équivalence avec les
        // gardes à littéral de rôle. Une permission est ENCORE accordée si l'un
        // des RÔLES ACTIFS du porteur de jeton (autorité Spring `ROLE_<clé>`)
        // la détient dans la matrice par défaut. La matrice (migration V258)
        // n'accorde une clé qu'aux rôles qui la détenaient déjà dans le
        // littéral `hasAnyRole(...)`, et le filtre JWT attribue déjà
        // `ROLE_PASTEUR` à un ADMIN actif : la décision reproduit donc
        // exactement `hasAnyRole`, sans jamais élargir l'accès. Purement
        // ADDITIVE — ne retire aucun droit, et ne change rien tant qu'un
        // endpoint n'a pas basculé de `hasAnyRole(...)` vers `@authz.can('<CLÉ>')`.
        // Restreinte aux vérifications de portée tenant : l'isolation par scope
        // (DEPARTMENT/CHURCH/FAMILY/ASSIGNED/OWN, éprouvée par
        // MultiTenantSecurityTests) reste portée uniquement par les appartenances
        // et les assignations V3 — le pont ne saute jamais par-dessus un scope.
        return grantsViaActiveAuthorities(permissionKey, tenantId, scopeType, scopeId);
    }

    /**
     * Le pont d'équivalence ne reproduit que les littéraux de rôle legacy,
     * qui étaient toujours de portée tenant (le filtrage par nœud se faisait
     * dans les services). Un appel scopé sur un nœud ({@code scopeId != null}
     * ou scope autre que TENANT) ne doit JAMAIS être accordé par les seules
     * autorités du jeton : sinon le pont élargirait l'accès au-delà de
     * l'appartenance, ce qu'il est conçu pour ne jamais faire.
     */
    private boolean grantsViaActiveAuthorities(String permissionKey, UUID tenantId,
                                               MembershipScopeType scopeType, UUID scopeId) {
        if (scopeType != MembershipScopeType.TENANT || scopeId != null) {
            return false;
        }
        return activeAuthorityGrants(permissionKey, tenantId);
    }

    /**
     * Clés de rôle portées par l'authentification courante (autorité Spring
     * {@code ROLE_<clé>}), normalisées en MAJUSCULES. Vide hors requête
     * authentifiée. Sert au pont d'équivalence rôle-actif → permission.
     *
     * <p>La lecture du {@code SecurityContextHolder} est déléguée à
     * {@link SecurityUtils#getActiveAuthorityRoleKeys()} (couche infrastructure) :
     * le domaine reste exempt de toute dépendance Spring Security (règle R1),
     * exactement comme pour {@link SecurityUtils#getCurrentUserId()} déjà utilisé
     * ici.
     */
    private Set<String> activeRoleKeys() {
        return SecurityUtils.getActiveAuthorityRoleKeys();
    }

    /**
     * L'un des rôles actifs (autorité Spring) détient-il {@code permissionKey}
     * dans la matrice ? Résolution par clé de rôle GLOBALLE (les rôles legacy
     * ADMIN/PASTEUR/… sont des rôles système, {@code tenant_id IS NULL}) ; si
     * l'église a créé une surcouche locale pour cette clé, elle prime.
     */
    private boolean activeAuthorityGrants(String permissionKey, UUID tenantId) {
        for (String roleKey : activeRoleKeys()) {
            Role role = roleRepository.findByTenantIdAndKey(tenantId, roleKey)
                    .or(() -> roleRepository.findGlobalByKey(roleKey))
                    .orElse(null);
            if (role != null && roleHasPermission(role, permissionKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * PARAMÉTRISATION PAR ÉGLISE (Phase 2-client) — MIROIR SERVEUR du pont
     * d'équivalence : toutes les clés permission accordées à l'authentification
     * COURANTE par ses autorités actives (rôles globaux de la matrice V258 ;
     * la surcouche tenant — personnalisée par chaque église — prime et redefinit
     * l'ensemble). Vide hors requête authentifiée : le set servi reste
     * calculable hors session (nomination, job) sans élargir quoi que ce soit.
     *
     * <p>Ce miroir est la condition d'équivalence du client : l'ensemble SERVI
     * par {@code GET /api/v1/me/permissions} doit couvrir exactement ce que les
     * gardes convertis {@code @authz.can('<CLÉ>')} admettent en portée tenant —
     * sinon l'UI cacherait des actions qu'un porteur de jeton sans appartenance
     * peut bel et bien exécuter (les gardes legacy ne l'exigeaient pas).
     *
     * <p>Pas d'annotation {@code @Transactional} ajoutée ici : la règle gelée R4
     * (domaine sans annotations de conteneur) compte chaque méthode annotée —
     * l'appelant ({@code PermissionResolver.resolvePermissions}, {@code can(…)})
     * porte déjà la frontière transactionnelle readOnly.
     */
    public Set<String> permissionKeysForActiveAuthorities(UUID tenantId) {
        Set<String> keys = new LinkedHashSet<>();
        for (String roleKey : activeRoleKeys()) {
            Role role = roleRepository.findByTenantIdAndKey(tenantId, roleKey)
                    .or(() -> roleRepository.findGlobalByKey(roleKey))
                    .orElse(null);
            if (role != null && role.getPermissions() != null) {
                role.getPermissions().forEach(p -> keys.add(p.getKey()));
            }
        }
        return keys;
    }

    /**
     * V3 §C — le membre porte-t-il, via une {@link MemberRoleAssignment} ACTIVE,
     * un rôle contenant {@code permissionKey} et couvrant le nœud {@code scopeId}
     * (assignation de portée tenant, ou posée sur le nœud / un ancêtre) ?
     */
    private boolean hasAssignmentPermission(UUID userId, UUID tenantId, String permissionKey, UUID scopeId) {
        List<MemberRoleAssignment> active = assignmentRepository.findByTenantIdAndUserIdAndStatus(
                tenantId, userId, MemberRoleAssignment.AssignmentStatus.ACTIVE);
        if (active.isEmpty()) {
            return false;
        }
        String targetPath = scopeId == null ? null : orgNodeRepository.findById(scopeId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .map(OrganizationNode::getPath).orElse(null);
        // scopeId fourni mais inconnu/étranger au tenant → aucune couverture nœud.
        if (scopeId != null && targetPath == null) {
            return false;
        }
        for (MemberRoleAssignment a : active) {
            if (!coversScopePath(a.getNodeId(), targetPath, tenantId)) {
                continue;
            }
            Set<String> perms = permissionRepository.findByRoleId(a.getRoleId()).stream()
                    .map(Permission::getKey).collect(Collectors.toSet());
            if (perms.stream().anyMatch(p -> p.equalsIgnoreCase(permissionKey))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Une assignation couvre la cible si elle est de portée tenant
     * ({@code nodeId == null}) ou posée sur un ancêtre (ou le nœud lui-même)
     * de {@code targetPath}. Sans cible nœud ({@code targetPath == null}),
     * seules les assignations tenant comptent.
     */
    private boolean coversScopePath(UUID assignmentNodeId, String targetPath, UUID tenantId) {
        if (assignmentNodeId == null) {
            return true; // portée tenant : couvre tout le tenant
        }
        if (targetPath == null) {
            return false;
        }
        String ancestorPath = orgNodeRepository.findById(assignmentNodeId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .map(OrganizationNode::getPath).orElse(null);
        if (ancestorPath == null) {
            return false;
        }
        return targetPath.equals(ancestorPath) || targetPath.startsWith(ancestorPath + ".");
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

    /** True when the active user has an administrator membership in the current tenant. */
    public boolean isTenantAdmin() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.requireTenantId();
        return isTenantAdmin(userId, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean isTenantAdmin(UUID userId, UUID tenantId) {
        return membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                        userId, tenantId, MembershipStatus.ACTIVE).stream()
                .filter(membership -> membership.getScopeType() == MembershipScopeType.TENANT)
                .anyMatch(membership -> {
                    String roleKey = membership.getRole() != null
                            ? membership.getRole().getKey() : membership.getRoleLegacy();
                    return roleKey != null
                            && TENANT_ADMIN_ROLE_KEYS.contains(roleKey.trim().toUpperCase(Locale.ROOT));
                });
    }

    /**
     * True when the user owns the current tenant ({@code TENANT_OWNER}).
     *
     * <p>SPF ONBOARDING FLOWS §7.0 / T-B0 (F10) : distingue le propriétaire
     * (« le roi ») d'un administrateur simplement délégué. Réservé aux
     * opérations de propriété : {@code transfer}, {@code promote-admin},
     * {@code demote-admin}.
     *
     * <p>La lecture passe par {@code findAllByUserIdAndTenantIdAndStatus} et non
     * une requête {@code Optional} sur {@code (userId, tenantId, status)} : un
     * membre ayant adhéré à la racine puis à une sous-glise possède deux
     * lignes ACTIVE, et un {@code Optional} lèverait alors
     * {@code IncorrectResultSizeDataAccessException} (faille F17).
     */
    public boolean isTenantOwner() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.requireTenantId();
        return isTenantOwner(userId, tenantId);
    }

    @Transactional(readOnly = true)
    public boolean isTenantOwner(UUID userId, UUID tenantId) {
        return membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                        userId, tenantId, MembershipStatus.ACTIVE).stream()
                .filter(membership -> membership.getScopeType() == MembershipScopeType.TENANT)
                .anyMatch(membership -> {
                    String roleKey = membership.getRole() != null
                            ? membership.getRole().getKey() : membership.getRoleLegacy();
                    return roleKey != null
                            && TENANT_OWNER_ROLE_KEY.equals(roleKey.trim().toUpperCase(Locale.ROOT));
                });
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
        List<TenantMembership> memberships = membershipRepository.findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
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
        List<TenantMembership> memberships = membershipRepository.findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
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

    public boolean isPlatformSuperAdmin() {
        return isPlatformSuperAdmin(SecurityUtils.getCurrentUserId());
    }

    public Set<String> getPlatformRoleKeys(UUID userId) {
        return new LinkedHashSet<>(membershipRepository.findPlatformRoleKeysByUserId(userId, MembershipStatus.ACTIVE.name()));
    }

    public boolean isPlatformSuperAdmin(UUID userId) {
        return getPlatformRoleKeys(userId).contains("PLATFORM_SUPER_ADMIN");
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