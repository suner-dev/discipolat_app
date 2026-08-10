package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.ChurchSettings;
import com.discipolat.modules.platform.domain.ChurchSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API des paramètres d'identité & de marque.
 * - GET  /api/v1/public/settings → identité publique (landing, thème) sans authentification
 * - GET  /api/v1/settings        → vue complète (utilisateurs authentifiés)
 * - PUT  /api/v1/settings        → mise à jour (ADMIN)
 * - POST /api/v1/settings/reset  → réinitialisation des valeurs par défaut (ADMIN)
 */
@RestController
public class SettingsController {

    private final ChurchSettingsService settingsService;

    public SettingsController(ChurchSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/api/v1/public/settings")
    public ResponseEntity<PublicBrandingResponse> publicBranding() {
        return ResponseEntity.ok(PublicBrandingResponse.from(settingsService.getSettings()));
    }

    @GetMapping("/api/v1/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChurchSettingsResponse> get() {
        return ResponseEntity.ok(ChurchSettingsResponse.from(settingsService.getSettings()));
    }

    @PutMapping("/api/v1/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChurchSettingsResponse> update(@Valid @RequestBody UpdateChurchSettingsRequest request) {
        ChurchSettings settings = settingsService.update(request);
        return ResponseEntity.ok(ChurchSettingsResponse.from(settings));
    }

    @PostMapping("/api/v1/settings/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChurchSettingsResponse> reset() {
        return ResponseEntity.ok(ChurchSettingsResponse.from(settingsService.resetToDefaults()));
    }
}
