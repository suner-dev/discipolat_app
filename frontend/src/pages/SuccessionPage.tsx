import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { GitBranch, Plus, UserCheck, Clock, CheckCircle2, Loader2, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface SuccessionPlan {
  id: string;
  candidatId: string;
  rôleCible: string;
  mentorId?: string;
  readiness: string;
  statut: string;
  planFormation?: string;
  createdAt: string;
}

export default function SuccessionPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [newPlan, setNewPlan] = useState({ candidatId: '', rôleCible: '', mentorId: '', planFormation: '' });

  const { data: plans = [], isLoading } = useQuery({
    queryKey: ['succession'],
    queryFn: async () => (await api.get<SuccessionPlan[]>('/succession')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!newPlan.candidatId || !newPlan.rôleCible) { toast('Remplissez les champs requis', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/succession', newPlan);
    },
    onSuccess: () => {
      toast.success('Plan créé');
      setShowCreate(false);
      setNewPlan({ candidatId: '', rôleCible: '', mentorId: '', planFormation: '' });
      queryClient.invalidateQueries({ queryKey: ['succession'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const updateStatusMutation = useMutation({
    mutationFn: async ({ id, statut }: { id: string; statut: string }) =>
      api.patch(`/succession/${id}/status`, { statut }),
    onSuccess: () => { toast.success('Statut mis à jour'); queryClient.invalidateQueries({ queryKey: ['succession'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/succession/${id}`),
    onSuccess: () => { toast.success('Supprimé'); queryClient.invalidateQueries({ queryKey: ['succession'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <GitBranch className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Plan de Succession</h1>
          <p className="page-subtitle">Préparez les futurs leaders de votre église</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="ml-auto px-4 py-2 rounded-xl bg-gradient-to-r from-violet-500 to-purple-500 text-white text-sm font-medium hover:from-violet-600 hover:to-purple-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> Nouveau plan
        </button>
      </div>

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        plans.length === 0 ? (
          <EmptyState icon={<GitBranch className="w-8 h-8 text-gray-400" />}
            title="Aucun plan de succession"
            message="Identifiez les futurs leaders et créez des plans de transition"
            action={{ label: 'Créer un plan', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {plans.map(plan => (
              <div key={plan.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-semibold text-gray-900 dark:text-white text-sm">Rôle: {plan.rôleCible}</h3>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${plan.statut === 'COMPLÉTÉ' ? 'bg-green-100 text-green-700' : plan.statut === 'PRÊT' ? 'bg-blue-100 text-blue-700' : 'bg-amber-100 text-amber-700'}`}>
                    {plan.statut}
                  </span>
                </div>
                <div className="text-xs text-gray-500 space-y-1 mb-3">
                  <div className="flex items-center gap-1"><UserCheck className="w-3 h-3" /> Candidat: {plan.candidatId.slice(0, 8)}...</div>
                  {plan.mentorId && <div className="flex items-center gap-1"><Clock className="w-3 h-3" /> Mentor: {plan.mentorId.slice(0, 8)}...</div>}
                  <div>Readiness: {plan.readiness}</div>
                  {plan.planFormation && <div className="text-gray-400 mt-1">{plan.planFormation}</div>}
                </div>
                <div className="flex gap-2">
                  {plan.statut !== 'COMPLÉTÉ' && (
                    <button onClick={() => updateStatusMutation.mutate({ id: plan.id, statut: plan.statut === 'PRÊT' ? 'COMPLÉTÉ' : 'PRÊT' })}
                      className="px-3 py-1 rounded-lg bg-violet-500 text-white text-xs font-medium hover:bg-violet-600 flex items-center gap-1">
                      <CheckCircle2 className="w-3 h-3" />
                      {plan.statut === 'PRÊT' ? 'Compléter' : 'Marquer prêt'}
                    </button>
                  )}
                  <button onClick={() => deleteMutation.mutate(plan.id)}
                    className="px-3 py-1 rounded-lg bg-red-100 text-red-600 text-xs font-medium hover:bg-red-200 flex items-center gap-1">
                    <Trash2 className="w-3 h-3" /> Supprimer
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau plan de succession</h2>
            <div className="space-y-4">
              <input type="text" value={newPlan.candidatId} onChange={e => setNewPlan({ ...newPlan, candidatId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID du candidat" />
              <input type="text" value={newPlan.rôleCible} onChange={e => setNewPlan({ ...newPlan, rôleCible: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="Rôle cible (ex: RESPONSABLE)" />
              <input type="text" value={newPlan.mentorId} onChange={e => setNewPlan({ ...newPlan, mentorId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID du mentor (optionnel)" />
              <textarea value={newPlan.planFormation} onChange={e => setNewPlan({ ...newPlan, planFormation: e.target.value })}
                rows={3} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none"
                placeholder="Plan de formation" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
                className="px-4 py-2 rounded-xl bg-violet-500 text-white text-sm font-medium hover:bg-violet-600 flex items-center gap-2">
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
