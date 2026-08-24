package com.discipolat.modules.spiritualChallenges.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.spiritualChallenges.domain.SpiritualChallenge;
import com.discipolat.modules.spiritualChallenges.domain.SpiritualChallengeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spiritual-challenges")
public class SpiritualChallengeController {

    private final SpiritualChallengeService service;

    public SpiritualChallengeController(SpiritualChallengeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SpiritualChallenge>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SpiritualChallenge> challenges = service.list(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(challenges.getContent(), page, size,
                challenges.getTotalElements(), challenges.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpiritualChallenge> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpiritualChallenge> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String deadlineStr = (String) body.get("deadline");
        LocalDateTime deadline = deadlineStr != null ? LocalDateTime.parse(deadlineStr) : null;
        UUID assignéÀ = body.get("assignéÀ") != null ? UUID.fromString((String) body.get("assignéÀ")) : null;
        SpiritualChallenge challenge = service.create(
                (String) body.get("titre"),
                (String) body.get("description"),
                (String) body.get("type"),
                assignéÀ,
                body.get("objectifJours") != null ? (int) body.get("objectifJours") : 7,
                deadline,
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(challenge);
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpiritualChallenge> progress(@PathVariable UUID id) {
        return ResponseEntity.ok(service.progress(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpiritualChallenge> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatut(id, body.get("statut")));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.getStats());
    }
}
