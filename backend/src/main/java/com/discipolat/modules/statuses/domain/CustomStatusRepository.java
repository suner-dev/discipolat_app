package com.discipolat.modules.statuses.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * G2.7 — Repository des statuts configurables.
 * Règle §0.3 n°3 : toutes les lectures sont scopées par tenant.
 */
@Repository
public interface CustomStatusRepository extends TenantAwareRepository<CustomStatus, UUID> {

    Optional<CustomStatus> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CustomStatus> findByTenantIdAndEntityTypeAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID tenantId, String entityType);

    List<CustomStatus> findByTenantIdAndEntityTypeAndSpaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(
            UUID tenantId, String entityType, UUID spaceId);

    List<CustomStatus> findByTenantIdAndEntityTypeAndSpaceIdIsNullAndDeletedAtIsNullOrderByDisplayOrderAsc(
            UUID tenantId, String entityType);

    Optional<CustomStatus> findByTenantIdAndEntityTypeAndSpaceIdAndCodeAndDeletedAtIsNull(
            UUID tenantId, String entityType, UUID spaceId, String code);

    Optional<CustomStatus> findByTenantIdAndEntityTypeAndSpaceIdIsNullAndCodeAndDeletedAtIsNull(
            UUID tenantId, String entityType, String code);

    List<CustomStatus> findByTenantIdAndSpaceIdAndDeletedAtIsNull(UUID tenantId, UUID spaceId);
}
