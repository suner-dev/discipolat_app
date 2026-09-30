package com.discipolat.modules.events.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.Payloads;
import com.discipolat.modules.audit.service.AuditEventService;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventSpace;
import com.discipolat.modules.events.domain.EventTeam;
import com.discipolat.modules.events.domain.EventTask;
import com.discipolat.modules.events.domain.EventAsset;
import com.discipolat.modules.events.domain.EventExpense;
import com.discipolat.modules.events.domain.EventAttendance;
import com.discipolat.modules.events.domain.EventDocument;
import com.discipolat.modules.events.domain.EventSchedule;
import com.discipolat.modules.events.domain.Location;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    // Depuis l'arbitrage D1 (V203), l'entité unique de la table « event » est
    // Event (propriétés françaises). Les méthodes sont renommées côté EN
    // (createChurchEvent…) pour lever toute ambiguïté avec EventService, et
    // les horodatages OffsetDateTime du modèle vivant sont convertus à la
    // frontière dans la convention « naive = UTC » de V158.

    public Event createChurchEvent(UUID tenantId, UUID actorId, Event event) {
        event.setTenantId(tenantId);
        event.setCreatedBy(actorId);
        Event saved = churchEventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "CHURCH_EVENT_CREATED", "CHURCH_EVENT", saved.getId(),
                Map.of(), Map.of("title", saved.getTitre()), null, null);

        outboxPublisher.publish("CHURCH_EVENT", saved.getId(), "EventCreated",
                Map.of("eventId", saved.getId().toString(), "title", saved.getTitre(), "startAt", saved.getDateDebut().toString()));

        return saved;
    }

    public Event updateChurchEvent(UUID tenantId, UUID actorId, UUID eventId, Event updates) {
        Event event = getChurchEvent(tenantId, eventId);
        if (updates.getTitre() != null) event.setTitre(updates.getTitre());
        if (updates.getDescription() != null) event.setDescription(updates.getDescription());
        if (updates.getTypeEvenement() != null) event.setTypeEvenement(updates.getTypeEvenement());
        if (updates.getStatut() != null) event.setStatut(updates.getStatut());
        if (updates.getDateDebut() != null) event.setDateDebut(updates.getDateDebut());
        if (updates.getDateFin() != null) event.setDateFin(updates.getDateFin());
        if (updates.getTimezone() != null) event.setTimezone(updates.getTimezone());
        if (updates.getVisibility() != null) event.setVisibility(updates.getVisibility());
        if (updates.getOrganisateurId() != null) event.setOrganisateurId(updates.getOrganisateurId());
        if (updates.getIsRecurring() != null) event.setIsRecurring(updates.getIsRecurring());
        if (updates.getRecurrenceRule() != null) event.setRecurrenceRule(updates.getRecurrenceRule());

        Event saved = churchEventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "CHURCH_EVENT_UPDATED", "CHURCH_EVENT", saved.getId(),
                Map.of(), Map.of("title", saved.getTitre()), null, null);

        outboxPublisher.publish("CHURCH_EVENT", saved.getId(), "EventUpdated",
                Map.of("eventId", saved.getId().toString(), "title", saved.getTitre()));

        return saved;
    }

    public void deleteChurchEvent(UUID tenantId, UUID actorId, UUID eventId) {
        Event event = getChurchEvent(tenantId, eventId);
        event.setDeletedAt(LocalDateTime.now());
        event.setStatut("ARCHIVED");
        churchEventRepository.save(event);

        auditEventService.log(tenantId, actorId, null, "CHURCH_EVENT_DELETED", "CHURCH_EVENT", eventId,
                Map.of(), Map.of(), null, null);

        outboxPublisher.publish("CHURCH_EVENT", eventId, "EventDeleted",
                Map.of("eventId", eventId.toString()));
    }

    @Transactional(readOnly = true)
    public Event getChurchEvent(UUID tenantId, UUID eventId) {
        return churchEventRepository.findByIdAndTenantIdAndDeletedAtIsNull(eventId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));
    }

    @Transactional(readOnly = true)
    public Page<Event> getChurchEvents(UUID tenantId, Pageable pageable) {
        return churchEventRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Event> getChurchEventsCalendar(UUID tenantId, OffsetDateTime from, OffsetDateTime to) {
        return churchEventRepository.findCalendarByTenantId(tenantId,
                naiveUtc(from), naiveUtc(to));
    }

    /** OffsetDateTime -> LocalDateTime « naive = UTC » (convention V158). */
    public static LocalDateTime naiveUtc(OffsetDateTime odt) {
        return odt == null ? null : odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** LocalDateTime « naive = UTC » -> OffsetDateTime du fil EN (rendu +00:00). */
    public static OffsetDateTime offsetUtc(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }

    // ========== EVENT SPACES ==========

    public EventSpace addSpaceToEvent(UUID tenantId, UUID eventId, UUID spaceId, String role) {
        // Validation will be done by SpaceRepository
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
        getChurchEvent(tenantId, eventId);
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
        getChurchEvent(tenantId, eventId);
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
        Event event = getChurchEvent(tenantId, eventId);
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
                Payloads.of(
                        "eventId", eventId.toString(),
                        "eventTitle", event.getTitre(),
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
                .orElseThrow(() -> new EntityNotFoundException("EventAttendance", "eventId/personId", eventId + "/" + personId));

        attendance.setCheckOutAt(OffsetDateTime.now());
        return eventAttendanceRepository.save(attendance);
    }

    public EventAttendance flashAttendance(UUID tenantId, UUID eventId, String phoneNormalized, UUID actorId) {
        Person person = personRepository.findByTenantIdAndPhoneNormalizedAndDeletedAtIsNull(tenantId, phoneNormalized)
                .orElseThrow(() -> new EntityNotFoundException("Person", "phoneNormalized", phoneNormalized));
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
        getChurchEvent(tenantId, eventId);
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