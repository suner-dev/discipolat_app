package com.discipolat.modules.calendar.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.calendar.domain.CalendarEvent;
import com.discipolat.modules.calendar.domain.CalendarService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<PageResponse<CalendarEvent>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<CalendarEvent> result = service.list(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/range")
    public ResponseEntity<List<CalendarEvent>> range(
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(service.getBetween(LocalDateTime.parse(start), LocalDateTime.parse(end)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CalendarEvent> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CalendarEvent> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CalendarEvent event = service.create(
                (String) body.get("titre"),
                (String) body.getOrDefault("description", ""),
                LocalDateTime.parse((String) body.get("dateDebut")),
                LocalDateTime.parse((String) body.get("dateFin")),
                (String) body.get("lieu"),
                (String) body.get("categorie"),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CalendarEvent> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.update(id,
                (String) body.get("titre"),
                (String) body.get("description"),
                body.get("dateDebut") != null ? LocalDateTime.parse((String) body.get("dateDebut")) : null,
                body.get("dateFin") != null ? LocalDateTime.parse((String) body.get("dateFin")) : null,
                (String) body.get("lieu")
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
