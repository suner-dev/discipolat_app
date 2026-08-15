package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Présence des membres du département à un événement rattaché au département.
 * <p>
 * La feuille de présence (lecture complète) reste réservée aux rôles de
 * gestion (ADMIN / PASTEUR / RESPONSABLE). Le pointage d'un membre est ouvert
 * à tous les rôles d'encadrement : le responsable du département, mais aussi
 * le chef de famille et le faiseur de l'âme (vérifié côté service).
 */
@RestController
@RequestMapping("/api/v1/departments/{departmentId}/events/{eventId}/attendance")
public class DepartmentEventAttendanceController {

    private final DepartmentManagementService managementService;

    public DepartmentEventAttendanceController(DepartmentManagementService managementService) {
        this.managementService = managementService;
    }

    /** Feuille de présence d'un événement : membres du département + statut pointé. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> eventAttendance(@PathVariable UUID departmentId,
                                                               @PathVariable UUID eventId) {
        return ResponseEntity.ok(managementService.getEventAttendance(departmentId, eventId));
    }

    /** Marque un membre présent/absent à un événement du département. */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> markEventAttendance(@PathVariable UUID departmentId,
                                                                   @PathVariable UUID eventId,
                                                                   @Valid @RequestBody DepartmentEventAttendanceRequest request) {
        return ResponseEntity.ok(managementService.markEventAttendance(
                departmentId, eventId, request.soulId(), request.present()));
    }

    /** Marque tous les membres actifs du département présents (ou absents). */
    @PostMapping("/mark-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> markAllAttendance(@PathVariable UUID departmentId,
                                                                 @PathVariable UUID eventId,
                                                                 @RequestParam(defaultValue = "true") boolean present) {
        return ResponseEntity.ok(managementService.markAllEventAttendance(departmentId, eventId, present));
    }

    /** Export CSV de la feuille de présence de l'événement. */
    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<String> exportAttendance(@PathVariable UUID departmentId,
                                                   @PathVariable UUID eventId) {
        String csv = managementService.exportEventAttendanceCsv(departmentId, eventId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=feuille-presence.csv")
                .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }
}
