package com.discipolat.modules.spaces.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * G2.6 — Repository des espaces. Règle §0.3 n°3 : jamais de findById(id) nu,
 * toujours findByIdAndTenantId pour toute donnée multi-tenant.
 */
@Repository
public interface SpaceRepository extends TenantAwareRepository<Space, UUID> {

    Optional<Space> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Space> findByTenantIdAndCode(UUID tenantId, String code);

    List<Space> findByTenantId(UUID tenantId);

    List<Space> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    List<Space> findByTenantIdAndSpaceType(UUID tenantId, SpaceType spaceType);

    List<Space> findByTenantIdAndSpaceTypeAndDeletedAtIsNull(UUID tenantId, SpaceType spaceType);

    List<Space> findByTenantIdAndOrganizationUnitId(UUID tenantId, UUID organizationUnitId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndSpaceTypeAndDeletedAtIsNull(UUID tenantId, SpaceType spaceType);
}
