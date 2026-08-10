package com.discipolat.modules.audit.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class PermissionService {

    private final JdbcTemplate jdbcTemplate;

    public PermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ======================== Matrice des permissions ======================== */

    public List<Map<String, Object>> getAllPermissions() {
        return jdbcTemplate.queryForList(
                "SELECT role, permission, enabled FROM role_permissions ORDER BY role, permission");
    }

    public List<Map<String, Object>> getPermissionsByRole(String role) {
        return jdbcTemplate.queryForList(
                "SELECT role, permission, enabled FROM role_permissions WHERE role = ? ORDER BY permission",
                role.toUpperCase());
    }

    public Map<String, Object> updatePermission(String role, String permission, boolean enabled) {
        jdbcTemplate.update(
                "UPDATE role_permissions SET enabled = ?, updated_at = ? WHERE role = ? AND permission = ?",
                enabled, LocalDateTime.now(), role.toUpperCase(), permission.toUpperCase());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", role.toUpperCase());
        result.put("permission", permission.toUpperCase());
        result.put("enabled", enabled);
        return result;
    }

    /**
     * Vérifie si un rôle donné a une permission activée explicitement.
     * Permissif par défaut : si aucune ligne n'existe, retourne true.
     */
    public boolean hasPermission(String role, String permission) {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT enabled FROM role_permissions WHERE role = ? AND permission = ?",
                role.toUpperCase(), permission.toUpperCase());
        if (results.isEmpty()) return true; // pas de ligne = permissif (préservation du comportement existant)
        return (boolean) results.getFirst().get("enabled");
    }

    /* ======================== Catalogue des permissions ======================== */

    public List<Map<String, Object>> listPermissionCatalog() {
        return jdbcTemplate.queryForList(
                "SELECT key, label, module, description, ordre FROM permission_catalog ORDER BY ordre");
    }

    /* ======================== Gestion des rôles ======================== */

    public List<Map<String, Object>> listRoles() {
        return jdbcTemplate.queryForList(
                "SELECT pr.key, pr.label, pr.description, pr.system, " +
                "(SELECT COUNT(*) FROM role_permissions rp WHERE rp.role = pr.key AND rp.enabled = true) AS nb_permissions " +
                "FROM platform_roles pr ORDER BY pr.system DESC, pr.key");
    }

    public Map<String, Object> getRole(String key) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM platform_roles WHERE key = ?", key.toUpperCase());
        if (rows.isEmpty()) throw new NoSuchElementException("Rôle introuvable : " + key);
        return rows.getFirst();
    }

    public void createRole(String key, String label, String description) {
        String k = key.toUpperCase();
        jdbcTemplate.update(
                "INSERT INTO platform_roles (key, label, description, system) VALUES (?, ?, ?, FALSE)",
                k, label, description);
    }

    public void updateRole(String key, String label, String description) {
        String k = key.toUpperCase();
        if (label != null && !label.isBlank()) {
            jdbcTemplate.update("UPDATE platform_roles SET label = ?, updated_at = ? WHERE key = ?",
                    label, LocalDateTime.now(), k);
        }
        if (description != null) {
            jdbcTemplate.update("UPDATE platform_roles SET description = ?, updated_at = ? WHERE key = ?",
                    description, LocalDateTime.now(), k);
        }
    }

    /**
     * Duplique un rôle (source) : copie ses permissions activées vers un nouveau rôle.
     * Le nouveau rôle est marqué comme non système.
     */
    public void duplicateRole(String sourceKey, String newKey, String label) {
        String src = sourceKey.toUpperCase();
        String dst = newKey.toUpperCase();
        // Créer l'entrée dans platform_roles
        jdbcTemplate.update(
                "INSERT INTO platform_roles (key, label, description, system) VALUES (?, ?, ?, FALSE)",
                dst, label, "Dupliqué de " + src);
        // Copier les permissions activées
        jdbcTemplate.update(
                "INSERT INTO role_permissions (role, permission, enabled) " +
                "SELECT ?, permission, enabled FROM role_permissions WHERE role = ? AND enabled = true",
                dst, src);
    }

    public void deleteRole(String key) {
        String k = key.toUpperCase();
        Map<String, Object> role = getRole(k);
        if ((boolean) role.get("system")) {
            throw new IllegalStateException("Impossible de supprimer un rôle système : " + k);
        }
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role = ?", k);
        jdbcTemplate.update("DELETE FROM platform_roles WHERE key = ?", k);
    }

    /**
     * Vérifie si un utilisateur (avec ses rôles) possède une permission.
     * Super-utilisateurs (ADMIN, PASTEUR) : toujours vrai.
     * Permission explicite désactivée → faux.
     * Absence de ligne → vrai (permissif).
     */
    public boolean userHasPermission(Collection<String> userRoles, String permission) {
        if (userRoles == null || userRoles.isEmpty()) return false;
        Set<String> roles = new HashSet<>();
        for (String r : userRoles) {
            String upper = r.toUpperCase();
            roles.add(upper);
            if ("ADMIN".equals(upper) || "PASTEUR".equals(upper)) return true;
        }
        // Vérifie chaque rôle : si un rôle a la permission explicitement désactivée → false.
        // Si aucun rôle n'a de ligne et au moins un rôle existe → true.
        boolean anyExplicit = false;
        for (String role : roles) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT enabled FROM role_permissions WHERE role = ? AND permission = ?",
                    role.toUpperCase(), permission.toUpperCase());
            if (rows.isEmpty()) continue;
            anyExplicit = true;
            if ((boolean) rows.getFirst().get("enabled")) return true;
        }
        // Si aucun rôle n'a de ligne explicite : permissif → true.
        return !anyExplicit;
    }
}
