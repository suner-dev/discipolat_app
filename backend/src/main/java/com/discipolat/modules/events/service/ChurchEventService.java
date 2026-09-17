package com.discipolat.modules.events.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.service.AuditEventService;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.events.domain.*;
import com.discipolat.modules.events.repository.*;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ChurchEventService {

    private final ChurchEventRepository churchEventRepository;
    private final EventSpaceRepository eventSpaceRepository;
    private final EventTeamRepository eventTeamRepository;
    private final EventTaskRepository eventTaskRepository;
    private final EventAssetRepository eventAssetRepository;
    private final EventExpenseRepository eventExpenseRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final EventDocumentRepository eventDocumentRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final LocationRepository locationRepository;
    private final SpaceRepository spaceRepository;
    private final PersonRepository personRepository;
    private final AuditEventService auditEventService;
    private final OutboxPublisher outboxPublisher;

    // ========== CHURCH EVENT CRUD ==========

    public ChurchEvent createEvent(UUID tenantId, UUID actorId, ChurchEvent event) {
        event.setTenantId(tenantId);
        event.setCreatedBy(actorId);
        ChurchEvent saved = churchEventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "CHURCH_EVENT_CREATED", "CHURCH_EVENT", saved.getId(),
                Map.of(), Map.of("title", saved.getTitle()), null, null);

        return saved;
    }

    public ChurchEvent updateEvent(UUID tenantId, UUID actorId, UUID eventId, ChurchEvent updates) {
        ChurchEvent event = getEvent(tenantId, eventId);
        if (updates.getTitle() != null) event.setTitle(updates.getTitle());
        if (updates.getDescription() != null) event.setDescription(updates.getDescription());
        if (updates.getType() != null) event.setType(updates.getType());
        if (updates.getStatus() != null) event.setStatus(updates.getStatus());
        if (updates.getStartAt() != null) event.setStartAt(updates.getStartAt());
        if (updates.getEndAt() != null) event.setEndAt(updates.getEndAt());
        if (updates.getTimezone() != null) event.setTimezone(updates.getTimezone());
        if (updates.getVisibility() != null) event.setVisibility(updates.getVisibility());

        ChurchEvent saved = churchEventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "CHURCH_EVENT_UPDATED", "CHURCH_EVENT", saved.getId(),
                Map.of(), Map.of("title", saved.getTitle()), null, null);

        return saved;
    }

    public void deleteEvent(UUID tenantId, UUID actorId, UUID eventId) {
        ChurchEvent event = getEvent(tenantId, eventId);
        event.setDeletedAt(OffsetDateTime.now());
        event.setStatus("ARCHIVED");
        churchEventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "CHURCH_EVENT_DELETED", "CHURCH_EVENT", eventId,
                Map.of(), Map.of(), null, null);
    }

    @Transactional(readOnly = true)
    public ChurchEvent getEvent(UUID tenantId, UUID eventId) {
        return churchEventRepository.findByIdAndTenantIdAndDeletedAtIsNull(eventId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("ChurchEvent", eventId));
    }

    @Transactional(readOnly = true)
    public Page<ChurchEvent> getEvents(UUID tenantId, Pageable pageable) {
        return churchEventRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ChurchEvent> getEventsCalendar(UUID tenantId, OffsetDateTime from, OffsetDateTime to) {
        return churchEventRepository.findByTenantIdAndStartAtBetween(tenantId, from, to);
    }

    // ========== EVENT SPACES ==========

    public EventSpace addSpaceToEvent(UUID tenantId, UUID eventId, UUID spaceId, String role) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId));

        EventSpace es = EventSpace.builder()
                .tenantId(tenantId)
                .churchEventId(eventId)
                .spaceId(spaceId)
                .role(role)
                .build();
        return eventSpaceRepository.save(es);
    }

    public void removeSpaceFromEvent(UUID tenantId, UUID eventId, UUID spaceId) {
        eventSpaceRepository.deleteByChurchEventIdAndSpaceId(eventId, spaceId);
    }

    @Transactional(readOnly = true)
    public List<EventSpace> getEventSpaces(UUID eventId) {
        return eventSpaceRepository.findByChurchEventId(eventId);
    }

    // ========== EVENT TEAMS ==========

    public EventTeam createTeam(UUID tenantId, UUID eventId, EventTeam team) {
        getEvent(tenantId, eventId);
        team.setTenantId(tenantId);
        team.setChurchEventId(eventId);
        return eventTeamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public List<EventTeam> getEventTeams(UUID eventId) {
        return eventTeamRepository.findByChurchEventId(eventId);
    }

    // ========== EVENT TASKS ==========

    public EventTask createTask(UUID tenantId, UUID eventId, EventTask task) {
        getEvent(tenantId, eventId);
        task.setTenantId(tenantId);
        task.setChurchEventId(eventId);
        return eventTaskRepository.save(task);
    }

    public EventTask updateTaskStatus(UUID tenantId, UUID taskId, String status, UUID actorId) {
        EventTask task = eventTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("EventTask", taskId));

        task.setStatus(status);
        if ("DONE".equals(status)) {
            task.setCompletedAt(OffsetDateTime.now());
        }
        return eventTaskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<EventTask> getEventTasks(UUID tenantId, UUID eventId) {
        return eventTaskRepository.findByTenantIdAndChurchEventIdOrderByDueAtAsc(tenantId, eventId);
    }

    // ========== ATTENDANCE / CHECK-IN ==========

    public EventAttendance checkIn(UUID tenantId, UUID eventId, UUID personId, String method, UUID spaceId, UUID actorId) {
        ChurchEvent event = getEvent(tenantId, eventId);
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Person", personId));

        EventAttendance attendance = eventAttendanceRepository.findByChurchEventIdAndPersonId(eventId, personId)
                .orElseGet(() -> EventAttendance.builder()
                        .tenantId(tenantId)
                        .churchEventId(eventId)
                        .personId(personId)
                        .build());

        attendance.setStatus("PRESENT");
        attendance.setCheckInAt(OffsetDateTime.now());
        attendance.setCheckInMethod(method);
        attendance.setSpaceId(spaceId);

        EventAttendance saved = eventAttendanceRepository.save(attendance);

        outboxPublisher.publish("CHURCH_EVENT_ATTENDANCE", saved.getId(), "AttendanceRecorded",
                Map.of(
                        "eventId", eventId.toString(),
                        "eventTitle", event.getTitle(),
                        "personId", personId.toString(),
                        "personName", person.getFullName(),
                        "method", method,
                        "spaceId", spaceId != null ? spaceId.toString() : null
                ));

        auditEventService.log(tenantId, actorId, null, "ATTENDANCE_RECORDED", "CHURCH_EVENT_ATTENDANCE", saved.getId(),
                Map.of(), Map.of("personId", personId.toString()), null, null);

        return saved;
    }

    public EventAttendance checkOut(UUID tenantId, UUID eventId, UUID personId) {
        EventAttendance attendance = eventAttendanceRepository.findByChurchEventIdAndPersonId(eventId, personId)
                .orElseThrow(() -> new EntityNotFoundException("EventAttendance", eventId + "/" + personId));

        attendance.setCheckOutAt(OffsetDateTime.now());
        return eventAttendanceRepository.save(attendance);
    }

    public EventAttendance flashAttendance(UUID tenantId, UUID eventId, String phoneNormalized, UUID actorId) {
        Person person = personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, phoneNormalized)
                .orElseThrow(() -> new EntityNotFoundException("Person with phone: " + phoneNormalized));
        return checkIn(tenantId, eventId, person.getId(), "PHONE_FLASH", null, actorId);
    }

    @Transactional(readOnly = true)
    public List<EventAttendance> getEventAttendance(UUID eventId) {
        return eventAttendanceRepository.findByChurchEventId(eventId);
    }

    @Transactional(readOnly = true)
    public long getPresentCount(UUID tenantId, UUID eventId) {
        return eventAttendanceRepository.countPresentByChurchEventId(tenantId, eventId);
    }

    // ========== EVENT SCHEDULE ==========

    public EventSchedule addScheduleItem(UUID tenantId, UUID eventId, EventSchedule item) {
        getEvent(tenantId, eventId);
        item.setTenantId(tenantId);
        item.setChurchEventId(eventId);
        return eventScheduleRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<EventSchedule> getEventSchedule(UUID tenantId, UUID eventId) {
        return eventScheduleRepository.findByTenantIdAndChurchEventIdOrderByStartAtAsc(tenantId, eventId);
    }

    // ========== LOCATION ==========

    public Location createLocation(UUID tenantId, Location location) {
        location.setTenantId(tenantId);
        return locationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public List<Location> getLocations(UUID tenantId) {
        return locationRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId);
    }
}