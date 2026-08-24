package com.discipolat.modules.calendar.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    List<CalendarEvent> findByTenantIdAndDébutBetweenOrderByDébutAsc(UUID tenantId, LocalDateTime start, LocalDateTime end);
    List<CalendarEvent> findByTenantIdOrderByDébutAsc(UUID tenantId);
    List<CalendarEvent> findByExternalId(String externalId);
}
