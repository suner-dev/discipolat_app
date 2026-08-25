package com.discipolat.modules.audit.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(jdbcTemplate);
    }

    @Test
    void getAllPermissions_ShouldReturnList() {
        List<Map<String, Object>> expectedPermissions = List.of(
                Map.of("role", "ADMIN", "permission", "USER_CREATE", "enabled", true),
                Map.of("role", "PASTEUR", "permission", "FAMILY_CREATE", "enabled", true)
        );

        when(jdbcTemplate.queryForList(anyString())).thenReturn(expectedPermissions);

        List<Map<String, Object>> result = permissionService.getAllPermissions();

        assertEquals(2, result.size());
        assertEquals("ADMIN", result.get(0).get("role"));
        verify(jdbcTemplate).queryForList(
                "SELECT role, permission, enabled, can_read, can_write, can_delete, scope FROM role_permissions ORDER BY role, permission");
    }

    @Test
    void getAllPermissions_WhenEmpty_ShouldReturnEmptyList() {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

        List<Map<String, Object>> result = permissionService.getAllPermissions();

        assertTrue(result.isEmpty());
    }

    @Test
    void getPermissionsByRole_ShouldReturnFilteredResults() {
        String role = "faiseur";
        List<Map<String, Object>> expected = List.of(
                Map.of("role", "FAISEUR", "permission", "SOUL_CREATE", "enabled", true)
        );

        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(expected);

        List<Map<String, Object>> result = permissionService.getPermissionsByRole(role);

        assertEquals(1, result.size());
        assertEquals("FAISEUR", result.get(0).get("role"));
        // Verify that the role was upper-cased
        verify(jdbcTemplate).queryForList(
                "SELECT role, permission, enabled FROM role_permissions WHERE role = ? ORDER BY permission",
                "FAISEUR");
    }

    @Test
    void getPermissionsByRole_WithUnknownRole_ShouldReturnEmpty() {
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(List.of());

        List<Map<String, Object>> result = permissionService.getPermissionsByRole("UNKNOWN_ROLE");

        assertTrue(result.isEmpty());
    }

    @Test
    void updatePermission_EnablePermission_ShouldUpdateAndReturnResult() {
        String role = "faiseur";
        String permission = "soul_create";
        boolean enabled = true;

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        Map<String, Object> result = permissionService.updatePermission(role, permission, enabled);

        assertEquals("FAISEUR", result.get("role"));
        assertEquals("SOUL_CREATE", result.get("permission"));
        assertEquals(true, result.get("enabled"));
        // RWD : activer une permission active lecture ET écriture.
        assertEquals(true, result.get("canRead"));
        assertEquals(true, result.get("canWrite"));
        assertEquals(false, result.get("canDelete"));
        verify(jdbcTemplate).update(
                eq("UPDATE role_permissions SET enabled = ?, can_read = ?, can_write = ?, updated_at = ? WHERE role = ? AND permission = ?"),
                eq(true), eq(true), eq(true), any(), eq("FAISEUR"), eq("SOUL_CREATE"));
    }

    @Test
    void updatePermission_DisablePermission_ShouldUpdateAndReturnDisabled() {
        String role = "pasteur";
        String permission = "family_delete";
        boolean enabled = false;

        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        Map<String, Object> result = permissionService.updatePermission(role, permission, enabled);

        assertEquals("PASTEUR", result.get("role"));
        assertEquals("FAMILY_DELETE", result.get("permission"));
        assertEquals(false, result.get("enabled"));
        assertEquals(false, result.get("canRead"));
        assertEquals(false, result.get("canWrite"));
        assertEquals(false, result.get("canDelete"));
    }

    @Test
    void updatePermission_WithUnknownRoleOrPermission_ShouldStillReturnResult() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        Map<String, Object> result = permissionService.updatePermission("unknown", "unknown_perm", true);

        assertNotNull(result);
        assertEquals("UNKNOWN", result.get("role"));
    }

    @Test
    void hasPermission_WithEnabledPermission_ShouldReturnTrue() {
        String role = "faiseur";
        String permission = "soul_create";

        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString()))
                .thenReturn(List.of(Map.of("enabled", true)));

        boolean result = permissionService.hasPermission(role, permission);

        assertTrue(result);
        // Le rôle et la permission sont normalisés en majuscules.
        verify(jdbcTemplate).queryForList(
                "SELECT enabled FROM role_permissions WHERE role = ? AND permission = ?",
                "FAISEUR", "SOUL_CREATE");
    }

    @Test
    void hasPermission_WithDisabledPermission_ShouldReturnFalse() {
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString()))
                .thenReturn(List.of(Map.of("enabled", false)));

        boolean result = permissionService.hasPermission("FAISEUR", "FAMILY_DELETE");

        assertFalse(result);
    }

    @Test
    void hasPermission_WithUnknownRole_ShouldReturnFalseRestrictif() {
        // Sémantique sécurisée : pas de ligne dans la matrice = refusé (restrictif par défaut).
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyString())).thenReturn(List.of());

        boolean result = permissionService.hasPermission("UNKNOWN", "ANY_PERMISSION");

        assertFalse(result);
    }

    @Test
    void hasPermission_WithAdminRole_ShouldAlwaysReturnTrue() {
        boolean result = permissionService.hasPermission("ADMIN", "ANY_PERMISSION");

        assertTrue(result);
    }

    @Test
    void hasPermission_WithPasteurRole_ShouldAlwaysReturnTrue() {
        boolean result = permissionService.hasPermission("PASTEUR", "ANY_PERMISSION");

        assertTrue(result);
    }
}
