package com.discipolat.modules.demo.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.modules.demo.domain.DemoRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API des demandes de démonstration (landing page).
 *
 * POST /api/v1/public/demo-requests  → soumettre une demande (public, rate-limité)
 * GET  /api/v1/admin/demo-requests   → lister les demandes (ADMIN, PASTEUR)
 */
@RestController
public class DemoRequestController {

    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final DemoRequestService demoRequestService;
    private final PerIpRateLimiter rateLimiter;

    public DemoRequestController(DemoRequestService demoRequestService, PerIpRateLimiter rateLimiter) {
        this.demoRequestService = demoRequestService;
        this.rateLimiter = rateLimiter;
    }

    /** Soumission publique depuis la landing page (rate-limitée par IP : anti-spam). */
    @PostMapping("/api/v1/public/demo-requests")
    public ResponseEntity<DemoRequestResponse> create(
            @Valid @RequestBody CreateDemoRequestRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = PerIpRateLimiter.extractClientIp(httpRequest);
        RateLimitResult rl = rateLimiter.tryConsumeDemoRequest(clientIp);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .header(HEADER_RETRY_AFTER, String.valueOf(rl.retryAfterSeconds()))
                    .build();
        }

        DemoRequestResponse response = demoRequestService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HEADER_RATE_LIMIT_REMAINING, String.valueOf(rl.remainingTokens()))
                .body(response);
    }

    /** Consultation des demandes — réservée à l'administration. */
    @GetMapping("/api/v1/admin/demo-requests")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<List<DemoRequestResponse>> list() {
        return ResponseEntity.ok(demoRequestService.listAll());
    }
}