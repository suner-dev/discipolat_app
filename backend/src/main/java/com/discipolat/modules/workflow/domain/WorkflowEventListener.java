package com.discipolat.modules.workflow.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.notifications.domain.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * §G2.5 promis « consommé par le temps réel (G2.10) et l'audit (G2.9) »,
 * §G6.4 chemin critique « workflow approbation » : ce relais transforme les
 * événements du moteur en EFFETS RÉELS — notification in-app du destinataire
 * (tâche assignée / escaladée) et TaskAssigned via l'outbox (relay STOMP).
 * Défectif par conception : une anomalie de notification ne casse jamais le
 * workflow (le taskRepository est déjà cohérent à l'émission).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventListener {

    private final WorkflowTaskRepository taskRepository;
    private final NotificationService notificationService;
    private final OutboxPublisher outboxPublisher;

    @EventListener
    public void onWorkflowEvent(WorkflowEvent event) {
        try {
            switch (event.getEventType()) {
                case "WorkflowTaskCreated" -> handleTaskCreated(event);
                case "WorkflowTaskEscalated" -> handleTaskCreated(event);
                default -> {
                    // WorkflowApproved/Rejected/AutoAction : déjà propagés
                    // (publishStatusChanged + outbox ASSET via le moteur).
                }
            }
        } catch (RuntimeException e) {
            log.warn("Effet temps réel/notification workflow ignore ({}): {}",
                    event.getEventType(), e.getMessage());
        }
    }

    private void handleTaskCreated(WorkflowEvent event) {
        if (event.getTaskId() == null) {
            return;
        }
        taskRepository.findById(event.getTaskId()).ifPresent(task -> {
            boolean escalated = "WorkflowTaskEscalated".equals(event.getEventType());
            if (task.getAssigneeId() != null) {
                notificationService.create(event.getTenantId(), task.getAssigneeId(),
                        TypeNotification.TACHE_ASSIGNEE, CanalNotification.IN_APP,
                        escalated ? "Tâche escaladée" : "Nouvelle tâche d'approbation",
                        (escalated ? "Cette tâche vous a été escaladée : " : "Tâche assignée : ")
                                + (event.getDetail() != null ? event.getDetail() : task.getId()),
                        task.getId(), "WORKFLOW_TASK");
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", task.getId().toString());
            payload.put("instanceId", String.valueOf(task.getInstanceId()));
            if (task.getAssigneeId() != null) payload.put("assigneeId", task.getAssigneeId().toString());
            payload.put("escalated", escalated);
            outboxPublisher.publish(event.getTenantId(), "WORKFLOW_TASK", task.getId(),
                    "TaskAssigned", payload);
        });
    }
}
