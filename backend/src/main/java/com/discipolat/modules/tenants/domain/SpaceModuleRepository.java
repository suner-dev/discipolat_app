package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * G2.2 — Repository pour les modules par espace (organization_unit)
 */
@Repository
public interface SpaceModuleRepository extends TenantAwareRepository<SpaceModule, UUID> {

    List<SpaceModule> findByTenantIdAndSpaceId(UUID tenantId, UUID spaceId);

    Optional<SpaceModule> findByTenantIdAndSpaceIdAndModuleCode(UUID tenantId, UUID spaceId, String moduleCode);

    List<SpaceModule> findByTenantIdAndSpaceIdAndEnabledTrue(UUID tenantId, UUID spaceId);

    @Query("SELECT sm FROM SpaceModule sm WHERE sm.tenantId = :tenantId AND sm.spaceId = :spaceId AND sm.enabled = true ORDER BY sm.displayOrder ASC")
    List<SpaceModule> findEnabledBySpaceIdOrderByDisplayOrder(@Param("tenantId") UUID tenantId, @Param("spaceId") UUID spaceId);

    long countByTenantIdAndSpaceId(UUID tenantId, UUID spaceId);
}