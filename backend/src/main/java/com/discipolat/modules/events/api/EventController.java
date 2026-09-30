package com.discipolat.modules.events.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventGeofence;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.files.domain.EntityAttachment;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.events.domain.EventRegistration;
import com.discipolat.modules.events.domain.EventService;
import com.discipolat.modules.events.domain.WeeklyProgramTemplate;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')")
public class EventController {

    private final EventService eventService;
    private final EntityAttachmentService attachmentService;

    public EventController(EventService eventService, EntityAttachmentService attachmentService) {
        this.eventService = eventService;
        this.attachmentService = attachmentService;
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.from(event,
                attachmentService.itemsFor(EntityAttachment.EntityType.EVENT, event.getId()));
    }

    /** BigDecimal (JSON) vers Double (entite), en preservant l'absence. */
    private static Double toDouble(java.math.BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        Event event = Event.builder()
                .typeEvenement(request.typeEvenement())
                .titre(request.titre())
                .description(request.description())
                .lieu(request.lieu())
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .limitePlaces(request.limitePlaces())
                .familleId(request.familleId())
                .departmentId(request.departmentId())
                .imageUrl(request.imageUrl())
                .tags(request.tags() == null ? null : request.tags().toArray(new String[0]))
                .publicEvent(Boolean.TRUE.equals(request.isPublic()))
                .requiresRegistration(Boolean.TRUE.equals(request.requiresRegistration()))
                .checkinEnabled(Boolean.TRUE.equals(request.hasCheckin()))
                .streamId(request.streamId())
                .latitude(toDouble(request.latitude()))
                .longitude(toDouble(request.longitude()))
                .geofenceRadiusMeters(request.geofenceRadiusMeters() == null
                        ? 200 : request.geofenceRadiusMeters())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(eventService.create(event, request.fichierIds())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<EventResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(eventService.findById(id)));
    }

