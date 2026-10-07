import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/tasks/models/task_model.dart';

part 'tasks_service.g.dart';

/// Service tasks — branché sur le contrat RÉEL du backend
/// (backend/…/modules/tasks/api/TaskController.java, `/api/v1/tasks`, V234).
///
/// Règles issues de la lecture du serveur :
/// - ids structurels (task, comment, attachment…) = Long → int Dart ;
/// - références personnes = UUID → String (le serveur fait
///   `UUID.fromString` sur assignedToId, un int partirait en 400) ;
/// - enums envoyés en NOM SERVEUR (`IN_PROGRESS`, pas `inProgress`) — le
///   serveur normalise les deux mais le contrat strict évite toute surprise ;
/// - dates envoyées en ISO-8601 UTC : `Instant.parse` côté serveur lève une
///   500 sur tout autre format ;
/// - aucun endpoint d'upload de pièce jointe n'existe : liste et suppression
///   seulement (ne rien inventer) ;
/// - DELETE /tasks/{id} est un archivage statut CANCELLED côté serveur (R9
///   « pas de purge »), pas une suppression physique.
@riverpod
TasksService tasksService(TasksServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return TasksService(api);
}

class TasksService {
  final ApiService _api;

  TasksService(this._api);

  // ── Tâches ───────────────────────────────────────────────────────────────

