package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventScheduleRepository extends JpaRepository<EventSchedule, UUID> {

    List<EventSchedule> findByChurchEventIdOrderByOrderIndexAsc(UUID churchEventId);

    @Query("SELECT s FROM EventSchedule s WHERE s.tenantId = :tenantId AND s.churchEventId = :churchEventId ORDER BY s.startAt ASC")
    List<EventSchedule> findByTenantIdAndChurchEventIdOrderByStartAtAsc(@Param("tenantId") UUID tenantId, @Param("churchEventId") UUID churchEventId);
}