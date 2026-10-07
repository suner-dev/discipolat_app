// Modèle tasks — aligné sur le contrat serveur RÉEL (vérifié dans
// backend/…/modules/tasks : TaskController, TaskService.taskView, V234).
//
// Types d'identifiants (à ne pas confondre avec le reste de l'app) :
// - Task, TaskAttachment, TaskComment, TaskDependency, TaskTemplate,
//   KanbanColumn, TaskTimeEntry : id = `GenerationType.IDENTITY` (Long)
//   → int côté Dart. CE N'EST PAS de la dette UUID, le serveur fait
//   `@PathVariable Long id`.
// - Références PERSONNES (assignedToId, assignedById, authorId,
//   uploadedById, userId du time entry) : `UUID` côté serveur → String ici.
//   Le serveur les sérialise en chaîne et fait `UUID.fromString` sur les
//   entrées ; un int casserait le fromJson (cast num→String) et le PATCH
//   /assign (400).
// - Références structurelles Long : projectId, departmentId, parentTaskId,
//   parentCommentId, dependsOnTaskId, recurrenceRuleId → int.
//
// Vues serveur partielles : certaines réponses (createComment,
// stopTimeEntry, createDependency) n'incluent PAS tous les champs des vues
// en liste — le parsing est donc tolérant (null quand absent), jamais un
// cast dur.

import 'package:flutter/material.dart';

// ── Helpers de parsing tolérants ────────────────────────────────────────────

int? _i(Object? v) => v is num ? v.toInt() : (v == null ? null : int.tryParse(v.toString()));

String? _s(Object? v) => v?.toString();

String _str(Object? v, [String fallback = '']) => v == null ? fallback : v.toString();

bool _b(Object? v, {bool fallback = false}) {
  if (v is bool) return v;
  if (v == null) return fallback;
  return v.toString() == 'true' || v == 1;
}

/// `Instant.toString()` côté Java → ISO-8601 avec ou sans fraction, en Z.
DateTime? _dt(Object? v) {
  if (v == null) return null;
  return DateTime.tryParse(v.toString());
}

List<String>? _tags(Object? v) {
  if (v is List) return v.map((e) => e.toString()).toList();
  return null;
}

List<T>? _list<T>(Object? v, T Function(Map<String, dynamic>) build) {
  if (v is! List) return null;
  return v.whereType<Map<String, dynamic>>().map(build).toList();
}

// ── Enums (noms serveur : @Enumerated(EnumType.STRING)) ─────────────────────

enum TaskType {
  task('TASK'),
  subtask('SUBTASK'),
  epic('EPIC'),
  story('STORY'),
  bug('BUG'),
  feature('FEATURE'),
  chores('CHORES'),
  meeting('MEETING'),
  call('CALL'),
  review('REVIEW');

  const TaskType(this.wire);

  /// Nom exact envoyé/renvoyé par le serveur.
  final String wire;

  /// Tolérant : le serveur accepte camelCase en entrée mais répond en NOM
  /// serveur ; valeur inconnue → `task` (jamais d'exception dans une liste).
  static TaskType fromWire(String? v) {
    for (final e in TaskType.values) {
      if (e.wire.toLowerCase() == (v ?? '').toLowerCase() ||
          e.name == v) {
        return e;
      }
    }
    return TaskType.task;
  }

