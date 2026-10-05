package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §C — repository des affiliations
 * membre×rôle×nœud. Tous les finders scopent par {@code tenantId}.
 */
public interface MemberRoleAssignmentRepository extends JpaRepository<MemberRoleAssignment, UUID> {

    List<MemberRoleAssignment> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    List<MemberRoleAssignment> findByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId,
                                                                MemberRoleAssignment.AssignmentStatus status);

    Optional<MemberRoleAssignment> findByTenantIdAndId(UUID tenantId, UUID id);

    /** Ligne ACTIVE sur le même (user, role, node) — upsert / anti-doublon. */
    Optional<MemberRoleAssignment> findByUserIdAndRoleIdAndNodeIdAndStatus(UUID userId, UUID roleId,
                                                                           UUID nodeId,
                                                                           MemberRoleAssignment.AssignmentStatus status);

    /** Porteurs d'un rôle donné sur un nœud (vue « équipe de ce campus »). */
    List<MemberRoleAssignment> findByTenantIdAndNodeIdAndStatus(UUID tenantId, UUID nodeId,
                                                                MemberRoleAssignment.AssignmentStatus status);

    List<MemberRoleAssignment> findByTenantIdAndStatus(UUID tenantId,
                                                       MemberRoleAssignment.AssignmentStatus status);
}
