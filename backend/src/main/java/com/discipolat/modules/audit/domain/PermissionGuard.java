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

    /**
     * Vérifie un niveau de permission granulaire (READ, WRITE, DELETE, MANAGE).
     * Utilisation : @perm.hasLevel('SOULS', 'WRITE')
     */
    public boolean hasLevel(String domain, String action, String level) {
        List<String> roles = securityUtils.getAllUserRoles();
        if (roles.isEmpty()) {
            String single = securityUtils.getCurrentUserRole();
            if (single != null) roles = List.of(single);
        }
        String permission = domain.toUpperCase() + "_" + action.toUpperCase();
        for (String role : roles) {
            if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("PASTEUR")) return true;
            if (permissionService.hasPermissionLevel(role, permission, level)) return true;
        }
        return roles.isEmpty(); // permissif si aucun rôle
    }

    /**
     * Vérifie que l'utilisateur a accès en écriture (write ou delete).
     */
    public boolean canWrite(String domain, String action) {
        return hasLevel(domain, action, "WRITE");
    }

    /**
     * Vérifie que l'utilisateur a accès en suppression.
     */
    public boolean canDelete(String domain, String action) {
        return hasLevel(domain, action, "DELETE");
    }

    /**
     * Retourne le scope configuré pour un rôle et une permission donnés.
     */
    public String getScope(String domain, String action) {
        List<String> roles = securityUtils.getAllUserRoles();
        if (roles.isEmpty()) return "GLOBAL";
        String permission = domain.toUpperCase() + "_" + action.toUpperCase();
        // Retourne le scope du premier rôle non-global trouvé
        for (String role : roles) {
            String scope = permissionService.getPermissionScope(role, permission);
            if (scope != null && !"GLOBAL".equals(scope)) return scope;
        }
        return "GLOBAL";
    }
}