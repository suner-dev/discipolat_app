package com.discipolat.modules.statuses.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.statuses.domain.CustomStatus;
import com.discipolat.modules.statuses.domain.CustomStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G2.7 — API des statuts configurables et de leurs transitions contrôlées.
 */
@RestController
@RequestMapping("/api/v1/statuses")
public class CustomStatusController {

    private final CustomStatusService statusService;

    public CustomStatusController(CustomStatusService statusService) {
        this.statusService = statusService;
    }

    private UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
        return tenantId;
    }

    /** Kanban : colonnes ordonnées (jeu de l'espace, sinon hérité du tenant). */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> board(
            @RequestParam String entityType,
            @RequestParam(required = false) UUID spaceId) {
        return ResponseEntity.ok(statusService.getStatusBoard(tenantId(), entityType, spaceId));
    }

    @GetMapping("/set")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CustomStatus>> set(
            @RequestParam String entityType,
            @RequestParam(required = false) UUID spaceId) {
        return ResponseEntity.ok(statusService.resolveStatusSet(tenantId(), entityType, spaceId));
    }

    @GetMapping("/{statusId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomStatus> get(@PathVariable UUID statusId) {
        return ResponseEntity.ok(statusService.getStatus(tenantId(), statusId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<CustomStatus> create(
            @RequestParam(required = false) UUID spaceId,
            @RequestBody StatusRequest request) {
        return ResponseEntity.ok(statusService.createStatus(tenantId(), spaceId, request.toCommand()));
    }

    @PutMapping("/{statusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<CustomStatus> update(@PathVariable UUID statusId, @RequestBody StatusRequest request) {
        return ResponseEntity.ok(statusService.updateStatus(tenantId(), statusId, request.toCommand()));
    }

    @DeleteMapping("/{statusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Void> delete(@PathVariable UUID statusId) {
        statusService.deleteStatus(tenantId(), statusId);
        return ResponseEntity.noContent().build();
    }

    /** Dry-run : valide une transition sans l'appliquer. */
    @GetMapping("/validate-transition")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> validateTransition(
            @RequestParam String entityType,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) UUID spaceId) {
        statusService.validateTransition(tenantId(), entityType, spaceId, from, to);
        return ResponseEntity.ok(Map.of("allowed", true, "from", from, "to", to));
    }

    /** Applique un changement de statut contrôlé (audit + événement StatusChanged). */
    @PostMapping("/change")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Map<String, Object>> change(
            @RequestParam String entityType,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam UUID entityId,
            @RequestParam(required = false) UUID spaceId) {
        statusService.changeStatus(tenantId(), entityType, spaceId, entityId, from, to,
                SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("status", "OK", "from", from, "to", to));
    }

    public record StatusRequest(
            String entityType,
            String code,
            String name,
            String color,
            String icon,
            Integer displayOrder,
            Boolean initial,
            Boolean finalStatus,
            List<String> allowedTransitions
    ) {
        CustomStatusService.StatusCommand toCommand() {
            return new CustomStatusService.StatusCommand(
                    entityType, code, name, color, icon, displayOrder, initial, finalStatus, allowedTransitions);
        }
    }
}
