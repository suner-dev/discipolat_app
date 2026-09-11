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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des rôles (par tenant et système)
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       AuditService auditService,
                       EntityPropagationPublisher propagationPublisher) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
    }

    @Transactional(readOnly = true)
    public List<Role> getRoles(UUID tenantId) {
        return roleRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Role> getSystemRoles() {
        return roleRepository.findByTenantIdIsNull();
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRolesForTenant(UUID tenantId) {
        // Combine tenant roles + system roles
        List<Role> tenantRoles = roleRepository.findByTenantId(tenantId);
        List<Role> systemRoles = roleRepository.findByTenantIdIsNull();
        systemRoles.addAll(tenantRoles);
        return systemRoles;
    }

    public Role createRole(UUID tenantId, String key, String label, String description, UUID creatorId) {
        // Vérifier unicité
        if (roleRepository.findByTenantIdAndKey(tenantId, key).isPresent()) {
            throw new BusinessRuleException("Un rôle avec cette clé existe déjà: " + key, "ROLE_KEY_EXISTS");
        }

        Role role = Role.builder()
                .tenantId(tenantId)
                .key(key.toUpperCase())
                .label(label)
                .description(description)
                .system(false)
                .priority(100)
                .build();

        role = roleRepository.save(role);

        auditService.log(
                creatorId,
                tenantId,
                "ROLE_CREATED",
                "ROLE",
                role.getId(),
                "SUCCESS",
                Map.of("key", key, "label", label),
                null, null, null
        );

        return role;
    }

    public Role createSystemRole(String key, String label, String description, int priority, UUID creatorId) {
        if (roleRepository.findByTenantIdAndKey(null, key).isPresent()) {
            throw new BusinessRuleException("Un rôle système avec cette clé existe déjà: " + key, "ROLE_KEY_EXISTS");
        }

        Role role = Role.builder()
                .tenantId(null)
                .key(key.toUpperCase())
                .label(label)
                .description(description)
                .system(true)
                .priority(priority)
                .build();

        role = roleRepository.save(role);

        auditService.log(
                creatorId,
                null,
                "SYSTEM_ROLE_CREATED",
                "ROLE",
                role.getId(),
                "SUCCESS",
                Map.of("key", key, "label", label),
                null, null, null
        );

        return role;
    }

    public Role updateRole(UUID roleId, String label, String description, Integer priority, UUID updaterId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier un rôle système", "SYSTEM_ROLE_IMMUTABLE");
        }

        String oldLabel = role.getLabel();
        if (label != null) role.setLabel(label);
        if (description != null) role.setDescription(description);
        if (priority != null) role.setPriority(priority);

        role = roleRepository.save(role);

        auditService.log(
                updaterId,
                role.getTenantId(),
                "ROLE_UPDATED",
                "ROLE",
                role.getId(),
                "SUCCESS",
                Map.of("oldLabel", oldLabel, "newLabel", role.getLabel()),
                null, null, null
        );

        return role;
    }

    public void assignPermissions(UUID roleId, Set<UUID> permissionIds, UUID updaterId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de modifier les permissions d'un rôle système", "SYSTEM_ROLE_IMMUTABLE");
        }

        Set<Permission> permissions = permissionRepository.findAllById(permissionIds).stream()
                .collect(Collectors.toSet());

        // Vérifier que toutes les permissions sont compatibles (même tenant ou globales)
        for (Permission p : permissions) {
            if (p.getTenantId() != null && !p.getTenantId().equals(role.getTenantId())) {
                throw new BusinessRuleException(
                        "Permission " + p.getKey() + " appartient à un autre tenant", "PERMISSION_TENANT_MISMATCH");
            }
        }

        role.setPermissions(permissions);
        roleRepository.save(role);

        auditService.log(
                updaterId,
                role.getTenantId(),
                "ROLE_PERMISSIONS_UPDATED",
                "ROLE",
                role.getId(),
                "SUCCESS",
                Map.of("permissionCount", permissions.size()),
                null, null, null
        );
    }

    public void deleteRole(UUID roleId, UUID deleterId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role", roleId));

        if (role.getSystem()) {
            throw new BusinessRuleException("Impossible de supprimer un rôle système", "SYSTEM_ROLE_IMMUTABLE");
        }

        roleRepository.delete(role);

        auditService.log(
                deleterId,
                role.getTenantId(),
                "ROLE_DELETED",
                "ROLE",
                roleId,
                "SUCCESS",
                Map.of("key", role.getKey()),
                null, null, null
        );
    }
}