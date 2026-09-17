package com.discipolat.modules.workflow.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * G2.5 — DoD : « 3 scénarios de test complets (approbation simple, rejet,
 * escalade par timeout) ».
 */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceTest {

    @Mock private WorkflowDefinitionRepository definitionRepository;
    @Mock private WorkflowStepRepository stepRepository;
    @Mock private WorkflowTransitionRepository transitionRepository;
    @Mock private WorkflowInstanceRepository instanceRepository;
    @Mock private WorkflowTaskRepository taskRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityPropagationPublisher entityPropagationPublisher;

    private WorkflowEngineService engine;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ASSIGNEE = UUID.randomUUID();
    private static final UUID ENTITY = UUID.randomUUID();
    private static final String CODE = "MATERIAL_REQUEST";

    private WorkflowDefinition definition;
    private WorkflowStep approvalStep;

    @BeforeEach
    void setUp() {
        engine = new WorkflowEngineService(definitionRepository, stepRepository, transitionRepository,
                instanceRepository, taskRepository, auditService, eventPublisher, entityPropagationPublisher);

        definition = WorkflowDefinition.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).entityType("ASSET")
                .code(CODE).name("Demande de matériel").enabled(true).version(1).build();

        approvalStep = WorkflowStep.builder()
                .id(UUID.randomUUID()).workflowId(definition.getId()).stepOrder(0)
                .stepType(WorkflowStepType.APPROVAL).name("Validation responsable")
                .assigneeRole("CHEF_DEPARTEMENT").escalationRole("PASTEUR")
                .timeoutHours(24).build();
    }

    private void stubStartDependencies() {
        when(definitionRepository
                .findFirstByTenantIdAndSpaceIdIsNullAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
                        TENANT, CODE))
                .thenReturn(Optional.of(definition));
        when(stepRepository.findFirstByWorkflowIdOrderByStepOrderAsc(definition.getId()))
                .thenReturn(Optional.of(approvalStep));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> {
            WorkflowInstance i = inv.getArgument(0);
            if (i.getId() == null) i.setId(UUID.randomUUID());
            return i;
        });
        when(taskRepository.save(any(WorkflowTask.class))).thenAnswer(inv -> {
            WorkflowTask t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
    }

    private WorkflowTask captureCreatedTask() {
        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(taskRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(t -> t.getStatus() == WorkflowTaskStatus.PENDING)
                .findFirst().orElseThrow();
    }

    // ---------- Scénario 1 : approbation simple ----------

    @Test
    void scenario1_approbationSimple_aboutitAuStatutApprouve() {
        stubStartDependencies();

        WorkflowInstance instance = engine.startInstance(TENANT, ACTOR, CODE, ENTITY, null, ASSIGNEE);

        assertEquals(WorkflowInstanceStatus.RUNNING, instance.getStatus());
        WorkflowTask task = captureCreatedTask();
        assertEquals(WorkflowTaskStatus.PENDING, task.getStatus());
        assertEquals("CHEF_DEPARTEMENT", task.getAssigneeRole());
        assertNotNull(task.getDueAt(), "le délai d'escalade est positionné depuis timeoutHours");

        // L'approbateur approuve
        when(taskRepository.findByIdAndTenantId(task.getId(), TENANT)).thenReturn(Optional.of(task));
        when(instanceRepository.findByIdAndTenantId(instance.getId(), TENANT)).thenReturn(Optional.of(instance));
        when(stepRepository.findById(approvalStep.getId())).thenReturn(Optional.of(approvalStep));
        when(transitionRepository.findFirstByFromStepIdAndOnEvent(approvalStep.getId(), "APPROVE"))
                .thenReturn(Optional.empty());
        when(stepRepository.findFirstByWorkflowIdAndStepOrderGreaterThanOrderByStepOrderAsc(
                definition.getId(), 0)).thenReturn(Optional.empty());

        engine.approve(TENANT, ASSIGNEE, task.getId(), "OK matériel disponible");

        assertEquals(WorkflowTaskStatus.APPROVED, task.getStatus());
        assertEquals(WorkflowInstanceStatus.APPROVED, instance.getStatus());
        verify(auditService).logSimple(eq("WORKFLOW_TASK_APPROVED"), eq("WORKFLOW_TASK"), any());
    }

    // ---------- Scénario 2 : rejet ----------

    @Test
    void scenario2_rejet_conclutLeWorkflowEnRejete() {
        stubStartDependencies();

        WorkflowInstance instance = engine.startInstance(TENANT, ACTOR, CODE, ENTITY, null, ASSIGNEE);
        WorkflowTask task = captureCreatedTask();

        when(taskRepository.findByIdAndTenantId(task.getId(), TENANT)).thenReturn(Optional.of(task));
        when(instanceRepository.findByIdAndTenantId(instance.getId(), TENANT)).thenReturn(Optional.of(instance));
        when(stepRepository.findById(approvalStep.getId())).thenReturn(Optional.of(approvalStep));
        when(transitionRepository.findFirstByFromStepIdAndOnEvent(approvalStep.getId(), "REJECT"))
                .thenReturn(Optional.empty());

        engine.reject(TENANT, ASSIGNEE, task.getId(), "Budget insuffisant");

        assertEquals(WorkflowTaskStatus.REJECTED, task.getStatus());
        assertEquals("Budget insuffisant", task.getComment());
        assertNotNull(task.getResolvedAt());
        assertEquals(WorkflowInstanceStatus.REJECTED, instance.getStatus());
        verify(auditService).logSimple(eq("WORKFLOW_TASK_REJECTED"), eq("WORKFLOW_TASK"), any());
    }

    // ---------- Scénario 3 : escalade par timeout ----------

    @Test
    void scenario3_escaladeParTimeout_reassigneAuRoleSuperieur() {
        UUID instanceId = UUID.randomUUID();
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(instanceId).tenantId(TENANT).workflowId(definition.getId())
                .entityId(ENTITY).currentStepId(approvalStep.getId())
                .status(WorkflowInstanceStatus.RUNNING).build();

        WorkflowTask overdue = WorkflowTask.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).instanceId(instanceId)
                .stepId(approvalStep.getId()).assigneeId(ASSIGNEE).assigneeRole("CHEF_DEPARTEMENT")
                .status(WorkflowTaskStatus.PENDING)
                .dueAt(Instant.now().minusSeconds(3600))
                .build();

        Instant now = Instant.now();
        when(taskRepository.findByTenantIdAndStatusAndDueAtBefore(TENANT, WorkflowTaskStatus.PENDING, now))
                .thenReturn(List.of(overdue));
        when(stepRepository.findById(approvalStep.getId())).thenReturn(Optional.of(approvalStep));
        when(instanceRepository.findByIdAndTenantId(instanceId, TENANT)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(WorkflowTask.class))).thenAnswer(inv -> {
            WorkflowTask t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });

        int escalated = engine.escalateOverdueTasks(TENANT, now);

        assertEquals(1, escalated);
        assertEquals(WorkflowTaskStatus.ESCALATED, overdue.getStatus());
        assertEquals(WorkflowInstanceStatus.ESCALATED, instance.getStatus());

        ArgumentCaptor<WorkflowTask> captor = ArgumentCaptor.forClass(WorkflowTask.class);
        verify(taskRepository, atLeastOnce()).save(captor.capture());
        WorkflowTask newTask = captor.getAllValues().stream()
                .filter(t -> t.getEscalatedFromTaskId() != null)
                .findFirst().orElseThrow();
        assertEquals("PASTEUR", newTask.getAssigneeRole(), "escalade vers le rôle supérieur");
        assertEquals(overdue.getId(), newTask.getEscalatedFromTaskId());
        assertEquals(WorkflowTaskStatus.PENDING, newTask.getStatus());
        verify(auditService).logSimple(eq("WORKFLOW_TASK_ESCALATED"), eq("WORKFLOW_TASK"), any());
    }

    // ---------- Garde-fous ----------

    @Test
    void approve_tacheDejaResolue_estRefusee() {
        WorkflowTask task = WorkflowTask.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).status(WorkflowTaskStatus.APPROVED).build();
        when(taskRepository.findByIdAndTenantId(task.getId(), TENANT)).thenReturn(Optional.of(task));

        assertThrows(IllegalStateException.class,
                () -> engine.approve(TENANT, ACTOR, task.getId(), "double approbation"));
    }

    @Test
    void startInstance_workflowInconnu_leveNotFound() {
        when(definitionRepository
                .findFirstByTenantIdAndSpaceIdIsNullAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
                        any(), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> engine.startInstance(TENANT, ACTOR, "INCONNU", ENTITY, null, null));
    }

    @Test
    void startInstance_workflowDeLEspacePrioritaire() {
        UUID space = UUID.randomUUID();
        when(definitionRepository
                .findFirstByTenantIdAndSpaceIdAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
                        TENANT, space, CODE))
                .thenReturn(Optional.of(definition));
        when(stepRepository.findFirstByWorkflowIdOrderByStepOrderAsc(definition.getId()))
                .thenReturn(Optional.of(approvalStep));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> {
            WorkflowInstance i = inv.getArgument(0);
            if (i.getId() == null) i.setId(UUID.randomUUID());
            return i;
        });
        when(taskRepository.save(any(WorkflowTask.class))).thenAnswer(inv -> {
            WorkflowTask t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });

        WorkflowInstance instance = engine.startInstance(TENANT, ACTOR, CODE, ENTITY, space, ASSIGNEE);

        assertEquals(space, instance.getSpaceId());
        verify(definitionRepository, never())
                .findFirstByTenantIdAndSpaceIdIsNullAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
                        any(), anyString());
    }
}