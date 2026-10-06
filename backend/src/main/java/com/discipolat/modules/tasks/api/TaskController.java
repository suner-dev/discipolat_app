package com.discipolat.modules.tasks.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tasks.domain.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tasks — V234. Contrat exact du mobile
 * (mobile/lib/features/tasks/services/tasks_service.dart).
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listTasks(tenantId, page, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getTask(tenantId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTask(tenantId, body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.updateTask(tenantId, id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        UUID tenantId = TenantContext.getTenantId();
        service.deleteTask(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.updateStatus(tenantId, id, (String) body.get("status")));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        UUID assignedToId = UUID.fromString(String.valueOf(body.get("assignedToId")));
        return ResponseEntity.ok(service.assignTask(tenantId, id, assignedToId));
    }

    @GetMapping("/{taskId}/subtasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> subtasks(@PathVariable Long taskId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listSubtasks(tenantId, taskId));
    }

    @PostMapping("/{parentTaskId}/subtasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> createSubtask(@PathVariable Long parentTaskId, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSubtask(tenantId, parentTaskId, body));
    }

    @GetMapping("/{taskId}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> attachments(@PathVariable Long taskId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listAttachments(tenantId, taskId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        UUID tenantId = TenantContext.getTenantId();
        service.deleteAttachment(tenantId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> comments(@PathVariable Long taskId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listComments(tenantId, taskId));
    }

    @PostMapping("/{taskId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createComment(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createComment(tenantId, taskId, body));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        UUID tenantId = TenantContext.getTenantId();
        service.deleteComment(tenantId, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/dependencies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> dependencies(@PathVariable Long taskId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listDependencies(tenantId, taskId));
    }

    @PostMapping("/{taskId}/dependencies")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> createDependency(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDependency(tenantId, taskId, body));
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> deleteDependency(@PathVariable Long dependencyId) {
        UUID tenantId = TenantContext.getTenantId();
        service.deleteDependency(tenantId, dependencyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/kanban/columns")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> kanbanColumns() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listKanbanColumns(tenantId));
    }

    @PutMapping("/kanban/columns/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> updateKanbanColumn(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.updateKanbanColumn(tenantId, id, body));
    }

    @PostMapping("/{taskId}/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> reorder(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.updateTask(tenantId, taskId, body));
    }

    @GetMapping("/{taskId}/time-entries")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> timeEntries(@PathVariable Long taskId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listTimeEntries(tenantId, taskId));
    }

    @PostMapping("/{taskId}/time-entries/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> startTimeEntry(@PathVariable Long taskId, @RequestBody(required = false) Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startTimeEntry(tenantId, taskId, body == null ? Map.of() : body));
    }

    @PostMapping("/time-entries/{timeEntryId}/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> stopTimeEntry(@PathVariable Long timeEntryId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.stopTimeEntry(tenantId, timeEntryId));
    }

    @GetMapping("/{taskId}/time-total")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> timeTotal(@PathVariable Long taskId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.getTimeTotal(tenantId, taskId));
    }

    @GetMapping("/templates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> templates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listTemplates(tenantId, page, size));
    }

    @PostMapping("/templates/{templateId}/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> createFromTemplate(@PathVariable Long templateId, @RequestBody(required = false) Map<String, Object> body) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createFromTemplate(tenantId, templateId, body == null ? Map.of() : body));
    }

    @GetMapping("/reports/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> reportStatistics() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.reportStatistics(tenantId));
    }

    @GetMapping("/reports/by-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> reportByStatus() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.reportByStatus(tenantId));
    }

    @GetMapping("/reports/by-assignee")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> reportByAssignee() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.reportByAssignee(tenantId));
    }

    @GetMapping("/overdue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> overdue() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(service.listOverdue(tenantId));
    }
}
