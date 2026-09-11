package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service pour la gestion des permissions (catalogue global + tenant)
 */
@Service
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public PermissionService(PermissionRepository permissionRepository, AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Permission> getSystemPermissions() {
        return permissionRepository.findByTenantIdIsNull();
    }

    @Transactional(readOnly = true)
    public List<Permission> getTenantPermissions(UUID tenantId) {
        return permissionRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByScope(PermissionScope scope) {
        return permissionRepository.findByScope(scope);
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public Optional<Permission> getByKey(String key) {
        return permissionRepository.findByKey(key);
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsForRole(UUID roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    public Permission createPermission(UUID tenantId, String key, String label, String description,
                                       PermissionScope scope, String category, UUID creatorId) {
        if (permissionRepository.findByKey(key).isPresent()) {
            throw new IllegalArgumentException("Permission with key already exists: " + key);
        }

        Permission permission = Permission.builder()
                .tenantId(tenantId)
                .key(key.toUpperCase())
                .label(label)
                .description(description)
                .scope(scope)
                .category(category)
                .system(tenantId == null)
                .build();

        permission = permissionRepository.save(permission);

        auditService.log(
                creatorId,
                tenantId,
                "PERMISSION_CREATED",
                "PERMISSION",
                permission.getId(),
                "SUCCESS",
                Map.of("key", key, "label", label, "scope", scope.name()),
                null, null, null
        );

        return permission;
    }
}