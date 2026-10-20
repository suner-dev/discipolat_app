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
    /** §G3.4/§G6.4 — archives intégrales d'événement (snapshot versionné au clôturage). */
    private final ChurchEventArchiveRepository archiveRepository;
    private final com.discipolat.modules.dresscode.domain.DressCodeRepository dressCodeRepository;
    private final com.discipolat.modules.dresscode.domain.DressCodeRuleRepository dressCodeRuleRepository;

    // ========== ARCHIVES (§G3.4) ==========

    /**
     * Clôture et archive INTÉGRALEMENT un événement : snapshot JSON versionné
     * (espaces, équipes, tâches, programme, matériel, dépenses, documents,
     * présences, dress codes + règles) — jamais d'écrasement : chaque re-clôture
     * crée une nouvelle version, l'événement passe au statut ARCHIVED.
     */
    public com.discipolat.modules.events.domain.ChurchEventArchive archiveEvent(
            UUID tenantId, UUID actorId, UUID eventId) {
        // Develop1 écrivait cette méthode sur son entité propre « ChurchEvent ».
        // main a unifié les événements sur la table vivante `event` (V203) : une
        // seconde entité JPA sur cette table ferait échouer Hibernate au
        // démarrage. Le snapshot est donc lu via l'entité vivante, SANS perte : le
        // champ archivé conserve l'instantané complet en JSONB.
        //
        // V272 — verrou pessimiste sur la ligne événement : deux clôtures
        // simultanées d'un MÊME événement sont sérialisées, le calcul
        // `max(version) + 1` devient sûr. L'index unique
        // uq_event_archive_event_version (V272) reste le dernier rempart.
        // Les critères (tenant, non supprimé) sont identiques à la lecture
        // simple : le verrou n'affaiblit jamais l'isolation.
        Event event = churchEventRepository.findForUpdate(eventId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Event", eventId));

        List<EventSpace> spaces = eventSpaceRepository.findByChurchEventId(eventId);
        List<EventTeam> teams = eventTeamRepository.findByChurchEventId(eventId);
        List<EventTask> tasks = eventTaskRepository.findByTenantIdAndChurchEventIdOrderByDueAtAsc(tenantId, eventId);
        List<EventSchedule> schedule = eventScheduleRepository.findByTenantIdAndChurchEventIdOrderByStartAtAsc(tenantId, eventId);
        List<EventAsset> assets = eventAssetRepository.findByChurchEventId(eventId);
        List<EventExpense> expenses = eventExpenseRepository.findByChurchEventId(eventId);
        List<EventDocument> documents = eventDocumentRepository.findByChurchEventId(eventId);
        List<EventAttendance> attendance = eventAttendanceRepository.findByChurchEventId(eventId);

        List<Map<String, Object>> dressCodes = new ArrayList<>();
        // §G6.4 — l'archive est un instantané HISTORIQUE : elle inclut aussi les
        // tenues déjà archivées pour cet événement (sinon archive trouée).
        for (var dc : dressCodeRepository.findByTenantIdAndEventId(tenantId, eventId)) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", dc.getId().toString());
            d.put("title", dc.getTitle());
            d.put("serviceName", dc.getServiceName());
            d.put("spaceId", dc.getSpaceId() != null ? dc.getSpaceId().toString() : null);
            d.put("status", dc.getStatus());
            d.put("rules", dressCodeRuleRepository.findByDressCodeId(dc.getId()).stream().map(r -> {
                Map<String, Object> rr = new LinkedHashMap<>();
                rr.put("groupName", r.getGroupName());
                rr.put("description", r.getDescription());
                return rr;
            }).toList());
            dressCodes.add(d);
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        LocalDateTime startAt = event.getDateDebut();
        snapshot.put("title", event.getTitre());
        snapshot.put("description", event.getDescription());
        snapshot.put("type", event.getTypeEvenement());
        snapshot.put("startAt", startAt != null ? startAt.toString() : null);
        snapshot.put("endAt", event.getDateFin() != null ? event.getDateFin().toString() : null);
        snapshot.put("spaces", spaces.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("spaceId", s.getSpaceId().toString());
            m.put("spaceName", spaceRepository.findById(s.getSpaceId()).map(Space::getName).orElse(null));
            m.put("role", s.getRole());
            return m;
        }).toList());
        snapshot.put("teams", teams.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            return m;
        }).toList());
        snapshot.put("tasks", tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", t.getTitle());
            m.put("status", t.getStatus());
            m.put("dueAt", t.getDueAt() != null ? t.getDueAt().toString() : null);
            return m;
        }).toList());
        snapshot.put("schedule", schedule.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", s.getTitle());
            m.put("startAt", s.getStartAt() != null ? s.getStartAt().toString() : null);
            m.put("endAt", s.getEndAt() != null ? s.getEndAt().toString() : null);
            return m;
        }).toList());
        snapshot.put("assets", assets.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assetId", a.getAssetId().toString());
            m.put("quantity", a.getQuantity());
            m.put("conditionBefore", a.getConditionBefore());
            m.put("conditionAfter", a.getConditionAfter());
            return m;
        }).toList());
        snapshot.put("expenses", expenses.size());
        snapshot.put("documents", documents.size());
        snapshot.put("attendanceCount", attendance.size());
        snapshot.put("dressCodes", dressCodes);

        int nextVersion = archiveRepository.findByEventIdOrderByVersionDesc(eventId).stream()
                .map(com.discipolat.modules.events.domain.ChurchEventArchive::getVersion)
                .max(Integer::compareTo).orElse(0) + 1;

        event.setStatut(Event.STATUT_ARCHIVE);
        churchEventRepository.save(event);

        com.discipolat.modules.events.domain.ChurchEventArchive archive =
                com.discipolat.modules.events.domain.ChurchEventArchive.builder()
                        .tenantId(tenantId)
                        .eventId(eventId)
                        .eventTitle(event.getTitre())
                        .eventStartAt(startAt != null ? offsetUtc(startAt) : OffsetDateTime.now())
                        .eventYear(startAt != null ? startAt.getYear() : OffsetDateTime.now().getYear())
                        .eventMonth(startAt != null ? startAt.getMonthValue() : OffsetDateTime.now().getMonthValue())
                        .spaceId(spaces.isEmpty() ? null : spaces.get(0).getSpaceId())
                        .version(nextVersion)
                        .snapshotJson(snapshot)
                        // V272 — dénormalisation pour la liste paginée : le
                        // compteur devient une colonne indexable, plus besoin de
                        // désérialiser le snapshot pour rendre une carte.
                        .attendanceCount(attendance.size())
                        .archivedBy(actorId)
                        .build();
        com.discipolat.modules.events.domain.ChurchEventArchive saved = archiveRepository.save(archive);

        auditEventService.log(tenantId, actorId, null, "EVENT_ARCHIVED", "CHURCH_EVENT", eventId,
                Map.of(), Map.of("archiveId", saved.getId().toString(), "version", nextVersion), null, null);
        outboxPublisher.publish(tenantId, "CHURCH_EVENT", eventId, "EventArchived",
                Map.of("archiveId", saved.getId().toString(), "title", event.getTitre()));
        return saved;
    }

    /** Écran « Archives » : consultation par années / mois / espace / événement.
     *  V272 — le filtre `eventId` (additif) permet à l'écran de lister les
     *  versions d'un événement pour la navigation de versions ; le filtre
     *  `spaceId` conserve son repli snapshot (espaces secondaires) — la
     *  variante paginée {@link #searchArchives} filtre sur la colonne indexée. */
    @Transactional(readOnly = true)
    public List<com.discipolat.modules.events.domain.ChurchEventArchive> getArchives(
            UUID tenantId, Integer year, Integer month, UUID spaceId, UUID eventId) {
        List<com.discipolat.modules.events.domain.ChurchEventArchive> result;
        if (year != null && month != null) {
            result = archiveRepository.findByTenantIdAndEventYearAndEventMonthOrderByEventStartAtDesc(tenantId, year, month);
        } else if (year != null) {
            result = archiveRepository.findByTenantIdAndEventYearOrderByEventStartAtDesc(tenantId, year);
        } else {
            result = archiveRepository.findByTenantIdOrderByEventStartAtDesc(tenantId);
        }
        if (eventId != null) {
            result = result.stream().filter(a -> eventId.equals(a.getEventId())).toList();
        }
        if (spaceId != null) {
            result = result.stream()
                    .filter(a -> spaceId.equals(a.getSpaceId())
                            || a.getSnapshotJson().get("spaces") instanceof List<?> l && l.stream()
                                    .filter(Map.class::isInstance).map(Map.class::cast)
                                    .anyMatch(sp -> spaceId.toString().equals(sp.get("spaceId"))))
                    .toList();
        }
        return result;
    }

    /** Variante historique (sans `eventId`) — conservée pour tout appelant existant. */
    @Transactional(readOnly = true)
    public List<com.discipolat.modules.events.domain.ChurchEventArchive> getArchives(
            UUID tenantId, Integer year, Integer month, UUID spaceId) {
        return getArchives(tenantId, year, month, spaceId, null);
    }

    /**
     * Recherche paginée de l'écran « Archives » (V272) — résumés SANS snapshot.
     * C'est le chemin servi par le web : coût borné quel que soit l'historique.
     * L'isolation tenant est portée par la requête elle-même.
     */
    @Transactional(readOnly = true)
    public Page<com.discipolat.modules.events.api.ChurchEventArchiveSummary> searchArchives(
            UUID tenantId, Integer year, Integer month, UUID spaceId, UUID eventId, Pageable pageable) {
        return archiveRepository.searchSummaries(tenantId, year, month, spaceId, eventId, pageable);
    }

    /**
     * Facettes années/mois de l'écran « Archives » (V272) : les filtres sont
     * dérivés des données réellement archivées — aucun libellé ni année n'est
     * codé en dur côté client, aucune liste complète n'est téléchargée.
     */
    @Transactional(readOnly = true)
    public List<com.discipolat.modules.events.api.ChurchEventArchiveFacet> getArchiveFacets(UUID tenantId) {
        Map<Integer, List<Integer>> monthsByYear = new LinkedHashMap<>();
        Map<Integer, Long> countByYear = new LinkedHashMap<>();
        for (Object[] row : archiveRepository.findYearMonthFacets(tenantId)) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            Long count = (Long) row[2];
            monthsByYear.computeIfAbsent(year, y -> new ArrayList<>()).add(month);
            countByYear.merge(year, count, Long::sum);
        }
        return monthsByYear.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<Integer>>comparingByKey(Comparator.reverseOrder()))
                .map(e -> new com.discipolat.modules.events.api.ChurchEventArchiveFacet(
                        e.getKey(), e.getValue().stream().sorted().toList(),
                        countByYear.getOrDefault(e.getKey(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public com.discipolat.modules.events.domain.ChurchEventArchive getArchive(UUID tenantId, UUID archiveId) {
        return archiveRepository.findById(archiveId)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("ChurchEventArchive", archiveId));
    }

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
        // V272 — cohérence d'archivage : ARCHIVED n'est PAS un statut éditable.
        // Un PUT qui l'écrirait fabriquerait un événement « archivé » sans
        // instantané dans event_archive (divergence relevée par l'audit : deux
        // chemins d'archivage, un seul producteur de snapshot). Le seul écritain
        // légitime de ce statut est la clôture POST /{id}/archive.
        if (updates.getStatut() != null) {
            if (Event.STATUT_ARCHIVE.equalsIgnoreCase(updates.getStatut())) {
                throw new IllegalArgumentException(
                        "Statut ARCHIVED réservé à la clôture d'archive "
                                + "(POST /api/v1/church-events/{eventId}/archive) : "
                                + "un simple PUT ne peut pas créer d'archivage sans instantané.");
            }
            event.setStatut(updates.getStatut());
        }
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

    /**
     * V272 — filtre `status` réellement appliqué (le contrôleur l'acceptait
     * déjà sans le transmettre : paramètre mort relevé par l'audit). Absent ou
     * vide → comportement historique strictement inchangé.
     */
    @Transactional(readOnly = true)
    public Page<Event> getChurchEvents(UUID tenantId, String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return getChurchEvents(tenantId, pageable);
        }
        return churchEventRepository.findByTenantIdAndStatutAndDeletedAtIsNull(
                tenantId, status.trim(), pageable);
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

    /** Supprime une équipe d'événement — double scope tenant + événement (anti-IDOR). */
    @Transactional
    public void deleteTeam(UUID tenantId, UUID eventId, UUID teamId) {
        getChurchEvent(tenantId, eventId);
        EventTeam team = eventTeamRepository.findByTenantIdAndChurchEventIdAndId(tenantId, eventId, teamId)
                .orElseThrow(() -> new EntityNotFoundException("EventTeam", teamId));
        eventTeamRepository.delete(team);
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