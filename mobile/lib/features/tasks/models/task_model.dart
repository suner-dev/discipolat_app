import 'package:flutter/material.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

part 'task_model.freezed.dart';
part 'task_model.g.dart';

@freezed
class Task with _$Task {
  const factory Task({
    required int id,
    required String title,
    String? description,
    required TaskType type,
    required TaskPriority priority,
    required TaskStatus status,
    int? projectId,
    String? projectName,
    int? assignedToId,
    String? assignedToName,
    int? assignedById,
    String? assignedByName,
    int? departmentId,
    String? departmentName,
    DateTime? dueDate,
    DateTime? startDate,
    DateTime? completedDate,
    int? estimatedHours,
    int? actualHours,
    List<String>? tags,
    List<TaskAttachment>? attachments,
    List<TaskComment>? comments,
    List<Task>? subtasks,
    List<TaskDependency>? dependencies,
    int? parentTaskId,
    String? parentTaskTitle,
    int? recurrenceRuleId,
    String? recurrencePattern,
    DateTime? recurrenceEndDate,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _Task;

  factory Task.fromJson(Map<String, dynamic> json) => _$TaskFromJson(json);

  const Task._();

  bool get isOverdue => dueDate != null && DateTime.now().isAfter(dueDate!) && status != TaskStatus.done;
  bool get isDueSoon => dueDate != null && DateTime.now().add(const Duration(days: 3)).isAfter(dueDate!) && status != TaskStatus.done;
  int get daysUntilDue => dueDate != null ? dueDate!.difference(DateTime.now()).inDays : 0;
}

@freezed
class TaskAttachment with _$TaskAttachment {
  const factory TaskAttachment({
    required int id,
    required int taskId,
    required String fileName,
    required String fileUrl,
    required String mimeType,
    required int fileSize,
    int? uploadedById,
    String? uploadedByName,
    required DateTime uploadedAt,
  }) = _TaskAttachment;

  factory TaskAttachment.fromJson(Map<String, dynamic> json) => _$TaskAttachmentFromJson(json);
}

@freezed
class TaskComment with _$TaskComment {
  const factory TaskComment({
    required int id,
    required int taskId,
    required int authorId,
    required String authorName,
    required String content,
    int? parentCommentId,
    String? parentAuthorName,
    required DateTime createdAt,
    DateTime? updatedAt,
    @Default(false) bool isSystem,
  }) = _TaskComment;

  factory TaskComment.fromJson(Map<String, dynamic> json) => _$TaskCommentFromJson(json);
}

@freezed
class TaskDependency with _$TaskDependency {
  const factory TaskDependency({
    required int id,
    required int taskId,
    required int dependsOnTaskId,
    required String dependsOnTaskTitle,
    required DependencyType type,
  }) = _TaskDependency;

  factory TaskDependency.fromJson(Map<String, dynamic> json) => _$TaskDependencyFromJson(json);
}

@freezed
class TaskTemplate with _$TaskTemplate {
  const factory TaskTemplate({
    required int id,
    required String name,
    String? description,
    required TaskType type,
    required TaskPriority priority,
    String? estimatedHours,
    List<String>? defaultTags,
    List<TaskTemplateSubtask>? subtasks,
    int? departmentId,
    String? departmentName,
    @Default(true) bool isActive,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _TaskTemplate;

  factory TaskTemplate.fromJson(Map<String, dynamic> json) => _$TaskTemplateFromJson(json);
}

@freezed
class TaskTemplateSubtask with _$TaskTemplateSubtask {
  const factory TaskTemplateSubtask({
    required int id,
    required int templateId,
    required String title,
    String? description,
    TaskPriority? priority,
    int? estimatedHours,
    int? order,
  }) = _TaskTemplateSubtask;

  factory TaskTemplateSubtask.fromJson(Map<String, dynamic> json) => _$TaskTemplateSubtaskFromJson(json);
}

@freezed
class KanbanColumn with _$KanbanColumn {
  const factory KanbanColumn({
    required int id,
    required String name,
    required TaskStatus status,
    required int order,
    int? wipLimit,
    String? color,
    @Default(true) bool isActive,
  }) = _KanbanColumn;

  factory KanbanColumn.fromJson(Map<String, dynamic> json) => _$KanbanColumnFromJson(json);
}

@freezed
class TaskTimeEntry with _$TaskTimeEntry {
  const factory TaskTimeEntry({
    required int id,
    required int taskId,
    required int userId,
    required String userName,
    required DateTime startTime,
    DateTime? endTime,
    int? durationMinutes,
    String? description,
    required DateTime createdAt,
  }) = _TaskTimeEntry;

  factory TaskTimeEntry.fromJson(Map<String, dynamic> json) => _$TaskTimeEntryFromJson(json);
}

enum TaskType {
  @JsonValue('TASK')
  task,
  @JsonValue('SUBTASK')
  subtask,
  @JsonValue('EPIC')
  epic,
  @JsonValue('STORY')
  story,
  @JsonValue('BUG')
  bug,
  @JsonValue('FEATURE')
  feature,
  @JsonValue('CHORES')
  chores,
  @JsonValue('MEETING')
  meeting,
  @JsonValue('CALL')
  call,
  @JsonValue('REVIEW')
  review;

  String get displayName {
    switch (this) {
      case TaskType.task:
        return 'Tâche';
      case TaskType.subtask:
        return 'Sous-tâche';
      case TaskType.epic:
        return 'Épopée';
      case TaskType.story:
        return 'Histoire';
      case TaskType.bug:
        return 'Bogue';
      case TaskType.feature:
        return 'Fonctionnalité';
      case TaskType.chores:
        return 'Corvée';
      case TaskType.meeting:
        return 'Réunion';
      case TaskType.call:
        return 'Appel';
      case TaskType.review:
        return 'Revue';
    }
  }

  Color getColor() {
    switch (this) {
      case TaskType.task:
        return Colors.blue;
      case TaskType.subtask:
        return Colors.teal;
      case TaskType.epic:
        return Colors.purple;
      case TaskType.story:
        return Colors.indigo;
      case TaskType.bug:
        return Colors.red;
      case TaskType.feature:
        return Colors.green;
      case TaskType.chores:
        return Colors.grey;
      case TaskType.meeting:
        return Colors.orange;
      case TaskType.call:
        return Colors.cyan;
      case TaskType.review:
        return Colors.amber;
    }
  }
}

enum TaskPriority {
  @JsonValue('LOW')
  low,
  @JsonValue('MEDIUM')
  medium,
  @JsonValue('HIGH')
  high,
  @JsonValue('URGENT')
  urgent,
  @JsonValue('CRITICAL')
  critical;

  String get displayName {
    switch (this) {
      case TaskPriority.low:
        return 'Faible';
      case TaskPriority.medium:
        return 'Moyenne';
      case TaskPriority.high:
        return 'Élevée';
      case TaskPriority.urgent:
        return 'Urgent';
      case TaskPriority.critical:
        return 'Critique';
    }
  }

  Color getColor() {
    switch (this) {
      case TaskPriority.low:
        return Colors.green;
      case TaskPriority.medium:
        return Colors.blue;
      case TaskPriority.high:
        return Colors.orange;
      case TaskPriority.urgent:
        return Colors.deepOrange;
      case TaskPriority.critical:
        return Colors.red;
    }
  }

  int getValue() {
    switch (this) {
      case TaskPriority.low:
        return 1;
      case TaskPriority.medium:
        return 2;
      case TaskPriority.high:
        return 3;
      case TaskPriority.urgent:
        return 4;
      case TaskPriority.critical:
        return 5;
    }
  }
}

enum TaskStatus {
  @JsonValue('BACKLOG')
  backlog,
  @JsonValue('TODO')
  todo,
  @JsonValue('IN_PROGRESS')
  inProgress,
  @JsonValue('IN_REVIEW')
  inReview,
  @JsonValue('BLOCKED')
  blocked,
  @JsonValue('DONE')
  done,
  @JsonValue('CANCELLED')
  cancelled;

  String get displayName {
    switch (this) {
      case TaskStatus.backlog:
        return 'Backlog';
      case TaskStatus.todo:
        return 'À faire';
      case TaskStatus.inProgress:
        return 'En cours';
      case TaskStatus.inReview:
        return 'En révision';
      case TaskStatus.blocked:
        return 'Bloqué';
      case TaskStatus.done:
        return 'Terminé';
      case TaskStatus.cancelled:
        return 'Annulé';
    }
  }

  Color getColor() {
    switch (this) {
      case TaskStatus.backlog:
        return Colors.grey;
      case TaskStatus.todo:
        return Colors.blue;
      case TaskStatus.inProgress:
        return Colors.orange;
      case TaskStatus.inReview:
        return Colors.purple;
      case TaskStatus.blocked:
        return Colors.red;
      case TaskStatus.done:
        return Colors.green;
      case TaskStatus.cancelled:
        return Colors.grey;
    }
  }

  int getOrder() {
    switch (this) {
      case TaskStatus.backlog:
        return 0;
      case TaskStatus.todo:
        return 1;
      case TaskStatus.inProgress:
        return 2;
      case TaskStatus.inReview:
        return 3;
      case TaskStatus.blocked:
        return 4;
      case TaskStatus.done:
        return 5;
      case TaskStatus.cancelled:
        return 6;
    }
  }
}

enum DependencyType {
  @JsonValue('BLOCKS')
  blocks,
  @JsonValue('IS_BLOCKED_BY')
  isBlockedBy,
  @JsonValue('RELATES_TO')
  relatesTo,
  @JsonValue('DUPLICATES')
  duplicates,
  @JsonValue('IS_DUPLICATED_BY')
  isDuplicatedBy;

  String get displayName {
    switch (this) {
      case DependencyType.blocks:
        return 'Bloque';
      case DependencyType.isBlockedBy:
        return 'Bloqué par';
      case DependencyType.relatesTo:
        return 'Relatif à';
      case DependencyType.duplicates:
        return 'Double';
      case DependencyType.isDuplicatedBy:
        return 'Doublon de';
    }
  }
}