package com.discipolat.modules.workflow.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G2.5 — Édition des étapes et transitions d'un workflow (éditeur visuel / drag & drop).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowDesignService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkflowStep> listSteps(UUID tenantId, UUID workflowId) {
        requireWorkflow(tenantId, workflowId);
        return stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId);
    }

    public WorkflowStep addStep(UUID tenantId, UUID workflowId, StepCommand command) {
        requireWorkflow(tenantId, workflowId);
        if (command.stepType() == null) throw new IllegalArgumentException("stepType est obligatoire");
        Integer order = command.stepOrder() != null ? command.stepOrder() : nextOrder(workflowId);
        WorkflowStep step = WorkflowStep.builder()
                .workflowId(workflowId)
                .stepOrder(order)
                .stepType(command.stepType())
                .name(command.name() != null ? command.name() : command.stepType().name())
                .conditionsJson(command.conditions() != null ? command.conditions() : Map.of())
                .assigneeRole(command.assigneeRole())
                .assigneeScope(command.assigneeScope())
                .timeoutHours(command.timeoutHours())
                .escalationRole(command.escalationRole())
                .autoActionJson(command.autoAction() != null ? command.autoAction() : Map.of())
                .build();
        WorkflowStep saved = stepRepository.save(step);
        auditService.logSimple("WORKFLOW_STEP_ADDED", "WORKFLOW_STEP", saved.getId());
        return saved;
    }

    public WorkflowStep updateStep(UUID tenantId, UUID stepId, StepCommand command) {
        WorkflowStep step = requireStep(tenantId, stepId);
        if (command.name() != null) step.setName(command.name());
        if (command.stepOrder() != null) step.setStepOrder(command.stepOrder());
        if (command.stepType() != null) step.setStepType(command.stepType());
        if (command.conditions() != null) step.setConditionsJson(command.conditions());
        if (command.assigneeRole() != null) step.setAssigneeRole(command.assigneeRole());
        if (command.assigneeScope() != null) step.setAssigneeScope(command.assigneeScope());
        if (command.timeoutHours() != null) step.setTimeoutHours(command.timeoutHours());
        if (command.escalationRole() != null) step.setEscalationRole(command.escalationRole());
        if (command.autoAction() != null) step.setAutoActionJson(command.autoAction());
        WorkflowStep saved = stepRepository.save(step);
        auditService.logSimple("WORKFLOW_STEP_UPDATED", "WORKFLOW_STEP", saved.getId());
        return saved;
    }

    public void deleteStep(UUID tenantId, UUID stepId) {
        WorkflowStep step = requireStep(tenantId, stepId);
        stepRepository.delete(step);
        auditService.logSimple("WORKFLOW_STEP_DELETED", "WORKFLOW_STEP", stepId);
    }

    public WorkflowTransition addTransition(UUID tenantId, UUID workflowId, TransitionCommand command) {
        requireWorkflow(tenantId, workflowId);
        requireStep(tenantId, command.fromStepId());
        requireStep(tenantId, command.toStepId());
        WorkflowTransition transition = WorkflowTransition.builder()
                .workflowId(workflowId)
                .fromStepId(command.fromStepId())
                .toStepId(command.toStepId())
                .onEvent(command.onEvent() != null ? command.onEvent() : WorkflowEngineService.EVENT_APPROVE)
                .build();
        WorkflowTransition saved = transitionRepository.save(transition);
        auditService.logSimple("WORKFLOW_TRANSITION_ADDED", "WORKFLOW_TRANSITION", saved.getId());
        return saved;
    }

    public void deleteTransition(UUID tenantId, UUID transitionId) {
        WorkflowTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowTransition", transitionId));
        requireWorkflow(tenantId, transition.getWorkflowId());
        transitionRepository.delete(transition);
        auditService.logSimple("WORKFLOW_TRANSITION_DELETED", "WORKFLOW_TRANSITION", transitionId);
    }

    private Integer nextOrder(UUID workflowId) {
        return stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId).stream()
                .map(WorkflowStep::getStepOrder)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private WorkflowDefinition requireWorkflow(UUID tenantId, UUID workflowId) {
        return definitionRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowDefinition", workflowId));
    }

    /** Vérifie que l'étape appartient bien à un workflow du tenant courant. */
    private WorkflowStep requireStep(UUID tenantId, UUID stepId) {
        WorkflowStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowStep", stepId));
        requireWorkflow(tenantId, step.getWorkflowId());
        return step;
    }

    public record StepCommand(
            Integer stepOrder,
            WorkflowStepType stepType,
            String name,
            Map<String, Object> conditions,
            String assigneeRole,
            String assigneeScope,
            Integer timeoutHours,
            String escalationRole,
            Map<String, Object> autoAction
    ) {}

    public record TransitionCommand(
            UUID fromStepId,
            UUID toStepId,
            String onEvent
    ) {}
}