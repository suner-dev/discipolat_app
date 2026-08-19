package com.discipolat.modules.audit.api;

import com.discipolat.modules.audit.domain.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /* ======================== Matrice ======================== */

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> getByRole(@PathVariable String role) {
        return ResponseEntity.ok(permissionService.getPermissionsByRole(role));
    }

    @PutMapping("/{role}/{permission}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String role,
            @PathVariable String permission,
            @RequestBody Map<String, Object> body) {
        // Supporte à la fois l'ancien format (enabled seul) et le nouveau (canRead/canWrite/canDelete/scope)
        if (body.containsKey("canRead") || body.containsKey("canWrite") || body.containsKey("canDelete") || body.containsKey("scope")) {
            boolean canRead = body.containsKey("canRead") ? (Boolean) body.get("canRead") : true;
            boolean canWrite = body.containsKey("canWrite") ? (Boolean) body.get("canWrite") : true;
            boolean canDelete = body.containsKey("canDelete") ? (Boolean) body.get("canDelete") : false;
            String scope = body.containsKey("scope") ? (String) body.get("scope") : "GLOBAL";
            return ResponseEntity.ok(permissionService.updatePermissionRWD(role, permission, canRead, canWrite, canDelete, scope));
        }
        // Rétrocompatibilité : ancien format { enabled: true/false }
        boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : true;
        return ResponseEntity.ok(permissionService.updatePermission(role, permission, enabled));
    }

    @PutMapping("/{role}/{permission}/rwd")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRWD(
            @PathVariable String role,
            @PathVariable String permission,
            @RequestBody Map<String, Object> body) {
        boolean canRead = body.containsKey("canRead") ? (Boolean) body.get("canRead") : true;
        boolean canWrite = body.containsKey("canWrite") ? (Boolean) body.get("canWrite") : true;
        boolean canDelete = body.containsKey("canDelete") ? (Boolean) body.get("canDelete") : false;
        String scope = body.containsKey("scope") ? (String) body.get("scope") : "GLOBAL";
        return ResponseEntity.ok(permissionService.updatePermissionRWD(role, permission, canRead, canWrite, canDelete, scope));
    }

    /* ======================== Catalogue ======================== */

    @GetMapping("/catalog")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> catalog() {
        return ResponseEntity.ok(permissionService.listPermissionCatalog());
    }

    /* ======================== Gestion des rôles ======================== */

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> roles() {
        return ResponseEntity.ok(permissionService.listRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createRole(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String label = body.get("label");
        String description = body.getOrDefault("description", "");
        if (key == null || key.isBlank() || label == null || label.isBlank()) {
            throw new IllegalArgumentException("key et label requis");
        }
        permissionService.createRole(key, label, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("key", key.toUpperCase()));
    }

    @PutMapping("/roles/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateRole(@PathVariable String key,
                                                          @RequestBody Map<String, String> body) {
        permissionService.updateRole(key, body.get("label"), body.get("description"));
        return ResponseEntity.ok(Map.of("key", key.toUpperCase()));
    }

    @PostMapping("/roles/duplicate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> duplicateRole(@RequestBody Map<String, String> body) {
        String sourceKey = body.get("sourceKey");
        String newKey = body.get("newKey");
        String label = body.getOrDefault("label", newKey);
        if (sourceKey == null || newKey == null) {
            throw new IllegalArgumentException("sourceKey et newKey requis");
        }
        permissionService.duplicateRole(sourceKey, newKey, label);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("key", newKey.toUpperCase()));
    }

    @DeleteMapping("/roles/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable String key) {
        permissionService.deleteRole(key);
        return ResponseEntity.noContent().build();
    }
}
