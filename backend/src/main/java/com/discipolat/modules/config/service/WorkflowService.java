package com.discipolat.modules.config.service;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.config.domain.WorkflowDefinition;
import com.discipolat.modules.config.domain.WorkflowInstance;
import com.discipolat.modules.config.domain.WorkflowStep;
import com.discipolat.modules.config.domain.WorkflowTask;
import com.discipolat.modules.config.domain.WorkflowTransition;
import com.discipolat.modules.config.repository.WorkflowDefinitionRepository;
import com.discipolat.modules.config.repository.WorkflowInstanceRepository;
import com.discipolat.modules.config.repository.WorkflowStepRepository;
import com.discipolat.modules.config.repository.WorkflowTaskRepository;
import com.discipolat.modules.config.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;

    public WorkflowService(WorkflowDefinitionRepository definitionRepository,
                           WorkflowStepRepository stepRepository,
                           WorkflowTransitionRepository transitionRepository,
                           WorkflowInstanceRepository instanceRepository,
                           WorkflowTaskRepository taskRepository) {
        this.definitionRepository = definitionRepository;
        this.stepRepository = stepRepository;
        this.transitionRepository = transitionRepository;
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
    }

    // ========== DEFINITIONS ==========

    public List<WorkflowDefinition> getDefinitions(UUID tenantId, UUID spaceId) {
        return definitionRepository.findResolvedDefinitions(tenantId, spaceId);
    }

    public WorkflowDefinition getDefinition(UUID id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowDefinition", id));
    }

    public WorkflowDefinition createDefinition(WorkflowDefinition def) {
        if (definitionRepository.existsById(def.getId())) {
            throw new IllegalArgumentException("Workflow existe déjà");
        }
        return definitionRepository.save(def);
    }

    public WorkflowDefinition updateDefinition(UUID id, WorkflowDefinition updated) {
        WorkflowDefinition existing = getDefinition(id);
        existing.setName(updated.getName());
        existing.setEntityType(updated.getEntityType());
        existing.setEnabled(updated.getEnabled());
        existing.setVersion(existing.getVersion() + 1);
        return definitionRepository.save(existing);
    }

    public void deleteDefinition(UUID id) {
        WorkflowDefinition def = getDefinition(id);
        def.setDeletedAt(OffsetDateTime.now());
        definitionRepository.save(def);
    }

    // ========== STEPS ==========

    public List<WorkflowStep> getSteps(UUID workflowId) {
        return stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId);
    }

    public WorkflowStep addStep(UUID workflowId, WorkflowStep step) {
        step.setWorkflowId(workflowId);
        return stepRepository.save(step);
    }

    public void reorderSteps(UUID workflowId, List<UUID> stepIdsInOrder) {
        int order = 0;
        for (UUID stepId : stepIdsInOrder) {
            WorkflowStep step = stepRepository.findById(stepId)
                    .orElseThrow(() -> new EntityNotFoundException("WorkflowStep", stepId));
            step.setStepOrder(order++);
            stepRepository.save(step);
        }
    }

    // ========== TRANSITIONS ==========

    public List<WorkflowTransition> getTransitions(UUID workflowId) {
        return transitionRepository.findByWorkflowId(workflowId);
    }

    public WorkflowTransition addTransition(UUID fromStepId, UUID toStepId, String onEvent) {
        WorkflowTransition t = new WorkflowTransition();
        t.setFromStepId(fromStepId);
        t.setToStepId(toStepId);
        t.setOnEvent(onEvent);
        return transitionRepository.save(t);
    }

    // ========== INSTANCES ==========

    public WorkflowInstance startInstance(UUID tenantId, UUID workflowId, UUID entityId, UUID spaceId, UUID actorId) {
        WorkflowDefinition def = getDefinition(workflowId);
        if (!def.getEnabled()) {
            throw new IllegalStateException("Workflow désactivé");
        }

        // Get initial step (order 0)
        WorkflowStep initialStep = stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId).stream()
                .filter(s -> s.getStepOrder() == 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun step initial (order 0)"));

        WorkflowInstance instance = WorkflowInstance.builder()
                .tenantId(tenantId)
                .workflowId(workflowId)
                .entityId(entityId)
                .spaceId(spaceId)
                .currentStepId(initialStep.getId())
                .status("RUNNING")
                .build();

        WorkflowInstance saved = instanceRepository.save(instance);

        // Create task for initial step
        createTaskForStep(saved.getId(), initialStep, actorId);

        return saved;
    }

    public WorkflowInstance getInstance(UUID id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowInstance", id));
    }

    // ========== TASKS & APPROVALS ==========

    private void createTaskForStep(UUID instanceId, WorkflowStep step, UUID actorId) {
        OffsetDateTime dueAt = step.getTimeoutHours() != null
                ? OffsetDateTime.now().plusHours(step.getTimeoutHours())
                : null;

        WorkflowTask task = WorkflowTask.builder()
                .tenantId(instanceRepository.findById(instanceId).get().getTenantId())
                .instanceId(instanceId)
                .stepId(step.getId())
                .assigneeId(actorId)
                .assigneeRole(step.getAssigneeRole())
                .status("PENDING")
                .dueAt(dueAt)
                .build();

        taskRepository.save(task);
    }

    public WorkflowTask approveTask(UUID taskId, UUID actorId, String comment) {
        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowTask", taskId));

        if (!"PENDING".equals(task.getStatus()) && !"ESCALATED".equals(task.getStatus())) {
            throw new IllegalStateException("Tâche déjà traitée");
        }

        task.setStatus("APPROVED");
        task.setResolvedAt(OffsetDateTime.now());
        task.setComment(comment);
        taskRepository.save(task);

        advanceInstance(task);
        return task;
    }

    public WorkflowTask rejectTask(UUID taskId, UUID actorId, String comment) {
        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowTask", taskId));

        if (!"PENDING".equals(task.getStatus()) && !"ESCALATED".equals(task.getStatus())) {
            throw new IllegalStateException("Tâche déjà traitée");
        }

        task.setStatus("REJECTED");
        task.setResolvedAt(OffsetDateTime.now());
        task.setComment(comment);
        taskRepository.save(task);

        // Reject ends the workflow
        WorkflowInstance instance = instanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new EntityNotFoundException("WorkflowInstance", task.getInstanceId()));
        instance.setStatus("REJECTED");
        instance.setUpdatedAt(OffsetDateTime.now());
        instanceRepository.save(instance);

        return task;
    }

    public WorkflowTask escalateTask(UUID taskId, UUID actorId, String comment) {
        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowTask", taskId));

        task.setStatus("ESCALATED");
        task.setResolvedAt(OffsetDateTime.now());
        task.setComment(comment);
        taskRepository.save(task);

        // Create escalated task
        WorkflowStep step = stepRepository.findById(task.getStepId())
                .orElseThrow(() -> new EntityNotFoundException("WorkflowStep", task.getStepId()));

        if (step.getEscalationRole() != null) {
            WorkflowTask escalated = WorkflowTask.builder()
                    .tenantId(task.getTenantId())
                    .instanceId(task.getInstanceId())
                    .stepId(step.getId())
                    .assigneeRole(step.getEscalationRole())
                    .status("PENDING")
                    .escalatedFromTaskId(task.getId())
                    .build();
            taskRepository.save(escalated);
        }

        return task;
    }

    private void advanceInstance(WorkflowTask completedTask) {
        WorkflowInstance instance = instanceRepository.findById(completedTask.getInstanceId())
                .orElseThrow(() -> new EntityNotFoundException("WorkflowInstance", completedTask.getInstanceId()));

        WorkflowStep currentStep = stepRepository.findById(completedTask.getStepId())
                .orElseThrow(() -> new EntityNotFoundException("WorkflowStep", completedTask.getStepId()));

        // Find transition on APPROVED event
        WorkflowTransition transition = transitionRepository.findByFromStepId(currentStep.getId()).stream()
                .filter(t -> "APPROVED".equals(t.getOnEvent()))
                .findFirst()
                .orElse(null);

        if (transition == null) {
            // No transition = workflow complete
            instance.setStatus("COMPLETED");
            instance.setCurrentStepId(null);
            instance.setUpdatedAt(OffsetDateTime.now());
            instanceRepository.save(instance);
            return;
        }

        // Move to next step
        WorkflowStep nextStep = stepRepository.findById(transition.getToStepId())
                .orElseThrow(() -> new EntityNotFoundException("WorkflowStep", transition.getToStepId()));

        instance.setCurrentStepId(nextStep.getId());
        instance.setUpdatedAt(OffsetDateTime.now());
        instanceRepository.save(instance);

        // Create task for next step
        createTaskForStep(instance.getId(), nextStep, null);
    }

    // ========== ESCALATION CHECK (scheduled) ==========

    @Transactional
    public void checkEscalations() {
        List<WorkflowTask> overdue = taskRepository.findByStatusAndDueAtBefore("PENDING", OffsetDateTime.now());
        for (WorkflowTask task : overdue) {
            WorkflowStep step = stepRepository.findById(task.getStepId())
                    .orElse(null);
            if (step != null && step.getEscalationRole() != null) {
                escalateTask(task.getId(), null, "Escalade automatique par timeout");
            }
        }
    }

    // ========== USER TASKS ==========

    public List<WorkflowTask> getMyTasks(UUID userId, String status) {
        if (status != null) {
            return taskRepository.findByAssigneeIdAndStatus(userId, status);
        }
        return taskRepository.findByAssigneeIdOrderByDueAtAsc(userId, org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
    }
}