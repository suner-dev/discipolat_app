package com.discipolat.modules.audit.repository;

import com.discipolat.modules.audit.domain.BusinessHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessHistoryRepository extends JpaRepository<BusinessHistory, Long> {

    List<BusinessHistory> findByTenantIdAndObjectTypeAndObjectIdOrderByHappenedAtDesc(UUID tenantId, String objectType, UUID objectId);

    Page<BusinessHistory> findByTenantIdAndObjectTypeAndObjectIdOrderByHappenedAtDesc(UUID tenantId, String objectType, UUID objectId, Pageable pageable);

    List<BusinessHistory> findByTenantIdAndSpaceIdOrderByHappenedAtDesc(UUID tenantId, UUID spaceId);

    Page<BusinessHistory> findByTenantIdAndSpaceIdOrderByHappenedAtDesc(UUID tenantId, UUID spaceId, Pageable pageable);

    List<BusinessHistory> findByTenantIdAndActorIdOrderByHappenedAtDesc(UUID tenantId, UUID actorId);

    @Query("SELECT b FROM BusinessHistory b WHERE b.tenantId = :tenantId AND b.happenedAt BETWEEN :from AND :to ORDER BY b.happenedAt DESC")
    List<BusinessHistory> findByTenantIdAndHappenedAtBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT b FROM BusinessHistory b WHERE b.tenantId = :tenantId AND b.happenedAt BETWEEN :from AND :to ORDER BY b.happenedAt DESC")
    Page<BusinessHistory> findByTenantIdAndHappenedAtBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);

    @Query("SELECT b FROM BusinessHistory b WHERE b.tenantId = :tenantId ORDER BY b.happenedAt DESC")
    Page<BusinessHistory> findByTenantIdOrderByHappenedAtDesc(@Param("tenantId") UUID tenantId, Pageable pageable);

    long countByTenantId(UUID tenantId);
}