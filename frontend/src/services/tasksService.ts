import api from '@/lib/api';

/**
 * Contrat exact du backend V234 — TaskController
 * (backend/.../tasks/api/TaskController.java), monture {@code /api/v1/tasks}.
 *
 * Identifiants BIGSERIAL (task/subtask/comment/attachment/dependency/
 * kanbanColumn/timeEntry/template) = {@code number} ; référence personne
 * {@code assignedToId} = UUID {@code string}. Réponses serveur typées
 * {@link Json} (aucune interface inventée).
 */
export type Json = Record<string, unknown>;

export interface TasksQuery {
  page?: number;
  size?: number;
  status?: string;
  priority?: string;
  type?: string;
  assignedToId?: string;
  projectId?: number;
  departmentId?: number;
  search?: string;
  overdue?: boolean;
  myTasks?: boolean;
}

// ---------- CRUD tâche ----------

export function listTasks(query: TasksQuery = {}): Promise<Json[]> {
  return api.get<Json[]>('/tasks', { params: query }).then((r) => r.data);
}

export function getTask(id: number): Promise<Json> {
  return api.get<Json>(`/tasks/${id}`).then((r) => r.data);
}

export function createTask(body: Json): Promise<Json> {
  return api.post<Json>('/tasks', body).then((r) => r.data);
}

export function updateTask(id: number, body: Json): Promise<Json> {
  return api.put<Json>(`/tasks/${id}`, body).then((r) => r.data);
}

export function deleteTask(id: number): Promise<void> {
  return api.delete(`/tasks/${id}`).then(() => undefined);
}

export function updateTaskStatus(id: number, status: string): Promise<Json> {
  return api.patch<Json>(`/tasks/${id}/status`, { status }).then((r) => r.data);
}

export function assignTask(id: number, assignedToId: string): Promise<Json> {
  return api.patch<Json>(`/tasks/${id}/assign`, { assignedToId }).then((r) => r.data);
}

// ---------- Sous-tâches ----------

export function listSubtasks(taskId: number): Promise<Json[]> {
  return api.get<Json[]>(`/tasks/${taskId}/subtasks`).then((r) => r.data);
}

export function createSubtask(parentTaskId: number, body: Json): Promise<Json> {
  return api.post<Json>(`/tasks/${parentTaskId}/subtasks`, body).then((r) => r.data);
}

// ---------- Pièces jointes ----------

export function listAttachments(taskId: number): Promise<Json[]> {
  return api.get<Json[]>(`/tasks/${taskId}/attachments`).then((r) => r.data);
}

export function deleteAttachment(attachmentId: number): Promise<void> {
  return api.delete(`/tasks/attachments/${attachmentId}`).then(() => undefined);
}

// ---------- Commentaires ----------

export function listComments(taskId: number): Promise<Json[]> {
  return api.get<Json[]>(`/tasks/${taskId}/comments`).then((r) => r.data);
}

export function createComment(taskId: number, body: Json): Promise<Json> {
  return api.post<Json>(`/tasks/${taskId}/comments`, body).then((r) => r.data);
}

export function deleteComment(commentId: number): Promise<void> {
  return api.delete(`/tasks/comments/${commentId}`).then(() => undefined);
}

// ---------- Dépendances ----------

export function listDependencies(taskId: number): Promise<Json[]> {
  return api.get<Json[]>(`/tasks/${taskId}/dependencies`).then((r) => r.data);
}

export function createDependency(taskId: number, body: Json): Promise<Json> {
  return api.post<Json>(`/tasks/${taskId}/dependencies`, body).then((r) => r.data);
}

export function deleteDependency(dependencyId: number): Promise<void> {
  return api.delete(`/tasks/dependencies/${dependencyId}`).then(() => undefined);
}

// ---------- Kanban ----------

export function listKanbanColumns(): Promise<Json[]> {
  return api.get<Json[]>('/tasks/kanban/columns').then((r) => r.data);
}

export function updateKanbanColumn(id: number, body: Json): Promise<Json> {
  return api.put<Json>(`/tasks/kanban/columns/${id}`, body).then((r) => r.data);
}

export function reorderTask(taskId: number, status: string | null, order: number | null): Promise<Json> {
  return api.post<Json>(`/tasks/${taskId}/reorder`, { status, order }).then((r) => r.data);
}

// ---------- Suivi du temps ----------

export function listTimeEntries(taskId: number): Promise<Json[]> {
  return api.get<Json[]>(`/tasks/${taskId}/time-entries`).then((r) => r.data);
}

export function startTimeEntry(taskId: number, body?: Json): Promise<Json> {
  return api.post<Json>(`/tasks/${taskId}/time-entries/start`, body ?? {}).then((r) => r.data);
}

export function stopTimeEntry(timeEntryId: number): Promise<Json> {
  return api.post<Json>(`/tasks/time-entries/${timeEntryId}/stop`).then((r) => r.data);
}

export function getTimeTotal(taskId: number): Promise<Json> {
  return api.get<Json>(`/tasks/${taskId}/time-total`).then((r) => r.data);
}

// ---------- Modèles ----------

export function listTemplates(page = 0, size = 20): Promise<Json[]> {
  return api.get<Json[]>('/tasks/templates', { params: { page, size } }).then((r) => r.data);
}

export function createFromTemplate(templateId: number, body?: Json): Promise<Json> {
  return api.post<Json>(`/tasks/templates/${templateId}/create`, body ?? {}).then((r) => r.data);
}

// ---------- Rapports ----------

export function reportStatistics(): Promise<Json> {
  return api.get<Json>('/tasks/reports/statistics').then((r) => r.data);
}

export function reportByStatus(): Promise<Json[]> {
  return api.get<Json[]>('/tasks/reports/by-status').then((r) => r.data);
}

export function reportByAssignee(): Promise<Json[]> {
  return api.get<Json[]>('/tasks/reports/by-assignee').then((r) => r.data);
}

export function listOverdue(): Promise<Json[]> {
  return api.get<Json[]>('/tasks/overdue').then((r) => r.data);
}
