import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TasksBoardPage from '@/pages/TasksBoardPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn((e: unknown) =>
    (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail || 'Erreur'),
}));

const svc = vi.hoisted(() => ({
  listTasks: vi.fn(),
  listKanbanColumns: vi.fn(),
  listOverdue: vi.fn(),
  reportStatistics: vi.fn(),
  reportByStatus: vi.fn(),
  reportByAssignee: vi.fn(),
  createTask: vi.fn(),
  updateTaskStatus: vi.fn(),
  reorderTask: vi.fn(),
  deleteTask: vi.fn(),
}));

vi.mock('@/services/tasksService', () => svc);

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

const TASK = {
  id: 3, title: 'Visiter un membre', description: null, type: 'TASK', priority: 'HIGH',
  status: 'TODO', assignedToId: 'a-uuid', dueDate: '2026-10-10T00:00:00Z', createdAt: '2026-10-01T08:00:00Z',
};
const COLUMN = { id: 1, name: 'À faire', status: 'TODO', order: 1, wipLimit: 0, color: '#ccc', isActive: true };

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <TasksBoardPage />
    </QueryClientProvider>
  );
}

describe('TasksBoardPage — câblée sur tasksService (V234)', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    svc.listTasks.mockResolvedValue([TASK]);
    svc.listKanbanColumns.mockResolvedValue([COLUMN]);
    svc.listOverdue.mockResolvedValue([TASK]);
    svc.reportStatistics.mockResolvedValue({ total: 12, done: 5, inProgress: 4, blocked: 3 });
    svc.reportByStatus.mockResolvedValue([{ status: 'TODO', count: 5 }, { status: 'DONE', count: 5 }]);
    svc.reportByAssignee.mockResolvedValue([{ assignedToId: 'a-uuid', assignedToName: 'Paul', count: 4 }]);
    svc.createTask.mockResolvedValue(TASK);
    svc.updateTaskStatus.mockResolvedValue(TASK);
    svc.reorderTask.mockResolvedValue(TASK);
    svc.deleteTask.mockResolvedValue(undefined);
  });

  it('charge la liste via listTasks avec les critères de query serveur', async () => {
    renderPage();
    await screen.findByText('Visiter un membre');
    expect(svc.listTasks).toHaveBeenCalled();
    expect(svc.listTasks.mock.calls[0][0]).toEqual(expect.objectContaining({ size: 50 }));
  });

  it('crée une tâche avec uniquement les clés lues par TaskService.createTask', async () => {
    const { container } = renderPage();
    await waitFor(() => expect(svc.listTasks).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /nouvelle tâche/i }));
    const inputs = container.querySelectorAll('input');
    fireEvent.change(inputs[0], { target: { value: 'Préparer la réunion' } });
    fireEvent.click(screen.getByRole('button', { name: /^créer$/i }));
    await waitFor(() => {
      expect(svc.createTask).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Préparer la réunion', type: 'TASK', priority: 'MEDIUM',
      }));
    });
    const body = svc.createTask.mock.calls[0][0];
    // Seules des clés réellement lues par TaskService.createTask (L156-171).
    for (const key of Object.keys(body)) {
      expect(['title', 'description', 'type', 'priority', 'assignedToId', 'dueDate']).toContain(key);
    }
    expect(body).not.toHaveProperty('status');
    expect(body).not.toHaveProperty('id');
  });

  it('change le statut d\'une tâche via PATCH status (updateTaskStatus)', async () => {
    renderPage();
    await screen.findByText('Visiter un membre');
    const select = document.querySelector('select.ml-auto') as HTMLSelectElement;
    fireEvent.change(select, { target: { value: 'DONE' } });
    await waitFor(() => {
      expect(svc.updateTaskStatus).toHaveBeenCalledWith(3, 'DONE');
    });
  });

  it('archive une tâche via deleteTask (statut CANCELLED côté serveur)', async () => {
    renderPage();
    await screen.findByText('Visiter un membre');
    fireEvent.click(document.querySelector('button.text-red-400') as HTMLElement);
    await waitFor(() => expect(svc.deleteTask).toHaveBeenCalledWith(3));
  });

  it('onglet kanban : colonnes + déplacement via reorderTask', async () => {
    renderPage();
    await waitFor(() => expect(svc.listTasks).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^kanban$/i }));
    await waitFor(() => {
      expect(svc.listKanbanColumns).toHaveBeenCalled();
      expect(screen.getByText(/À faire/)).toBeInTheDocument();
    });
    const moveBtn = screen.getAllByRole('button', { name: /todo|in_progress/i })[0];
    fireEvent.click(moveBtn);
    await waitFor(() => expect(svc.reorderTask).toHaveBeenCalled());
  });

  it('onglet rapports : statistiques serveur affichées', async () => {
    renderPage();
    await waitFor(() => expect(svc.listTasks).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^reports$/i }));
    await waitFor(() => {
      expect(svc.reportStatistics).toHaveBeenCalled();
      expect(screen.getByText('Paul')).toBeInTheDocument();
    });
  });

  it('onglet en retard : listOverdue consommé', async () => {
    renderPage();
    await waitFor(() => expect(svc.listTasks).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^overdue$/i }));
    await waitFor(() => {
      expect(svc.listOverdue).toHaveBeenCalled();
      expect(screen.getByText('Visiter un membre')).toBeInTheDocument();
    });
  });
});
