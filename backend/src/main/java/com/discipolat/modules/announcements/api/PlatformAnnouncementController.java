package com.discipolat.modules.announcements.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.announcements.domain.PublicAnnouncementService;
import com.discipolat.modules.announcements.domain.PublicAnnouncement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS — file de modération des annonces publiques,
 * réservée à la console Super Admin (D4).
 */
@RestController
@RequestMapping("/api/v1/platform/announcements")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class PlatformAnnouncementController {

    private final PublicAnnouncementService announcementService;

    public PlatformAnnouncementController(PublicAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    public record RejectRequest(String note) {
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> queue(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(announcementService.moderationQueue(status).stream()
                .map(this::toView).toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable UUID id) {
        UUID moderator = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(toView(announcementService.approve(id, moderator)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable UUID id,
                                                      @RequestBody(required = false) RejectRequest request) {
        UUID moderator = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(toView(announcementService.reject(
                id, moderator, request == null ? null : request.note())));
    }

    private Map<String, Object> toView(PublicAnnouncement a) {
        Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("id", a.getId());
        view.put("tenantId", a.getTenantId());
        view.put("title", a.getTitle());
        view.put("description", a.getDescription());
        view.put("city", a.getCity());
        view.put("country", a.getCountry());
        view.put("eventAt", a.getEventAt());
        view.put("linkUrl", a.getLinkUrl());
        view.put("accessRef", a.getAccessRef());
        view.put("status", a.getStatus().name());
        view.put("moderationNote", a.getModerationNote());
        view.put("publishedAt", a.getPublishedAt());
        view.put("expiresAt", a.getExpiresAt());
        view.put("createdAt", a.getCreatedAt());
        return view;
    }
}
