package com.discipolat.modules.sabbathDashboard.api;

import com.discipolat.modules.sabbathDashboard.domain.SabbathDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * P3 #106 — Tableau de bord sabbatique.
 */
@RestController
@RequestMapping("/api/v1/sabbath-dashboard")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR','RESPONSABLE')")
public class SabbathDashboardController {

    private final SabbathDashboardService service;

    public SabbathDashboardController(SabbathDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(service.getDashboard());
    }
}
