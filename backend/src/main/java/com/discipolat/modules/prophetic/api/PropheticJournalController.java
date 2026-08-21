package com.discipolat.modules.prophetic.api;

import com.discipolat.modules.prophetic.domain.PropheticEntry;
import com.discipolat.modules.prophetic.domain.PropheticJournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prophetic")
public class PropheticJournalController {

    private final PropheticJournalService service;

    public PropheticJournalController(PropheticJournalService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PropheticEntry> create(@RequestBody PropheticEntry entry) {
        return ResponseEntity.ok(service.create(entry));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PropheticEntry> update(@PathVariable UUID id, @RequestBody PropheticEntry entry) {
        return ResponseEntity.ok(service.update(id, entry));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropheticEntry> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<PropheticEntry>> myEntries() {
        UUID userId = service.getCurrentUserId();
        return ResponseEntity.ok(service.findByAuthor(userId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<PropheticEntry>> publicEntries() {
        return ResponseEntity.ok(service.findPublic());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<PropheticEntry>> findByType(@PathVariable PropheticEntry.EntryType type) {
        return ResponseEntity.ok(service.findByType(type));
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<PropheticEntry>> findByTag(@PathVariable String tag) {
        return ResponseEntity.ok(service.findByTag(tag));
    }

    @GetMapping("/{id}/correlated")
    public ResponseEntity<List<PropheticEntry>> correlated(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findCorrelated(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats());
    }
}
