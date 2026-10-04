package com.discipolat.modules.announcements.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.announcements.domain.PublicAnnouncementService;
import com.discipolat.modules.announcements.domain.PublicAnnouncement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS — annonces d'une église (admin tenant).
 */
@RestController
@RequestMapping("/api/v1/tenant/announcements")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
public class TenantAnnouncementController {

    private final PublicAnnouncementService announcementService;

    public TenantAnnouncementController(PublicAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    public record AnnouncementBody(String title, String description, String imageUrl,
                                   String city, String country, Instant eventAt,
                                   String linkUrl, String accessRef, Instant expiresAt) {
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(announcementService.listForTenant(tenantId).stream()
                .map(this::toView).toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody AnnouncementBody body) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        PublicAnnouncement created = announcementService.create(tenantId, actor, toInput(body));
        return ResponseEntity.status(201).body(toView(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody AnnouncementBody body) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(toView(announcementService.update(id, tenantId, toInput(body))));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submit(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(toView(announcementService.submit(id, tenantId)));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Map<String, Object>> unpublish(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(toView(announcementService.unpublish(id, tenantId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        announcementService.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    private PublicAnnouncementService.AnnouncementInput toInput(AnnouncementBody body) {
        return new PublicAnnouncementService.AnnouncementInput(body.title(), body.description(),
                body.imageUrl(), body.city(), body.country(), body.eventAt(),
                body.linkUrl(), body.accessRef(), body.expiresAt());
    }

    private Map<String, Object> toView(PublicAnnouncement a) {
        Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("id", a.getId());
        view.put("title", a.getTitle());
        view.put("description", a.getDescription());
        view.put("imageUrl", a.getImageUrl());
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
