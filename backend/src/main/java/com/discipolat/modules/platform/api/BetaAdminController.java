package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.BetaResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Administration de l'environnement de bêta-test.
 *
 * GET  /api/v1/admin/beta/status → état du bêta-testing (ADMIN)
 * POST /api/v1/admin/beta/reset   → restauration des données de démo (ADMIN,
 *                                    uniquement si activé sur cet environnement)
 */
@RestController
@RequestMapping("/api/v1/admin/beta")
public class BetaAdminController {

    private final BetaResetService betaResetService;

    public BetaAdminController(BetaResetService betaResetService) {
        this.betaResetService = betaResetService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(betaResetService.status());
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> reset() {
        return ResponseEntity.ok(betaResetService.reset());
    }
}
