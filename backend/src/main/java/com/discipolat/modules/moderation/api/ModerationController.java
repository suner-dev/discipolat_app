package com.discipolat.modules.moderation.api;

import com.discipolat.modules.moderation.domain.ContentModerationService;
import com.discipolat.modules.moderation.domain.ModerationItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
public class ModerationController {

    private final ContentModerationService moderationService;

    public ModerationController(ContentModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody ModerationItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moderationService.submitForModeration(item));
    }

    @GetMapping("/pending")
    public ResponseEntity<?> listPending() {
        return ResponseEntity.ok(moderationService.listPending());
    }

    @GetMapping
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(moderationService.listAll());
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        ModerationItem.Status decision = ModerationItem.Status.valueOf((String) body.get("decision"));
        String notes = (String) body.getOrDefault("notes", "");
        UUID reviewerId = body.containsKey("reviewerId") ? UUID.fromString((String) body.get("reviewerId")) : null;
        return ResponseEntity.ok(moderationService.reviewItem(id, decision, notes, reviewerId));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(moderationService.getStats());
    }
}
