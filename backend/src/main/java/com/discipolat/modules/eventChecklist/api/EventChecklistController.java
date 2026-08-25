package com.discipolat.modules.eventChecklist.api;

import com.discipolat.modules.eventChecklist.domain.EventChecklistItem;
import com.discipolat.modules.eventChecklist.domain.EventChecklistService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping({"/api/event-checklists", "/api/v1/event-checklists"})
@PreAuthorize("isAuthenticated()")
public class EventChecklistController {

    private final EventChecklistService checklistService;

    public EventChecklistController(EventChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(checklistService.getByEvent(eventId));
    }

    @PostMapping("/event/{eventId}/generate")
    public ResponseEntity<?> generate(@PathVariable UUID eventId, @RequestBody Map<String, String> body) {
        String eventType = body.getOrDefault("eventType", "DEFAULT");
        LocalDateTime eventDate = body.containsKey("eventDate") ? LocalDateTime.parse(body.get("eventDate")) : LocalDateTime.now().plusDays(7);
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.generateForEvent(eventId, eventType, eventDate));
    }

        @GetMapping
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(checklistService.listAll());
    }

    @PostMapping
    public ResponseEntity<?> addItem(@RequestBody EventChecklistItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checklistService.addItem(item));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(checklistService.toggleItem(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        checklistService.deleteItem(id);
        return ResponseEntity.ok(Map.of("message", "Élément supprimé"));
    }

    @GetMapping("/event/{eventId}/progress")
    public ResponseEntity<?> progress(@PathVariable UUID eventId) {
        return ResponseEntity.ok(checklistService.getEventProgress(eventId));
    }
}
