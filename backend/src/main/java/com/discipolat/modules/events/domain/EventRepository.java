package com.discipolat.modules.events.domain;

import com.discipolat.modules.events.domain.Event;
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
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByTenantIdAndDeletedAtIsNullOrderByStartAtAsc(UUID tenantId);

    Page<Event> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    List<Event> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, String status);

    @Query("SELECT e FROM Event e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL AND e.startAt BETWEEN :from AND :to ORDER BY e.startAt ASC")
    List<Event> findByTenantIdAndStartAtBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT e FROM Event e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL AND e.startAt BETWEEN :from AND :to ORDER BY e.startAt ASC")
    Page<Event> findByTenantIdAndStartAtBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);

    Optional<Event> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}