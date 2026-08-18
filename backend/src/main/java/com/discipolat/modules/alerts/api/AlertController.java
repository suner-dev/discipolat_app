package com.discipolat.modules.alerts.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<AlertResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID familleId) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "dateDeclenchement"));
        Page<Alert> alerts = alertService.findAll(statut, familleId, pageable);
        Page<AlertResponse> response = alerts.map(AlertResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<AlertResponse>> findActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Alert> alerts = alertService.findActive(pageable);
        Page<AlertResponse> response = alerts.map(AlertResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<AlertResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(AlertResponse.from(alertService.findById(id)));
    }

    /** Création manuelle d'une alerte ciblée (pasteur, responsable, chef de famille, faiseur). */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<AlertResponse> createManual(@Valid @RequestBody CreateAlertRequest request) {
        return ResponseEntity.ok(AlertResponse.from(alertService.createManual(request)));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<AlertResponse> resolve(@PathVariable UUID id) {
        return ResponseEntity.ok(AlertResponse.from(alertService.resolve(id)));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable UUID id) {
        return ResponseEntity.ok(AlertResponse.from(alertService.acknowledge(id)));
    }

    /** Résolution en lot — le Pasteur peut marquer plusieurs alertes comme résolues d'un coup. */
    @PostMapping("/resolve-batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> resolveBatch(@RequestBody List<UUID> ids) {
        int resolved = alertService.resolveBatch(ids);
        return ResponseEntity.ok(Map.of("resolved", resolved));
    }

    /** Statistiques globales des alertes (pour graphiques Pasteur). */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(alertService.getAlertStats());
    }
}
