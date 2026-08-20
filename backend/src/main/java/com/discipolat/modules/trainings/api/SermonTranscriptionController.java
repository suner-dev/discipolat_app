package com.discipolat.modules.trainings.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.trainings.domain.SermonTranscription;
import com.discipolat.modules.trainings.domain.SermonTranscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sermons")
@PreAuthorize("hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE')")
@RequiredArgsConstructor
public class SermonTranscriptionController {

    private final SermonTranscriptionRepository repository;

    @GetMapping
    public ResponseEntity<Page<SermonTranscription>> list(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<SermonTranscription> page = (q != null && !q.isBlank())
                ? repository.search(tenantId, q, pageable)
                : repository.findByTenantIdOrderByRecordedAtDesc(tenantId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SermonTranscription> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SermonTranscription> create(@RequestBody SermonTranscription sermon) {
        sermon.setId(null);
        sermon.setTenantId(TenantContext.getTenantId());
        sermon.setTranscriptionStatus("PENDING");
        sermon.setCreatedAt(LocalDateTime.now());
        sermon.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(repository.save(sermon));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SermonTranscription> update(
            @PathVariable UUID id,
            @RequestBody SermonTranscription update) {
        return repository.findById(id).map(existing -> {
            existing.setTitle(update.getTitle());
            existing.setSpeaker(update.getSpeaker());
            existing.setTheme(update.getTheme());
            existing.setReferenceBiblique(update.getReferenceBiblique());
            existing.setFullText(update.getFullText());
            existing.setSummary(update.getSummary());
            existing.setKeyVerses(update.getKeyVerses());
            existing.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(repository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/transcribe")
    public ResponseEntity<Map<String, String>> triggerTranscription(@PathVariable UUID id) {
        // In production: enqueue audio file to Whisper API or similar
        return repository.findById(id).map(sermon -> {
            sermon.setTranscriptionStatus("PROCESSING");
            repository.save(sermon);
            return ResponseEntity.ok(Map.of(
                "status", "PROCESSING",
                "message", "Transcription en cours pour: " + sermon.getTitle()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        UUID tenantId = TenantContext.getTenantId();
        long total = repository.countByTenantId(tenantId);
        return ResponseEntity.ok(Map.of("totalSermons", total));
    }
}