  /// GET /tasks — seuls les filtres réellement lus par le contrôleur.
  /// (fromDate/toDate n'existent pas côté serveur : les envoyer était du
  /// bruit silencieusement ignoré.)
  Future<List<Task>> getTasks({
    int page = 0,
    int size = 20,
    TaskStatus? status,
    TaskPriority? priority,
    TaskType? type,
    String? assignedToId,
    int? projectId,
    int? departmentId,
    String? search,
    bool? overdue,
    bool? myTasks,
  }) async {
    try {
      final response = await _api.get('/tasks', queryParameters: {
        'page': page,
        'size': size,
        if (status != null) 'status': status.wire,
        if (priority != null) 'priority': priority.wire,
        if (type != null) 'type': type.wire,
        if (assignedToId != null) 'assignedToId': assignedToId,
        if (projectId != null) 'projectId': projectId,
        if (departmentId != null) 'departmentId': departmentId,
        if (search != null && search.trim().isNotEmpty) 'search': search.trim(),
        if (overdue == true) 'overdue': 'true',
        if (myTasks == true) 'myTasks': 'true',
      });
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(Task.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des tâches: $e');
    }
  }

  Future<Task> getTask(int id) async {
    try {
      final response = await _api.get('/tasks/$id');
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la tâche: $e');
    }
  }

  Future<Task> createTask({
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
  }) async {
    try {
      final response = await _api.post('/tasks', data: Task.createBody(
            title: title,
            description: description,
            type: type,
            priority: priority,
            projectId: projectId,
            assignedToId: assignedToId,
            departmentId: departmentId,
            dueDate: dueDate,
            startDate: startDate,
            estimatedHours: estimatedHours,
            tags: tags,
            parentTaskId: parentTaskId,
            recurrencePattern: recurrencePattern,
            recurrenceEndDate: recurrenceEndDate,
          ));
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  /// PUT /tasks/{id} — le serveur n'applique QUE les clés présentes.
  Future<Task> updateTask(int id, {
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
  }) async {
    try {
      final response = await _api.put('/tasks/$id', data: Task.updateBody(
            title: title,
            description: description,
            type: type,
            priority: priority,
            status: status,
            projectId: projectId,
            assignedToId: assignedToId,
            departmentId: departmentId,
            dueDate: dueDate,
            startDate: startDate,
            estimatedHours: estimatedHours,
            actualHours: actualHours,
            tags: tags,
            parentTaskId: parentTaskId,
            recurrencePattern: recurrencePattern,
            recurrenceEndDate: recurrenceEndDate,
          ));
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  /// Archivage (statut CANCELLED côté serveur, R9 « pas de purge »).
  Future<void> deleteTask(int id) async {
    try {
      await _api.delete('/tasks/$id');
    } catch (e) {
      throw Exception('Erreur lors de l\'archivage: $e');
    }
  }

  Future<Task> updateTaskStatus(int id, TaskStatus status) async {
    try {
      final response = await _api.patch('/tasks/$id/status',
          data: {'status': status.wire});
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour du statut: $e');
    }
  }

  /// [userId] = UUID String (le serveur fait `UUID.fromString`).
  Future<Task> assignTask(int id, String userId) async {
    try {
      final response = await _api.patch('/tasks/$id/assign',
          data: {'assignedToId': userId});
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'assignation: $e');
    }
  }

  // ── Sous-tâches ──────────────────────────────────────────────────────────

  Future<List<Task>> getSubtasks(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/subtasks');
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(Task.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des sous-tâches: $e');
    }
  }

  Future<Task> createSubtask(int parentTaskId,
      {required String title, String? description, TaskPriority? priority}) async {
    try {
      final response = await _api.post('/tasks/$parentTaskId/subtasks',
          data: {
            'title': title,
            if (description != null && description.isNotEmpty)
              'description': description,
            if (priority != null) 'priority': priority.wire,
          });
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // ── Pièces jointes (pas d'endpoint d'upload côté serveur) ───────────────

  Future<List<TaskAttachment>> getAttachments(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/attachments');
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(TaskAttachment.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des pièces jointes: $e');
    }
  }

  Future<void> deleteAttachment(int attachmentId) async {
    try {
      await _api.delete('/tasks/attachments/$attachmentId');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // ── Commentaires ─────────────────────────────────────────────────────────

  Future<List<TaskComment>> getComments(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/comments');
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(TaskComment.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des commentaires: $e');
    }
  }

  Future<TaskComment> addComment(int taskId, String content,
      {int? parentCommentId}) async {
    try {
      final response = await _api.post('/tasks/$taskId/comments', data: {
        'content': content,
        if (parentCommentId != null) 'parentCommentId': parentCommentId,
      });
      return TaskComment.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout du commentaire: $e');
    }
  }

  Future<void> deleteComment(int commentId) async {
    try {
      await _api.delete('/tasks/comments/$commentId');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // ── Dépendances ──────────────────────────────────────────────────────────

  Future<List<TaskDependency>> getDependencies(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/dependencies');
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(TaskDependency.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des dépendances: $e');
    }
  }

  Future<TaskDependency> addDependency(
      int taskId, int dependsOnTaskId, DependencyType type) async {
    try {
      final response = await _api.post('/tasks/$taskId/dependencies', data: {
        'dependsOnTaskId': dependsOnTaskId,
        'type': type.wire,
      });
      return TaskDependency.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout de la dépendance: $e');
    }
  }

  Future<void> removeDependency(int dependencyId) async {
    try {
      await _api.delete('/tasks/dependencies/$dependencyId');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // ── Kanban ───────────────────────────────────────────────────────────────

  /// GET /kanban/columns — le serveur n'accepte AUCUN paramètre (l'ancien
  /// `projectId` envoyé était ignoré ; colonnes = actif + ordre croissant).
  Future<List<KanbanColumn>> getKanbanColumns() async {
    try {
      final response = await _api.get('/tasks/kanban/columns');
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(KanbanColumn.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des colonnes: $e');
    }
  }

  Future<KanbanColumn> updateKanbanColumn(int id, {
    String? name,
    TaskStatus? status,
    int? order,
    int? wipLimit,
    String? color,
    bool? isActive,
  }) async {
    try {
      final response = await _api.put('/tasks/kanban/columns/$id',
          data: KanbanColumn.updateBody(
            name: name,
            status: status,
            order: order,
            wipLimit: wipLimit,
            color: color,
            isActive: isActive,
          ));
      return KanbanColumn.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  /// POST /tasks/{id}/reorder — persiste statut ET rang (V239 serveur).
  Future<void> reorderTask(int taskId, {TaskStatus? status, int? order}) async {
    try {
      await _api.post('/tasks/$taskId/reorder', data: {
        if (status != null) 'status': status.wire,
        if (order != null) 'order': order,
      });
    } catch (e) {
      throw Exception('Erreur lors du réordonnancement: $e');
    }
  }

  // ── Suivi du temps ───────────────────────────────────────────────────────

  Future<List<TaskTimeEntry>> getTimeEntries(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/time-entries');
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(TaskTimeEntry.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des entrées de temps: $e');
    }
  }

  Future<TaskTimeEntry> startTimeEntry(int taskId, {String? description}) async {
    try {
      final response = await _api.post('/tasks/$taskId/time-entries/start',
          data: {if (description != null) 'description': description});
      return TaskTimeEntry.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du démarrage du chronomètre: $e');
    }
  }

  Future<TaskTimeEntry> stopTimeEntry(int timeEntryId) async {
    try {
      final response = await _api.post('/tasks/time-entries/$timeEntryId/stop');
      return TaskTimeEntry.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'arrêt du chronomètre: $e');
    }
  }

  /// GET /tasks/{id}/time-total → {taskId, totalMinutes}.
  Future<int> getTotalTimeSpent(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/time-total');
      return (response.data as Map<String, dynamic>)['totalMinutes'] as int? ?? 0;
    } catch (e) {
      return 0;
    }
  }

  // ── Templates ────────────────────────────────────────────────────────────

  /// GET /tasks/templates — le serveur ne prend que page/size (les filtres
  /// departmentId/isActive inventés étaient ignorés).
  Future<List<TaskTemplate>> getTemplates({int page = 0, int size = 20}) async {
    try {
      final response = await _api.get('/tasks/templates',
          queryParameters: {'page': page, 'size': size});
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(TaskTemplate.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des modèles: $e');
    }
  }

  /// POST /tasks/templates/{id}/create — le serveur instantiate le template
  /// uniquement ; body optionnel.
  Future<Task> createTaskFromTemplate(int templateId) async {
    try {
      final response =
          await _api.post('/tasks/templates/$templateId/create', data: {});
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création depuis le modèle: $e');
    }
  }

  // ── Rapports ─────────────────────────────────────────────────────────────

  /// GET /tasks/reports/statistics → {total, done, inProgress, blocked}.
  /// (Sans paramètres : les dates/projet/département envoyés avant étaient
  /// ignorés par le serveur.)
  Future<Map<String, dynamic>> getTaskStatistics() async {
    try {
      final response = await _api.get('/tasks/reports/statistics');
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement des statistiques: $e');
    }
  }

  /// GET /tasks/reports/by-status → [{status, count}] (statuts server names).
  Future<List<Map<String, dynamic>>> getTasksByStatus() async {
    try {
      final response = await _api.get('/tasks/reports/by-status');
      final data = response.data as List;
      return data.whereType<Map<String, dynamic>>().toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par statut: $e');
    }
  }

  /// GET /tasks/reports/by-assignee → [{assignedToId, assignedToName, count}].
  Future<List<Map<String, dynamic>>> getTasksByAssignee() async {
    try {
      final response = await _api.get('/tasks/reports/by-assignee');
      final data = response.data as List;
      return data.whereType<Map<String, dynamic>>().toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par assigné: $e');
    }
  }

  /// GET /tasks/overdue → vue taskView, typée ici (avant : Map brute).
  Future<List<Task>> getOverdueTasks() async {
    try {
      final response = await _api.get('/tasks/overdue');
      final data = response.data as List;
      return data
          .whereType<Map<String, dynamic>>()
          .map(Task.fromJson)
          .toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des tâches en retard: $e');
    }
  }
}
