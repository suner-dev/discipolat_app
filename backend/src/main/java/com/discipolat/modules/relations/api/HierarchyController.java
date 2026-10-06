package com.discipolat.modules.relations.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.relations.domain.UserHierarchyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Hiérarchie complète d'un membre — V231 : rôles (legacy + capacités×nœuds),
 * branches de l'arbre organisationnel avec la chaîne des responsables jusqu'à
 * la racine, relations personnelles déclarées, encadrement pastoral, chaîne
 * ascendante unifiée.
 */
@RestController
@RequestMapping("/api/v1/hierarchy")
public class HierarchyController {

    private final UserHierarchyService service;

    public HierarchyController(UserHierarchyService service) {
        this.service = service;
    }

    /** Ma hiérarchie (rôles, branches, encadrés déclarés et org). */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> myHierarchy() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.getHierarchy(tenantId, userId, userId));
    }

    /** Hiérarchie d'un membre — même garde que la fiche /users/{id}/detail. */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> hierarchyOf(@PathVariable UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getHierarchy(tenantId, userId, SecurityUtils.getCurrentUserId()));
    }
}
