package com.discipolat.modules.bibleReading.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.bibleReading.domain.BibleReadingEntry;
import com.discipolat.modules.bibleReading.domain.BibleReadingPlan;
import com.discipolat.modules.bibleReading.domain.BibleReadingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bible-reading")
@PreAuthorize("isAuthenticated()")
public class BibleReadingController {

    private final BibleReadingService service;

    public BibleReadingController(BibleReadingService service) { this.service = service; }

    // ─── Plans ───

    @GetMapping("/plans")
    public List<BibleReadingPlan> listPlans() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<BibleReadingPlan> userPlans = service.listPlansByUser(userId);
        List<BibleReadingPlan> sharedPlans = service.listSharedPlans();
        // Merge without duplicates
        for (BibleReadingPlan sp : sharedPlans) {
            if (userPlans.stream().noneMatch(p -> p.getId().equals(sp.getId()))) {
                userPlans.add(sp);
            }
        }
        return userPlans;
    }

    @GetMapping("/plans/{id}")
    public BibleReadingPlan getPlan(@PathVariable UUID id) {
        return service.getPlan(id);
    }

    @PostMapping("/plans")
    public ResponseEntity<BibleReadingPlan> createPlan(@RequestBody BibleReadingPlan plan) {
        plan.setCreateurId(SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPlan(plan));
    }

    @PutMapping("/plans/{id}")
    public BibleReadingPlan updatePlan(@PathVariable UUID id, @RequestBody BibleReadingPlan updates) {
        return service.updatePlan(id, updates);
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        service.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Entries ───

    @GetMapping("/plans/{planId}/entries")
    public List<BibleReadingEntry> listEntries(@PathVariable UUID planId) {
        return service.listEntries(planId, SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/today")
    public List<BibleReadingEntry> today() {
        return service.listToday(SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/entries/{id}")
    public BibleReadingEntry getEntry(@PathVariable UUID id) {
        return service.getEntry(id);
    }

    @PostMapping("/entries")
    public ResponseEntity<BibleReadingEntry> createEntry(@RequestBody BibleReadingEntry entry) {
        entry.setUtilisateurId(SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEntry(entry));
    }

    @PostMapping("/entries/{id}/mark-read")
    public BibleReadingEntry markRead(@PathVariable UUID id) {
        return service.markAsRead(id);
    }

    @PutMapping("/entries/{id}/note")
    public BibleReadingEntry addNote(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return service.addNote(id, body.get("note"));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable UUID id) {
        service.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Stats ───

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.getStats(SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/family-progress")
    public List<Map<String, Object>> familyProgress() {
        return service.getFamilyProgress(SecurityUtils.getCurrentUserId());
    }
}
