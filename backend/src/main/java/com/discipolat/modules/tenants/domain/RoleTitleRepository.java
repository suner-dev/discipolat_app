package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §B — repository des intitulés. Tous les
 * finders scopent par {@code tenantId} (isolation multi-tenant stricte).
 */
public interface RoleTitleRepository extends JpaRepository<RoleTitle, UUID> {

    List<RoleTitle> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);

    Optional<RoleTitle> findByTenantIdAndRoleIdAndNodeId(UUID tenantId, UUID roleId, UUID nodeId);

    /** L'intitulé « défaut » du tenant ({@code node_id IS NULL}). */
    Optional<RoleTitle> findByTenantIdAndRoleIdAndNodeIdIsNull(UUID tenantId, UUID roleId);

    List<RoleTitle> findByTenantIdAndNodeId(UUID tenantId, UUID nodeId);

    void deleteByTenantIdAndRoleIdAndNodeId(UUID tenantId, UUID roleId, UUID nodeId);
}
