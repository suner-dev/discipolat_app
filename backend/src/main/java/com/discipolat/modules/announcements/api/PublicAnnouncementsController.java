package com.discipolat.modules.announcements.api;

import com.discipolat.modules.announcements.domain.PublicAnnouncementService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * SPEC_ONBOARDING_FLOWS — carrousel du landing page : annonces PUBLISHED,
 * non expirées, vitrine uniquement (aucun identifiant interne, aucune PII).
 * GET sous /api/v1/public/** → permitAll (cf. SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/public/announcements")
public class PublicAnnouncementsController {

    private final PublicAnnouncementService announcementService;

    public PublicAnnouncementsController(PublicAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<Map<String, Object>> items = announcementService.publicFeed();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("total", items.size(), "content", items));
    }
}
