package com.discipolat.modules.usageAnalytics.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.usageAnalytics.domain.UsageAnalyticsService;
import com.discipolat.modules.usageAnalytics.domain.UsageEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P3 #109 — Analytics d'usage self-hosted.
 * Track ouvert à tous les utilisateurs authentifiés ; summary réservé admin/pasteur.
 */
@RestController
@RequestMapping("/api/v1/usage-analytics")
public class UsageAnalyticsController {

    private final UsageAnalyticsService service;

    public UsageAnalyticsController(UsageAnalyticsService service) {
        this.service = service;
    }

    @PostMapping("/track")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> track(@RequestBody List<UsageEvent> events) {
        UUID userId = SecurityUtils.getCurrentUserId();
        service.track(TenantContext.getCurrentTenantId(), userId, events);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<?> summary(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(service.summary(days));
    }
}
