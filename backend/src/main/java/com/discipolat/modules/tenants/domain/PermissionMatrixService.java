package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la matrice de permissions : visualisation et gestion
 * des permissions par rôle, scope, catégorie.
 */
@Service
@Transactional
public class PermissionMatrixService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantMembershipRepository membershipRepository;
    private final OrganizationNodeRepository nodeRepository;
    private final AuditService auditService;

    public PermissionMatrixService(RoleRepository roleRepository,
                                   PermissionRepository permissionRepository,
                                   TenantMembershipRepository membershipRepository,
                                   OrganizationNodeRepository nodeRepository,
                                   AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.membershipRepository = membershipRepository;
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
    }

    // ==================== MATRIX VIEWS ====================

    @Transactional(readOnly = true)
    public PermissionMatrix getFullMatrix(UUID tenantId) {
        List<Role> roles = getEffectiveRoles(tenantId);
        List<Permission> permissions = permissionRepository.findByTenantIdIsNull(); // System permissions

        Map<Role, Set<String>> rolePermissions = new LinkedHashMap<>();
        for (Role role : roles) {
            Set<String> perms = role.getPermissions().stream()
                    .map(Permission::getKey)
                    .collect(Collectors.toSet());
            rolePermissions.put(role, perms);
        }

        Map<String, List<Permission>> permsByCategory = permissions.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory() != null ? p.getCategory() : "GENERAL"));

        Map<PermissionScope, List<Permission>> permsByScope = permissions.stream()
                .collect(Collectors.groupingBy(Permission::getScope));

        return new PermissionMatrix(tenantId, roles, permissions, rolePermissions, permsByCategory, permsByScope);
    }

    @Transactional(readOnly = true)
    public PermissionMatrix getMatrixForScope(UUID tenantId, PermissionScope scope) {
        List<Role> roles = getEffectiveRoles(tenantId);
        List<Permission> permissions = permissionRepository.findByScope(scope);

        Map<Role, Set<String>> rolePermissions = new LinkedHashMap<>();
        for (Role role : roles) {
            Set<String> perms = role.getPermissions().stream()
                    .filter(p -> p.getScope() == scope)
                    .map(Permission::getKey)
                    .collect(Collectors.toSet());
            rolePermissions.put(role, perms);
        }

        return new PermissionMatrix(tenantId, roles, permissions, rolePermissions, Map.of(), Map.of());
    }

    private List<Role> getEffectiveRoles(UUID tenantId) {
        List<Role> systemRoles = roleRepository.findByTenantIdIsNull();
        List<Role> tenantRoles = roleRepository.findByTenantId(tenantId);
        List<Role> all = new ArrayList<>(systemRoles);
        all.addAll(tenantRoles);
        return all.stream().sorted(Comparator.comparingInt(Role::getPriority).reversed()).toList();
    }

    // ==================== USER EFFECTIVE PERMISSIONS ====================

    @Transactional(readOnly = true)
    public UserPermissionMatrix getUserEffectivePermissions(UUID userId, UUID tenantId) {
        List<TenantMembership> memberships = membershipRepository.findByUserIdAndTenantIdAndStatusList(userId, tenantId, MembershipStatus.ACTIVE);

        Set<String> allPermissions = new HashSet<>();
        Map<MembershipScopeType, Set<String>> permissionsByScope = new EnumMap<>(MembershipScopeType.class);
        Map<UUID, Set<String>> permissionsByNode = new HashMap<>();

        for (TenantMembership membership : memberships) {
            if (membership.getRole() != null) {
                Set<String> rolePerms = membership.getRole().getPermissions().stream()
                        .map(Permission::getKey)
                        .collect(Collectors.toSet());

                allPermissions.addAll(rolePerms);

                MembershipScopeType scope = membership.getScopeType();
                permissionsByScope.computeIfAbsent(scope, k -> new HashSet<>()).addAll(rolePerms);

                if (membership.getScopeId() != null) {
                    permissionsByNode.computeIfAbsent(membership.getScopeId(), k -> new HashSet<>()).addAll(rolePerms);
                }
            }
        }

        return new UserPermissionMatrix(userId, tenantId, allPermissions, permissionsByScope, permissionsByNode, memberships);
    }

    @Transactional(readOnly = true)
    public List<UserPermissionSummary> getAllUsersPermissions(UUID tenantId) {
        List<TenantMembership> memberships = membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);

        Map<UUID, UserPermissionSummary> userMap = new HashMap<>();
        for (TenantMembership m : memberships) {
            UserPermissionSummary summary = userMap.computeIfAbsent(m.getUserId(), id ->
                    new UserPermissionSummary(id, m.getUserId(), new HashSet<>(), new EnumMap<>(MembershipScopeType.class), new HashMap<>()));

            if (m.getRole() != null) {
                Set<String> rolePerms = m.getRole().getPermissions().stream()
                        .map(Permission::getKey)
                        .collect(Collectors.toSet());
                summary.allPermissions().addAll(rolePerms);
                summary.permissionsByScope().computeIfAbsent(m.getScopeType(), k -> new HashSet<>()).addAll(rolePerms);
                if (m.getScopeId() != null) {
                    summary.permissionsByNode().computeIfAbsent(m.getScopeId(), k -> new HashSet<>()).addAll(rolePerms);
                }
            }
        }

        return new ArrayList<>(userMap.values());
    }

    // ==================== NODE-BASED PERMISSIONS ====================

    @Transactional(readOnly = true)
    public NodePermissionView getNodePermissions(UUID tenantId, UUID nodeId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));

        if (!node.getTenantId().equals(tenantId)) {
            throw new BusinessRuleException("Nœud n'appartient pas à ce tenant", "TENANT_MISMATCH");
        }

        // Get all memberships that have access to this node
        List<TenantMembership> memberships = membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);

        List<NodeMemberPermission> members = memberships.stream()
                .filter(m -> {
                    if (m.getScopeType() == MembershipScopeType.TENANT) return true;
                    if (m.getScopeId() == null) return false;
                    // Check if node is descendant of membership scope
                    return isDescendantOf(nodeId, m.getScopeId());
                })
                .map(m -> new NodeMemberPermission(
                        m.getUserId(),
                        m.getRole() != null ? m.getRole().getKey() : "UNKNOWN",
                        m.getScopeType(),
                        m.getScopeId(),
                        m.getRole() != null ? m.getRole().getPermissions().stream().map(Permission::getKey).collect(Collectors.toSet()) : Set.of()
                ))
                .toList();

        return new NodePermissionView(nodeId, node.getName(), node.getType(), members);
    }

    private boolean isDescendantOf(UUID nodeId, UUID ancestorId) {
        Optional<OrganizationNode> node = nodeRepository.findById(nodeId);
        Optional<OrganizationNode> ancestor = nodeRepository.findById(ancestorId);
        if (node.isEmpty() || ancestor.isEmpty()) return false;
        return node.get().getPath().startsWith(ancestor.get().getPath());
    }

    // ==================== PERMISSION CHECKS ====================

    @Transactional(readOnly = true)
    public boolean userHasPermission(UUID userId, UUID tenantId, String permissionKey, UUID nodeId) {
        List<TenantMembership> memberships = membershipRepository.findByUserIdAndTenantIdAndStatusList(userId, tenantId, MembershipStatus.ACTIVE);

        for (TenantMembership membership : memberships) {
            if (membership.getRole() != null) {
                boolean hasPerm = membership.getRole().getPermissions().stream()
                        .anyMatch(p -> p.getKey().equals(permissionKey));
                if (hasPerm && membershipMatchesNode(membership, nodeId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean membershipMatchesNode(TenantMembership membership, UUID nodeId) {
        if (membership.getScopeType() == MembershipScopeType.TENANT) return true;
        if (membership.getScopeId() == null) return false;
        return isDescendantOf(nodeId, membership.getScopeId());
    }

    // ==================== AUDIT & REPORTING ====================

    @Transactional(readOnly = true)
    public PermissionAuditReport generateAuditReport(UUID tenantId) {
        List<Role> roles = getEffectiveRoles(tenantId);
        List<Permission> permissions = permissionRepository.findByTenantIdIsNull();

        List<RolePermissionSummary> roleSummaries = roles.stream().map(role -> {
            Set<String> perms = role.getPermissions().stream().map(Permission::getKey).collect(Collectors.toSet());
            long categoryCount = role.getPermissions().stream()
                    .collect(Collectors.groupingBy(Permission::getCategory, Collectors.counting())).size();
            return new RolePermissionSummary(role.getId(), role.getKey(), role.getLabel(), role.getSystem(), role.getPriority(), perms.size(), (int) categoryCount);
        }).toList();

        List<PermissionUsage> permUsage = permissions.stream().map(perm -> {
            long roleCount = roles.stream().filter(r -> r.getPermissions().contains(perm)).count();
            long userCount = membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                    .filter(m -> m.getRole() != null && m.getRole().getPermissions().contains(perm)).count();
            return new PermissionUsage(perm.getKey(), perm.getLabel(), perm.getScope(), perm.getCategory(), roleCount, userCount);
        }).toList();

        return new PermissionAuditReport(tenantId, roleSummaries, permUsage);
    }

    // ==================== RECORDS ====================

    public record PermissionMatrix(
            UUID tenantId,
            List<Role> roles,
            List<Permission> permissions,
            Map<Role, Set<String>> rolePermissions,
            Map<String, List<Permission>> permissionsByCategory,
            Map<PermissionScope, List<Permission>> permissionsByScope
    ) {}

    public record UserPermissionMatrix(
            UUID userId,
            UUID tenantId,
            Set<String> allPermissions,
            Map<MembershipScopeType, Set<String>> permissionsByScope,
            Map<UUID, Set<String>> permissionsByNode,
            List<TenantMembership> memberships
    ) {}

    public record UserPermissionSummary(
            UUID userId,
            UUID membershipId,
            Set<String> allPermissions,
            Map<MembershipScopeType, Set<String>> permissionsByScope,
            Map<UUID, Set<String>> permissionsByNode
    ) {}

    public record NodePermissionView(
            UUID nodeId,
            String nodeName,
            OrganizationNodeType nodeType,
            List<NodeMemberPermission> members
    ) {}

    public record NodeMemberPermission(
            UUID userId,
            String roleKey,
            MembershipScopeType scopeType,
            UUID scopeId,
            Set<String> permissions
    ) {}

    public record PermissionAuditReport(
            UUID tenantId,
            List<RolePermissionSummary> roles,
            List<PermissionUsage> permissions
    ) {}

    public record RolePermissionSummary(
            UUID roleId,
            String key,
            String label,
            boolean system,
            int priority,
            int permissionCount,
            int categoryCount
    ) {}

    public record PermissionUsage(
            String key,
            String label,
            PermissionScope scope,
            String category,
            long roleCount,
            long userCount
    ) {}
}