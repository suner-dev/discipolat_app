package com.discipolat.modules.followUpRequests.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.followUpRequests.domain.FollowUpRequest;
import com.discipolat.modules.followUpRequests.domain.FollowUpRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P3 #112 — Demandes de suivi.
 */
@RestController
@RequestMapping("/api/v1/follow-up-requests")
public class FollowUpRequestController {

    private final FollowUpRequestService service;

    public FollowUpRequestController(FollowUpRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FollowUpRequest> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String name = body.get("requesterName") == null ? "" : body.get("requesterName").toString();
        FollowUpRequest r = service.create(userId, name,
                body.get("type") == null ? null : body.get("type").toString(),
                body.get("message") == null ? "" : body.get("message").toString(),
                body.get("preferredFamilyId") == null ? null : UUID.fromString(body.get("preferredFamilyId").toString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FollowUpRequest>> mine() {
        return ResponseEntity.ok(service.myRequests(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/assigned-to-me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FollowUpRequest>> assignedToMe() {
        return ResponseEntity.ok(service.assignedToMe(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')")
    public ResponseEntity<List<FollowUpRequest>> pending() {
        return ResponseEntity.ok(service.pending());
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')")
    public ResponseEntity<FollowUpRequest> assign(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        UUID assignee = body.get("assignedToId") != null
                ? UUID.fromString(body.get("assignedToId").toString())
                : SecurityUtils.getCurrentUserId();
        String assigneeName = body.get("assignedToName") == null ? "" : body.get("assignedToName").toString();
        return ResponseEntity.ok(service.assign(id, assignee, assigneeName));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')")
    public ResponseEntity<FollowUpRequest> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.updateStatus(id, body.get("status"), body.get("notes")));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.stats());
    }
}
