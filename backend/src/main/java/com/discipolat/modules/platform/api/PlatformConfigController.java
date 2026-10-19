package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.platform.domain.ConfigRevision;
import com.discipolat.modules.platform.domain.ConfigRevisionService;
import com.discipolat.modules.platform.domain.MenuEntry;
import com.discipolat.modules.platform.domain.PlatformConfigService;
import com.discipolat.modules.platform.domain.PlatformModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    private final ConfigRevisionService revisionService;

    public PlatformConfigController(PlatformConfigService platformService, SecurityUtils securityUtils,
                                    ConfigRevisionService revisionService) {
        this.platformService = platformService;
        this.securityUtils = securityUtils;
        this.revisionService = revisionService;
    }

    /* ------------------------------ Menus ------------------------------ */

    @GetMapping("/menus")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuEntry>> myMenus() {
        return ResponseEntity.ok(platformService.menusForRoles(menuRolesForActiveWorkspace()));
    }

    @GetMapping("/gating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuGateInfo>> gating() {
        return ResponseEntity.ok(platformService.gateInfo(menuRolesForActiveWorkspace()));
    }

    /**
     * Rôles qui déterminent l'espace de travail affiché.
     *
     * <p>Un compte peut détenir plusieurs rôles (ex. {@code RESPONSABLE +
     * FAISEUR}), mais l'UI navigue selon un unique rôle ACTIF. Parer le menu de
     * l'UNION des rôles détenus faisait apparaître au responsable les écrans
     * réservés au faiseur (CRM Faiseur, Rapport faiseur, Cercle des faiseurs…)
     * — exactement ce que la convention {@link SecurityUtils#hasActiveRole}
     * proscrit : « access controls must use [the active role], not the full set
     * of roles held ».
     *
     * <p>Les super-utilisateurs (ADMIN/PASTEUR) ne sont pas cloisonnés par rôle
     * : ils conservent la vue complète de l'église (union de leurs rôles). Si
     * aucun rôle actif n'est résoluble (jeton sans claim {@code activeRole}),
     * on retombe sur l'ancien comportement par sûreté — jamais un menu vide.
     */
    private List<String> menuRolesForActiveWorkspace() {
        String activeRole = securityUtils.getCurrentUserRole();
        if (activeRole == null || securityUtils.isSuperUser()) {
            List<String> all = securityUtils.getAllUserRoles();
            if (!all.isEmpty()) return all;
            return activeRole != null ? List.of(activeRole) : List.of();
        }
        return List.of(activeRole);
    }

    @GetMapping("/admin/menus")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<List<MenuEntry>> allMenus() {
        return ResponseEntity.ok(platformService.listAllMenus());
    }

    @PostMapping("/menus")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<MenuEntry> createMenu(@Valid @RequestBody MenuEntry menu) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createMenu(menu));
    }

    @PutMapping("/menus/{id}")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<MenuEntry> updateMenu(@PathVariable UUID id, @Valid @RequestBody MenuEntry menu) {
        return ResponseEntity.ok(platformService.updateMenu(id, menu));
    }

    @DeleteMapping("/menus/{id}")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Void> deleteMenu(@PathVariable UUID id) {
        platformService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/menus/reorder")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
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
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<PlatformModule> toggleModule(@PathVariable String key,
                                                       @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        return ResponseEntity.ok(platformService.toggleModule(key, enabled));
    }

    @PostMapping("/modules")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<PlatformModule> createModule(@Valid @RequestBody PlatformModule module) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createModule(module));
    }

    @PutMapping("/modules/{key}/edit")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<PlatformModule> updateModule(@PathVariable String key,
                                                       @Valid @RequestBody PlatformModule module) {
        return ResponseEntity.ok(platformService.updateModule(key, module));
    }

    @DeleteMapping("/modules/{key}")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Void> deleteModule(@PathVariable String key) {
        platformService.deleteModule(key);
        return ResponseEntity.noContent().build();
    }

    /* --------------------------- Versionnage --------------------------- */

    /**
     * Historique des révisions de configuration (modules/menus). Filtré
     * optionnellement par type d'entité, trié du plus récent au plus ancien.
     */
    @GetMapping("/revisions")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<PageResponse<ConfigRevision>> revisions(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ConfigRevision> result = revisionService.list(entityType, pageable);
        return ResponseEntity.ok(PageResponse.of(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }
}
