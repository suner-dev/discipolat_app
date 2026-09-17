package com.discipolat.modules.audit.repository;

import com.discipolat.modules.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByTenantIdOrderByTimestampDesc(UUID tenantId);

    Page<AuditEvent> findByTenantIdOrderByTimestampDesc(UUID tenantId, Pageable pageable);

    List<AuditEvent> findByTenantIdAndActorIdOrderByTimestampDesc(UUID tenantId, UUID actorId);

    Page<AuditEvent> findByTenantIdAndActorIdOrderByTimestampDesc(UUID tenantId, UUID actorId, Pageable pageable);

    List<AuditEvent> findByTenantIdAndEntityAndEntityIdOrderByTimestampDesc(UUID tenantId, String entity, UUID entityId);

    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tenantId AND a.timestamp BETWEEN :from AND :to ORDER BY a.timestamp DESC")
    List<AuditEvent> findByTenantIdAndTimestampBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tenantId AND a.timestamp BETWEEN :from AND :to ORDER BY a.timestamp DESC")
    Page<AuditEvent> findByTenantIdAndTimestampBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditEvent> findByTenantIdOrderByTimestampDescPageable(@Param("tenantId") UUID tenantId, Pageable pageable);

    Optional<AuditEvent> findFirstByTenantIdOrderByTimestampDesc(UUID tenantId);

    @Query("SELECT a.hash FROM AuditEvent a WHERE a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    List<String> findHashesByTenantIdOrderByTimestampDesc(@Param("tenantId") UUID tenantId, org.springframework.data.domain.Pageable pageable);

    long countByTenantId(UUID tenantId);
}