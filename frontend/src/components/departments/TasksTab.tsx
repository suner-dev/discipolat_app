import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  ListTodo, Plus, Trash2, Clock, AlertTriangle, CheckCircle2,
  CalendarDays, Flag,
} from 'lucide-react';
import type { Team, Task } from './types';
import { STATUT_TASK_BADGE, PRIORITE_COLORS } from './types';

export function TasksTab({ taskStats, teams, members, deptId, onChanged }: {
  taskStats: any; teams: Team[]; members: any[]; deptId: string; onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const { data: tasks = [] } = useQuery({
    queryKey: ['department', deptId, 'tasks'],
    queryFn: async () => (await api.get(`/departments/${deptId}/tasks`)).data as Task[],
    enabled: !!deptId,
  });
  const [titre, setTitre] = useState('');
  const [teamId, setTeamId] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [priorite, setPriorite] = useState('MOYENNE');
  const [echeance, setEcheance] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/tasks`, {
        titre, teamId: teamId || null, assignedTo: assignedTo || null, priorite, echeance: echeance || null,
      });
    },
    onSuccess: () => { toast.success('Tâche créée ✅'); setTitre(''); setTeamId(''); setAssignedTo(''); setPriorite('MOYENNE'); setEcheance(''); setShowCreate(false); invalidateAll(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ taskId, data }: { taskId: string; data: any }) =>
      (await api.put(`/departments/${deptId}/tasks/${taskId}`, data)).data,
    onSuccess: () => invalidateAll(),
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const deleteMutation = useMutation({
    mutationFn: async (taskId: string) => api.delete(`/departments/${deptId}/tasks/${taskId}`),
    onSuccess: () => { toast.success('Tâche supprimée'); invalidateAll(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function invalidateAll() {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'tasks'] });
    queryClient.invalidateQueries({ queryKey: ['department'] });
    onChanged();
  }

  const setStatus = (task: Task, statut: string) =>
    updateMutation.mutate({ taskId: task.id, data: { titre: task.titre, statut, priorite: task.priorite, avancement: task.avancement } });
  const setAvancement = (task: Task, avancement: number) =>
    updateMutation.mutate({ taskId: task.id, data: { titre: task.titre, statut: task.statut, priorite: task.priorite, avancement } });

  const kpis = [
    { label: 'En cours', value: taskStats.enCours ?? 0, color: 'text-blue-500', icon: Clock },
    { label: 'À faire', value: taskStats.aFaire ?? 0, color: 'text-gray-500', icon: ListTodo },
    { label: 'En retard', value: taskStats.enRetard ?? 0, color: 'text-red-500', icon: AlertTriangle },
    { label: 'Terminées', value: (taskStats.terminees ?? 0) + (taskStats.validees ?? 0), color: 'text-emerald-500', icon: CheckCircle2 },
  ];

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {kpis.map((k) => {
          const Icon = k.icon;
          return (
            <div key={k.label} className="stat-card p-3 text-center">
              <Icon className={`w-4 h-4 mx-auto mb-1 ${k.color}`} />
              <p className={`stat-value text-xl ${k.color}`}>{k.value}</p>
              <span className="stat-label text-[10px]">{k.label}</span>
            </div>
          );
        })}
      </div>

      <div className="glass-card p-5">
        <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <ListTodo className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Tâches ({tasks.length})</h3>
          </div>
          <button onClick={() => setShowCreate(!showCreate)} className="btn-primary btn-sm cursor-pointer">
            <Plus className="w-4 h-4" /> {showCreate ? 'Fermer' : 'Nouvelle tâche'}
          </button>
        </div>

        {showCreate && (
          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div>
                <label className="label">Titre *</label>
                <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)} placeholder="Ex : Préparer la console son…" />
              </div>
              <div>
                <label className="label">Assignée à</label>
                <select className="input" value={assignedTo} onChange={(e) => setAssignedTo(e.target.value)}>
                  <option value="">— Non assignée —</option>
                  {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Équipe</label>
                <select className="input" value={teamId} onChange={(e) => setTeamId(e.target.value)}>
                  <option value="">— Aucune —</option>
                  {teams.filter((t) => t.statut === 'ACTIVE').map((t) => <option key={t.id} value={t.id}>{t.nom}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="label">Priorité</label>
                  <select className="input" value={priorite} onChange={(e) => setPriorite(e.target.value)}>
                    <option value="BASSE">Basse</option>
                    <option value="MOYENNE">Moyenne</option>
                    <option value="HAUTE">Haute</option>
                  </select>
                </div>
                <div>
                  <label className="label">Échéance</label>
                  <input type="date" className="input" value={echeance} onChange={(e) => setEcheance(e.target.value)} />
                </div>
              </div>
            </div>
            <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || createMutation.isPending} className="btn-primary btn-sm mt-3 cursor-pointer">
              <Plus className="w-4 h-4" /> Créer la tâche
            </button>
          </div>
        )}

        {tasks.length === 0 ? (
          <div className="text-center py-8">
            <ListTodo className="w-10 h-10 text-gray-300 mx-auto mb-2" />
            <p className="text-sm text-gray-400">Aucune tâche</p>
          </div>
        ) : (
          <div className="space-y-2">
            {tasks.map((t) => (
              <div key={t.id} className={`p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border ${t.enRetard ? 'border-red-300/50 dark:border-red-500/30' : 'border-transparent'}`}>
                <div className="flex items-center gap-3 flex-wrap">
                  <Flag className={`w-4 h-4 shrink-0 ${PRIORITE_COLORS[t.priorite] || 'text-gray-400'}`} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`text-sm font-semibold ${t.statut === 'ANNULEE' ? 'line-through text-gray-400' : 'text-gray-900 dark:text-gray-100'}`}>{t.titre}</span>
                      {t.enRetard && <span className="badge text-[9px] badge-danger flex items-center gap-1"><AlertTriangle className="w-2.5 h-2.5" /> En retard</span>}
                    </div>
                    <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5 flex-wrap">
                      {t.assigneeNom && <span>👤 {t.assigneeNom}</span>}
                      {t.teamNom && <span>· {t.teamNom}</span>}
                      {t.echeance && <span className="flex items-center gap-0.5">· <CalendarDays className="w-3 h-3" /> {t.echeance}</span>}
                    </div>
                    <div className="flex items-center gap-2 mt-2">
                      <input
                        type="range" min={0} max={100} step={5}
                        value={t.avancement}
                        onChange={(e) => setAvancement(t, Number(e.target.value))}
                        className="w-40 accent-emerald-500"
                      />
                      <span className="text-[10px] font-bold text-emerald-600">{t.avancement}%</span>
                    </div>
                  </div>
                  <select
                    value={t.statut}
                    onChange={(e) => setStatus(t, e.target.value)}
                    className="input w-32 py-1 text-xs"
                  >
                    {Object.entries(STATUT_TASK_BADGE).map(([k]) => (
                      <option key={k} value={k}>{k.replace('_', ' ').toLowerCase()}</option>
                    ))}
                  </select>
                  <button onClick={() => deleteMutation.mutate(t.id)} title="Supprimer" className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer">
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

