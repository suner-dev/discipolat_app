package com.discipolat.modules.audit.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Matrice des permissions (role → permission → enabled).
 *
 * <p>Les tables {@code role_permissions}, {@code permission_catalog} et
 * {@code platform_roles} sont multi-tenant (colonne {@code tenant_id}) : ce
 * service utilise du SQL brut, qui échappe au filtre Hibernate — chaque requête
 * filtre donc explicitement sur le tenant courant quand un contexte tenant est
 * actif. Sans contexte tenant (tâches système), le comportement historique est
 * conservé.
 */
@Service
@Transactional
public class PermissionService {

    private final JdbcTemplate jdbcTemplate;

    public PermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ======================== Helpers tenant ======================== */

    private UUID tenantId() {
        return TenantContext.getTenantId();
    }

    private String andTenant() {
        return tenantId() != null ? " AND tenant_id = ?" : "";
    }

    private Object[] params(Object... base) {
        UUID tenantId = tenantId();
        if (tenantId == null) {
            return base;
        }
        Object[] all = Arrays.copyOf(base, base.length + 1);
        all[base.length] = tenantId;
        return all;
    }

    /* ======================== Matrice des permissions ======================== */

    public List<Map<String, Object>> getAllPermissions() {
        UUID tenantId = tenantId();
        if (tenantId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT role, permission, enabled, can_read, can_write, can_delete, scope FROM role_permissions WHERE tenant_id = ? ORDER BY role, permission",
                    tenantId);
        }
        return jdbcTemplate.queryForList(
                "SELECT role, permission, enabled, can_read, can_write, can_delete, scope FROM role_permissions ORDER BY role, permission");
    }

    public List<Map<String, Object>> getPermissionsByRole(String role) {
        return jdbcTemplate.queryForList(
                "SELECT role, permission, enabled FROM role_permissions WHERE role = ?"
                        + andTenant() + " ORDER BY permission",
                params(role.toUpperCase()));
    }

    public Map<String, Object> updatePermission(String role, String permission, boolean enabled) {
        jdbcTemplate.update(
                "UPDATE role_permissions SET enabled = ?, can_read = ?, can_write = ?, updated_at = ? WHERE role = ? AND permission = ?"
                        + andTenant(),
                params(enabled, enabled, enabled, LocalDateTime.now(), role.toUpperCase(), permission.toUpperCase()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", role.toUpperCase());
        result.put("permission", permission.toUpperCase());
        result.put("enabled", enabled);
        result.put("canRead", enabled);
        result.put("canWrite", enabled);
        result.put("canDelete", false);
        return result;
    }

    /**
     * Met à jour les permissions granulaires (lecture/écriture/suppression) + scope.
     */
    public Map<String, Object> updatePermissionRWD(String role, String permission,
                                                     boolean canRead, boolean canWrite, boolean canDelete,
                                                     String scope) {
        String r = role.toUpperCase();
        String p = permission.toUpperCase();
        String s = scope != null ? scope.toUpperCase() : "GLOBAL";
        boolean enabled = canRead || canWrite || canDelete;
        jdbcTemplate.update(
                "UPDATE role_permissions SET enabled = ?, can_read = ?, can_write = ?, can_delete = ?, scope = ?, updated_at = ?"
                        + " WHERE role = ? AND permission = ?" + andTenant(),
                params(enabled, canRead, canWrite, canDelete, s, LocalDateTime.now(), r, p));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", r);
        result.put("permission", p);
        result.put("enabled", enabled);
        result.put("canRead", canRead);
        result.put("canWrite", canWrite);
        result.put("canDelete", canDelete);
        result.put("scope", s);
        return result;
    }

    /**
     * Vérifie si un rôle donné a une permission activée explicitement.
     * RESTRICTIF par défaut : si aucune ligne n'existe, retourne false (sécurité).
     * Les rôles ADMIN et PASTEUR conservent un accès complet via {@link #userHasPermission}.
     */
    public boolean hasPermission(String role, String permission) {
        String r = role.toUpperCase();
        if ("ADMIN".equals(r) || "PASTEUR".equals(r)) return true;
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT enabled FROM role_permissions WHERE role = ? AND permission = ?"
                        + andTenant(),
                params(r, permission.toUpperCase()));
        if (results.isEmpty()) return false; // RESTRICTIF: pas de ligne = refusé
        return (boolean) results.getFirst().get("enabled");
    }

    /**
     * Vérifie si un rôle a un niveau de permission (read/write/delete) donné.
     * Niveau "manage" = write OR delete.
     */
    private static final Set<String> VALID_COLUMNS = Set.of("can_read", "can_write", "can_delete", "enabled");

    public boolean hasPermissionLevel(String role, String permission, String level) {
        String r = role.toUpperCase();
        if ("ADMIN".equals(r) || "PASTEUR".equals(r)) return true;

        String col;
        boolean isManage = "MANAGE".equalsIgnoreCase(level);
        if (isManage) {
            col = "can_write";
        } else {
            String c = switch (level.toUpperCase()) {
                case "READ" -> "can_read";
                case "WRITE" -> "can_write";
                case "DELETE" -> "can_delete";
                default -> "enabled";
            };
            col = c;
        }

        if (!VALID_COLUMNS.contains(col)) return false;

        if (isManage) {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    "SELECT can_write, can_delete FROM role_permissions WHERE role = ? AND permission = ?"
                            + andTenant(),
                    params(r, permission.toUpperCase()));
            if (results.isEmpty()) return false;
            Map<String, Object> row = results.getFirst();
            return (boolean) row.get("can_write") || (boolean) row.get("can_delete");
        }
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT " + col + " FROM role_permissions WHERE role = ? AND permission = ?"
                        + andTenant(),
                params(r, permission.toUpperCase()));
        if (results.isEmpty()) return false;
        return (boolean) results.getFirst().get(col);
    }

    /**
     * Retourne le scope configuré pour un rôle + permission.
     */
    public String getPermissionScope(String role, String permission) {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT scope FROM role_permissions WHERE role = ? AND permission = ?"
                        + andTenant(),
                params(role.toUpperCase(), permission.toUpperCase()));
        if (results.isEmpty()) return "NONE";
        Object scope = results.getFirst().get("scope");
        return scope != null ? scope.toString() : "GLOBAL";
    }

    /* ======================== Catalogue des permissions ======================== */

    public List<Map<String, Object>> listPermissionCatalog() {
        UUID tenantId = tenantId();
        if (tenantId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT key, label, module, description, ordre FROM permission_catalog WHERE tenant_id = ? ORDER BY ordre",
                    tenantId);
        }
        return jdbcTemplate.queryForList(
                "SELECT key, label, module, description, ordre FROM permission_catalog ORDER BY ordre");
    }

    /* ======================== Gestion des rôles ======================== */

    public List<Map<String, Object>> listRoles() {
        UUID tenantId = tenantId();
        if (tenantId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT pr.key, pr.label, pr.description, pr.system, "
                            + "(SELECT COUNT(*) FROM role_permissions rp WHERE rp.role = pr.key AND rp.enabled = true AND rp.tenant_id = ?) AS nb_permissions "
                            + "FROM platform_roles pr WHERE pr.tenant_id = ? ORDER BY pr.system DESC, pr.key",
                    tenantId, tenantId);
        }
        return jdbcTemplate.queryForList(
                "SELECT pr.key, pr.label, pr.description, pr.system, "
                        + "(SELECT COUNT(*) FROM role_permissions rp WHERE rp.role = pr.key AND rp.enabled = true) AS nb_permissions "
                        + "FROM platform_roles pr ORDER BY pr.system DESC, pr.key");
    }

    public Map<String, Object> getRole(String key) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM platform_roles WHERE key = ?" + andTenant(),
                params(key.toUpperCase()));
        if (rows.isEmpty()) throw new NoSuchElementException("Rôle introuvable : " + key);
        return rows.getFirst();
    }

    public void createRole(String key, String label, String description) {
        String k = key.toUpperCase();
        UUID tenantId = tenantId();
        if (tenantId != null) {
            jdbcTemplate.update(
                    "INSERT INTO platform_roles (key, label, description, system, tenant_id) VALUES (?, ?, ?, FALSE, ?)",
                    k, label, description, tenantId);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO platform_roles (key, label, description, system) VALUES (?, ?, ?, FALSE)",
                    k, label, description);
        }
    }

    public void updateRole(String key, String label, String description) {
        String k = key.toUpperCase();
        if (label != null && !label.isBlank()) {
            jdbcTemplate.update("UPDATE platform_roles SET label = ?, updated_at = ? WHERE key = ?" + andTenant(),
                    params(label, LocalDateTime.now(), k));
        }
        if (description != null) {
            jdbcTemplate.update("UPDATE platform_roles SET description = ?, updated_at = ? WHERE key = ?" + andTenant(),
                    params(description, LocalDateTime.now(), k));
        }
    }

    /**
     * Duplique un rôle (source) : copie ses permissions activées vers un nouveau rôle.
     * Le nouveau rôle est marqué comme non système.
     */
    public void duplicateRole(String sourceKey, String newKey, String label) {
        String src = sourceKey.toUpperCase();
        String dst = newKey.toUpperCase();
        UUID tenantId = tenantId();
        if (tenantId != null) {
            // Créer l'entrée dans platform_roles
            jdbcTemplate.update(
                    "INSERT INTO platform_roles (key, label, description, system, tenant_id) VALUES (?, ?, ?, FALSE, ?)",
                    dst, label, "Dupliqué de " + src, tenantId);
            // Copier les permissions activées du même tenant
            jdbcTemplate.update(
                    "INSERT INTO role_permissions (role, permission, enabled, tenant_id) "
                            + "SELECT ?, permission, enabled, ? FROM role_permissions WHERE role = ? AND enabled = true AND tenant_id = ?",
                    dst, tenantId, src, tenantId);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO platform_roles (key, label, description, system) VALUES (?, ?, ?, FALSE)",
                    dst, label, "Dupliqué de " + src);
            jdbcTemplate.update(
                    "INSERT INTO role_permissions (role, permission, enabled) "
                            + "SELECT ?, permission, enabled FROM role_permissions WHERE role = ? AND enabled = true",
                    dst, src);
        }
    }

    public void deleteRole(String key) {
        String k = key.toUpperCase();
        Map<String, Object> role = getRole(k);
        if ((boolean) role.get("system")) {
            throw new IllegalStateException("Impossible de supprimer un rôle système : " + k);
        }
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role = ?" + andTenant(), params(k));
        jdbcTemplate.update("DELETE FROM platform_roles WHERE key = ?" + andTenant(), params(k));
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
                    "SELECT enabled FROM role_permissions WHERE role = ? AND permission = ?" + andTenant(),
                    params(role.toUpperCase(), permission.toUpperCase()));
            if (rows.isEmpty()) continue;
            anyExplicit = true;
            if ((boolean) rows.getFirst().get("enabled")) return true;
        }
        // Si aucun rôle n'a de ligne explicite : restrictif → false.
        return false;
    }
}