  String get displayName {
    switch (this) {
      case TaskType.task:
        return 'Tâche';
      case TaskType.subtask:
        return 'Sous-tâche';
      case TaskType.epic:
        return 'Épopée';
      case TaskType.story:
        return 'Récit';
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
  low('LOW'),
  medium('MEDIUM'),
  high('HIGH'),
  urgent('URGENT'),
  critical('CRITICAL');

  const TaskPriority(this.wire);

  final String wire;

  static TaskPriority fromWire(String? v) {
    for (final e in TaskPriority.values) {
      if (e.wire.toLowerCase() == (v ?? '').toLowerCase() ||
          e.name == v) {
        return e;
      }
    }
    return TaskPriority.medium;
  }

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
  backlog('BACKLOG'),
  todo('TODO'),
  inProgress('IN_PROGRESS'),
  inReview('IN_REVIEW'),
  blocked('BLOCKED'),
  done('DONE'),
  cancelled('CANCELLED');

  const TaskStatus(this.wire);

  /// Nom serveur — c'est CE CI qui doit être envoyé en query/body, le
  /// serveur normalise aussi camelCase mais ne comptons pas dessus.
  final String wire;

  static TaskStatus fromWire(String? v) {
    for (final e in TaskStatus.values) {
      if (e.wire.toLowerCase() == (v ?? '').toLowerCase() ||
          e.name == v) {
        return e;
      }
    }
    return TaskStatus.todo;
  }

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
  blocks('BLOCKS'),
  isBlockedBy('IS_BLOCKED_BY'),
  relatesTo('RELATES_TO'),
  duplicates('DUPLICATES'),
  isDuplicatedBy('IS_DUPLICATED_BY');

  const DependencyType(this.wire);

  final String wire;

  static DependencyType fromWire(String? v) {
    for (final e in DependencyType.values) {
      if (e.wire.toLowerCase() == (v ?? '').toLowerCase() ||
          e.name == v) {
        return e;
      }
    }
    return DependencyType.blocks;
  }

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

// ── Task (vue taskView du serveur) ──────────────────────────────────────────

class Task {
  const Task({
    required this.id,
    required this.title,
    this.description,
    this.type = TaskType.task,
    this.priority = TaskPriority.medium,
    this.status = TaskStatus.todo,
    this.projectId,
    this.projectName,
    this.assignedToId,
    this.assignedToName,
    this.assignedById,
    this.assignedByName,
    this.departmentId,
    this.departmentName,
    this.dueDate,
    this.startDate,
    this.completedDate,
    this.estimatedHours,
    this.actualHours,
    this.tags,
    this.attachments,
    this.comments,
    this.subtasks,
    this.dependencies,
    this.parentTaskId,
    this.parentTaskTitle,
    this.sortOrder,
    this.recurrenceRuleId,
    this.recurrencePattern,
    this.recurrenceEndDate,
    required this.createdAt,
    this.updatedAt,
  });

  factory Task.fromJson(Map<String, dynamic> json) => Task(
        id: _i(json['id']) ?? 0,
        title: _str(json['title']),
        description: _s(json['description']),
        type: TaskType.fromWire(_s(json['type'])),
        priority: TaskPriority.fromWire(_s(json['priority'])),
        status: TaskStatus.fromWire(_s(json['status'])),
        projectId: _i(json['projectId']),
        projectName: _s(json['projectName']),
        assignedToId: _s(json['assignedToId']),
        assignedToName: _s(json['assignedToName']),
        assignedById: _s(json['assignedById']),
        assignedByName: _s(json['assignedByName']),
        departmentId: _i(json['departmentId']),
        departmentName: _s(json['departmentName']),
        dueDate: _dt(json['dueDate']),
        startDate: _dt(json['startDate']),
        completedDate: _dt(json['completedDate']),
        estimatedHours: _i(json['estimatedHours']),
        actualHours: _i(json['actualHours']),
        tags: _tags(json['tags']),
        attachments: _list(json['attachments'], TaskAttachment.fromJson),
        comments: _list(json['comments'], TaskComment.fromJson),
        subtasks: _list(json['subtasks'], Task.fromJson),
        dependencies: _list(json['dependencies'], TaskDependency.fromJson),
        parentTaskId: _i(json['parentTaskId']),
        parentTaskTitle: _s(json['parentTaskTitle']),
        sortOrder: _i(json['sortOrder']),
        recurrenceRuleId: _i(json['recurrenceRuleId']),
        recurrencePattern: _s(json['recurrencePattern']),
        recurrenceEndDate: _dt(json['recurrenceEndDate']),
        createdAt: _dt(json['createdAt']) ?? DateTime.fromMillisecondsSinceEpoch(0),
        updatedAt: _dt(json['updatedAt']),
      );

  final int id;
  final String title;
  final String? description;
  final TaskType type;
  final TaskPriority priority;
  final TaskStatus status;
  final int? projectId;

  /// Absent de taskView : enrichissement éventuel côté client, jamais envoyé.
  final String? projectName;

  /// UUID (String) — le serveur fait `UUID.fromString` sur ce champ.
  final String? assignedToId;
  final String? assignedToName;
  final String? assignedById;
  final String? assignedByName;
  final int? departmentId;
  final String? departmentName;
  final DateTime? dueDate;
  final DateTime? startDate;
  final DateTime? completedDate;
  final int? estimatedHours;
  final int? actualHours;
  final List<String>? tags;
  final List<TaskAttachment>? attachments;
  final List<TaskComment>? comments;
  final List<Task>? subtasks;
  final List<TaskDependency>? dependencies;
  final int? parentTaskId;
  final String? parentTaskTitle;

  /// Rang kanban — persisté par POST /tasks/{id}/reorder (V239 serveur).
  final int? sortOrder;
  final int? recurrenceRuleId;
  final String? recurrencePattern;
  final DateTime? recurrenceEndDate;
  final DateTime createdAt;
  final DateTime? updatedAt;

  bool get isOverdue =>
      dueDate != null &&
      DateTime.now().isAfter(dueDate!) &&
      status != TaskStatus.done &&
      status != TaskStatus.cancelled;

  bool get isDueSoon =>
      dueDate != null &&
      DateTime.now().add(const Duration(days: 3)).isAfter(dueDate!) &&
      status != TaskStatus.done &&
      status != TaskStatus.cancelled;

  int get daysUntilDue =>
      dueDate != null ? dueDate!.difference(DateTime.now()).inDays : 0;

  /// Corps exact de POST /tasks (clés lues par TaskService.createTask).
  /// `status` est ignoré à la création (imposé TODO) — ne pas l'envoyer.
  static Map<String, dynamic> createBody({
    required String title,
    String? description,
    TaskType? type,
    TaskPriority? priority,
    int? projectId,
    String? assignedToId,
    int? departmentId,
    DateTime? dueDate,
    DateTime? startDate,
    int? estimatedHours,
    List<String>? tags,
    int? parentTaskId,
    String? recurrencePattern,
    DateTime? recurrenceEndDate,
  }) =>
      {
        'title': title,
        if (description != null && description.isNotEmpty) 'description': description,
        if (type != null) 'type': type.wire,
        if (priority != null) 'priority': priority.wire,
        if (projectId != null) 'projectId': projectId,
        if (assignedToId != null) 'assignedToId': assignedToId,
        if (departmentId != null) 'departmentId': departmentId,
        if (dueDate != null) 'dueDate': dueDate.toUtc().toIso8601String(),
        if (startDate != null) 'startDate': startDate.toUtc().toIso8601String(),
        if (estimatedHours != null) 'estimatedHours': estimatedHours,
        if (tags != null && tags.isNotEmpty) 'tags': tags,
        if (parentTaskId != null) 'parentTaskId': parentTaskId,
        if (recurrencePattern != null) 'recurrencePattern': recurrencePattern,
        if (recurrenceEndDate != null) 'recurrenceEndDate': recurrenceEndDate.toUtc().toIso8601String(),
      };

  /// Corps de PUT /tasks/{id} — seules les clés présentes sont appliquées
  /// (`if (body.containsKey(…))` côté serveur). Les dates doivent être en
  /// ISO-8601 UTC : le serveur fait `Instant.parse`, un format exotique → 500.
  static Map<String, dynamic> updateBody({
    String? title,
    String? description,
    TaskType? type,
    TaskPriority? priority,
    TaskStatus? status,
    int? projectId,
    String? assignedToId,
    int? departmentId,
    DateTime? dueDate,
    DateTime? startDate,
    int? estimatedHours,
    int? actualHours,
    List<String>? tags,
    int? parentTaskId,
    String? recurrencePattern,
    DateTime? recurrenceEndDate,
  }) =>
      {
        if (title != null) 'title': title,
        if (description != null) 'description': description,
        if (type != null) 'type': type.wire,
        if (priority != null) 'priority': priority.wire,
        if (status != null) 'status': status.wire,
        if (projectId != null) 'projectId': projectId,
        if (assignedToId != null) 'assignedToId': assignedToId,
        if (departmentId != null) 'departmentId': departmentId,
        if (dueDate != null) 'dueDate': dueDate.toUtc().toIso8601String(),
        if (startDate != null) 'startDate': startDate.toUtc().toIso8601String(),
        if (estimatedHours != null) 'estimatedHours': estimatedHours,
        if (actualHours != null) 'actualHours': actualHours,
        if (tags != null) 'tags': tags,
        if (parentTaskId != null) 'parentTaskId': parentTaskId,
        if (recurrencePattern != null) 'recurrencePattern': recurrencePattern,
        if (recurrenceEndDate != null) 'recurrenceEndDate': recurrenceEndDate.toUtc().toIso8601String(),
      };
}

// ── Pièces jointes ──────────────────────────────────────────────────────────

class TaskAttachment {
  const TaskAttachment({
    required this.id,
    required this.taskId,
    required this.fileName,
    required this.fileUrl,
    this.mimeType = '',
    this.fileSize = 0,
    this.uploadedById,
    this.uploadedByName,
    required this.uploadedAt,
  });

  factory TaskAttachment.fromJson(Map<String, dynamic> json) =>
      TaskAttachment(
        id: _i(json['id']) ?? 0,
        taskId: _i(json['taskId']) ?? 0,
        fileName: _str(json['fileName']),
        fileUrl: _str(json['fileUrl']),
        mimeType: _str(json['mimeType']),
        fileSize: _i(json['fileSize']) ?? 0,
        uploadedById: _s(json['uploadedById']),
        uploadedByName: _s(json['uploadedByName']),
        uploadedAt: _dt(json['uploadedAt']) ?? DateTime.fromMillisecondsSinceEpoch(0),
      );

  final int id;
  final int taskId;
  final String fileName;
  final String fileUrl;
  final String mimeType;
  final int fileSize;

  /// UUID (String) du compte, pas un int.
  final String? uploadedById;
  final String? uploadedByName;
  final DateTime uploadedAt;
}

// ── Commentaires ────────────────────────────────────────────────────────────

class TaskComment {
  const TaskComment({
    required this.id,
    required this.taskId,
    required this.authorId,
    this.authorName,
    required this.content,
    this.parentCommentId,
    this.parentAuthorName,
    required this.createdAt,
    this.updatedAt,
    this.isSystem = false,
  });

  factory TaskComment.fromJson(Map<String, dynamic> json) => TaskComment(
        id: _i(json['id']) ?? 0,
        taskId: _i(json['taskId']) ?? 0,
        authorId: _str(json['authorId']),
        authorName: _s(json['authorName']),
        content: _str(json['content']),
        parentCommentId: _i(json['parentCommentId']),
        parentAuthorName: _s(json['parentAuthorName']),
        createdAt: _dt(json['createdAt']) ?? DateTime.fromMillisecondsSinceEpoch(0),
        updatedAt: _dt(json['updatedAt']),
        isSystem: _b(json['isSystem']),
      );

  final int id;
  final int taskId;

  /// UUID (String) — la vue createComment n'inclut PAS authorName, d'où son
  /// caractère optionnel côté affichage.
  final String authorId;
  final String? authorName;
  final String content;
  final int? parentCommentId;
  final String? parentAuthorName;
  final DateTime createdAt;
  final DateTime? updatedAt;
  final bool isSystem;
}

// ── Dépendances ─────────────────────────────────────────────────────────────

class TaskDependency {
  const TaskDependency({
    required this.id,
    required this.taskId,
    required this.dependsOnTaskId,
    this.dependsOnTaskTitle,
    this.type = DependencyType.blocks,
  });

  factory TaskDependency.fromJson(Map<String, dynamic> json) =>
      TaskDependency(
        id: _i(json['id']) ?? 0,
        taskId: _i(json['taskId']) ?? 0,
        dependsOnTaskId: _i(json['dependsOnTaskId']) ?? 0,
        dependsOnTaskTitle: _s(json['dependsOnTaskTitle']),
        type: DependencyType.fromWire(_s(json['type'])),
      );

  final int id;
  final int taskId;
  final int dependsOnTaskId;

  /// Présent en liste, absent à la création (le client garde le titre en
  /// mémoire ou le réaffiche après rechargement).
  final String? dependsOnTaskTitle;
  final DependencyType type;
}

// ── Templates ───────────────────────────────────────────────────────────────

class TaskTemplate {
  const TaskTemplate({
    required this.id,
    required this.name,
    this.description,
    this.type = TaskType.task,
    this.priority = TaskPriority.medium,
    this.estimatedHours,
    this.defaultTags,
    this.subtasks,
    this.departmentId,
    this.departmentName,
    this.isActive = true,
    required this.createdAt,
    this.updatedAt,
  });

  factory TaskTemplate.fromJson(Map<String, dynamic> json) => TaskTemplate(
        id: _i(json['id']) ?? 0,
        name: _str(json['name']),
        description: _s(json['description']),
        type: TaskType.fromWire(_s(json['type'])),
        priority: TaskPriority.fromWire(_s(json['priority'])),
        // La vue serveur renvoie un Integer (pas une String comme l'ancien
        // modèle freezed — le cast dur `as String?` plantait sur les valeurs
        // non nulles).
        estimatedHours: _i(json['estimatedHours']),
        defaultTags: _tags(json['defaultTags']),
        subtasks: _list(json['subtasks'], TaskTemplateSubtask.fromJson),
        departmentId: _i(json['departmentId']),
        departmentName: _s(json['departmentName']),
        isActive: _b(json['isActive'], fallback: true),
        createdAt: _dt(json['createdAt']) ?? DateTime.fromMillisecondsSinceEpoch(0),
        updatedAt: _dt(json['updatedAt']),
      );

  final int id;
  final String name;
  final String? description;
  final TaskType type;
  final TaskPriority priority;
  final int? estimatedHours;
  final List<String>? defaultTags;

  /// Jamais émis par listTemplates — sous-tâches client uniquement.
  final List<TaskTemplateSubtask>? subtasks;
  final int? departmentId;
  final String? departmentName;
  final bool isActive;
  final DateTime createdAt;
  final DateTime? updatedAt;
}

class TaskTemplateSubtask {
  const TaskTemplateSubtask({
    required this.id,
    required this.templateId,
    required this.title,
    this.description,
    this.priority,
    this.estimatedHours,
    this.order,
  });

  factory TaskTemplateSubtask.fromJson(Map<String, dynamic> json) =>
      TaskTemplateSubtask(
        id: _i(json['id']) ?? 0,
        templateId: _i(json['templateId']) ?? 0,
        title: _str(json['title']),
        description: _s(json['description']),
        priority: json['priority'] == null
            ? null
            : TaskPriority.fromWire(_s(json['priority'])),
        estimatedHours: _i(json['estimatedHours']),
        order: _i(json['order']),
      );

  final int id;
  final int templateId;
  final String title;
  final String? description;
  final TaskPriority? priority;
  final int? estimatedHours;
  final int? order;
}

// ── Kanban ──────────────────────────────────────────────────────────────────

class KanbanColumn {
  const KanbanColumn({
    required this.id,
    required this.name,
    required this.status,
    this.order = 0,
    this.wipLimit,
    this.color,
    this.isActive = true,
  });

  factory KanbanColumn.fromJson(Map<String, dynamic> json) => KanbanColumn(
        id: _i(json['id']) ?? 0,
        name: _str(json['name']),
        status: TaskStatus.fromWire(_s(json['status'])),
        order: _i(json['order']) ?? 0,
        wipLimit: _i(json['wipLimit']),
        color: _s(json['color']),
        isActive: _b(json['isActive'], fallback: true),
      );

  final int id;
  final String name;
  final TaskStatus status;
  final int order;
  final int? wipLimit;

  /// Chaîne hexadécimale (#RRGGBB) côté serveur — pas une int couleur.
  final String? color;
  final bool isActive;

  /// Corps de PUT /tasks/kanban/columns/{id} — clés lues par le serveur :
  /// name, status, order, wipLimit, color, isActive.
  static Map<String, dynamic> updateBody({
    String? name,
    TaskStatus? status,
    int? order,
    int? wipLimit,
    String? color,
    bool? isActive,
  }) =>
      {
        if (name != null) 'name': name,
        if (status != null) 'status': status.wire,
        if (order != null) 'order': order,
        if (wipLimit != null) 'wipLimit': wipLimit,
        if (color != null) 'color': color,
        if (isActive != null) 'isActive': isActive,
      };
}

// ── Suivi temps ─────────────────────────────────────────────────────────────

class TaskTimeEntry {
  const TaskTimeEntry({
    required this.id,
    required this.taskId,
    required this.userId,
    this.userName,
    required this.startTime,
    this.endTime,
    this.durationMinutes,
    this.description,
    this.createdAt,
  });

  factory TaskTimeEntry.fromJson(Map<String, dynamic> json) => TaskTimeEntry(
        id: _i(json['id']) ?? 0,
        taskId: _i(json['taskId']) ?? 0,
        // UUID (String) de l'utilisateur — l'ancien `required int userId`
        // crashait au parsing (le serveur renvoie une chaîne UUID).
        userId: _str(json['userId']),
        // Absent des réponses start/stop, présent en liste.
        userName: _s(json['userName']),
        startTime: _dt(json['startTime']) ?? DateTime.fromMillisecondsSinceEpoch(0),
        endTime: _dt(json['endTime']),
        durationMinutes: _i(json['durationMinutes']),
        description: _s(json['description']),
        // Absent de la réponse stop.
        createdAt: _dt(json['createdAt']),
      );

  final int id;
  final int taskId;
  final String userId;
  final String? userName;
  final DateTime startTime;
  final DateTime? endTime;
  final int? durationMinutes;
  final String? description;
  final DateTime? createdAt;

  bool get isRunning => endTime == null;
}
