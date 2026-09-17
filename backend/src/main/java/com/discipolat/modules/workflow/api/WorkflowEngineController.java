package com.discipolat.modules.workflow.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.workflow.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G2.5 — API du moteur de workflow configurable (définitions, éditeur visuel,
 * instances, approbations, escalades).
 */
@RestController
@RequestMapping("/api/v1/workflow-engine")
public class WorkflowEngineController {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowDesignService designService;
    private final WorkflowEngineService engine;

    public WorkflowEngineController(WorkflowDefinitionService definitionService,
                                    WorkflowDesignService designService,
                                    WorkflowEngineService engine) {
        this.definitionService = definitionService;
        this.designService = designService;
        this.engine = engine;
    }

    private UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
        return tenantId;
    }

    // ==================== Définitions ====================

    @GetMapping("/definitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkflowDefinition>> listDefinitions() {
        return ResponseEntity.ok(definitionService.list(tenantId()));
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<WorkflowDefinition> createDefinition(@RequestBody DefinitionRequest request) {
        return ResponseEntity.ok(definitionService.create(tenantId(), request.toCommand()));
    }

    @GetMapping("/definitions/{workflowId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkflowDefinition> getDefinition(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(definitionService.get(tenantId(), workflowId));
    }

    @PutMapping("/definitions/{workflowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<WorkflowDefinition> updateDefinition(@PathVariable UUID workflowId,
                                                              @RequestBody DefinitionRequest request) {
        return ResponseEntity.ok(definitionService.update(tenantId(), workflowId, request.toCommand()));
    }

    @DeleteMapping("/definitions/{workflowId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> deleteDefinition(@PathVariable UUID workflowId) {
        definitionService.delete(tenantId(), workflowId);
        return ResponseEntity.noContent().build();
    }

    /** Graphe complet (nœuds + transitions) pour l'éditeur visuel. */
    @GetMapping("/definitions/{workflowId}/graph")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> graph(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(engine.getDefinitionGraph(tenantId(), workflowId));
    }

    // ==================== Étapes & transitions (éditeur) ====================

    @GetMapping("/definitions/{workflowId}/steps")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkflowStep>> listSteps(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(designService.listSteps(tenantId(), workflowId));
    }

    @PostMapping("/definitions/{workflowId}/steps")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<WorkflowStep> addStep(@PathVariable UUID workflowId, @RequestBody StepRequest request) {
        return ResponseEntity.ok(designService.addStep(tenantId(), workflowId, request.toCommand()));
    }

    @PutMapping("/steps/{stepId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<WorkflowStep> updateStep(@PathVariable UUID stepId, @RequestBody StepRequest request) {
        return ResponseEntity.ok(designService.updateStep(tenantId(), stepId, request.toCommand()));
    }

    @DeleteMapping("/steps/{stepId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Void> deleteStep(@PathVariable UUID stepId) {
        designService.deleteStep(tenantId(), stepId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/definitions/{workflowId}/transitions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<WorkflowTransition> addTransition(@PathVariable UUID workflowId,
                                                            @RequestBody TransitionRequest request) {
        return ResponseEntity.ok(designService.addTransition(tenantId(), workflowId, request.toCommand()));
    }

    @DeleteMapping("/transitions/{transitionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Void> deleteTransition(@PathVariable UUID transitionId) {
        designService.deleteTransition(tenantId(), transitionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Instances & tâches ====================

    @PostMapping("/instances")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkflowInstance> startInstance(@RequestBody StartInstanceRequest request) {
        WorkflowInstance instance = engine.startInstance(tenantId(), SecurityUtils.getCurrentUserId(),
                request.workflowCode(), request.entityId(), request.spaceId(), request.assigneeId());
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/instances/{instanceId}/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> timeline(@PathVariable UUID instanceId) {
        return ResponseEntity.ok(engine.getInstanceTimeline(tenantId(), instanceId));
    }

    @PostMapping("/instances/{instanceId}/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<WorkflowInstance> reset(@PathVariable UUID instanceId) {
        return ResponseEntity.ok(engine.reset(tenantId(), SecurityUtils.getCurrentUserId(), instanceId));
    }

    /** Volet « Mon approbation ». */
    @GetMapping("/tasks/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkflowTask>> pendingTasks() {
        return ResponseEntity.ok(engine.getPendingTasksFor(tenantId(), SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/tasks/{taskId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkflowInstance> approve(@PathVariable UUID taskId,
                                                    @RequestBody(required = false) DecisionRequest request) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(engine.approve(tenantId(), SecurityUtils.getCurrentUserId(), taskId, comment));
    }

    @PostMapping("/tasks/{taskId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkflowInstance> reject(@PathVariable UUID taskId,
                                                   @RequestBody(required = false) DecisionRequest request) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(engine.reject(tenantId(), SecurityUtils.getCurrentUserId(), taskId, comment));
    }

    @PostMapping("/tasks/{taskId}/comment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkflowTask> comment(@PathVariable UUID taskId,
                                               @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(engine.addComment(tenantId(), taskId, request.comment()));
    }

    /** Déclenche l'escalade des tâches en retard (appelé par un job planifié ou un admin). */
    @PostMapping("/escalations/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> runEscalations() {
        int count = engine.escalateOverdueTasks(tenantId(), Instant.now());
        return ResponseEntity.ok(Map.of("escalated", count, "ranAt", Instant.now().toString()));
    }

    // ==================== DTOs ====================

    public record DefinitionRequest(
            String entityType,
            String code,
            String name,
            UUID spaceId,
            Boolean enabled
    ) {
        WorkflowDefinitionService.DefinitionCommand toCommand() {
            return new WorkflowDefinitionService.DefinitionCommand(entityType, code, name, spaceId, enabled);
        }
    }

    public record StepRequest(
            Integer stepOrder,
            WorkflowStepType stepType,
            String name,
            Map<String, Object> conditions,
            String assigneeRole,
            String assigneeScope,
            Integer timeoutHours,
            String escalationRole,
            Map<String, Object> autoAction
    ) {
        WorkflowDesignService.StepCommand toCommand() {
            return new WorkflowDesignService.StepCommand(stepOrder, stepType, name, conditions,
                    assigneeRole, assigneeScope, timeoutHours, escalationRole, autoAction);
        }
    }

    public record TransitionRequest(
            UUID fromStepId,
            UUID toStepId,
            String onEvent
    ) {
        WorkflowDesignService.TransitionCommand toCommand() {
            return new WorkflowDesignService.TransitionCommand(fromStepId, toStepId, onEvent);
        }
    }

    public record StartInstanceRequest(
            String workflowCode,
            UUID entityId,
            UUID spaceId,
            UUID assigneeId
    ) {}

    public record DecisionRequest(String comment) {}
}