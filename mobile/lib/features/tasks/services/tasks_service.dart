import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/tasks/models/task_model.dart';

part 'tasks_service.g.dart';

@riverpod
TasksService tasksService(TasksServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return TasksService(api);
}

class TasksService {
  final ApiService _api;

  TasksService(this._api);

  // Tasks
  Future<List<Task>> getTasks({
    int page = 0,
    int size = 20,
    TaskStatus? status,
    TaskPriority? priority,
    TaskType? type,
    int? assignedToId,
    int? projectId,
    int? departmentId,
    DateTime? fromDate,
    DateTime? toDate,
    String? search,
    bool? overdueOnly,
    bool? myTasks,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (status != null) 'status': status.name,
        if (priority != null) 'priority': priority.name,
        if (type != null) 'type': type.name,
        if (assignedToId != null) 'assignedToId': assignedToId,
        if (projectId != null) 'projectId': projectId,
        if (departmentId != null) 'departmentId': departmentId,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
        if (search != null && search.isNotEmpty) 'search': search,
        if (overdueOnly == true) 'overdue': 'true',
        if (myTasks == true) 'myTasks': 'true',
      };
      final response = await _api.get('/tasks', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Task.fromJson(json as Map<String, dynamic>)).toList();
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

  Future<Task> createTask(Task task) async {
    try {
      final response = await _api.post('/tasks', data: task.toJson());
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<Task> updateTask(int id, Task task) async {
    try {
      final response = await _api.put('/tasks/$id', data: task.toJson());
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<void> deleteTask(int id) async {
    try {
      await _api.delete('/tasks/$id');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  Future<Task> updateTaskStatus(int id, TaskStatus status) async {
    try {
      final response = await _api.patch('/tasks/$id/status', data: {'status': status.name});
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour du statut: $e');
    }
  }

  Future<Task> assignTask(int id, int userId) async {
    try {
      final response = await _api.patch('/tasks/$id/assign', data: {'assignedToId': userId});
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'assignation: $e');
    }
  }

  // Subtasks
  Future<List<Task>> getSubtasks(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/subtasks');
      final data = response.data as List;
      return data.map((json) => Task.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des sous-tâches: $e');
    }
  }

  Future<Task> createSubtask(int parentTaskId, Task subtask) async {
    try {
      final response = await _api.post('/tasks/$parentTaskId/subtasks', data: subtask.toJson());
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Attachments
  Future<List<TaskAttachment>> getAttachments(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/attachments');
      final data = response.data as List;
      return data.map((json) => TaskAttachment.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des pièces jointes: $e');
    }
  }

  Future<TaskAttachment> uploadAttachment(int taskId, String filePath, String fileName, String mimeType) async {
    try {
      // TODO: Implement file upload
      throw Exception('Upload de fichier à implémenter');
    } catch (e) {
      throw Exception('Erreur lors de l\'upload: $e');
    }
  }

  Future<void> deleteAttachment(int attachmentId) async {
    try {
      await _api.delete('/tasks/attachments/$attachmentId');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // Comments
  Future<List<TaskComment>> getComments(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/comments');
      final data = response.data as List;
      return data.map((json) => TaskComment.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des commentaires: $e');
    }
  }

  Future<TaskComment> addComment(int taskId, String content, {int? parentCommentId}) async {
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

  // Dependencies
  Future<List<TaskDependency>> getDependencies(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/dependencies');
      final data = response.data as List;
      return data.map((json) => TaskDependency.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des dépendances: $e');
    }
  }

  Future<TaskDependency> addDependency(int taskId, int dependsOnTaskId, DependencyType type) async {
    try {
      final response = await _api.post('/tasks/$taskId/dependencies', data: {
        'dependsOnTaskId': dependsOnTaskId,
        'type': type.name,
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

  // Kanban
  Future<List<KanbanColumn>> getKanbanColumns({int? projectId}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (projectId != null) queryParams['projectId'] = projectId;
      final response = await _api.get('/tasks/kanban/columns', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => KanbanColumn.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des colonnes: $e');
    }
  }

  Future<KanbanColumn> updateKanbanColumn(int id, KanbanColumn column) async {
    try {
      final response = await _api.put('/tasks/kanban/columns/$id', data: column.toJson());
      return KanbanColumn.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<void> reorderTasks(int taskId, TaskStatus newStatus, int newOrder) async {
    try {
      await _api.post('/tasks/$taskId/reorder', data: {
        'status': newStatus.name,
        'order': newOrder,
      });
    } catch (e) {
      throw Exception('Erreur lors du réordonnancement: $e');
    }
  }

  // Time Tracking
  Future<List<TaskTimeEntry>> getTimeEntries(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/time-entries');
      final data = response.data as List;
      return data.map((json) => TaskTimeEntry.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des entrées de temps: $e');
    }
  }

  Future<TaskTimeEntry> startTimeEntry(int taskId, {String? description}) async {
    try {
      final response = await _api.post('/tasks/$taskId/time-entries/start', data: {
        'description': description,
      });
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

  Future<int> getTotalTimeSpent(int taskId) async {
    try {
      final response = await _api.get('/tasks/$taskId/time-total');
      return (response.data as Map<String, dynamic>)['totalMinutes'] as int? ?? 0;
    } catch (e) {
      return 0;
    }
  }

  // Templates
  Future<List<TaskTemplate>> getTemplates({int? departmentId, bool? isActive}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (departmentId != null) queryParams['departmentId'] = departmentId;
      if (isActive != null) queryParams['isActive'] = isActive.toString();
      final response = await _api.get('/tasks/templates', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => TaskTemplate.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des modèles: $e');
    }
  }

  Future<Task> createTaskFromTemplate(int templateId, {int? projectId, int? assignedToId}) async {
    try {
      final response = await _api.post('/tasks/templates/$templateId/create', data: {
        if (projectId != null) 'projectId': projectId,
        if (assignedToId != null) 'assignedToId': assignedToId,
      });
      return Task.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création depuis le modèle: $e');
    }
  }

  // Reports
  Future<Map<String, dynamic>> getTaskStatistics({DateTime? fromDate, DateTime? toDate, int? projectId, int? departmentId}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (fromDate != null) queryParams['fromDate'] = fromDate.toIso8601String();
      if (toDate != null) queryParams['toDate'] = toDate.toIso8601String();
      if (projectId != null) queryParams['projectId'] = projectId;
      if (departmentId != null) queryParams['departmentId'] = departmentId;
      final response = await _api.get('/tasks/reports/statistics', queryParameters: queryParams);
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement des statistiques: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getTasksByStatus() async {
    try {
      final response = await _api.get('/tasks/reports/by-status');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par statut: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getTasksByAssignee() async {
    try {
      final response = await _api.get('/tasks/reports/by-assignee');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport par assigné: $e');
    }
  }

  Future<List<Map<String, dynamic>>> getOverdueTasks() async {
    try {
      final response = await _api.get('/tasks/overdue');
      final data = response.data as List;
      return data.map((json) => json as Map<String, dynamic>).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des tâches en retard: $e');
    }
  }
}