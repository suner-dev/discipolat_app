package com.discipolat.modules.audit.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PermissionService {

    private final JdbcTemplate jdbcTemplate;

    public PermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    public boolean hasPermission(String role, String permission) {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT enabled FROM role_permissions WHERE role = ? AND permission = ? AND enabled = true",
                role, permission);
        return !results.isEmpty();
    }
}
