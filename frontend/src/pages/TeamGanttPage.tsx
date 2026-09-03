import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CalendarDays, Plus, AlertTriangle, Users, Loader2, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface TeamAssignment {
  id: string;
  equipeId: string;
  role: string;
  membreId?: string;
  debut: string;
  fin: string;
  statut: string;
  notes?: string;
}

const STATUT_COLORS: Record<string, string> = {
  EN_COURS: 'bg-green-100 text-green-700',
  PLANIFIÉE: 'bg-blue-100 text-blue-700',
  TERMINÉE: 'bg-gray-100 text-gray-500',
  ANNULÉE: 'bg-red-100 text-red-700',
};

export default function TeamGanttPage() {
  const queryClient = useQueryClient();
  const [start, setStart] = useState(() => { const d = new Date(); d.setDate(1); return d.toISOString().slice(0, 10); });
  const [end, setEnd] = useState(() => { const d = new Date(); d.setMonth(d.getMonth() + 1); d.setDate(0); return d.toISOString().slice(0, 10); });
  const [showCreate, setShowCreate] = useState(false);
  const [newAssignment, setNewAssignment] = useState({ equipeId: '', role: '', membreId: '', debut: '', fin: '', notes: '' });

  const { data: assignments = [], isLoading } = useQuery({
    queryKey: ['team-gantt', start, end],
    queryFn: async () => (await api.get<TeamAssignment[]>(`/team-gantt?start=${start}T00:00:00&end=${end}T23:59:59`)).data,
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!newAssignment.equipeId.trim() || !newAssignment.role.trim()) { toast('Remplissez les champs requis', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/team-gantt', { ...newAssignment, debut: newAssignment.debut + 'T00:00:00', fin: newAssignment.fin + 'T23:59:59' });
    },
    onSuccess: () => {
      toast.success('Affectation créée');
      setShowCreate(false);
      setNewAssignment({ equipeId: '', role: '', membreId: '', debut: '', fin: '', notes: '' });
      queryClient.invalidateQueries({ queryKey: ['team-gantt'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/team-gantt/${id}`),
    onSuccess: () => { toast.success('Supprimé'); queryClient.invalidateQueries({ queryKey: ['team-gantt'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white shadow-lg">
          <CalendarDays className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Planning Gantt d'Équipe</h1>
          <p className="page-subtitle">Planification des affectations d'équipe</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="ml-auto px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-500 text-white text-sm font-medium hover:from-indigo-600 hover:to-purple-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> Nouvelle affectation
        </button>
      </div>

      <div className="flex gap-4 mb-6">
        <div>
          <label className="block text-xs text-gray-500 mb-1">Du</label>
          <input type="date" value={start} onChange={e => setStart(e.target.value)}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Au</label>
          <input type="date" value={end} onChange={e => setEnd(e.target.value)}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
        </div>
      </div>

      {isLoading ? <SkeletonLoader lines={6} variant="table" /> :
        assignments.length === 0 ? (
          <EmptyState icon={<CalendarDays className="w-8 h-8 text-gray-400" />}
            title="Aucune affectation"
            message="Planifiez des affectations d'équipe pour cette période"
            action={{ label: 'Ajouter', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="space-y-3">
            <div className="bg-white dark:bg-white/5 rounded-xl border border-gray-200 dark:border-white/10 p-4 overflow-x-auto">
              <div className="min-w-[600px]">
                <div className="flex gap-1 mb-2 text-xs text-gray-400">
                  {Array.from({ length: 7 }).map((_, i) => {
                    const d = new Date(start);
                    d.setDate(d.getDate() + i);
                    return <div key={i} className="flex-1 text-center">{d.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric' })}</div>;
                  })}
                </div>
                {assignments.map(a => (
                  <div key={a.id} className="flex items-center gap-2 mb-2">
                    <div className="w-32 text-xs text-gray-600 dark:text-gray-400 truncate">{a.role}</div>
                    <div className="flex-1 relative h-8 bg-gray-50 dark:bg-white/5 rounded-lg">
                      <div className={`absolute top-1 bottom-1 rounded-md ${(STATUT_COLORS[a.statut] || 'bg-gray-100 text-gray-700')} flex items-center px-2`}
                        style={{ left: '10%', width: '30%' }}>
                        <span className="text-xs font-medium truncate">{a.role}</span>
                      </div>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUT_COLORS[a.statut] || 'bg-gray-100 text-gray-700'}`}>{a.statut}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              {assignments.map(a => (
                <div key={a.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-medium text-gray-900 dark:text-white text-sm">{a.role}</h3>
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUT_COLORS[a.statut] || 'bg-gray-100 text-gray-700'}`}>{a.statut}</span>
                    </div>
                    <div className="text-xs text-gray-500 space-y-1">
                      <div>{new Date(a.debut).toLocaleDateString('fr-FR')} → {new Date(a.fin).toLocaleDateString('fr-FR')}</div>
                      {a.membreId && <div>Membre: {a.membreId.slice(0, 8)}...</div>}
                      {a.notes && <div>{a.notes}</div>}
                    </div>
                  </div>
                  <button onClick={() => deleteMutation.mutate(a.id)} className="text-red-400 hover:text-red-600 p-1">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle affectation</h2>
            <div className="space-y-4">
              <input type="text" value={newAssignment.equipeId} onChange={e => setNewAssignment({ ...newAssignment, equipeId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID de l'équipe" />
              <input type="text" value={newAssignment.role} onChange={e => setNewAssignment({ ...newAssignment, role: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="Rôle (ex: ACCUEIL)" />
              <input type="text" value={newAssignment.membreId} onChange={e => setNewAssignment({ ...newAssignment, membreId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID du membre (optionnel)" />
              <div className="flex gap-3">
                <div className="flex-1">
                  <label className="block text-xs text-gray-500 mb-1">Début</label>
                  <input type="date" value={newAssignment.debut} onChange={e => setNewAssignment({ ...newAssignment, debut: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
                </div>
                <div className="flex-1">
                  <label className="block text-xs text-gray-500 mb-1">Fin</label>
                  <input type="date" value={newAssignment.fin} onChange={e => setNewAssignment({ ...newAssignment, fin: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
                </div>
              </div>
              <textarea value={newAssignment.notes} onChange={e => setNewAssignment({ ...newAssignment, notes: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none"
                placeholder="Notes (optionnel)" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
                className="px-4 py-2 rounded-xl bg-indigo-500 text-white text-sm font-medium hover:bg-indigo-600 flex items-center gap-2">
                {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
