package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.platform.domain.MenuEntry;
import com.discipolat.modules.platform.domain.PlatformConfigService;
import com.discipolat.modules.platform.domain.PlatformModule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API de configuration de la plateforme.
 *
 * GET    /api/v1/platform/menus            → menus visibles pour l'utilisateur connecté
 * GET    /api/v1/platform/modules          → état de tous les modules (utilisateur authentifié)
 * PUT    /api/v1/platform/modules/{key}    → activer/désactiver (ADMIN)
 * POST   /api/v1/platform/modules          → créer un module (ADMIN)
 * PUT    /api/v1/platform/modules/{key}    → modifier un module (ADMIN)
 * DELETE /api/v1/platform/modules/{key}    → supprimer un module (ADMIN)
 * GET    /api/v1/platform/admin/menus      → tous les menus (ADMIN)
 * POST   /api/v1/platform/menus            → créer un menu (ADMIN)
 * PUT    /api/v1/platform/menus/{id}       → modifier un menu (ADMIN)
 * DELETE /api/v1/platform/menus/{id}       → supprimer un menu (ADMIN)
 * POST   /api/v1/platform/menus/reorder    → réordonner (ADMIN)
 */
@RestController
@RequestMapping("/api/v1/platform")
public class PlatformConfigController {

    private final PlatformConfigService platformService;
    private final SecurityUtils securityUtils;

    public PlatformConfigController(PlatformConfigService platformService, SecurityUtils securityUtils) {
        this.platformService = platformService;
        this.securityUtils = securityUtils;
    }

    /* ------------------------------ Menus ------------------------------ */

    @GetMapping("/menus")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuEntry>> myMenus() {
        List<String> roles = securityUtils.getAllUserRoles();
        if (roles.isEmpty()) {
            String activeRole = securityUtils.getCurrentUserRole();
            if (activeRole != null) roles = List.of(activeRole);
        }
        return ResponseEntity.ok(platformService.menusForRoles(roles));
    }

    @GetMapping("/gating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuGateInfo>> gating() {
        List<String> roles = securityUtils.getAllUserRoles();
        if (roles.isEmpty()) {
            String activeRole = securityUtils.getCurrentUserRole();
            if (activeRole != null) roles = List.of(activeRole);
        }
        return ResponseEntity.ok(platformService.gateInfo(roles));
    }

    @GetMapping("/admin/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MenuEntry>> allMenus() {
        return ResponseEntity.ok(platformService.listAllMenus());
    }

    @PostMapping("/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuEntry> createMenu(@RequestBody MenuEntry menu) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createMenu(menu));
    }

    @PutMapping("/menus/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuEntry> updateMenu(@PathVariable UUID id, @RequestBody MenuEntry menu) {
        return ResponseEntity.ok(platformService.updateMenu(id, menu));
    }

    @DeleteMapping("/menus/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMenu(@PathVariable UUID id) {
        platformService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/menus/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MenuEntry>> reorderMenus(@RequestBody List<MenuOrderItem> items) {
        return ResponseEntity.ok(platformService.reorderMenus(items));
    }

    /* ----------------------------- Modules ----------------------------- */

    @GetMapping("/modules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PlatformModule>> modules() {
        return ResponseEntity.ok(platformService.listModules());
    }

    @PutMapping("/modules/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformModule> toggleModule(@PathVariable String key,
                                                       @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        return ResponseEntity.ok(platformService.toggleModule(key, enabled));
    }

    @PostMapping("/modules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformModule> createModule(@RequestBody PlatformModule module) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createModule(module));
    }

    @PutMapping("/modules/{key}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformModule> updateModule(@PathVariable String key,
                                                       @RequestBody PlatformModule module) {
        return ResponseEntity.ok(platformService.updateModule(key, module));
    }

    @DeleteMapping("/modules/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModule(@PathVariable String key) {
        platformService.deleteModule(key);
        return ResponseEntity.noContent().build();
    }
}
