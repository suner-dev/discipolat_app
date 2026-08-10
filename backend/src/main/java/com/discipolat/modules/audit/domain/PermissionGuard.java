package com.discipolat.modules.audit.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bean de vérification des permissions utilisable dans les expressions
 * {@code @PreAuthorize("@perm.has('USER','DELETE')")}.
 *
 * Logique :
 * - Les super-utilisateurs (ADMIN, PASTEUR) passent toujours.
 * - Permission explicitement désactivée dans la matrice → refus.
 * - Aucune ligne dans la matrice → accord (permissif, rétrocompatible).
 */
@Component("perm")
public class PermissionGuard {

    private final PermissionService permissionService;
    private final SecurityUtils securityUtils;

    public PermissionGuard(PermissionService permissionService, SecurityUtils securityUtils) {
        this.permissionService = permissionService;
        this.securityUtils = securityUtils;
    }

    /**
     * Vérifie la permission "domain_action" pour l'utilisateur actuel,
     * en considérant tous ses rôles (getAllUserRoles).
     */
    public boolean has(String domain, String action) {
        List<String> roles = securityUtils.getAllUserRoles();
        if (roles.isEmpty()) {
            String single = securityUtils.getCurrentUserRole();
            if (single != null) roles = List.of(single);
        }
        String permission = domain.toUpperCase() + "_" + action.toUpperCase();
        return permissionService.userHasPermission(roles, permission);
    }
}