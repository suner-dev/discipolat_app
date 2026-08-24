package com.discipolat.modules.announcements.api;

import com.discipolat.modules.announcements.domain.AnnouncementService;
import com.discipolat.modules.announcements.domain.ScheduledAnnouncement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(announcementService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        return ResponseEntity.ok(announcementService.get(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ScheduledAnnouncement announcement) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(announcement));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ScheduledAnnouncement announcement) {
        return ResponseEntity.ok(announcementService.update(id, announcement));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<?> schedule(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        LocalDateTime scheduledAt = LocalDateTime.parse(body.get("scheduledAt"));
        return ResponseEntity.ok(announcementService.schedule(id, scheduledAt));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(announcementService.publishNow(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID id) {
        announcementService.cancel(id);
        return ResponseEntity.ok(Map.of("message", "Annonce annulée"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        announcementService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Annonce supprimée"));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(announcementService.getStats());
    }
}
