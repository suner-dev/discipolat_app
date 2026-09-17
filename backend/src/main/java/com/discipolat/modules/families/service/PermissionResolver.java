package com.discipolat.modules.families.service;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.core.domain.PermissionVersion;
import com.discipolat.modules.core.repository.PermissionVersionRepository;
import com.discipolat.modules.families.domain.PastorateAppointment;
import com.discipolat.modules.families.repository.PastorateAppointmentRepository;
import com.discipolat.modules.people.domain.RoleAssignment;
import com.discipolat.modules.people.domain.SpaceMembership;
import com.discipolat.modules.people.repository.RoleAssignmentRepository;
import com.discipolat.modules.people.repository.SpaceMembershipRepository;
import com.discipolat.modules.tenants.domain.Permission;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionResolver {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final PastorateAppointmentRepository pastorateAppointmentRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final RoleRepository roleRepository;
    private final PermissionVersionRepository permissionVersionRepository;

    /**
     * Calcule l'ensemble complet des permissions pour un utilisateur.
     * Agrège : tenant memberships, role assignments, space memberships, pastorate appointments.
     */
    @Transactional(readOnly = true)
    public Set<String> resolvePermissions(UUID tenantId, UUID userId) {
        Set<String> permissions = new HashSet<>();

        // 1. Tenant memberships (rôles globaux)
        List<TenantMembership> tenantMemberships = tenantMembershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, tenantId, com.discipolat.modules.tenants.domain.MembershipStatus.ACTIVE);
        for (TenantMembership tm : tenantMemberships) {
            if (tm.getRole() != null) {
                addRolePermissions(permissions, tm.getRole().getKey(), tenantId);
            }
        }

        // 2. Role assignments (rôles sur org units ou espaces)
        List<RoleAssignment> roleAssignments = roleAssignmentRepository
                .findByTenantIdAndPersonIdAndStatus(tenantId, userId, "ACTIVE");
        for (RoleAssignment ra : roleAssignments) {
            addRolePermissionsByRoleId(permissions, ra.getRoleId(), tenantId);
        }

        // 3. Space memberships (rôles d'espace)
        List<SpaceMembership> spaceMemberships = spaceMembershipRepository
                .findByPersonIdAndStatus(userId, "ACTIVE");
        for (SpaceMembership sm : spaceMemberships) {
            if (sm.getMembershipType() != null) {
                addSpaceRolePermissions(permissions, sm.getMembershipType());
            }
        }

        // 4. Pastorate appointments (rôles pastoraux)
        // TODO: add pastorate appointments when needed

        return permissions;
    }

    /**
     * Résout les permissions et met à jour le cache versionné.
     * Appelé après tout changement de rôle/permission.
     */
    @Transactional
    public void refreshPermissionCache(UUID tenantId, UUID userId) {
        Set<String> permissions = resolvePermissions(tenantId, userId);

        PermissionVersion pv = permissionVersionRepository
                .findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> {
                    PermissionVersion pvNew = new PermissionVersion();
                    pvNew.setTenantId(tenantId);
                    pvNew.setUserId(userId);
                    pvNew.setVersion(0L);
                    return pvNew;
                });

        pv.setVersion(pv.getVersion() + 1);
        pv.setPermissionsJson(new ArrayList<>(permissions));
        pv.setUpdatedAt(java.time.OffsetDateTime.now());
        permissionVersionRepository.save(pv);

        log.debug("Permission cache updated for user {} v{}", userId, pv.getVersion());
    }

    /**
     * Récupère la version actuelle des permissions (pour poll côté client).
     */
    @Transactional(readOnly = true)
    public Long getPermissionVersion(UUID tenantId, UUID userId) {
        return permissionVersionRepository.findByTenantIdAndUserId(tenantId, userId)
                .map(PermissionVersion::getVersion)
                .orElse(0L);
    }

    /**
     * Récupère les permissions en cache (pour éviter recalcul si version inchangée).
     */
    @Transactional(readOnly = true)
    public List<String> getCachedPermissions(UUID tenantId, UUID userId) {
        return permissionVersionRepository.findByTenantIdAndUserId(tenantId, userId)
                .map(pv -> new ArrayList<>(pv.getPermissionsJson()))
                .orElseGet(ArrayList::new);
    }

    private void addRolePermissions(Set<String> permissions, String roleKey, UUID tenantId) {
        Optional<Role> roleOpt = roleRepository.findByTenantIdAndKey(tenantId, roleKey);
        if (roleOpt.isPresent()) {
            Role role = roleOpt.get();
            if (role.getPermissions() != null) {
                for (Permission perm : role.getPermissions()) {
                    permissions.add(perm.getKey());
                }
            }
        }
    }

    private void addRolePermissionsByRoleId(Set<String> permissions, UUID roleId, UUID tenantId) {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isPresent()) {
            Role role = roleOpt.get();
            if (role.getTenantId().equals(tenantId) && role.getPermissions() != null) {
                for (Permission perm : role.getPermissions()) {
                    permissions.add(perm.getKey());
                }
            }
        }
    }

    private void addSpaceRolePermissions(Set<String> permissions, String membershipType) {
        // Map membership type to permissions
        Map<String, Set<String>> spaceRolePerms = Map.of(
                "LEADER", Set.of("SPACE_MANAGE", "SPACE_MEMBER_READ", "SPACE_MEMBER_CREATE", "SPACE_MEMBER_UPDATE", "SPACE_REPORT_READ", "SPACE_REPORT_CREATE"),
                "CO_LEADER", Set.of("SPACE_MEMBER_READ", "SPACE_MEMBER_CREATE", "SPACE_REPORT_CREATE"),
                "MEMBER", Set.of("SPACE_MEMBER_READ", "SPACE_REPORT_CREATE"),
                "ASSISTANT", Set.of("SPACE_MEMBER_READ", "SPACE_REPORT_CREATE"),
                "OBSERVER", Set.of("SPACE_MEMBER_READ")
        );
        spaceRolePerms.getOrDefault(membershipType, Collections.emptySet()).forEach(permissions::add);
    }
}