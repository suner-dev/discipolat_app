package com.discipolat.modules.calendar.api;

import com.discipolat.modules.calendar.domain.CalendarEvent;
import com.discipolat.modules.calendar.domain.CalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarService service;

    public CalendarController(CalendarService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CalendarEvent>> list(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        if (start != null && end != null) {
            return ResponseEntity.ok(service.listByRange(LocalDateTime.parse(start), LocalDateTime.parse(end)));
        }
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CalendarEvent> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CalendarEvent> create(@RequestBody Map<String, Object> body) {
        CalendarEvent event = service.create(
                (String) body.get("titre"),
                (String) body.get("description"),
                LocalDateTime.parse((String) body.get("début")),
                LocalDateTime.parse((String) body.get("fin")),
                (String) body.get("lieu"),
                (String) body.get("source"),
                body.get("événementId") != null ? UUID.fromString((String) body.get("événementId")) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CalendarEvent> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatut(id, body.get("statut")));
    }

    @GetMapping("/{id}/ical")
    public ResponseEntity<String> getICal(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header("Content-Disposition", "attachment; filename=event-" + id + ".ics")
                .body(service.generateICal(id));
    }

    /**
     * Flux iCal complet — URL de souscription pour Google Calendar / Outlook / Apple.
     * GET /api/v1/calendar/feed.ics
     */
    @GetMapping(value = "/feed.ics", produces = "text/calendar")
    public ResponseEntity<String> getICalFeed() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header("Content-Disposition", "inline; filename=discipolat-calendar.ics")
                .body(service.generateICalFeed());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
