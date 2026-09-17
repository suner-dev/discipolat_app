package com.discipolat.modules.workflow.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.ForbiddenException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * G2.5 — Moteur de workflow configurable.
 *
 * Exécution événementielle : un événement entrant (action utilisateur ou
 * événement d'outbox §G2.8) fait avancer l'instance, crée une
 * {@link WorkflowTask}, déclenche les auto-actions de l'étape, puis escalade
 * au supérieur si le délai est dépassé.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowEngineService {

    /** Événements de transition canoniques. */
    public static final String EVENT_APPROVE = "APPROVE";
    public static final String EVENT_REJECT = "REJECT";
    public static final String EVENT_TIMEOUT = "TIMEOUT";
    public static final String EVENT_SUBMIT = "SUBMIT";

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityPropagationPublisher propagationPublisher;

    // ==================== Démarrage d'une instance ====================

    /**
     * Démarre un workflow identifié par son code, pour un objet métier donné.
     * Le workflow de l'espace primant sur celui du tenant (héritage §G1.7).
     */
    public WorkflowInstance startInstance(UUID tenantId, UUID actorId, String workflowCode,
                                          UUID entityId, UUID spaceId, UUID assigneeId) {
        WorkflowDefinition definition = resolveDefinition(tenantId, workflowCode, spaceId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowDefinition", "code", workflowCode));

        WorkflowStep firstStep = stepRepository.findFirstByWorkflowIdOrderByStepOrderAsc(definition.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Le workflow " + workflowCode + " n'a aucune étape configurée"));

        WorkflowInstance instance = WorkflowInstance.builder()
                .tenantId(tenantId)
                .workflowId(definition.getId())
                .entityId(entityId)
                .spaceId(spaceId)
                .currentStepId(firstStep.getId())
                .status(WorkflowInstanceStatus.RUNNING)
                .build();

        WorkflowInstance saved = instanceRepository.save(instance);
        auditService.logSimple("WORKFLOW_STARTED", definition.getEntityType(), entityId);
        eventPublisher.publishEvent(new WorkflowEvent("WorkflowStarted", tenantId, saved.getId(),
                null, entityId, definition.getCode()));
        enterStep(saved, firstStep, actorId, assigneeId);
        return saved;
    }

    /** Résolution du workflow : celui de l'espace (prioritaire) sinon celui du tenant. */
    @Transactional(readOnly = true)
    public Optional<WorkflowDefinition> resolveDefinition(UUID tenantId, String workflowCode, UUID spaceId) {
        if (spaceId != null) {
            Optional<WorkflowDefinition> specific = definitionRepository
                    .findFirstByTenantIdAndSpaceIdAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
                            tenantId, spaceId, workflowCode);
            if (specific.isPresent()) {
                return specific;
            }
        }
        return definitionRepository
                .findFirstByTenantIdAndSpaceIdIsNullAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
                        tenantId, workflowCode);
    }

    // ==================== Entrée dans une étape ====================

    /**
     * Entre dans une étape : crée la tâche si approbation, déclenche
     * les auto-actions, et poursuit automatiquement si l'étape est terminale.
     */
    public void enterStep(WorkflowInstance instance, WorkflowStep step, UUID actorId, UUID assigneeId) {
        instance.setCurrentStepId(step.getId());
        instanceRepository.save(instance);

        // Effet de bord générique de l'étape (EXPENSE, ASSET_STATUS, AUTO_ACTION, NOTIFY…)
        applyAutoAction(instance, step, actorId);

        boolean needsHuman = step.getStepType() == WorkflowStepType.APPROVAL
                || step.getStepType() == WorkflowStepType.FORM;

        if (needsHuman) {
            createTask(instance, step, assigneeId);
        } else {
            // Étape automatique : on avance immédiatement le long de la transition AUTO.
            advance(instance, step, EVENT_SUBMIT, actorId, assigneeId);
        }
    }

    private WorkflowTask createTask(WorkflowInstance instance, WorkflowStep step, UUID assigneeId) {
        Instant dueAt = step.getTimeoutHours() != null
                ? Instant.now().plus(Duration.ofHours(step.getTimeoutHours()))
                : null;
        WorkflowTask task = WorkflowTask.builder()
                .tenantId(instance.getTenantId())
                .instanceId(instance.getId())
                .stepId(step.getId())
                .assigneeId(assigneeId)
                .assigneeRole(step.getAssigneeRole())
                .status(WorkflowTaskStatus.PENDING)
                .dueAt(dueAt)
                .build();
        WorkflowTask saved = taskRepository.save(task);
        auditService.logSimple("WORKFLOW_TASK_CREATED", "WORKFLOW_TASK", saved.getId());
        eventPublisher.publishEvent(new WorkflowEvent("WorkflowTaskCreated", instance.getTenantId(),
                instance.getId(), saved.getId(), instance.getEntityId(), step.getName()));
        return saved;
    }

    private void applyAutoAction(WorkflowInstance instance, WorkflowStep step, UUID actorId) {
        Map<String, Object> action = step.getAutoActionJson();
        if (action == null || action.isEmpty()) {
            return;
        }
        String kind = String.valueOf(action.getOrDefault("type", step.getStepType().name()));
        auditService.logSimple("WORKFLOW_AUTO_ACTION", "WORKFLOW_STEP", step.getId());
        // Relayé à l'outbox §G2.8 : les consumers FINANCE / ASSET_STATUS / NOTIFY
        // appliquent l'effet métier (création de dépense, changement de statut…).
        eventPublisher.publishEvent(new WorkflowEvent("WorkflowAutoAction", instance.getTenantId(),
                instance.getId(), null, instance.getEntityId(), kind + ":" + step.getName()));
    }

    // ==================== Progression ====================

    /**
     * Fait avancer une instance depuis une étape selon l'événement fourni.
     * Retourne true si l'instance est terminée (dernière étape franchie).
     */
    public boolean advance(WorkflowInstance instance, WorkflowStep fromStep, String onEvent,
                           UUID actorId, UUID assigneeId) {
        Optional<WorkflowTransition> transition = transitionRepository
                .findFirstByFromStepIdAndOnEvent(fromStep.getId(), onEvent);

        WorkflowStep nextStep = transition
                .flatMap(t -> stepRepository.findById(t.getToStepId()))
                .or(() -> onEvent.equals(EVENT_APPROVE) || onEvent.equals(EVENT_SUBMIT)
                        ? stepRepository.findFirstByWorkflowIdAndStepOrderGreaterThanOrderByStepOrderAsc(
                                fromStep.getWorkflowId(), fromStep.getStepOrder())
                        : Optional.empty())
                .orElse(null);

        if (nextStep == null) {
            instance.setStatus(onEvent.equals(EVENT_REJECT)
                    ? WorkflowInstanceStatus.REJECTED
                    : WorkflowInstanceStatus.APPROVED);
            instance.setCurrentStepId(null);
            instanceRepository.save(instance);
            auditService.logSimple(onEvent.equals(EVENT_REJECT) ? "WORKFLOW_REJECTED" : "WORKFLOW_APPROVED",
                    "WORKFLOW_INSTANCE", instance.getId());
            eventPublisher.publishEvent(new WorkflowEvent(
                    onEvent.equals(EVENT_REJECT) ? "WorkflowRejected" : "WorkflowApproved",
                    instance.getTenantId(), instance.getId(), null, instance.getEntityId(), fromStep.getName()));
            return true;
        }

        enterStep(instance, nextStep, actorId, assigneeId);
        return false;
    }

    // ==================== Approbation / rejet ====================

    /** Approuve une tâche et fait avancer le workflow (scénario « approbation simple »). */
    public WorkflowInstance approve(UUID tenantId, UUID actorId, UUID taskId, String comment) {
        WorkflowTask task = requirePendingTask(tenantId, taskId);
        WorkflowInstance instance = requireInstance(tenantId, task.getInstanceId());
        WorkflowStep step = stepRepository.findById(task.getStepId())
                .orElseThrow(() -> new EntityNotFoundException("WorkflowStep", task.getStepId()));

        task.setStatus(WorkflowTaskStatus.APPROVED);
        task.setResolvedAt(Instant.now());
        if (comment != null) task.setComment(comment);
        taskRepository.save(task);
        auditService.logSimple("WORKFLOW_TASK_APPROVED", "WORKFLOW_TASK", taskId);

        advance(instance, step, EVENT_APPROVE, actorId, task.getAssigneeId());
        propagationPublisher.publishStatusChanged("WORKFLOW_INSTANCE", instance.getId(),
                "RUNNING", instance.getStatus().name(), "Workflow approuvé à l'étape " + step.getName());
        return instance;
    }

    /** Rejette une tâche (scénario « rejet ») : l'instance passe en REJECTED. */
    public WorkflowInstance reject(UUID tenantId, UUID actorId, UUID taskId, String comment) {
        WorkflowTask task = requirePendingTask(tenantId, taskId);
        WorkflowInstance instance = requireInstance(tenantId, task.getInstanceId());
        WorkflowStep step = stepRepository.findById(task.getStepId())
                .orElseThrow(() -> new EntityNotFoundException("WorkflowStep", task.getStepId()));

        task.setStatus(WorkflowTaskStatus.REJECTED);
        task.setResolvedAt(Instant.now());
        if (comment != null) task.setComment(comment);
        taskRepository.save(task);
        auditService.logSimple("WORKFLOW_TASK_REJECTED", "WORKFLOW_TASK", taskId);

        advance(instance, step, EVENT_REJECT, actorId, task.getAssigneeId());
        propagationPublisher.publishStatusChanged("WORKFLOW_INSTANCE", instance.getId(),
                "RUNNING", instance.getStatus().name(), "Workflow rejeté à l'étape " + step.getName());
        return instance;
    }

    /** Ajoute un commentaire sans résoudre la tâche. */
    public WorkflowTask addComment(UUID tenantId, UUID taskId, String comment) {
        WorkflowTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowTask", taskId));
        task.setComment(comment);
        WorkflowTask saved = taskRepository.save(task);
        auditService.logSimple("WORKFLOW_TASK_COMMENTED", "WORKFLOW_TASK", taskId);
        return saved;
    }

    /** Remet une instance à sa première étape (annule les tâches en attente). */
    public WorkflowInstance reset(UUID tenantId, UUID actorId, UUID instanceId) {
        WorkflowInstance instance = requireInstance(tenantId, instanceId);
        taskRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId).stream()
                .filter(t -> t.getStatus() == WorkflowTaskStatus.PENDING)
                .forEach(t -> {
                    t.setStatus(WorkflowTaskStatus.CANCELLED);
                    t.setResolvedAt(Instant.now());
                    taskRepository.save(t);
                });
        WorkflowStep first = stepRepository.findFirstByWorkflowIdOrderByStepOrderAsc(instance.getWorkflowId())
                .orElseThrow(() -> new IllegalStateException("Workflow sans étape"));
        instance.setStatus(WorkflowInstanceStatus.RUNNING);
        enterStep(instance, first, actorId, null);
        auditService.logSimple("WORKFLOW_RESET", "WORKFLOW_INSTANCE", instanceId);
        return instance;
    }

    // ==================== Escalade par dépassement de délai ====================

    /**
     * Scénario « escalade par timeout » : toute tâche PENDING dont le délai est
     * dépassé est escaladée au rôle supérieur et une nouvelle tâche est créée.
     * Retourne le nombre de tâches escaladées.
     */
    public int escalateOverdueTasks(UUID tenantId, Instant now) {
        List<WorkflowTask> overdue = taskRepository
                .findByTenantIdAndStatusAndDueAtBefore(tenantId, WorkflowTaskStatus.PENDING, now);
        int escalatedCount = 0;

        for (WorkflowTask task : overdue) {
            WorkflowStep step = stepRepository.findById(task.getStepId()).orElse(null);

            task.setStatus(WorkflowTaskStatus.ESCALATED);
            task.setResolvedAt(now);
            taskRepository.save(task);

            WorkflowInstance instance = instanceRepository
                    .findByIdAndTenantId(task.getInstanceId(), tenantId).orElse(null);
            if (instance == null) {
                escalatedCount++;
                continue;
            }
            instance.setStatus(WorkflowInstanceStatus.ESCALATED);
            instanceRepository.save(instance);

            String escalationRole = step != null ? step.getEscalationRole() : null;
            Integer timeout = step != null ? step.getTimeoutHours() : null;

            WorkflowTask escalated = WorkflowTask.builder()
                    .tenantId(tenantId)
                    .instanceId(instance.getId())
                    .stepId(task.getStepId())
                    .assigneeRole(escalationRole)
                    .status(WorkflowTaskStatus.PENDING)
                    .dueAt(timeout != null ? now.plus(Duration.ofHours(timeout)) : null)
                    .escalatedFromTaskId(task.getId())
                    .build();
            WorkflowTask savedEscalated = taskRepository.save(escalated);

            auditService.logSimple("WORKFLOW_TASK_ESCALATED", "WORKFLOW_TASK", task.getId());
            eventPublisher.publishEvent(new WorkflowEvent("WorkflowTaskEscalated", tenantId,
                    instance.getId(), savedEscalated.getId(), instance.getEntityId(), escalationRole));
            propagationPublisher.publishStatusChanged("WORKFLOW_TASK", task.getId(),
                    "PENDING", "ESCALATED", "Escalade vers " + escalationRole);
            escalatedCount++;
        }
        return escalatedCount;
    }

    // ==================== Helpers ====================

    private WorkflowTask requirePendingTask(UUID tenantId, UUID taskId) {
        WorkflowTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowTask", taskId));
        if (task.getStatus() != WorkflowTaskStatus.PENDING) {
            throw new IllegalStateException("La tâche " + taskId + " est déjà " + task.getStatus());
        }
        return task;
    }

    private WorkflowInstance requireInstance(UUID tenantId, UUID instanceId) {
        return instanceRepository.findByIdAndTenantId(instanceId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowInstance", instanceId));
    }

    /** Timeline d'une instance : étapes franchies, tâches et résolutions. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getInstanceTimeline(UUID tenantId, UUID instanceId) {
        WorkflowInstance instance = requireInstance(tenantId, instanceId);
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (WorkflowTask task : taskRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId)) {
            WorkflowStep step = stepRepository.findById(task.getStepId()).orElse(null);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("taskId", task.getId());
            entry.put("stepId", task.getStepId());
            entry.put("stepName", step != null ? step.getName() : null);
            entry.put("stepType", step != null ? step.getStepType() : null);
            entry.put("assigneeId", task.getAssigneeId());
            entry.put("assigneeRole", task.getAssigneeRole());
            entry.put("status", task.getStatus());
            entry.put("dueAt", task.getDueAt());
            entry.put("resolvedAt", task.getResolvedAt());
            entry.put("comment", task.getComment());
            entry.put("escalatedFromTaskId", task.getEscalatedFromTaskId());
            entry.put("createdAt", task.getCreatedAt());
            timeline.add(entry);
        }
        return timeline;
    }

    /** Tâches en attente pour un acteur : volet « Mon approbation » (web + mobile). */
    @Transactional(readOnly = true)
    public List<WorkflowTask> getPendingTasksFor(UUID tenantId, UUID actorId) {
        return taskRepository.findByTenantIdAndAssigneeIdAndStatus(tenantId, actorId, WorkflowTaskStatus.PENDING);
    }

    /** Graphe de définition : nœuds (étapes) + transitions, pour l'éditeur visuel. */
    @Transactional(readOnly = true)
    public Map<String, Object> getDefinitionGraph(UUID tenantId, UUID workflowId) {
        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowDefinition", workflowId));
        List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId);
        List<WorkflowTransition> transitions = transitionRepository.findByWorkflowId(workflowId);

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("definition", definition);
        graph.put("nodes", steps);
        graph.put("edges", transitions);
        return graph;
    }
}