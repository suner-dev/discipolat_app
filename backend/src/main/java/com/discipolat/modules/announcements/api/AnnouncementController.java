package com.discipolat.modules.announcements.api;

import com.discipolat.modules.announcements.domain.AnnouncementService;
import com.discipolat.modules.announcements.domain.ScheduledAnnouncement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/announcements", "/api/v1/announcements"})
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

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ScheduledAnnouncement announcement) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(announcement));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ScheduledAnnouncement announcement) {
        return ResponseEntity.ok(announcementService.update(id, announcement));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping("/{id}/schedule")
    public ResponseEntity<?> schedule(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        LocalDateTime scheduledAt = LocalDateTime.parse(body.get("scheduledAt"));
        return ResponseEntity.ok(announcementService.schedule(id, scheduledAt));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(announcementService.publishNow(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID id) {
        announcementService.cancel(id);
        return ResponseEntity.ok(Map.of("message", "Annonce annulée"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
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
