import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '@/lib/api';
import * as svc from './tasksService';

const mock = api as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

describe('tasksService — contrat exact TaskController', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mock.get.mockResolvedValue({ data: [] });
    mock.post.mockResolvedValue({ data: {} });
    mock.put.mockResolvedValue({ data: {} });
    mock.patch.mockResolvedValue({ data: {} });
    mock.delete.mockResolvedValue({ data: {} });
  });

  it('CRUD: list (query), get, create, update, delete', async () => {
    await svc.listTasks({ status: 'OPEN', assignedToId: 'u-1', overdue: true });
    expect(mock.get).toHaveBeenCalledWith('/tasks', {
      params: { status: 'OPEN', assignedToId: 'u-1', overdue: true },
    });
    await svc.getTask(3);
    expect(mock.get).toHaveBeenCalledWith('/tasks/3');
    await svc.createTask({ title: 't' });
    expect(mock.post).toHaveBeenCalledWith('/tasks', { title: 't' });
    await svc.updateTask(3, { title: 't2' });
    expect(mock.put).toHaveBeenCalledWith('/tasks/3', { title: 't2' });
    await svc.deleteTask(3);
    expect(mock.delete).toHaveBeenCalledWith('/tasks/3');
  });

  it('status + assign en PATCH', async () => {
    await svc.updateTaskStatus(3, 'DONE');
    expect(mock.patch).toHaveBeenCalledWith('/tasks/3/status', { status: 'DONE' });
    await svc.assignTask(3, 'u-9');
    expect(mock.patch).toHaveBeenCalledWith('/tasks/3/assign', { assignedToId: 'u-9' });
  });

  it('sous-tâches, pièces jointes, commentaires, dépendances', async () => {
    await svc.listSubtasks(1);
    expect(mock.get).toHaveBeenCalledWith('/tasks/1/subtasks');
    await svc.createSubtask(1, { title: 's' });
    expect(mock.post).toHaveBeenCalledWith('/tasks/1/subtasks', { title: 's' });
    await svc.listAttachments(1);
    expect(mock.get).toHaveBeenCalledWith('/tasks/1/attachments');
    await svc.deleteAttachment(2);
    expect(mock.delete).toHaveBeenCalledWith('/tasks/attachments/2');
    await svc.listComments(1);
    expect(mock.get).toHaveBeenCalledWith('/tasks/1/comments');
    await svc.createComment(1, { text: 'c' });
    expect(mock.post).toHaveBeenCalledWith('/tasks/1/comments', { text: 'c' });
    await svc.deleteComment(4);
    expect(mock.delete).toHaveBeenCalledWith('/tasks/comments/4');
    await svc.listDependencies(1);
    expect(mock.get).toHaveBeenCalledWith('/tasks/1/dependencies');
    await svc.createDependency(1, { dependsOnId: 2 });
    expect(mock.post).toHaveBeenCalledWith('/tasks/1/dependencies', { dependsOnId: 2 });
    await svc.deleteDependency(6);
    expect(mock.delete).toHaveBeenCalledWith('/tasks/dependencies/6');
  });

  it('kanban: colonnes, mise à jour, reorder', async () => {
    await svc.listKanbanColumns();
    expect(mock.get).toHaveBeenCalledWith('/tasks/kanban/columns');
    await svc.updateKanbanColumn(1, { name: 'Fait' });
    expect(mock.put).toHaveBeenCalledWith('/tasks/kanban/columns/1', { name: 'Fait' });
    await svc.reorderTask(7, 'DONE', 2);
    expect(mock.post).toHaveBeenCalledWith('/tasks/7/reorder', { status: 'DONE', order: 2 });
  });

  it('suivi du temps', async () => {
    await svc.listTimeEntries(1);
    expect(mock.get).toHaveBeenCalledWith('/tasks/1/time-entries');
    await svc.startTimeEntry(1);
    expect(mock.post).toHaveBeenCalledWith('/tasks/1/time-entries/start', {});
    await svc.stopTimeEntry(5);
    expect(mock.post).toHaveBeenCalledWith('/tasks/time-entries/5/stop');
    await svc.getTimeTotal(1);
    expect(mock.get).toHaveBeenCalledWith('/tasks/1/time-total');
  });

  it('modèles + rapports + overdue', async () => {
    await svc.listTemplates(0, 20);
    expect(mock.get).toHaveBeenCalledWith('/tasks/templates', { params: { page: 0, size: 20 } });
    await svc.createFromTemplate(9, { dueDate: '2026-01-01' });
    expect(mock.post).toHaveBeenCalledWith('/tasks/templates/9/create', { dueDate: '2026-01-01' });
    await svc.reportStatistics();
    expect(mock.get).toHaveBeenCalledWith('/tasks/reports/statistics');
    await svc.reportByStatus();
    expect(mock.get).toHaveBeenCalledWith('/tasks/reports/by-status');
    await svc.reportByAssignee();
    expect(mock.get).toHaveBeenCalledWith('/tasks/reports/by-assignee');
    await svc.listOverdue();
    expect(mock.get).toHaveBeenCalledWith('/tasks/overdue');
  });
});
