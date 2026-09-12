package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends TenantAwareRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantIdOrderByTimestampDesc(UUID tenantId, Pageable pageable);

    List<AuditLog> findByTenantIdAndActorIdOrderByTimestampDesc(UUID tenantId, UUID actorId);

    List<AuditLog> findByTenantIdAndActionOrderByTimestampDesc(UUID tenantId, String action);

    List<AuditLog> findByTenantIdAndResourceAndResourceIdOrderByTimestampDesc(UUID tenantId, String resource, UUID resourceId);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<AuditLog> findByTenantIdAndTimestampBetween(
            @Param("tenantId") UUID tenantId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    long countByTenantIdAndTimestampAfter(UUID tenantId, Instant since);

    long countByTenantIdAndTimestampAfter(UUID tenantId, LocalDateTime since);

    Page<com.discipolat.modules.tenants.domain.AuditLog> findByTenantId(UUID tenantId, Pageable pageable);
}