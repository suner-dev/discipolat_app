package com.discipolat.modules.tasks.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tasks — V234.
 *
 * <p>Implémente le contrat du mobile (mobile/lib/features/tasks) :
 * tâches, sous-tâches, commentaires, dépendances, kanban, temps, templates, rapports.
 *
 * <p><b>Isolation multi-tenant</b> : chaque repository porte un
 * {@code tenantId} explicite. Aucun IDOR.
 */
@Service
@Transactional
public class TaskService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final TaskTemplateRepository templateRepository;
    private final TaskTemplateSubtaskRepository templateSubtaskRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final TaskTimeEntryRepository timeEntryRepository;
    /** Libellés d'utilisateurs attendus par les modèles mobiles. */
    private final com.discipolat.modules.users.domain.UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       TaskAttachmentRepository attachmentRepository,
                       TaskCommentRepository commentRepository,
                       TaskDependencyRepository dependencyRepository,
                       TaskTemplateRepository templateRepository,
                       TaskTemplateSubtaskRepository templateSubtaskRepository,
                       KanbanColumnRepository kanbanColumnRepository,
                       TaskTimeEntryRepository timeEntryRepository,
                       com.discipolat.modules.users.domain.UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.attachmentRepository = attachmentRepository;
        this.commentRepository = commentRepository;
        this.dependencyRepository = dependencyRepository;
        this.templateRepository = templateRepository;
        this.templateSubtaskRepository = templateSubtaskRepository;
        this.kanbanColumnRepository = kanbanColumnRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.userRepository = userRepository;
    }

    private Pageable clamp(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    // ==================== TASKS ====================

    /**
     * Liste paginée avec TOUS les filtres du client mobile.
     *
     * <p>La version précédente ne lisait que {@code status} : {@code priority},
     * {@code type}, {@code assignedToId}, {@code projectId},
     * {@code departmentId}, {@code search}, {@code overdue} et {@code myTasks}
     * étaient acceptés puis ignorés — l'appelant croyait filtrer, le serveur
     * renvoyait l'intégralité des tâches du tenant.
     */
    /**
     * Surcharge de compatibilité : l'ancien appel à 4 arguments
     * (tenant, page, size, status) reste valide et se ramène sur le filtre
     * complet. Conservée pour ne casser aucun appelant existant.
     */
    public List<Map<String, Object>> listTasks(UUID tenantId, int page, int size, String status) {
        return listTasks(tenantId,
                new com.discipolat.modules.tasks.api.dto.TaskDtos.TaskFilter(
                        status, null, null, null, null, null, null, null, null),
                page, size);
    }

    public List<Map<String, Object>> listTasks(UUID tenantId,
                                                com.discipolat.modules.tasks.api.dto.TaskDtos.TaskFilter filter,
                                                int page, int size) {
        Pageable p = clamp(page, size);
        if (filter == null) {
            return taskRepository.findByTenantId(tenantId, p).getContent().stream().map(this::taskView).toList();
        }
        Page<Task> result = taskRepository.findAll(
                tenantId,
                filter.status() == null ? null : parseStatus(filter.status()),
                filter.priority() == null ? null : parsePriority(filter.priority()),
                filter.type() == null ? null : parseType(filter.type()),
                filter.assignedToId(),
                filter.projectId(),
                filter.departmentId(),
                blankToNull(filter.search()),
                Boolean.TRUE.equals(filter.overdue()),
                // Boolean.TRUE.equals : un `== true` déballerait un null et lèverait
                // une NullPointerException quand le client n'envoie pas myTasks.
                Boolean.TRUE.equals(filter.myTasks()) ? currentUserIdOrNull() : null,
                Instant.now(),
                Task.TaskStatus.DONE,
                p);
        return result.getContent().stream().map(this::taskView).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Identifiant de l'utilisateur courant, ou {@code null} hors contexte authentifié. */
    private static UUID currentUserIdOrNull() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (RuntimeException noSecurityContext) {
            return null;
        }
    }

    private Task.TaskPriority parsePriority(String value) {
        return requireEnum(Task.TaskPriority.class, value);
    }

    private Task.TaskType parseType(String value) {
        return requireEnum(Task.TaskType.class, value);
    }

    /** Parse un enum en rejetant les valeurs inconnues (400) au lieu d'un repli muet. */
    private static <T extends Enum<T>> T requireEnum(Class<T> cls, String value) {
        String v = value.trim();
        for (T candidate : cls.getEnumConstants()) {
            if (candidate.name().equalsIgnoreCase(v)) return candidate;
            // Le client Dart envoie parfois le nom camelCase de son enum
            // (« inProgress ») : on accepte cette forme sans ambiguïté.
            if (candidate.name().replace("_", "").equalsIgnoreCase(v.replace("_", ""))) return candidate;
        }
        throw new IllegalArgumentException("Valeur invalide pour " + cls.getSimpleName() + " : \"" + value + "\"");
    }

    public Map<String, Object> getTask(UUID tenantId, Long id) {
        return taskView(requireTask(tenantId, id));
    }

    public Map<String, Object> createTask(UUID tenantId, Map<String, Object> body) {
        Task task = Task.builder()
                .tenantId(tenantId)
                .title(str(body.get("title")))
                .description(str(body.get("description")))
                .type(optionalEnum(Task.TaskType.class, body.get("type"), Task.TaskType.TASK))
                .priority(optionalEnum(Task.TaskPriority.class, body.get("priority"), Task.TaskPriority.MEDIUM))
                .status(Task.TaskStatus.TODO)
                .projectId(longVal(body.get("projectId")))
                .assignedToId(requireTenantUser(tenantId, uuidVal(str(body.get("assignedToId")))))
                .assignedById(SecurityUtils.getCurrentUserId())
                .departmentId(longVal(body.get("departmentId")))
                .dueDate(instantVal(body.get("dueDate")))
                .startDate(instantVal(body.get("startDate")))
                .estimatedHours(intVal(body.get("estimatedHours")))
                .tags(strList(body.get("tags")))
                .parentTaskId(longVal(body.get("parentTaskId")))
                .recurrencePattern(str(body.get("recurrencePattern")))
                .recurrenceEndDate(instantVal(body.get("recurrenceEndDate")))
                .build();
        return taskView(taskRepository.save(task));
    }

    public Map<String, Object> updateTask(UUID tenantId, Long id, Map<String, Object> body) {
        Task task = requireTask(tenantId, id);
        if (body.containsKey("title")) task.setTitle(str(body.get("title")));
        if (body.containsKey("description")) task.setDescription(str(body.get("description")));
        if (body.containsKey("type")) task.setType(optionalEnum(Task.TaskType.class, body.get("type"), task.getType()));
        if (body.containsKey("priority")) task.setPriority(optionalEnum(Task.TaskPriority.class, body.get("priority"), task.getPriority()));
        if (body.containsKey("status")) task.setStatus(parseStatus(str(body.get("status"))));
        if (body.containsKey("projectId")) task.setProjectId(longVal(body.get("projectId")));
        if (body.containsKey("assignedToId")) {
            task.setAssignedToId(requireTenantUser(tenantId, uuidVal(str(body.get("assignedToId")))));
        }
        if (body.containsKey("departmentId")) task.setDepartmentId(longVal(body.get("departmentId")));
        if (body.containsKey("dueDate")) task.setDueDate(instantVal(body.get("dueDate")));
        if (body.containsKey("startDate")) task.setStartDate(instantVal(body.get("startDate")));
        if (body.containsKey("estimatedHours")) task.setEstimatedHours(intVal(body.get("estimatedHours")));
        if (body.containsKey("actualHours")) task.setActualHours(intVal(body.get("actualHours")));
        if (body.containsKey("tags")) task.setTags(strList(body.get("tags")));
        if (body.containsKey("parentTaskId")) task.setParentTaskId(longVal(body.get("parentTaskId")));
        if (body.containsKey("recurrencePattern")) task.setRecurrencePattern(str(body.get("recurrencePattern")));
        if (body.containsKey("recurrenceEndDate")) task.setRecurrenceEndDate(instantVal(body.get("recurrenceEndDate")));
        return taskView(taskRepository.save(task));
    }

    /**
     * Déplacement kanban : nouveau statut et/ou nouveau rang.
     *
     * <p>Un statut absent ne change que le rang, et inversement — ce qui permet
     * au client de réordonner dans une colonne sans réécrire le statut.
     */
    public Map<String, Object> reorderTask(UUID tenantId, Long id, String status, Integer order) {
        Task task = requireTask(tenantId, id);
        if (status != null && !status.isBlank()) {
            Task.TaskStatus newStatus = parseStatus(status);
            task.setStatus(newStatus);
            task.setCompletedDate(newStatus == Task.TaskStatus.DONE ? Instant.now() : null);
        }
        if (order != null) task.setSortOrder(order);
        return taskView(taskRepository.save(task));
    }

    /**
     * Suppression = archivage (R9 : « Pas de purge »).
     *
     * <p>La version précédente appelait {@code repository.delete(task)} : une
     * suppression DÉFINITIVE, avec ses pièces jointes, commentaires et
     * dépendances (ON DELETE CASCADE). La règle R9 impose un statut, et le
     * modèle mobile connaît {@code CANCELLED}.
     */
    public void deleteTask(UUID tenantId, Long id) {
        Task task = requireTask(tenantId, id);
        task.setStatus(Task.TaskStatus.CANCELLED);
        taskRepository.save(task);
    }

    public Map<String, Object> updateStatus(UUID tenantId, Long id, String status) {
        Task task = requireTask(tenantId, id);
        task.setStatus(parseStatus(status));
        if (task.getStatus() == Task.TaskStatus.DONE) task.setCompletedDate(Instant.now());
        return taskView(taskRepository.save(task));
    }

    public Map<String, Object> assignTask(UUID tenantId, Long id, UUID assignedToId) {
        Task task = requireTask(tenantId, id);
        // R4 : l'assigné doit être membre du tenant (une FK ne garantit que
        // l'existence du compte, pas son appartenance à l'église courante).
        if (assignedToId != null
                && userRepository.findByIdWithActiveMembershipInTenant(assignedToId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("User", "id", assignedToId.toString());
        }
        task.setAssignedToId(assignedToId);
        task.setAssignedById(SecurityUtils.getCurrentUserId());
        return taskView(taskRepository.save(task));
    }

    private Task requireTask(UUID tenantId, Long id) {
        return taskRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("Task", "id", String.valueOf(id)));
    }

    private Map<String, Object> taskView(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("type", t.getType().name());
        m.put("priority", t.getPriority().name());
        m.put("status", t.getStatus().name());
        m.put("projectId", t.getProjectId());
        m.put("assignedToId", t.getAssignedToId());
        m.put("assignedById", t.getAssignedById());
        m.put("departmentId", t.getDepartmentId());
        m.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
        m.put("startDate", t.getStartDate() != null ? t.getStartDate().toString() : null);
        m.put("completedDate", t.getCompletedDate() != null ? t.getCompletedDate().toString() : null);
        m.put("estimatedHours", t.getEstimatedHours());
        m.put("actualHours", t.getActualHours());
        m.put("tags", t.getTags());
        m.put("parentTaskId", t.getParentTaskId());
        m.put("sortOrder", t.getSortOrder());
        m.put("recurrencePattern", t.getRecurrencePattern());
        m.put("recurrenceEndDate", t.getRecurrenceEndDate() != null ? t.getRecurrenceEndDate().toString() : null);
        m.put("createdAt", t.getCreatedAt().toString());
        m.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
        return m;
    }

    // ==================== SUBTASKS ====================

    public List<Map<String, Object>> listSubtasks(UUID tenantId, Long taskId) {
        requireTask(tenantId, taskId);
        return taskRepository.findByTenantIdAndParentTaskId(tenantId, taskId, Pageable.unpaged())
                .getContent().stream().map(this::taskView).toList();
    }

    public Map<String, Object> createSubtask(UUID tenantId, Long parentTaskId, Map<String, Object> body) {
        requireTask(tenantId, parentTaskId);
        Task subtask = Task.builder()
                .tenantId(tenantId)
                .title(str(body.get("title")))
                .description(str(body.get("description")))
                .type(Task.TaskType.SUBTASK)
                .priority(optionalEnum(Task.TaskPriority.class, body.get("priority"), Task.TaskPriority.MEDIUM))
                .status(Task.TaskStatus.TODO)
                .parentTaskId(parentTaskId)
                .build();
        return taskView(taskRepository.save(subtask));
    }

    // ==================== ATTACHMENTS ====================

    public List<Map<String, Object>> listAttachments(UUID tenantId, Long taskId) {
        requireTask(tenantId, taskId);
        return attachmentRepository.findByTenantIdAndTaskId(tenantId, taskId).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("taskId", a.getTaskId());
            m.put("fileName", a.getFileName());
            m.put("fileUrl", a.getFileUrl());
            m.put("mimeType", a.getMimeType());
            m.put("fileSize", a.getFileSize());
            m.put("uploadedById", a.getUploadedById());
            m.put("uploadedAt", a.getUploadedAt().toString());
            return m;
        }).toList();
    }

    public void deleteAttachment(UUID tenantId, Long attachmentId) {
        TaskAttachment a = attachmentRepository.findByTenantIdAndId(tenantId, attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("TaskAttachment", "id", String.valueOf(attachmentId)));
        attachmentRepository.delete(a);
    }

    // ==================== COMMENTS ====================

    public List<Map<String, Object>> listComments(UUID tenantId, Long taskId) {
        requireTask(tenantId, taskId);
        return commentRepository.findByTenantIdAndTaskIdOrderByCreatedAtAsc(tenantId, taskId).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("taskId", c.getTaskId());
            m.put("authorId", c.getAuthorId());
            // `authorName` est `required` côté mobile : sans lui, le fromJson échoue.
            m.put("authorName", currentUserName(c.getAuthorId()));
            m.put("content", c.getContent());
            m.put("parentCommentId", c.getParentCommentId());
            m.put("isSystem", c.isSystem());
            m.put("createdAt", c.getCreatedAt().toString());
            m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
            return m;
        }).toList();
    }

    public Map<String, Object> createComment(UUID tenantId, Long taskId, Map<String, Object> body) {
        requireTask(tenantId, taskId);
        TaskComment c = TaskComment.builder()
                .tenantId(tenantId)
                .taskId(taskId)
                .authorId(SecurityUtils.getCurrentUserId())
                .content(str(body.get("content")))
                .parentCommentId(longVal(body.get("parentCommentId")))
                .isSystem(false)
                .build();
        TaskComment saved = commentRepository.save(c);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("taskId", saved.getTaskId());
        m.put("authorId", saved.getAuthorId());
        m.put("content", saved.getContent());
        m.put("parentCommentId", saved.getParentCommentId());
        m.put("isSystem", saved.isSystem());
        m.put("createdAt", saved.getCreatedAt().toString());
        return m;
    }

    public void deleteComment(UUID tenantId, Long commentId) {
        TaskComment c = commentRepository.findByTenantIdAndId(tenantId, commentId)
                .orElseThrow(() -> new EntityNotFoundException("TaskComment", "id", String.valueOf(commentId)));
        commentRepository.delete(c);
    }

    // ==================== DEPENDENCIES ====================

    public List<Map<String, Object>> listDependencies(UUID tenantId, Long taskId) {
        requireTask(tenantId, taskId);
        return dependencyRepository.findByTenantIdAndTaskId(tenantId, taskId).stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("taskId", d.getTaskId());
            m.put("dependsOnTaskId", d.getDependsOnTaskId());
            // `dependsOnTaskTitle` est `required` côté mobile.
            m.put("dependsOnTaskTitle", taskTitle(tenantId, d.getDependsOnTaskId()));
            m.put("type", d.getType().name());
            return m;
        }).toList();
    }

    /** Titre de la tâche dont on dépend ; repli stable si introuvable. */
    private String taskTitle(UUID tenantId, Long taskId) {
        return taskRepository.findByTenantIdAndId(tenantId, taskId)
                .map(Task::getTitle)
                .orElse("Tâche " + taskId);
    }

    public Map<String, Object> createDependency(UUID tenantId, Long taskId, Map<String, Object> body) {
        requireTask(tenantId, taskId);
        Long dependsOnTaskId = longVal(body.get("dependsOnTaskId"));
        requireTask(tenantId, dependsOnTaskId);
        TaskDependency d = TaskDependency.builder()
                .tenantId(tenantId)
                .taskId(taskId)
                .dependsOnTaskId(dependsOnTaskId)
                .type(parseEnum(TaskDependency.DependencyType.class, str(body.get("type")), TaskDependency.DependencyType.BLOCKS))
                .build();
        TaskDependency saved = dependencyRepository.save(d);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("taskId", saved.getTaskId());
        m.put("dependsOnTaskId", saved.getDependsOnTaskId());
        m.put("type", saved.getType().name());
        return m;
    }

    public void deleteDependency(UUID tenantId, Long dependencyId) {
        TaskDependency d = dependencyRepository.findByTenantIdAndId(tenantId, dependencyId)
                .orElseThrow(() -> new EntityNotFoundException("TaskDependency", "id", String.valueOf(dependencyId)));
        dependencyRepository.delete(d);
    }

    // ==================== KANBAN ====================

    public List<Map<String, Object>> listKanbanColumns(UUID tenantId) {
        return kanbanColumnRepository.findByTenantIdAndIsActiveTrueOrderByOrderAsc(tenantId).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("status", c.getStatus().name());
            m.put("order", c.getOrder());
            m.put("wipLimit", c.getWipLimit());
            m.put("color", c.getColor());
            m.put("isActive", c.isActive());
            return m;
        }).toList();
    }

    public Map<String, Object> updateKanbanColumn(UUID tenantId, Long id, Map<String, Object> body) {
        KanbanColumn c = kanbanColumnRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("KanbanColumn", "id", String.valueOf(id)));
        if (body.containsKey("name")) c.setName(str(body.get("name")));
        if (body.containsKey("status")) c.setStatus(parseStatus(str(body.get("status"))));
        if (body.containsKey("order")) c.setOrder(intVal(body.get("order")));
        if (body.containsKey("wipLimit")) c.setWipLimit(intVal(body.get("wipLimit")));
        if (body.containsKey("color")) c.setColor(str(body.get("color")));
        if (body.containsKey("isActive")) c.setActive(boolVal(body.get("isActive")));
        KanbanColumn saved = kanbanColumnRepository.save(c);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("name", saved.getName());
        m.put("status", saved.getStatus().name());
        m.put("order", saved.getOrder());
        m.put("wipLimit", saved.getWipLimit());
        m.put("color", saved.getColor());
        m.put("isActive", saved.isActive());
        return m;
    }

    // ==================== TIME ENTRIES ====================

    public List<Map<String, Object>> listTimeEntries(UUID tenantId, Long taskId) {
        requireTask(tenantId, taskId);
        return timeEntryRepository.findByTenantIdAndTaskIdOrderByStartTimeAsc(tenantId, taskId).stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("taskId", e.getTaskId());
            m.put("userId", e.getUserId());
            // `userName` est `required` côté mobile.
            m.put("userName", currentUserName(e.getUserId()));
            m.put("startTime", e.getStartTime().toString());
            m.put("endTime", e.getEndTime() != null ? e.getEndTime().toString() : null);
            m.put("durationMinutes", e.getDurationMinutes());
            m.put("description", e.getDescription());
            m.put("createdAt", e.getCreatedAt().toString());
            return m;
        }).toList();
    }

    public Map<String, Object> startTimeEntry(UUID tenantId, Long taskId, Map<String, Object> body) {
        requireTask(tenantId, taskId);
        TaskTimeEntry e = TaskTimeEntry.builder()
                .tenantId(tenantId)
                .taskId(taskId)
                .userId(SecurityUtils.getCurrentUserId())
                .startTime(Instant.now())
                .description(str(body.get("description")))
                .build();
        TaskTimeEntry saved = timeEntryRepository.save(e);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("taskId", saved.getTaskId());
        m.put("userId", saved.getUserId());
        m.put("startTime", saved.getStartTime().toString());
        m.put("endTime", null);
        m.put("durationMinutes", null);
        m.put("description", saved.getDescription());
        m.put("createdAt", saved.getCreatedAt().toString());
        return m;
    }

    public Map<String, Object> stopTimeEntry(UUID tenantId, Long timeEntryId) {
        TaskTimeEntry e = timeEntryRepository.findByTenantIdAndId(tenantId, timeEntryId)
                .orElseThrow(() -> new EntityNotFoundException("TaskTimeEntry", "id", String.valueOf(timeEntryId)));
        e.setEndTime(Instant.now());
        long minutes = java.time.Duration.between(e.getStartTime(), e.getEndTime()).toMinutes();
        e.setDurationMinutes((int) minutes);
        TaskTimeEntry saved = timeEntryRepository.save(e);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("taskId", saved.getTaskId());
        m.put("userId", saved.getUserId());
        m.put("startTime", saved.getStartTime().toString());
        m.put("endTime", saved.getEndTime().toString());
        m.put("durationMinutes", saved.getDurationMinutes());
        m.put("description", saved.getDescription());
        return m;
    }

    public Map<String, Object> getTimeTotal(UUID tenantId, Long taskId) {
        requireTask(tenantId, taskId);
        Integer total = timeEntryRepository.sumDurationMinutes(tenantId, taskId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskId", taskId);
        m.put("totalMinutes", total != null ? total : 0);
        return m;
    }

    // ==================== TEMPLATES ====================

    public List<Map<String, Object>> listTemplates(UUID tenantId, int page, int size) {
        return templateRepository.findByTenantIdAndIsActiveTrue(tenantId, clamp(page, size))
                .getContent().stream().map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("description", t.getDescription());
                    m.put("type", t.getType().name());
                    m.put("priority", t.getPriority().name());
                    m.put("estimatedHours", t.getEstimatedHours());
                    m.put("defaultTags", t.getDefaultTags());
                    m.put("departmentId", t.getDepartmentId());
                    m.put("isActive", t.isActive());
                    m.put("createdAt", t.getCreatedAt().toString());
                    m.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
                    return m;
                }).toList();
    }

    public Map<String, Object> createFromTemplate(UUID tenantId, Long templateId, Map<String, Object> body) {
        TaskTemplate template = templateRepository.findByTenantIdAndId(tenantId, templateId)
                .orElseThrow(() -> new EntityNotFoundException("TaskTemplate", "id", String.valueOf(templateId)));
        Task task = Task.builder()
                .tenantId(tenantId)
                .title(template.getName())
                .description(template.getDescription())
                .type(template.getType())
                .priority(template.getPriority())
                .status(Task.TaskStatus.TODO)
                .departmentId(template.getDepartmentId())
                .estimatedHours(template.getEstimatedHours())
                .tags(template.getDefaultTags())
                .build();
        return taskView(taskRepository.save(task));
    }

    // ==================== REPORTS ====================

    public Map<String, Object> reportStatistics(UUID tenantId) {
        long total = taskRepository.countByTenantId(tenantId);
        long done = taskRepository.countByTenantIdAndStatus(tenantId, Task.TaskStatus.DONE);
        long inProgress = taskRepository.countByTenantIdAndStatus(tenantId, Task.TaskStatus.IN_PROGRESS);
        long blocked = taskRepository.countByTenantIdAndStatus(tenantId, Task.TaskStatus.BLOCKED);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("done", done);
        m.put("inProgress", inProgress);
        m.put("blocked", blocked);
        return m;
    }

    public List<Map<String, Object>> reportByStatus(UUID tenantId) {
        return java.util.Arrays.stream(Task.TaskStatus.values()).map(s -> {
            long count = taskRepository.countByTenantIdAndStatus(tenantId, s);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", s.name());
            m.put("count", count);
            return m;
        }).toList();
    }

    /**
     * Charge utile bornée pour une agrégation : évite de matérialiser toute la
     * table `tasks` d'un tenant en mémoire pour un simple comptage.
     */
    private static final int MAX_AGGREGATION_ROWS = 50_000;

    public List<Map<String, Object>> reportByAssignee(UUID tenantId) {
        return taskRepository.findByTenantId(tenantId, PageRequest.of(0, MAX_AGGREGATION_ROWS))
                .getContent().stream()
                .filter(t -> t.getAssignedToId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Task::getAssignedToId))
                .entrySet().stream().map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("assignedToId", e.getKey());
                    m.put("assignedToName", currentUserName(e.getKey()));
                    m.put("count", e.getValue().size());
                    return m;
                }).toList();
    }

    /** Nom lisible de l'assigné, attendu par le modèle mobile. */
    private String currentUserName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(u -> {
            String full = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                    + (u.getLastName() == null ? "" : u.getLastName())).trim();
            return full.isEmpty() ? (u.getEmail() != null ? u.getEmail() : "—") : full;
        }).orElse("—");
    }

    public List<Map<String, Object>> listOverdue(UUID tenantId) {
        return taskRepository.findByTenantIdAndDueDateBeforeAndStatusNot(tenantId, Instant.now(), Task.TaskStatus.DONE)
                .stream().map(this::taskView).toList();
    }

    // ==================== helpers ====================

    private Task.TaskStatus parseStatus(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("status est requis");
        String v = value.trim().toUpperCase().replace(" ", "_");
        try { return Task.TaskStatus.valueOf(v); } catch (IllegalArgumentException e) {
            // accept Dart camelCase names like "inProgress"
            for (Task.TaskStatus s : Task.TaskStatus.values()) {
                if (s.name().equalsIgnoreCase(value) || s.name().replace("_", "").equalsIgnoreCase(value.replace("_", ""))) return s;
            }
            throw new IllegalArgumentException("Statut inconnu : " + value);
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> cls, String value, T def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(cls, value.trim().toUpperCase()); } catch (IllegalArgumentException e) { return def; }
    }

    /**
     * Enum fourni à la création : valeur absente → repli, valeur INCONNUE → 400.
     * Empêche d'enregistrer silencieusement une tâche en type {@code TASK}
     * parce que le client a envoyé une faute de frappe.
     */
    private static <T extends Enum<T>> T optionalEnum(Class<T> cls, Object raw, T def) {
        if (raw == null || String.valueOf(raw).isBlank()) return def;
        return requireEnum(cls, String.valueOf(raw));
    }

    /** R4 : l'utilisateur référencé doit être membre du tenant ; null est toléré. */
    private UUID requireTenantUser(UUID tenantId, UUID userId) {
        if (userId == null) return null;
        if (userRepository.findByIdWithActiveMembershipInTenant(userId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("User", "id", userId.toString());
        }
        return userId;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static long longVal(Object o) { return o == null ? 0 : Long.parseLong(String.valueOf(o)); }
    private static int intVal(Object o) { return o == null ? 0 : Integer.parseInt(String.valueOf(o)); }
    private static boolean boolVal(Object o) { return o != null && Boolean.parseBoolean(String.valueOf(o)); }
    private static UUID uuidVal(Object o) { return o == null ? null : UUID.fromString(String.valueOf(o)); }
    private static Instant instantVal(Object o) { return o == null ? null : Instant.parse(String.valueOf(o)); }
    @SuppressWarnings("unchecked")
    private static List<String> strList(Object o) { return o == null ? List.of() : (List<String>) o; }
}
