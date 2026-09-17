package com.discipolat.modules.people.repository;

import com.discipolat.modules.people.domain.EventAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAssignmentRepository extends JpaRepository<EventAssignment, UUID> {

    List<EventAssignment> findByTenantIdAndEventId(UUID tenantId, UUID eventId);

    List<EventAssignment> findByPersonIdAndStatus(UUID personId, String status);

    List<EventAssignment> findByEventIdAndStatus(UUID eventId, String status);

    @Query("SELECT ea FROM EventAssignment ea WHERE ea.tenantId = :tenantId AND ea.eventId = :eventId AND ea.status IN ('ASSIGNED', 'CONFIRMED')")
    List<EventAssignment> findConfirmedByEventId(@Param("tenantId") UUID tenantId, @Param("eventId") UUID eventId);

    Page<EventAssignment> findByTenantIdAndEventIdOrderByStartAtAsc(UUID tenantId, UUID eventId, Pageable pageable);

    long countByEventIdAndStatus(UUID eventId, String status);
}