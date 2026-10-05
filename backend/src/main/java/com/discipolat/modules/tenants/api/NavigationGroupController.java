package com.discipolat.modules.tenants.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.NavigationGroup;
import com.discipolat.modules.tenants.domain.NavigationGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LOT 2 §GR — API des <b>groupes d'onglets</b> de la navigation.
 *
 * <p>Objectif produit : remplacer la longue liste plate d'onglets par de grands
 * groupes ; au clic sur un groupe, la liste de ses sous-onglets se déploie.
 *
 * <p><b>Lecture ouverte</b> ({@code isAuthenticated}) : n'importe quel membre
 * doit pouvoir récupérer la forme de son menu. La <b>lecture ne fuit rien</b> :
 * elle est filtrée sur les rôles de l'utilisateur et sur l'église courante
 * (les groupes d'une autre église ne sont jamais renvoyés).
 *
 * <p><b>Écriture</b> réservée à l'admin de l'église
 * ({@code @authz.isTenantAdmin()}, qui lit {@code tenant_memberships} — jamais
 * le claim du JWT, cf. F10). Les groupes <b>globaux</b> restent en lecture seule
 * ici : ils se modifient depuis la console plateforme.
 */
@RestController
@RequestMapping("/api/v1/tenant/navigation")
public class NavigationGroupController {

    private final NavigationGroupService navigationGroupService;
    private final SecurityUtils securityUtils;

    public NavigationGroupController(NavigationGroupService navigationGroupService, SecurityUtils securityUtils) {
        this.navigationGroupService = navigationGroupService;
        this.securityUtils = securityUtils;
    }

    public record GroupRequest(String key, String label, String description, String icon,
                               UUID parentGroupId, Integer displayOrder, List<String> roles,
                               String moduleKey, Boolean enabled, Boolean collapsedByDefault,
                               Boolean showCount) {
    }

    public record ItemsRequest(List<NavigationGroupService.ItemRef> items) {
    }

    /**
     * Forme du menu de l'utilisateur : groupes effectifs + affectations.
     * Alimente {@code Sidebar} (web) et {@code navigation_screen} (mobile).
     */
    @GetMapping("/groups")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> myNavigation() {
        return ResponseEntity.ok(navigationGroupService.navigationFor(TenantContext.getCurrentTenantId(), currentRoles()));
    }

    /** Groupes bruts + affectations, sans filtrage de rôle (écran d'admin). */
    @GetMapping("/groups/admin")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> allGroups() {
        return ResponseEntity.ok(navigationGroupService.navigationFor(TenantContext.getCurrentTenantId(), List.of()));
    }

    @PostMapping("/groups")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> create(@RequestBody GroupRequest req) {
        NavigationGroup group = navigationGroupService.create(TenantContext.getCurrentTenantId(),
                req.key(), req.label(), req.description(), req.icon(), req.parentGroupId(),
                req.displayOrder(), req.roles(), req.moduleKey(), req.enabled(),
                req.collapsedByDefault(), req.showCount());
        return ResponseEntity.status(HttpStatus.CREATED).body(navigationGroupService.toMap(group));
    }

    @PatchMapping("/groups/{id}")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id, @RequestBody GroupRequest req) {
        NavigationGroup group = navigationGroupService.update(TenantContext.getCurrentTenantId(), id,
                req.label(), req.description(), req.icon(), req.parentGroupId(), req.displayOrder(),
                req.roles(), req.moduleKey(), req.enabled(), req.collapsedByDefault(), req.showCount());
        return ResponseEntity.ok(navigationGroupService.toMap(group));
    }

    /** Remplace les affectations d'un groupe (écriture idempotente). */
    @PutMapping("/groups/{id}/items")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<List<Map<String, Object>>> setItems(@PathVariable UUID id,
                                                               @RequestBody ItemsRequest req) {
        List<Map<String, Object>> body = navigationGroupService
                .setItems(TenantContext.getCurrentTenantId(), id, req.items()).stream()
                .map(navigationGroupService::toItemMap)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/groups/{id}/items")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<List<Map<String, Object>>> items(@PathVariable UUID id) {
        return ResponseEntity.ok(navigationGroupService.itemsOf(TenantContext.getCurrentTenantId(), id).stream()
                .map(navigationGroupService::toItemMap)
                .toList());
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        navigationGroupService.delete(TenantContext.getCurrentTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    private List<String> currentRoles() {
        List<String> roles = securityUtils.getAllUserRoles();
        if (roles.isEmpty()) {
            String activeRole = securityUtils.getCurrentUserRole();
            if (activeRole != null) {
                roles = List.of(activeRole);
            }
        }
        return roles;
    }
}