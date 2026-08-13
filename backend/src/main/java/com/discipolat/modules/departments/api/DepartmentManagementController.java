package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentManagementService;
import com.discipolat.modules.departments.domain.DepartmentTask;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Department Management System — API de gestion avancée du département :
 * sous-départements/équipes (hiérarchie récursive), postes, affectations
 * de membres, tâches/charge de travail et journal d'activité.
 * <p>
 * L'accès est validé côté service : un responsable ne gère que SES
 * départements (même règle que le reste du module).
 */
@RestController
@RequestMapping("/api/v1/departments/{departmentId}")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
public class DepartmentManagementController {

    private final DepartmentManagementService managementService;

    public DepartmentManagementController(DepartmentManagementService managementService) {
        this.managementService = managementService;
    }

    // ======================= VUE D'ENSEMBLE =======================

    @GetMapping("/management")
    public ResponseEntity<Map<String, Object>> overview(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(managementService.getManagementOverview(departmentId));
    }

    @GetMapping("/activity")
    public ResponseEntity<List<Map<String, Object>>> activity(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(managementService.getActivity(departmentId));
    }

    // ======================= MEMBRES =======================

    /** Dossier de gestion : membres du département avec affectations courantes. */
    @GetMapping("/members/management")
    public ResponseEntity<List<Map<String, Object>>> membersManagement(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(managementService.getMembersManagement(departmentId));
    }

    /** Recherche de personnes déjà inscrites, non encore rattachées au département. */
    @GetMapping("/members/candidates")
    public ResponseEntity<List<Map<String, Object>>> candidates(@PathVariable UUID departmentId,
                                                                @RequestParam(required = false) String q) {
        return ResponseEntity.ok(managementService.findCandidates(departmentId, q));
    }

    /** Ajoute une personne déjà inscrite au département (traçabilité + notification). */
    @PostMapping("/members")
    public ResponseEntity<Map<String, Object>> addMember(@PathVariable UUID departmentId,
                                                         @Valid @RequestBody DepartmentMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementService.addMember(departmentId, request.soulId()));
    }

    /** Crée un nouveau membre et l'affecte au département. */
    @PostMapping("/members/create")
    public ResponseEntity<Map<String, Object>> createMember(@PathVariable UUID departmentId,
                                                            @Valid @RequestBody DepartmentCreateMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementService.createMember(departmentId, request));
    }

    /** Retire un membre du département (lien + affectations clôturées, tracé). */
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID departmentId, @PathVariable UUID memberId) {
        managementService.removeMember(departmentId, memberId);
        return ResponseEntity.noContent().build();
    }

    // ======================= ÉQUIPES / SOUS-DÉPARTEMENTS =======================

    @GetMapping("/teams")
    public ResponseEntity<List<Map<String, Object>>> teams(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(managementService.getTeams(departmentId));
    }

    @PostMapping("/teams")
    public ResponseEntity<Map<String, Object>> createTeam(@PathVariable UUID departmentId,
                                                          @Valid @RequestBody DepartmentTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managementService.createTeam(departmentId, request));
    }

    @PutMapping("/teams/{teamId}")
    public ResponseEntity<Map<String, Object>> updateTeam(@PathVariable UUID departmentId,
                                                          @PathVariable UUID teamId,
                                                          @Valid @RequestBody DepartmentTeamRequest request) {
        return ResponseEntity.ok(managementService.updateTeam(departmentId, teamId, request));
    }

    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> archiveTeam(@PathVariable UUID departmentId, @PathVariable UUID teamId) {
        managementService.archiveTeam(departmentId, teamId);
        return ResponseEntity.noContent().build();
    }

    // ======================= POSTES =======================

    @GetMapping("/positions")
    public ResponseEntity<List<Map<String, Object>>> positions(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(managementService.getPositions(departmentId));
    }

    @PostMapping("/positions")
    public ResponseEntity<Map<String, Object>> createPosition(@PathVariable UUID departmentId,
                                                              @Valid @RequestBody DepartmentPositionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managementService.createPosition(departmentId, request));
    }

    @PutMapping("/positions/{positionId}")
    public ResponseEntity<Map<String, Object>> updatePosition(@PathVariable UUID departmentId,
                                                              @PathVariable UUID positionId,
                                                              @Valid @RequestBody DepartmentPositionRequest request) {
        return ResponseEntity.ok(managementService.updatePosition(departmentId, positionId, request));
    }

    @DeleteMapping("/positions/{positionId}")
    public ResponseEntity<Void> archivePosition(@PathVariable UUID departmentId, @PathVariable UUID positionId) {
        managementService.archivePosition(departmentId, positionId);
        return ResponseEntity.noContent().build();
    }

    // ======================= AFFECTATIONS =======================

    @GetMapping("/assignments")
    public ResponseEntity<List<Map<String, Object>>> assignments(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(managementService.getAssignments(departmentId));
    }

    @PostMapping("/assignments")
    public ResponseEntity<Map<String, Object>> assign(@PathVariable UUID departmentId,
                                                      @Valid @RequestBody DepartmentAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managementService.assignMember(departmentId, request));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<Map<String, Object>> endAssignment(@PathVariable UUID departmentId,
                                                             @PathVariable UUID assignmentId) {
        return ResponseEntity.ok(managementService.endAssignment(departmentId, assignmentId));
    }

    // ======================= TÂCHES =======================

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> tasks(@PathVariable UUID departmentId,
                                                           @RequestParam(required = false) DepartmentTask.TaskStatus statut,
                                                           @RequestParam(required = false) UUID teamId) {
        return ResponseEntity.ok(managementService.getTasks(departmentId, statut, teamId));
    }

    @GetMapping("/tasks/stats")
    public ResponseEntity<Map<String, Object>> taskStats(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(managementService.getTaskStats(departmentId));
    }

    @PostMapping("/tasks")
    public ResponseEntity<Map<String, Object>> createTask(@PathVariable UUID departmentId,
                                                          @Valid @RequestBody DepartmentTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managementService.createTask(departmentId, request));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> updateTask(@PathVariable UUID departmentId,
                                                          @PathVariable UUID taskId,
                                                          @Valid @RequestBody DepartmentTaskRequest request) {
        return ResponseEntity.ok(managementService.updateTask(departmentId, taskId, request));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID departmentId, @PathVariable UUID taskId) {
        managementService.deleteTask(departmentId, taskId);
        return ResponseEntity.noContent().build();
    }
}
