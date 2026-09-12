package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des rôles (système + personnalisés par tenant).
 * Permet la création de rôles custom, assignation permissions, priorités.
 */
@Service
@Transactional
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public RoleManagementService(RoleRepository roleRepository,
                                 PermissionRepository permissionRepository,
                                 TenantMembershipRepository membershipRepository,
                                 AuditService auditService,
                                 EntityPropagationPublisher propagationPublisher) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.membershipRepository = membershipRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
    }

    // ==================== READ OPERATIONS ====================

    @Transactional(readOnly = true)
    public List<Role> getAllRoles(UUID tenantId) {
        // Return system roles + tenant custom roles
        List<Role> systemRoles = roleRepository.findByTenantIdIsNull();
        List<Role> tenantRoles = roleRepository.findByTenantId(tenantId);
        List<Role> all = new ArrayList<>(systemRoles);
        all.addAll(tenantRoles);
        return all.stream().sorted(Comparator.comparingInt(Role::getPriority).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<Role> getSystemRoles() {
        return roleRepository.findByTenantIdIsNull()
                .stream().sorted(Comparator.comparingInt(Role::getPriority).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<Role> getCustomRoles(UUID tenantId) {
        return roleRepository.findByTenantId(tenantId)
                .stream().sorted(Comparator.comparingInt(Role::getPriority).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Role> findById(UUID id) {
        return roleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Role> findByKey(UUID tenantId, String key) {
        Optional<Role> role = roleRepository.findByTenantIdAndKey(tenantId, key.toUpperCase());
        if (role.isEmpty() && tenantId != null) {
            role = roleRepository.findByTenantIdIsNullAndKey(key.toUpperCase());
        }
        return role;
    }

    @Transactional(readOnly = true)
    public Set<String> getRolePermissions(UUID roleId) {
        Optional<Role> role = roleRepository.findById(roleId);
        return role.map(r -> r.getPermissions().stream().map(Permission::getKey).collect(Collectors.toSet())).orElse(Set.of());
    }

    // ==================== PERMISSION CATALOG ====================

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByTenantIdIsNull(); // System permissions
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByScope(PermissionScope scope) {
        return permissionRepository.findByScope(scope);
    }

    @Transactional(readOnly = true)
    public Map<String, List<Permission>> getPermissionsGroupedByCategory() {
        return getAllPermissions().stream()
                .collect(Collectors.groupingBy(p -> p.getCategory() != null ? p.getCategory() : "GENERAL"));
    }

    @Transactional(readOnly = true)
    public Map<PermissionScope, List<Permission>> getPermissionsGroupedByScope() {
        return getAllPermissions().stream()
                .collect(Collectors.groupingBy(Permission::getScope));
    }

    // ==================== CREATE CUSTOM ROLE ====================

    public Role createCustomRole(UUID tenantId, CreateRoleRequest request, UUID creatorId) {
        String key = request.key().toUpperCase();

        // Check key doesn't exist in tenant or system
        if (roleRepository.findByTenantIdAndKey(tenantId, key).isPresent()) {
            throw new BusinessRuleException("Un rôle avec cette clé existe déjà: " + key, "ROLE_KEY_EXISTS");
        }
        if (roleRepository.findByTenantIdIsNullAndKey(key).isPresent()) {
            throw new BusinessRuleException("Cette clé est réservée aux rôles système: " + key, "SYSTEM_ROLE_KEY");
        }

        Role role = Role.builder()
                .tenantId(tenantId)
                .key(key)
                .label(request.label())
                .description(request.description())
                .system(false)
                .priority(request.priority() != null ? request.priority() : 0)
                .build();

        // Assign permissions if provided
        if (request.permissionKeys() != null && !request.permissionKeys().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (String permKey : request.permissionKeys()) {
                permissionRepository.findByKey(permKey).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);

        auditService.log(creatorId, tenantId, "ROLE_CREATED", "ROLE", role.getId(), "SUCCESS",
                Map.of("key", key, "label", request.label(), "custom", true), null, null, null);

        return role;
    }

    // ==================== UPDATE ROLE ====================

    public Role updateRole(UUID roleId, UpdateRoleRequest request, UUID updaterId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }

        String oldLabel = role.getLabel();
        String oldDescription = role.getDescription();
        Integer oldPriority = role.getPriority();

        if (request.label() != null) role.setLabel(request.label());
        if (request.description() != null) role.setDescription(request.description());
        if (request.priority() != null) role.setPriority(request.priority());

        role = roleRepository.save(role);

        auditService.log(updaterId, role.getTenantId(), "ROLE_UPDATED", "ROLE", role.getId(), "SUCCESS",
                Map.of("oldLabel", oldLabel, "newLabel", request.label(),
                        "oldDescription", oldDescription, "newDescription", request.description(),
                        "oldPriority", oldPriority, "newPriority", request.priority()),
                null, null, null);

        return role;
    }

    // ==================== MANAGE ROLE PERMISSIONS ====================

    public Role assignPermissions(UUID roleId, List<String> permissionKeys, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier les permissions d'un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }

        Set<Permission> permissions = new HashSet<>();
        for (String key : permissionKeys) {
            permissionRepository.findByKey(key).ifPresent(permissions::add);
        }

        role.setPermissions(permissions);
        role = roleRepository.save(role);

        auditService.log(assignerId, role.getTenantId(), "ROLE_PERMISSIONS_ASSIGNED", "ROLE", role.getId(), "SUCCESS",
                Map.of("permissionKeys", permissionKeys), null, null, null);

        return role;
    }

    public Role addPermission(UUID roleId, String permissionKey, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }

        permissionRepository.findByKey(permissionKey).ifPresent(role.getPermissions()::add);
        role = roleRepository.save(role);

        return role;
    }

    public Role removePermission(UUID roleId, String permissionKey, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier un rôle système", "CANNOT_MODIFY_SYSTEM_ROLE");
        }

        role.getPermissions().removeIf(p -> p.getKey().equals(permissionKey));
        role = roleRepository.save(role);

        return role;
    }

    // ==================== DELETE CUSTOM ROLE ====================

    public void deleteRole(UUID roleId, UUID deleterId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de supprimer un rôle système", "CANNOT_DELETE_SYSTEM_ROLE");
        }

        // Check if role is assigned to any membership
        long assignedCount = membershipRepository.countByRoleId(roleId);
        if (assignedCount > 0) {
            throw new BusinessRuleException("Impossible de supprimer: rôle assigné à " + assignedCount + " utilisateur(s)", "ROLE_IN_USE");
        }

        UUID tenantId = role.getTenantId();
        roleRepository.delete(role);

        auditService.log(deleterId, tenantId, "ROLE_DELETED", "ROLE", roleId, "SUCCESS",
                Map.of("key", role.getKey(), "label", role.getLabel()), null, null, null);
    }

    // ==================== ROLE ASSIGNMENT TO USERS ====================

    public TenantMembership assignRoleToUser(UUID userId, UUID tenantId, UUID roleId, UUID scopeId, UUID assignerId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        // Check user exists in tenant
        Optional<TenantMembership> existing = membershipRepository.findByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
        if (existing.isPresent()) {
            // Update existing membership
            TenantMembership membership = existing.get();
            membership.setRole(role);
            if (scopeId != null) {
                membership.setScopeId(scopeId);
                membership.setScopeType(MembershipScopeType.CHURCH); // Default, could be parameter
            }
            membershipRepository.save(membership);
            return membership;
        }

        // Create new membership
        TenantMembership membership = TenantMembership.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role(role)
                .scopeType(MembershipScopeType.TENANT)
                .scopeId(scopeId)
                .status(MembershipStatus.ACTIVE)
                .invitedBy(assignerId)
                .build();
        membershipRepository.save(membership);

        auditService.log(assignerId, tenantId, "ROLE_ASSIGNED", "MEMBERSHIP", membership.getId(), "SUCCESS",
                Map.of("userId", userId.toString(), "roleKey", role.getKey(), "scopeId", scopeId != null ? scopeId.toString() : null), null, null, null);

        return membership;
    }

    // ==================== ROLE HIERARCHY & INHERITANCE ====================

    @Transactional(readOnly = true)
    public List<Role> getRoleHierarchy(UUID tenantId) {
        List<Role> allRoles = getAllRoles(tenantId);
        // Sort by priority (highest first) - represents hierarchy
        return allRoles.stream()
                .sorted(Comparator.comparingInt(Role::getPriority).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Role, List<Role>> getRoleChildren(UUID tenantId) {
        List<Role> allRoles = getRoleHierarchy(tenantId);
        Map<Role, List<Role>> children = new LinkedHashMap<>();

        for (int i = 0; i < allRoles.size(); i++) {
            Role parent = allRoles.get(i);
            List<Role> childRoles = allRoles.subList(i + 1, allRoles.size());
            children.put(parent, childRoles);
        }

        return children;
    }

    // ==================== VALIDATION ====================

    @Transactional(readOnly = true)
    public ValidationResult validateRolePermissions(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check for conflicting permissions (e.g., READ + DELETE without UPDATE)
        Set<String> perms = role.getPermissions().stream().map(Permission::getKey).collect(Collectors.toSet());

        if (perms.contains("MEMBER_DELETE") && !perms.contains("MEMBER_UPDATE")) {
            warnings.add("DELETE sans UPDATE: l'utilisateur peut supprimer mais pas modifier");
        }
        if (perms.contains("TENANT_SETTINGS_UPDATE") && !perms.contains("TENANT_SETTINGS_READ")) {
            warnings.add("UPDATE settings sans READ: interface peut ne pas afficher les valeurs actuelles");
        }
        if (perms.contains("FINANCE_MANAGE") && !perms.contains("FINANCE_READ")) {
            warnings.add("Gestion finances sans lecture: risqué");
        }

        return new ValidationResult(errors.isEmpty(), warnings, errors);
    }

    // ==================== RECORDS ====================

    public record CreateRoleRequest(
            String key,
            String label,
            String description,
            Integer priority,
            List<String> permissionKeys
    ) {}

    public record UpdateRoleRequest(
            String label,
            String description,
            Integer priority
    ) {}

    public record ValidationResult(
            boolean valid,
            List<String> warnings,
            List<String> errors
    ) {}
}