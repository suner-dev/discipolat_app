import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { ClipboardList, Loader2, Plus, Trash2, Edit2 } from 'lucide-react';
import toast from 'react-hot-toast';

interface TeamTask {
  id: string;
  title: string;
  description?: string;
  status: string;
  priority: string;
  assigneeName?: string;
  dueDate?: string;
  createdAt: string;
}

const STATUS_STYLE: Record<string, string> = {
  TODO: 'text-gray-400 bg-gray-500/20',
  IN_PROGRESS: 'text-blue-400 bg-blue-500/20',
  DONE: 'text-green-400 bg-green-500/20',
  BLOCKED: 'text-red-400 bg-red-500/20',
};

const PRIORITY_STYLE: Record<string, string> = {
  LOW: 'text-gray-400',
  MEDIUM: 'text-yellow-400',
  HIGH: 'text-red-400',
};

export default function TeamTasksPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState({ title: '', description: '', priority: 'MEDIUM', status: 'TODO', dueDate: '' });

  const { data: tasks = [], isLoading } = useQuery({
    queryKey: ['team-tasks'],
    queryFn: async () => (await api.get('/team-tasks')).data as TeamTask[],
  });

  const createMutation = useMutation({
    mutationFn: async () => api.post('/team-tasks', form),
    onSuccess: () => { toast.success('Tâche créée'); resetForm(); qc.invalidateQueries({ queryKey: ['team-tasks'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const updateMutation = useMutation({
    mutationFn: async () => api.put(`/team-tasks/${editId}`, form),
    onSuccess: () => { toast.success('Tâche mise à jour'); resetForm(); qc.invalidateQueries({ queryKey: ['team-tasks'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/team-tasks/${id}`),
    onSuccess: () => { toast.success('Tâche supprimée'); qc.invalidateQueries({ queryKey: ['team-tasks'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const resetForm = () => { setShowForm(false); setEditId(null); setForm({ title: '', description: '', priority: 'MEDIUM', status: 'TODO', dueDate: '' }); };

  const startEdit = (task: TeamTask) => {
    setEditId(task.id);
    setForm({ title: task.title, description: task.description ?? '', priority: task.priority, status: task.status, dueDate: task.dueDate ?? '' });
    setShowForm(true);
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500 to-amber-600 text-white shadow-lg">
          <ClipboardList className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Tâches d'équipe</h1>
          <p className="page-subtitle">Gestion collaborative des tâches</p>
        </div>
        <button onClick={() => { resetForm(); setShowForm(true); }} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1">
          <Plus className="w-4 h-4" /> Nouvelle tâche
        </button>
      </div>

      {showForm && (
        <div className="glass-card p-6 mb-6 space-y-4 animate-slide-up">
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Titre</label>
              <input className="input w-full" placeholder="Titre de la tâche" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Priorité</label>
              <select className="input w-full" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                <option value="LOW">Basse</option>
                <option value="MEDIUM">Moyenne</option>
                <option value="HIGH">Haute</option>
              </select>
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Description</label>
            <textarea className="input w-full min-h-[60px]" placeholder="Description..." value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Statut</label>
              <select className="input w-full" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="TODO">À faire</option>
                <option value="IN_PROGRESS">En cours</option>
                <option value="DONE">Terminé</option>
                <option value="BLOCKED">Bloqué</option>
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Échéance</label>
              <input type="date" className="input w-full" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} />
            </div>
          </div>
          <div className="flex justify-end gap-3">
            <button onClick={resetForm} className="btn-sm px-4 py-2 rounded-lg glass-card">Annuler</button>
            <button onClick={() => editId ? updateMutation.mutate() : createMutation.mutate()} disabled={!form.title.trim() || createMutation.isPending || updateMutation.isPending}
              className="btn-primary btn-sm inline-flex items-center gap-1">
              {(createMutation.isPending || updateMutation.isPending) ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
              {editId ? 'Modifier' : 'Créer'}
            </button>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : tasks.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune tâche</div>
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <div key={task.id} className="glass-card p-5">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[task.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{task.status.replace(/_/g, ' ')}</span>
                    <span className={`text-xs font-medium ${PRIORITY_STYLE[task.priority] ?? 'text-gray-400'}`}>● {task.priority}</span>
                  </div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{task.title}</p>
                  {task.description && <p className="text-xs text-gray-500 mt-1">{task.description}</p>}
                  <div className="flex gap-3 mt-2 text-[11px] text-gray-500">
                    {task.assigneeName && <span>Assigné à: {task.assigneeName}</span>}
                    {task.dueDate && <span>Échéance: {new Date(task.dueDate).toLocaleDateString('fr-FR')}</span>}
                  </div>
                </div>
                <div className="flex gap-2 ml-4">
                  <button onClick={() => startEdit(task)} className="p-1.5 rounded-lg bg-white/5 text-gray-400 hover:text-white hover:bg-white/10 transition">
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button onClick={() => deleteMutation.mutate(task.id)} disabled={deleteMutation.isPending}
                    className="p-1.5 rounded-lg bg-white/5 text-red-400 hover:bg-red-500/20 transition">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
