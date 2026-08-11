package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.platform.domain.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API des retours testeurs (bêta-testing).
 *
 * POST  /api/v1/feedback                → soumettre un retour (authentifié)
 * GET   /api/v1/admin/feedback          → lister les retours (ADMIN, PASTEUR)
 * GET   /api/v1/admin/feedback/stats    → statistiques (ADMIN, PASTEUR)
 * PATCH /api/v1/admin/feedback/{id}/status → changer le statut (ADMIN)
 */
@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final SecurityUtils securityUtils;

    public FeedbackController(FeedbackService feedbackService, SecurityUtils securityUtils) {
        this.feedbackService = feedbackService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/api/v1/feedback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponse> create(@Valid @RequestBody CreateFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.create(securityUtils.getCurrentUserId(), request));
    }

    @GetMapping("/api/v1/admin/feedback")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<List<FeedbackResponse>> list() {
        return ResponseEntity.ok(feedbackService.listAll());
    }

    @GetMapping("/api/v1/admin/feedback/stats")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<FeedbackStatsResponse> stats() {
        return ResponseEntity.ok(feedbackService.stats());
    }

    @PatchMapping("/api/v1/admin/feedback/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeedbackResponse> updateStatus(@PathVariable UUID id,
                                                         @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "NOUVEAU");
        return ResponseEntity.ok(feedbackService.updateStatus(id, status));
    }
}
