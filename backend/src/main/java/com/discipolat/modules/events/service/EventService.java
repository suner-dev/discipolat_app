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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;
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

    // ========== EVENT CRUD ==========

    public Event createEvent(UUID tenantId, UUID actorId, Event event) {
        event.setTenantId(tenantId);
        event.setCreatedBy(actorId);
        Event saved = eventRepository.save(event);

        // Link spaces if provided (would be in a separate DTO in real impl)
        // outboxPublisher.publish(...)

        auditEventService.log(tenantId, actorId, null, "EVENT_CREATED", "EVENT", saved.getId(),
                Map.of(), Map.of("title", saved.getTitle()), null, null);

        return saved;
    }

    public Event updateEvent(UUID tenantId, UUID actorId, UUID eventId, Event updates) {
        Event event = getEvent(tenantId, eventId);
        // Apply updates
        if (updates.getTitle() != null) event.setTitle(updates.getTitle());
        if (updates.getDescription() != null) event.setDescription(updates.getDescription());
        if (updates.getType() != null) event.setType(updates.getType());
        if (updates.getStatus() != null) event.setStatus(updates.getStatus());
        if (updates.getStartAt() != null) event.setStartAt(updates.getStartAt());
        if (updates.getEndAt() != null) event.setEndAt(updates.getEndAt());
        if (updates.getTimezone() != null) event.setTimezone(updates.getTimezone());
        if (updates.getVisibility() != null) event.setVisibility(updates.getVisibility());

        Event saved = eventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "EVENT_UPDATED", "EVENT", saved.getId(),
                Map.of(), Map.of("title", saved.getTitle()), null, null);

        return saved;
    }

    public void deleteEvent(UUID tenantId, UUID actorId, UUID eventId) {
        Event event = getEvent(tenantId, eventId);
        event.setDeletedAt(OffsetDateTime.now());
        event.setStatus("ARCHIVED");
        eventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "EVENT_DELETED", "EVENT", eventId,
                Map.of(), Map.of(), null, null);
    }

    @Transactional(readOnly = true)
    public Event getEvent(UUID tenantId, UUID eventId) {
        return eventRepository.findByIdAndTenantIdAndDeletedAtIsNull(eventId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));
    }

    @Transactional(readOnly = true)
    public Page<Event> getEvents(UUID tenantId, Pageable pageable) {
        return eventRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsCalendar(UUID tenantId, OffsetDateTime from, OffsetDateTime to) {
        return eventRepository.findByTenantIdAndStartAtBetween(tenantId, from, to);
    }

    // ========== EVENT SPACES ==========

    public EventSpace addSpaceToEvent(UUID tenantId, UUID eventId, UUID spaceId, String role) {
        Event event = getEvent(tenantId, eventId);
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId));

        EventSpace es = EventSpace.builder()
                .tenantId(tenantId)
                .eventId(eventId)
                .spaceId(spaceId)
                .role(role)
                .build();
        return eventSpaceRepository.save(es);
    }

    public void removeSpaceFromEvent(UUID tenantId, UUID eventId, UUID spaceId) {
        eventSpaceRepository.deleteByEventIdAndSpaceId(eventId, spaceId);
    }

    @Transactional(readOnly = true)
    public List<EventSpace> getEventSpaces(UUID eventId) {
        return eventSpaceRepository.findByEventId(eventId);
    }

    // ========== EVENT TEAMS ==========

    public EventTeam createTeam(UUID tenantId, UUID eventId, EventTeam team) {
        getEvent(tenantId, eventId); // validate
        team.setTenantId(tenantId);
        team.setEventId(eventId);
        return eventTeamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public List<EventTeam> getEventTeams(UUID eventId) {
        return eventTeamRepository.findByEventId(eventId);
    }

    // ========== EVENT TASKS ==========

    public EventTask createTask(UUID tenantId, UUID eventId, EventTask task) {
        getEvent(tenantId, eventId);
        task.setTenantId(tenantId);
        task.setEventId(eventId);
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
        return eventTaskRepository.findByTenantIdAndEventIdOrderByDueAtAsc(tenantId, eventId);
    }

    // ========== ATTENDANCE / CHECK-IN ==========

    /**
     * Check-in d'une personne à un événement (QR, manuel, flash téléphone).
     * Met à jour event_attendance + outbox AttendanceRecorded.
     */
    public EventAttendance checkIn(UUID tenantId, UUID eventId, UUID personId, String method, UUID spaceId, UUID actorId) {
        Event event = getEvent(tenantId, eventId);
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Person", personId));

        EventAttendance attendance = eventAttendanceRepository.findByEventIdAndPersonId(eventId, personId)
                .orElseGet(() -> EventAttendance.builder()
                        .tenantId(tenantId)
                        .eventId(eventId)
                        .personId(personId)
                        .build());

        attendance.setStatus("PRESENT");
        attendance.setCheckInAt(OffsetDateTime.now());
        attendance.setCheckInMethod(method);
        attendance.setSpaceId(spaceId);

        EventAttendance saved = eventAttendanceRepository.save(attendance);

        // Outbox: AttendanceRecorded
        outboxPublisher.publish("EVENT_ATTENDANCE", saved.getId(), "AttendanceRecorded",
                Map.of(
                        "eventId", eventId.toString(),
                        "eventTitle", event.getTitle(),
                        "personId", personId.toString(),
                        "personName", person.getFullName(),
                        "method", method,
                        "spaceId", spaceId != null ? spaceId.toString() : null
                ));

        auditEventService.log(tenantId, actorId, null, "ATTENDANCE_RECORDED", "EVENT_ATTENDANCE", saved.getId(),
                Map.of(), Map.of("personId", personId.toString()), null, null);

        return saved;
    }

    /**
     * Check-out
     */
    public EventAttendance checkOut(UUID tenantId, UUID eventId, UUID personId) {
        EventAttendance attendance = eventAttendanceRepository.findByEventIdAndPersonId(eventId, personId)
                .orElseThrow(() -> new EntityNotFoundException("EventAttendance", eventId + "/" + personId));

        attendance.setCheckOutAt(OffsetDateTime.now());
        return eventAttendanceRepository.save(attendance);
    }

    /**
     * Flash présence par téléphone (pour membres sans smartphone évolué)
     */
    public EventAttendance flashAttendance(UUID tenantId, UUID eventId, String phoneNormalized, UUID actorId) {
        Person person = personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, phoneNormalized)
                .orElseThrow(() -> new EntityNotFoundException("Person with phone: " + phoneNormalized));
        return checkIn(tenantId, eventId, person.getId(), "PHONE_FLASH", null, actorId);
    }

    @Transactional(readOnly = true)
    public List<EventAttendance> getEventAttendance(UUID eventId) {
        return eventAttendanceRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public long getPresentCount(UUID tenantId, UUID eventId) {
        return eventAttendanceRepository.countPresentByEventId(tenantId, eventId);
    }

    // ========== EVENT SCHEDULE ==========

    public EventSchedule addScheduleItem(UUID tenantId, UUID eventId, EventSchedule item) {
        getEvent(tenantId, eventId);
        item.setTenantId(tenantId);
        item.setEventId(eventId);
        return eventScheduleRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<EventSchedule> getEventSchedule(UUID tenantId, UUID eventId) {
        return eventScheduleRepository.findByTenantIdAndEventIdOrderByStartAtAsc(tenantId, eventId);
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