    /** Événements rattachés à un département (espace Responsable). */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<EventResponse>> findByDepartmentId(
            @PathVariable UUID departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "dateDebut"));
        Page<EventResponse> response = eventService.findByDepartmentId(departmentId, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<EventResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) String typeEvenement,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "false") boolean upcomingOnly) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.ASC, "dateDebut"));
        Page<Event> events;
        if (upcomingOnly) {
            events = eventService.findUpcoming(pageable);
        } else if (familleId != null) {
            events = eventService.findByFamilleId(familleId, pageable);
        } else if (typeEvenement != null) {
            events = eventService.findByTypeEvenement(typeEvenement, pageable);
        } else if (statut != null) {
            events = eventService.findByStatut(statut, pageable);
        } else {
            events = eventService.findAll(pageable);
        }
        Page<EventResponse> response = events.map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<EventResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateEventRequest request) {
        Event event = Event.builder()
                .titre(request.titre())
                .description(request.description())
                .lieu(request.lieu())
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .limitePlaces(request.limitePlaces())
                .typeEvenement(request.typeEvenement())
                .statut(request.statut())
                .compteRendu(request.compteRendu())
                .departmentId(request.departmentId())
                .imageUrl(request.imageUrl())
                .tags(request.tags() == null ? null : request.tags().toArray(new String[0]))
                // Les booleens sont des patches partiels : null = inchange.
                .publicEvent(request.isPublic())
                .requiresRegistration(request.requiresRegistration())
                .checkinEnabled(request.hasCheckin())
                .streamId(request.streamId())
                .latitude(toDouble(request.latitude()))
                .longitude(toDouble(request.longitude()))
                .geofenceRadiusMeters(request.geofenceRadiusMeters() == null
                        ? 200 : request.geofenceRadiusMeters())
                .build();
        return ResponseEntity.ok(toResponse(eventService.update(id, event, request.fichierIds())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/register")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<EventRegistrationResponse> register(@PathVariable UUID eventId) {
        EventRegistration reg = eventService.register(eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EventRegistrationResponse.from(reg));
    }

    @DeleteMapping("/{eventId}/unregister")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Void> unregister(@PathVariable UUID eventId) {
        eventService.unregister(eventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/attendance")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<EventRegistrationResponse> markAttendance(
            @PathVariable UUID eventId,
            @RequestBody Map<String, Object> body) {
        UUID userId = UUID.fromString((String) body.get("userId"));
        boolean present = (Boolean) body.get("present");
        return ResponseEntity.ok(EventRegistrationResponse.from(
                eventService.markAttendance(eventId, userId, present)));
    }

    // ==================== POINTAGE GEOLOCALISE ====================

    /**
     * Pointage verifie par le serveur.
     *
     * <p>C'est le SEUL pointage dont la regle est opposable : le client transmet
     * sa position, le serveur detient le lieu et le rayon, et statue. Le controle
     * fait sur l'appareil (deja present dans l'app) n'est qu'un confort.
     *
     * <p>Codes : 201 accepte, 422 refuse (le corps porte le motif et les
     * chiffres, pour que l'ecran puisse expliquer et que la contestation soit
     * arbitrable), 403 pointage desactive sur l'evenement.
     */
    @PostMapping("/{eventId}/checkin")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<GeofenceCheckinResponse> checkin(
            @PathVariable UUID eventId,
            @RequestBody GeofenceCheckinRequest request) {

        Event event = eventService.findById(eventId);

        if (!Boolean.TRUE.equals(event.getCheckinEnabled())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    GeofenceCheckinResponse.of(false, "POINTAGE_DESACTIVE", 0, 0,
                            event.getGeofenceRadiusMeters(), 0,
                            "Le pointage n'est pas active pour cet evenement"));
        }

        if (request == null || request.latitude() == null || request.longitude() == null) {
            return ResponseEntity.unprocessableEntity().body(
                    GeofenceCheckinResponse.of(false, "POSITION_MANQUANTE", 0, 0,
                            event.getGeofenceRadiusMeters(), 0,
                            "Position manquante : activez la geolocalisation pour pointer"));
        }

        EventGeofence.Verdict verdict = EventGeofence.evaluate(
                event.getLatitude(), event.getLongitude(),
                event.getGeofenceRadiusMeters(),
                request.latitude(), request.longitude(), request.accuracyMeters());

        if (!verdict.accepted()) {
            return ResponseEntity.unprocessableEntity().body(
                    GeofenceCheckinResponse.of(false, verdict.reason().name(),
                            verdict.distanceMeters(), verdict.accuracyMeters(),
                            verdict.radiusMeters(), verdict.marginMeters(),
                            messageFor(verdict)));
        }

        // Pointage accepte : on enregistre la PREUVE (position, precision, distance).
        UUID userId = SecurityUtils.getCurrentUserId();
        eventService.markAttendanceWithProof(eventId, userId, true,
                request.latitude(), request.longitude(),
                verdict.accuracyMeters(), verdict.distanceMeters());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                GeofenceCheckinResponse.of(true, verdict.reason().name(),
                        verdict.distanceMeters(), verdict.accuracyMeters(),
                        verdict.radiusMeters(), verdict.marginMeters(),
                        "Pointage enregistre"));
    }

    private String messageFor(EventGeofence.Verdict verdict) {
        return switch (verdict.reason()) {
            case DANS_LE_PERIMETRE -> "Dans le perimetre";
            case HORS_PERIMETRE -> "Trop loin du lieu de l'evenement";
            case PRECISION_INSUFFISANTE -> "Position trop imprecise : essayez dehors";
            case LIEU_NON_CONFIGURE -> "Geolocalisation non configuree pour cet evenement";
            case COORDONNEES_INVALIDES -> "Position invalide";
        };
    }

    @GetMapping("/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<EventRegistrationResponse>> getRegistrations(
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getRegistrations(eventId)
                .stream().map(EventRegistrationResponse::from).toList());
    }

    // ======================== P3 #113 — ÉVÉNEMENTS À VENIR (MEMBRE) ========================

    /** Calendrier personnel du membre : événements à venir + son RSVP. */
    @GetMapping("/upcoming/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> myUpcoming(
            @RequestParam(defaultValue = "60") int days) {
        return ResponseEntity.ok(eventService.myUpcomingEvents(days));
    }

    /** RSVP membre : GOING / INTERESTED / CANCEL. */
    @PutMapping("/{eventId}/rsvp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventRegistrationResponse> setRsvp(
            @PathVariable UUID eventId,
            @RequestBody Map<String, String> body) {
        EventRegistration reg = eventService.setRsvp(eventId, body.get("rsvp"));
        return ResponseEntity.ok(reg == null ? null : EventRegistrationResponse.from(reg));
    }

    // ======================== WEEKLY PROGRAM TEMPLATES (US-50) ========================

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<WeeklyProgramTemplate>> getTemplates() {
        return ResponseEntity.ok(eventService.getActiveTemplates());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<WeeklyProgramTemplate> createTemplate(@Valid @RequestBody WeeklyProgramTemplate template) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createTemplate(template));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<WeeklyProgramTemplate> updateTemplate(
            @PathVariable UUID id, @Valid @RequestBody WeeklyProgramTemplate template) {
        return ResponseEntity.ok(eventService.updateTemplate(id, template));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        eventService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/templates/{id}/toggle")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Void> toggleTemplate(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        eventService.toggleTemplateActif(id, body.getOrDefault("actif", true));
        return ResponseEntity.ok().build();
    }

    /**
     * Generate events for a specific week from the active program templates.
     */
    @PostMapping("/program/generate")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<List<EventResponse>> generateWeekProgram(
            @RequestParam(required = false) String semaine) {
        LocalDate weekStart = semaine != null ? LocalDate.parse(semaine)
                : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        List<Event> events = eventService.generateWeekProgram(weekStart);
        return ResponseEntity.ok(events.stream().map(this::toResponse).toList());
    }

    /**
     * Generate events for the next 4 weeks from the active program templates.
     */
    @PostMapping("/program/generate-month")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<List<EventResponse>> generateMonthProgram() {
        List<Event> events = eventService.generateMonthProgram();
        return ResponseEntity.ok(events.stream().map(this::toResponse).toList());
    }

    /**
     * Get the program for a specific week.
     */
    @GetMapping("/program/week")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<EventResponse>> getWeekProgram(
            @RequestParam(required = false) String semaine) {
        LocalDate weekStart = semaine != null ? LocalDate.parse(semaine)
                : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        List<Event> events = eventService.getWeekProgram(weekStart);
        return ResponseEntity.ok(events.stream().map(this::toResponse).toList());
    }

    // ======================== US-55: EVENT STATISTICS ========================

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getEventStatistics(
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) String periodeDebut,
            @RequestParam(required = false) String periodeFin) {
        return ResponseEntity.ok(eventService.getEventStatistics(familleId, periodeDebut, periodeFin));
    }

    // ======================== CONSOLIDATED VIEW (Phase 6) ========================

    /**
     * Get all upcoming events consolidated — for Pasteur to see events from all departments/families.
     */
    @GetMapping("/consolidated")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> getConsolidatedUpcoming(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(eventService.getConsolidatedUpcoming(days));
    }

    /**
     * Get upcoming events grouped by family and type — consolidated.
     */
    @GetMapping("/consolidated/by-family")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Map<String, Object>> getConsolidatedByFamily(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(eventService.getConsolidatedByFamily(days));
    }
}
