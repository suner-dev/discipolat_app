package com.discipolat.modules.discipleship.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.discipleship.domain.DiscipleshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Discipleship — V233. Contrat exact du mobile
 * (mobile/lib/features/discipleship/services/discipleship_service.dart).
 *
 * <p>Toutes les routes exigent l'authentification ; les lectures sont
 * réservées aux rôles de supervision, les écritures à ADMIN/PASTEUR.
 */
@RestController
@RequestMapping("/api/v1/discipleship")
public class DiscipleshipController {

    private final DiscipleshipService service;

    public DiscipleshipController(DiscipleshipService service) { this.service = service; }

    // ---------- Journeys ----------

    @GetMapping("/journeys")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> journeys(
            @RequestParam(required = false) Boolean isActive) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listJourneys(tenantId, isActive));
    }

    @GetMapping("/journeys/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> journey(@PathVariable Long id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getJourney(tenantId, id));
    }

    @PostMapping("/journeys")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> createJourney(@RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createJourney(tenantId, body));
    }

    // ---------- Stages ----------

    @GetMapping("/journeys/{journeyId}/stages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> stages(@PathVariable Long journeyId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listStages(tenantId, journeyId));
    }

    @GetMapping("/stages/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stage(@PathVariable Long id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getStage(tenantId, id));
    }

    @PostMapping("/stages")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> createStage(@RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createStage(tenantId, body));
    }

    // ---------- Progress ----------

    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> progress(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long journeyId,
            @RequestParam(required = false) UUID discipleId,
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listProgress(tenantId, page, size, journeyId, discipleId, status));
    }

    @GetMapping("/progress/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> progressById(@PathVariable Long id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getProgress(tenantId, id));
    }

    @PostMapping("/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> createProgress(@RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProgress(tenantId, body));
    }

    @PatchMapping("/progress/{progressId}/requirements/{requirementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> updateRequirement(
            @PathVariable Long progressId, @PathVariable Long requirementId,
            @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.updateRequirementProgress(tenantId, progressId, requirementId, body));
    }

    @PostMapping("/progress/{progressId}/stages/{stageId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> completeStage(
            @PathVariable Long progressId, @PathVariable Long stageId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.completeStage(tenantId, progressId, stageId));
    }

    // ---------- Assignments ----------

    @GetMapping("/assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> assignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID mentorId,
            @RequestParam(required = false) UUID discipleId,
            @RequestParam(required = false) Long journeyId,
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listAssignments(tenantId, page, size, mentorId, discipleId, journeyId, status));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> createAssignment(@RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAssignment(tenantId, body));
    }

    @PostMapping("/assignments/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> endAssignment(@PathVariable Long id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.endAssignment(tenantId, id));
    }

    // ---------- Meetings ----------

    @GetMapping("/meetings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> meetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long assignmentId,
            @RequestParam(required = false) UUID mentorId,
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listMeetings(tenantId, page, size, assignmentId, mentorId, status));
    }

    @PostMapping("/meetings")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> scheduleMeeting(@RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.scheduleMeeting(tenantId, body));
    }

    @PostMapping("/meetings/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> completeMeeting(
            @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.completeMeeting(tenantId, id, body == null ? Map.of() : body));
    }

    // ---------- Reports ----------

    @GetMapping("/reports/{journeyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> report(@PathVariable Long journeyId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getReport(tenantId, journeyId));
    }

    @GetMapping("/reports/{journeyId}/top-mentors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> topMentors(
            @PathVariable Long journeyId,
            @RequestParam(defaultValue = "10") int limit) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getTopMentors(tenantId, journeyId, limit));
    }
}
