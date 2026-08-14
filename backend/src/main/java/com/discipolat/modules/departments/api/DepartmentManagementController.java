package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.DepartmentDossierService;
import com.discipolat.modules.departments.domain.DepartmentManagementService;
import com.discipolat.modules.departments.domain.DepartmentReportingService;
import com.discipolat.modules.departments.domain.DepartmentTask;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
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
    private final DepartmentDossierService dossierService;
    private final DepartmentReportingService reportingService;
    private final com.discipolat.modules.departments.domain.DepartmentService departmentService;

    public DepartmentManagementController(DepartmentManagementService managementService,
                                          DepartmentDossierService dossierService,
                                          DepartmentReportingService reportingService,
                                          com.discipolat.modules.departments.domain.DepartmentService departmentService) {
        this.managementService = managementService;
        this.dossierService = dossierService;
        this.reportingService = reportingService;
        this.departmentService = departmentService;
    }

    /** Nom du département normalisé pour le nom du fichier d'export. */
    private String departmentServiceName(UUID departmentId) {
        String nom = departmentService.findById(departmentId).getNom();
        return nom == null ? departmentId.toString().substring(0, 8)
                : nom.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
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

    // ======================= DOSSIER MEMBRE =======================

    /** Dossier complet d'un membre du département (profil, affectations, tâches, présences…). */
    @GetMapping("/members/{memberId}/dossier")
    public ResponseEntity<Map<String, Object>> memberDossier(@PathVariable UUID departmentId,
                                                              @PathVariable UUID memberId) {
        return ResponseEntity.ok(dossierService.getMemberDossier(departmentId, memberId));
    }

    // ======================= NOTES MEMBRE =======================

    @GetMapping("/members/{memberId}/notes")
    public ResponseEntity<List<Map<String, Object>>> memberNotes(@PathVariable UUID departmentId,
                                                                 @PathVariable UUID memberId) {
        return ResponseEntity.ok(dossierService.listMemberNotes(departmentId, memberId));
    }

    @PostMapping("/members/{memberId}/notes")
    public ResponseEntity<Map<String, Object>> addMemberNote(@PathVariable UUID departmentId,
                                                             @PathVariable UUID memberId,
                                                             @Valid @RequestBody DepartmentNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.addMemberNote(departmentId, memberId, request));
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteMemberNote(@PathVariable UUID departmentId, @PathVariable UUID noteId) {
        dossierService.deleteMemberNote(departmentId, noteId);
        return ResponseEntity.noContent().build();
    }

    // ======================= ANNONCES (communication interne) =======================

    @GetMapping("/announcements")
    public ResponseEntity<List<Map<String, Object>>> announcements(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(dossierService.listAnnouncements(departmentId));
    }

    @PostMapping("/announcements")
    public ResponseEntity<Map<String, Object>> createAnnouncement(@PathVariable UUID departmentId,
                                                                  @Valid @RequestBody DepartmentAnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.createAnnouncement(departmentId, request));
    }

    @DeleteMapping("/announcements/{announcementId}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable UUID departmentId,
                                                   @PathVariable UUID announcementId) {
        dossierService.deleteAnnouncement(departmentId, announcementId);
        return ResponseEntity.noContent().build();
    }

    // ======================= ALERTES INTELLIGENTES =======================

    @GetMapping("/alerts/smart")
    public ResponseEntity<List<Map<String, Object>>> smartAlerts(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(dossierService.getIntelligentAlerts(departmentId));
    }

    // ======================= STATISTIQUES =======================

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(dossierService.getDepartmentStats(departmentId));
    }

    // ======================= EXPORT / IMPORT MEMBRES =======================

    /** Export CSV des membres du département (respecte les permissions). */
    @GetMapping(value = "/members/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportMembers(@PathVariable UUID departmentId) {
        String csv = dossierService.exportMembersCsv(departmentId);
        String departmentNom = departmentServiceName(departmentId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=membres-" + departmentNom + ".csv")
                .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    /** Import CSV des membres : preview (aucune écriture) ou import effectif. */
    @PostMapping("/members/import")
    public ResponseEntity<Map<String, Object>> importMembers(@PathVariable UUID departmentId,
                                                             @RequestParam(defaultValue = "false") boolean preview,
                                                             @Valid @RequestBody DepartmentImportRequest request) {
        return ResponseEntity.ok(dossierService.importMembers(departmentId, request.rows(), preview));
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

    // ======================= OBJECTIFS DE PROGRESSION =======================

    @GetMapping("/members/{memberId}/objectives")
    public ResponseEntity<List<Map<String, Object>>> memberObjectives(@PathVariable UUID departmentId,
                                                                      @PathVariable UUID memberId) {
        return ResponseEntity.ok(managementService.getMemberObjectives(departmentId, memberId));
    }

    @PostMapping("/members/{memberId}/objectives")
    public ResponseEntity<Map<String, Object>> createObjective(@PathVariable UUID departmentId,
                                                               @PathVariable UUID memberId,
                                                               @Valid @RequestBody DepartmentObjectiveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementService.createObjective(departmentId, memberId, request));
    }

    @PutMapping("/objectives/{objectiveId}")
    public ResponseEntity<Map<String, Object>> updateObjective(@PathVariable UUID departmentId,
                                                               @PathVariable UUID objectiveId,
                                                               @Valid @RequestBody DepartmentObjectiveRequest request) {
        return ResponseEntity.ok(managementService.updateObjective(departmentId, objectiveId, request));
    }

    @DeleteMapping("/objectives/{objectiveId}")
    public ResponseEntity<Void> deleteObjective(@PathVariable UUID departmentId, @PathVariable UUID objectiveId) {
        managementService.deleteObjective(departmentId, objectiveId);
        return ResponseEntity.noContent().build();
    }

    // ======================= RAPPORTS DU RESPONSABLE SUR UN MEMBRE =======================

    @GetMapping("/members/{memberId}/reports")
    public ResponseEntity<List<Map<String, Object>>> memberReports(@PathVariable UUID departmentId,
                                                                   @PathVariable UUID memberId) {
        return ResponseEntity.ok(dossierService.listMemberReports(departmentId, memberId));
    }

    @PostMapping("/members/{memberId}/reports")
    public ResponseEntity<Map<String, Object>> createMemberReport(@PathVariable UUID departmentId,
                                                                  @PathVariable UUID memberId,
                                                                  @Valid @RequestBody DepartmentMemberReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dossierService.createMemberReport(departmentId, memberId, request));
    }

    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<Void> deleteMemberReport(@PathVariable UUID departmentId, @PathVariable UUID reportId) {
        dossierService.deleteMemberReport(departmentId, reportId);
        return ResponseEntity.noContent().build();
    }

    // ======================= RAPPORTS DE DÉPARTEMENT (synthèses sauvegardées) =======================

    /** Liste des rapports du département (synthèses sauvegardées). */
    @GetMapping("/reports/list")
    public ResponseEntity<List<Map<String, Object>>> departmentReports(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(reportingService.listReports(departmentId));
    }

    /** Génère et sauvegarde une synthèse du département sur les données réelles. */
    @PostMapping("/reports/generate")
    public ResponseEntity<Map<String, Object>> generateDepartmentReport(@PathVariable UUID departmentId,
                                                                        @Valid @RequestBody DepartmentReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportingService.generateReport(departmentId, request));
    }

    /** Sauvegarde un rapport rédigé manuellement. */
    @PostMapping("/reports")
    public ResponseEntity<Map<String, Object>> saveDepartmentReport(@PathVariable UUID departmentId,
                                                                    @Valid @RequestBody DepartmentReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportingService.saveManualReport(departmentId, request));
    }

    @DeleteMapping("/reports/saved/{reportId}")
    public ResponseEntity<Void> deleteDepartmentReport(@PathVariable UUID departmentId,
                                                       @PathVariable UUID reportId) {
        reportingService.deleteReport(departmentId, reportId);
        return ResponseEntity.noContent().build();
    }

    /** Export CSV d'un rapport de département. */
    @GetMapping(value = "/reports/saved/{reportId}/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportDepartmentReport(@PathVariable UUID departmentId,
                                                         @PathVariable UUID reportId) {
        String csv = reportingService.exportReportCsv(departmentId, reportId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=rapport-departement.csv")
                .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    // ======================= CHECKLISTS =======================

    @GetMapping("/checklists")
    public ResponseEntity<List<Map<String, Object>>> checklists(@PathVariable UUID departmentId,
                                                                @RequestParam(required = false) String cibleType,
                                                                @RequestParam(required = false) UUID cibleId) {
        return ResponseEntity.ok(reportingService.listChecklists(departmentId, cibleType, cibleId));
    }

    @PostMapping("/checklists")
    public ResponseEntity<Map<String, Object>> createChecklist(@PathVariable UUID departmentId,
                                                               @Valid @RequestBody DepartmentChecklistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportingService.createChecklist(departmentId, request));
    }

    @PutMapping("/checklists/{checklistId}")
    public ResponseEntity<Map<String, Object>> updateChecklist(@PathVariable UUID departmentId,
                                                               @PathVariable UUID checklistId,
                                                               @Valid @RequestBody DepartmentChecklistRequest request) {
        return ResponseEntity.ok(reportingService.updateChecklist(departmentId, checklistId, request));
    }

    @PostMapping("/checklists/{checklistId}/items")
    public ResponseEntity<Map<String, Object>> addChecklistItem(@PathVariable UUID departmentId,
                                                                @PathVariable UUID checklistId,
                                                                @Valid @RequestBody DepartmentChecklistItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportingService.addChecklistItem(departmentId, checklistId, request));
    }

    @PutMapping("/checklists/{checklistId}/items/{itemId}")
    public ResponseEntity<Map<String, Object>> toggleChecklistItem(@PathVariable UUID departmentId,
                                                                   @PathVariable UUID checklistId,
                                                                   @PathVariable UUID itemId,
                                                                   @Valid @RequestBody DepartmentChecklistItemRequest request) {
        return ResponseEntity.ok(reportingService.toggleChecklistItem(departmentId, checklistId, itemId, request));
    }

    @DeleteMapping("/checklists/{checklistId}/items/{itemId}")
    public ResponseEntity<Void> deleteChecklistItem(@PathVariable UUID departmentId,
                                                    @PathVariable UUID checklistId,
                                                    @PathVariable UUID itemId) {
        reportingService.deleteChecklistItem(departmentId, checklistId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/checklists/{checklistId}")
    public ResponseEntity<Void> deleteChecklist(@PathVariable UUID departmentId,
                                                @PathVariable UUID checklistId) {
        reportingService.deleteChecklist(departmentId, checklistId);
        return ResponseEntity.noContent().build();
    }

    // ======================= INVENTAIRE MATÉRIEL =======================

    @GetMapping("/equipment")
    public ResponseEntity<List<Map<String, Object>>> equipment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(reportingService.listEquipment(departmentId));
    }

    @PostMapping("/equipment")
    public ResponseEntity<Map<String, Object>> createEquipment(@PathVariable UUID departmentId,
                                                               @Valid @RequestBody DepartmentEquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportingService.createEquipment(departmentId, request));
    }

    @PutMapping("/equipment/{equipmentId}")
    public ResponseEntity<Map<String, Object>> updateEquipment(@PathVariable UUID departmentId,
                                                               @PathVariable UUID equipmentId,
                                                               @Valid @RequestBody DepartmentEquipmentRequest request) {
        return ResponseEntity.ok(reportingService.updateEquipment(departmentId, equipmentId, request));
    }

    @DeleteMapping("/equipment/{equipmentId}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable UUID departmentId,
                                                @PathVariable UUID equipmentId) {
        reportingService.deleteEquipment(departmentId, equipmentId);
        return ResponseEntity.noContent().build();
    }
}
