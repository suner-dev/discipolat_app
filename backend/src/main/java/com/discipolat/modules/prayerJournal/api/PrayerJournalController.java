package com.discipolat.modules.prayerJournal.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.prayerJournal.domain.PrayerJournalEntry;
import com.discipolat.modules.prayerJournal.domain.PrayerJournalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prayer-journal")
public class PrayerJournalController {

    private final PrayerJournalService service;
    private final SecurityUtils securityUtils;

    public PrayerJournalController(PrayerJournalService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    private void verifyOwnership(PrayerJournalEntry entry) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (!entry.getMembreId().equals(currentUserId) && !securityUtils.isSuperUser()) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette entrée de journal de prière");
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<PrayerJournalEntry>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID membreId) {
        UUID userId = membreId != null ? membreId : SecurityUtils.getCurrentUserId();
        Page<PrayerJournalEntry> entries = service.listByMember(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(entries.getContent(), page, size,
                entries.getTotalElements(), entries.getTotalPages()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrayerJournalEntry> get(@PathVariable UUID id) {
        PrayerJournalEntry entry = service.getById(id);
        verifyOwnership(entry);
        return ResponseEntity.ok(entry);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrayerJournalEntry> create(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PrayerJournalEntry entry = service.create(userId, body.get("contenu"),
                body.get("category"), body.get("visibilité"));
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @PatchMapping("/{id}/answered")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrayerJournalEntry> markAnswered(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        PrayerJournalEntry entry = service.getById(id);
        verifyOwnership(entry);
        return ResponseEntity.ok(service.markAnswered(id, body.get("réponse")));
    }

    @PatchMapping("/{id}/remembered")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrayerJournalEntry> markRemembered(@PathVariable UUID id) {
        PrayerJournalEntry entry = service.getById(id);
        verifyOwnership(entry);
        return ResponseEntity.ok(service.markRemembered(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        PrayerJournalEntry entry = service.getById(id);
        verifyOwnership(entry);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats(SecurityUtils.getCurrentUserId()));
    }
}
