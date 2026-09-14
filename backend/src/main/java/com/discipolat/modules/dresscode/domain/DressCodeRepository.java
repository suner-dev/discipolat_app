package com.discipolat.modules.dresscode.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DressCodeRepository extends JpaRepository<DressCode, UUID> {

    List<DressCode> findByTenantIdAndArchivedFalse(UUID tenantId);

    List<DressCode> findByTenantIdAndSpaceIdAndArchivedFalse(UUID tenantId, UUID spaceId);

    List<DressCode> findByTenantIdAndEventIdAndArchivedFalse(UUID tenantId, UUID eventId);

    @Query("SELECT dc FROM DressCode dc WHERE dc.tenantId = :tenantId " +
           "AND dc.archived = false " +
           "AND (:spaceId IS NULL OR dc.spaceId = :spaceId) " +
           "AND (:eventId IS NULL OR dc.eventId = :eventId)")
    List<DressCode> findFiltered(@Param("tenantId") UUID tenantId,
                                 @Param("spaceId") UUID spaceId,
                                 @Param("eventId") UUID eventId);

    long countByTenantIdAndArchivedFalse(UUID tenantId);
}
