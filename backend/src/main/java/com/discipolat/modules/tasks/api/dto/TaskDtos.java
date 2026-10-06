package com.discipolat.modules.tasks.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de l'API Tasks (V234). Les noms de composants reproduisent
 * EXACTEMENT les clés {@code toJson()}/{@code fromJson()} du mobile
 * ({@code task_model.g.dart}) — aucune transformation de nom côté client.
 *
 * <p>Regroupés dans une seule classe conteneur pour rester additifs et
 * faciles à auditer face au contrat mobile.
 */
public final class TaskDtos {

    private TaskDtos() {
    }

    // ==================== VUES (réponses) ====================

    /** Miroir de {@code Task.toJson()} mobile. */
    public record TaskView(
            UUID id,
            String title,
            String description,
            String type,
            String priority,
            String status,
            UUID projectId,
            String projectName,
            UUID assignedToId,
            String assignedToName,
            UUID assignedById,
            String assignedByName,
            UUID departmentId,
            String departmentName,
            Instant dueDate,
            Instant startDate,
            Instant completedDate,
            Integer estimatedHours,
            Integer actualHours,
            List<String> tags,
            List<TaskAttachmentView> attachments,
            List<TaskCommentView> comments,
            List<TaskView> subtasks,
            List<TaskDependencyView> dependencies,
            UUID parentTaskId,
            String parentTaskTitle,
            UUID recurrenceRuleId,
            String recurrencePattern,
            Instant recurrenceEndDate,
            Instant createdAt,
            Instant updatedAt) {
    }

    /** Miroir de {@code TaskAttachment.toJson()} mobile. */
    public record TaskAttachmentView(
            UUID id,
            UUID taskId,
            String fileName,
            String fileUrl,
            String mimeType,
            long fileSize,
            UUID uploadedById,
            String uploadedByName,
            Instant uploadedAt) {
    }

    /** Miroir de {@code TaskComment.toJson()} mobile. */
    public record TaskCommentView(
            UUID id,
            UUID taskId,
            UUID authorId,
            String authorName,
            String content,
            UUID parentCommentId,
            String parentAuthorName,
            Instant createdAt,
            Instant updatedAt,
            boolean isSystem) {
    }

    /** Miroir de {@code TaskDependency.toJson()} mobile. */
    public record TaskDependencyView(
            UUID id,
            UUID taskId,
            UUID dependsOnTaskId,
            String dependsOnTaskTitle,
            String type) {
    }

    /** Miroir de {@code KanbanColumn.toJson()} mobile. */
    public record KanbanColumnView(
            UUID id,
            String name,
            String status,
            int order,
            Integer wipLimit,
            String color,
            boolean isActive) {
    }

    /** Miroir de {@code TaskTimeEntry.toJson()} mobile. */
    public record TaskTimeEntryView(
            UUID id,
            UUID taskId,
            UUID userId,
            String userName,
            Instant startTime,
            Instant endTime,
            Integer durationMinutes,
            String description,
            Instant createdAt) {
    }

    /** Miroir de {@code TaskTemplate.toJson()} mobile. */
    public record TaskTemplateView(
            UUID id,
            String name,
            String description,
            String type,
            String priority,
            String estimatedHours,
            List<String> defaultTags,
            List<TaskTemplateSubtaskView> subtasks,
            UUID departmentId,
            String departmentName,
            boolean isActive,
            Instant createdAt,
            Instant updatedAt) {
    }

    /** Miroir de {@code TaskTemplateSubtask.toJson()} mobile. */
    public record TaskTemplateSubtaskView(
            UUID id,
            UUID templateId,
            String title,
            String description,
            String priority,
            Integer estimatedHours,
            Integer order) {
    }

    /** Réponse de {@code GET /tasks/{id}/time-total}. */
    public record TimeTotalView(long totalMinutes) {
    }

    // ==================== REQUÊTES ====================

    /** Corps de {@code POST /tasks} et {@code PUT /tasks/{id}} (mêmes clés mobiles). */
    public record TaskRequest(
            String title,
            String description,
            String type,
            String priority,
            String status,
            UUID projectId,
            String projectName,
            UUID assignedToId,
            UUID assignedById,
            UUID departmentId,
            String dueDate,
            String startDate,
            String completedDate,
            Integer estimatedHours,
            Integer actualHours,
            List<String> tags,
            UUID parentTaskId,
            UUID recurrenceRuleId,
            String recurrencePattern,
            String recurrenceEndDate) {
    }

    /** Corps de {@code PATCH /tasks/{id}/status}. */
    public record StatusRequest(String status) {
    }

    /** Corps de {@code PATCH /tasks/{id}/assign}. */
    public record AssignRequest(UUID assignedToId) {
    }

    /** Corps de {@code POST /tasks/{taskId}/comments}. */
    public record CommentRequest(String content, UUID parentCommentId) {
    }

    /** Corps de {@code POST /tasks/{taskId}/dependencies}. */
    public record DependencyRequest(UUID dependsOnTaskId, String type) {
    }

    /** Corps de {@code POST /tasks/{taskId}/reorder}. */
    public record ReorderRequest(String status, Integer order) {
    }

    /** Corps de {@code POST /tasks/{taskId}/time-entries/start}. */
    public record StartTimeRequest(String description) {
    }

    /** Corps de {@code PUT /tasks/kanban/columns/{id}} (mêmes clés mobiles). */
    public record KanbanColumnRequest(
            String name,
            String status,
            Integer order,
            Integer wipLimit,
            String color,
            Boolean isActive,
            UUID projectId) {
    }
}
