package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventAttendance;
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
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, UUID> {

    List<EventAttendance> findByChurchEventId(UUID churchEventId);

    List<EventAttendance> findByPersonId(UUID personId);

    List<EventAttendance> findByChurchEventIdAndStatus(UUID churchEventId, String status);

    Optional<EventAttendance> findByChurchEventIdAndPersonId(UUID churchEventId, UUID personId);

    @Query("SELECT a FROM EventAttendance a WHERE a.tenantId = :tenantId AND a.churchEventId = :churchEventId AND a.status = 'PRESENT'")
    long countPresentByChurchEventId(@Param("tenantId") UUID tenantId, @Param("churchEventId") UUID churchEventId);

    Page<EventAttendance> findByChurchEventIdOrderByCheckInAtAsc(UUID churchEventId, Pageable pageable);
}