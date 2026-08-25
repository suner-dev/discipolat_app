package com.discipolat.modules.spiritualJournal.api;

import com.discipolat.modules.spiritualJournal.domain.SpiritualJournal;
import com.discipolat.modules.spiritualJournal.domain.SpiritualJournalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spiritual-journals")
@PreAuthorize("isAuthenticated()")
public class SpiritualJournalController {

    private final SpiritualJournalService service;
    public SpiritualJournalController(SpiritualJournalService service) { this.service = service; }

    @GetMapping("/by-author/{authorId}")
    public List<SpiritualJournal> listByAuthor(@PathVariable UUID authorId) {
        return service.listByAuthor(authorId);
    }

    @GetMapping("/by-type/{authorId}/{type}")
    public List<SpiritualJournal> listByType(@PathVariable UUID authorId,
            @PathVariable SpiritualJournal.TypeEntree type) {
        return service.listByType(authorId, type);
    }

    @GetMapping("/favorites/{authorId}")
    public List<SpiritualJournal> listFavorites(@PathVariable UUID authorId) {
        return service.listFavorites(authorId);
    }

    @GetMapping("/{id}")
    public SpiritualJournal get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<SpiritualJournal> create(@RequestBody SpiritualJournal entry) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(entry));
    }

    @PutMapping("/{id}")
    public SpiritualJournal update(@PathVariable UUID id, @RequestBody SpiritualJournal updates) {
        return service.update(id, updates);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle-favorite")
    public SpiritualJournal toggleFavorite(@PathVariable UUID id) {
        return service.toggleFavorite(id);
    }

    @GetMapping("/stats/{authorId}")
    public Map<String, Object> stats(@PathVariable UUID authorId) {
        return service.getStats(authorId);
    }
}
