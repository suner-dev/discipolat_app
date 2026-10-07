package com.discipolat.modules.events.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.events.domain.*;
import com.discipolat.modules.events.service.ChurchEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/church-events")
// §G3.3/§G3.4 — les responsables de département/famille programment et pilotent
// leurs événements (le service garde le scope) ; legacy DEPARTMENT_LEADER/FAMILY_LEADER conservés.
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'DEPARTMENT_LEADER', 'FAMILY_LEADER')")
public class ChurchEventController {

    private final ChurchEventService churchEventService;

    public ChurchEventController(ChurchEventService churchEventService) {
        this.churchEventService = churchEventService;
    }

    // ========== CHURCH EVENT CRUD ==========
    // Contrat anglais fige sur DTO (ChurchEventDto) depuis l'arbitrage D1 :
    // l'entite unique « event » est Event, a proprietes francaises.

    @GetMapping
    public ResponseEntity<PageResponse<ChurchEventDto>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("dateDebut"));
        Page<ChurchEventDto> result = churchEventService.getChurchEvents(tenantId, pageable)
                .map(ChurchEventDto::from);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<ChurchEventDto>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.getChurchEventsCalendar(tenantId, from, to)
                .stream().map(ChurchEventDto::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChurchEventDto> getEvent(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(ChurchEventDto.from(churchEventService.getChurchEvent(tenantId, id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER')")
    public ResponseEntity<ChurchEventDto> createEvent(@RequestBody ChurchEventDto event) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ChurchEventDto.from(
                churchEventService.createChurchEvent(tenantId, actorId, event.toEntity())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER')")
    public ResponseEntity<ChurchEventDto> updateEvent(@PathVariable UUID id, @RequestBody ChurchEventDto event) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ChurchEventDto.from(
                churchEventService.updateChurchEvent(tenantId, actorId, id, event.toPatch())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        churchEventService.deleteChurchEvent(tenantId, actorId, id);
        return ResponseEntity.noContent().build();
    }

    // ========== ARCHIVES (§G3.4 / §G6.4) ==========

    /** Clôturage + archive intégrale versionnée (dress codes, équipes, tâches, programme, matériel, présences). */
    @PostMapping("/{eventId}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'DEPARTMENT_LEADER')")
    public ResponseEntity<com.discipolat.modules.events.domain.ChurchEventArchive> archiveEvent(
            @PathVariable UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(churchEventService.archiveEvent(tenantId, actorId, eventId));
    }

    /** Écran « Archives » : consultation par années / mois / espace. */
    @GetMapping("/archives")
    public ResponseEntity<List<com.discipolat.modules.events.domain.ChurchEventArchive>> getArchives(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) UUID spaceId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.getArchives(tenantId, year, month, spaceId));
    }

    @GetMapping("/archives/{archiveId}")
    public ResponseEntity<com.discipolat.modules.events.domain.ChurchEventArchive> getArchive(
            @PathVariable UUID archiveId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.getArchive(tenantId, archiveId));
    }

    // ========== SPACES ==========

    @PostMapping("/{eventId}/spaces")
    public ResponseEntity<EventSpace> addSpace(@PathVariable UUID eventId,
                                                @RequestParam UUID spaceId,
                                                @RequestParam(required = false) String role) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.addSpaceToEvent(tenantId, eventId, spaceId, role));
    }

    @DeleteMapping("/{eventId}/spaces/{spaceId}")
    public ResponseEntity<Void> removeSpace(@PathVariable UUID eventId, @PathVariable UUID spaceId) {
        UUID tenantId = TenantContext.requireTenantId();
        churchEventService.removeSpaceFromEvent(tenantId, eventId, spaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/spaces")
    public ResponseEntity<List<EventSpace>> getSpaces(@PathVariable UUID eventId) {
        return ResponseEntity.ok(churchEventService.getEventSpaces(eventId));
    }

    // ========== TEAMS ==========

    @PostMapping("/{eventId}/teams")
    public ResponseEntity<EventTeam> createTeam(@PathVariable UUID eventId, @RequestBody EventTeam team) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.createTeam(tenantId, eventId, team));
    }

    @GetMapping("/{eventId}/teams")
    public ResponseEntity<List<EventTeam>> getTeams(@PathVariable UUID eventId) {
        return ResponseEntity.ok(churchEventService.getEventTeams(eventId));
    }

    @DeleteMapping("/{eventId}/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID eventId, @PathVariable UUID teamId) {
        UUID tenantId = TenantContext.requireTenantId();
        churchEventService.deleteTeam(tenantId, eventId, teamId);
        return ResponseEntity.noContent().build();
    }

    // ========== TASKS ==========

    @PostMapping("/{eventId}/tasks")
    public ResponseEntity<EventTask> createTask(@PathVariable UUID eventId, @RequestBody EventTask task) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.createTask(tenantId, eventId, task));
    }

    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<EventTask> updateTaskStatus(@PathVariable UUID taskId, @RequestParam String status) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(churchEventService.updateTaskStatus(tenantId, taskId, status, actorId));
    }

    @GetMapping("/{eventId}/tasks")
    public ResponseEntity<List<EventTask>> getTasks(@PathVariable UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.getEventTasks(tenantId, eventId));
    }

    // ========== ATTENDANCE / CHECK-IN ==========

    @PostMapping("/{eventId}/checkin")
    public ResponseEntity<EventAttendance> checkIn(@PathVariable UUID eventId,
                                                    @RequestParam UUID personId,
                                                    @RequestParam(required = false) String method,
                                                    @RequestParam(required = false) UUID spaceId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(churchEventService.checkIn(tenantId, eventId, personId, method, spaceId, actorId));
    }

    @PostMapping("/{eventId}/checkout")
    public ResponseEntity<EventAttendance> checkOut(@PathVariable UUID eventId, @RequestParam UUID personId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.checkOut(tenantId, eventId, personId));
    }

    @PostMapping("/{eventId}/flash")
    public ResponseEntity<EventAttendance> flashAttendance(@PathVariable UUID eventId,
                                                            @RequestParam String phone) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(churchEventService.flashAttendance(tenantId, eventId, phone, actorId));
    }

    @GetMapping("/{eventId}/attendance")
    public ResponseEntity<List<EventAttendance>> getAttendance(@PathVariable UUID eventId) {
        return ResponseEntity.ok(churchEventService.getEventAttendance(eventId));
    }

    @GetMapping("/{eventId}/attendance/count")
    public ResponseEntity<Long> getPresentCount(@PathVariable UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.getPresentCount(tenantId, eventId));
    }

    // ========== SCHEDULE ==========

    @PostMapping("/{eventId}/schedule")
    public ResponseEntity<EventSchedule> addScheduleItem(@PathVariable UUID eventId, @RequestBody EventSchedule item) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.addScheduleItem(tenantId, eventId, item));
    }

    @GetMapping("/{eventId}/schedule")
    public ResponseEntity<List<EventSchedule>> getSchedule(@PathVariable UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.getEventSchedule(tenantId, eventId));
    }

    // ========== LOCATIONS ==========

    @PostMapping("/locations")
    public ResponseEntity<Location> createLocation(@RequestBody Location location) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.createLocation(tenantId, location));
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Location>> getLocations() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(churchEventService.getLocations(tenantId));
    }
}