package com.discipolat.modules.workflow.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.workflow.domain.Automation;
import com.discipolat.modules.workflow.domain.WorkflowAutomationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflow/automations")
public class WorkflowAutomationController {

    private final WorkflowAutomationService service;

    public WorkflowAutomationController(WorkflowAutomationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.can('WORKFLOW_EXECUTE_LIST', 'TENANT', null)")
    public ResponseEntity<List<Automation>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.can('WORKFLOW_EXECUTE_GET', 'TENANT', null)")
    public ResponseEntity<Automation> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("@authz.can('WORKFLOW_EXECUTE_CREATE', 'TENANT', null)")
    public ResponseEntity<Automation> create(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Automation a = service.create(
                (String) body.get("nom"),
                (String) body.getOrDefault("description", ""),
                (String) body.get("triggerType"),
                (String) body.getOrDefault("triggerConfig", "{}"),
                (String) body.get("actionType"),
                (String) body.getOrDefault("actionConfig", "{}"),
                userId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(a);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@authz.can('WORKFLOW_EXECUTE_TOGGLE', 'TENANT', null)")
    public ResponseEntity<Automation> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(service.toggleStatut(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can('WORKFLOW_EXECUTE_DELETEAUTOMATION', 'TENANT', null)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
