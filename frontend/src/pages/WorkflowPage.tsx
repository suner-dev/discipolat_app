import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Wrench, Plus, Trash2, Loader2, ToggleLeft, ToggleRight, Zap, Settings } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface Automation {
  id: string;
  nom: string;
  description: string;
  triggerType: string;
  triggerConfig: string;
  actionType: string;
  actionConfig: string;
  statut: string;
  createdBy: string;
  createdAt: string;
}

const TRIGGER_TYPES = ['MEMBER_ABSENT', 'NEW_SOUL', 'EVENT_REMINDER', 'PRAYER_REQUEST', 'ANNIVERSARY', 'SCORE_DROP', 'CUSTOM'];
const ACTION_TYPES = ['SEND_NOTIFICATION', 'SEND_EMAIL', 'UPDATE_FIELD', 'CREATE_TASK', 'TRIGGER_WEBHOOK', 'CUSTOM'];

export default function WorkflowPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [newAuto, setNewAuto] = useState({ nom: '', description: '', triggerType: 'MEMBER_ABSENT', triggerConfig: '{}', actionType: 'SEND_NOTIFICATION', actionConfig: '{}' });

  const { data: automations = [], isLoading } = useQuery({
    queryKey: ['workflow-automations'],
    queryFn: async () => (await api.get<Automation[]>('/workflow/automations')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!newAuto.nom.trim()) { toast('Entrez un nom', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/workflow/automations', newAuto);
    },
    onSuccess: () => {
      toast.success('Automatisation créée');
      setShowCreate(false);
      setNewAuto({ nom: '', description: '', triggerType: 'MEMBER_ABSENT', triggerConfig: '{}', actionType: 'SEND_NOTIFICATION', actionConfig: '{}' });
      queryClient.invalidateQueries({ queryKey: ['workflow-automations'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const toggleMutation = useMutation({
    mutationFn: async (id: string) => api.patch(`/workflow/automations/${id}/toggle`),
    onSuccess: () => { toast.success('Statut mis à jour'); queryClient.invalidateQueries({ queryKey: ['workflow-automations'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/workflow/automations/${id}`),
    onSuccess: () => { toast.success('Supprimé'); queryClient.invalidateQueries({ queryKey: ['workflow-automations'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg">
          <Zap className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Automatisations Workflow</h1>
          <p className="page-subtitle">Déclencheurs et actions automatiques</p>
        </div>
        <div className="ml-auto">
          <button onClick={() => setShowCreate(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-500 text-white text-sm font-medium hover:from-cyan-600 hover:to-blue-600 transition-all shadow-lg flex items-center gap-2">
            <Plus className="w-4 h-4" /> Nouvelle automatisation
          </button>
        </div>
      </div>

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        automations.length === 0 ? (
          <EmptyState icon={<Zap className="w-8 h-8 text-gray-400" />}
            title="Aucune automatisation"
            message="Créez des automatisations pour gagner du temps"
            action={{ label: 'Créer une automatisation', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {automations.map(a => (
              <div key={a.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{a.nom}</h3>
                  <div className="flex items-center gap-2">
                    <button onClick={() => toggleMutation.mutate(a.id)}
                      className={`transition ${a.statut === 'ACTIVE' ? 'text-green-500' : 'text-gray-400'}`}>
                      {a.statut === 'ACTIVE' ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
                    </button>
                    <button onClick={() => deleteMutation.mutate(a.id)} className="text-red-400 hover:text-red-600">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <p className="text-xs text-gray-500 mb-3">{a.description}</p>
                <div className="flex gap-2 text-xs">
                  <span className="px-2 py-0.5 rounded-full bg-cyan-100 dark:bg-cyan-500/20 text-cyan-700 dark:text-cyan-400">
                    <Zap className="w-3 h-3 inline mr-1" />{a.triggerType}
                  </span>
                  <span className="px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-400">
                    <Settings className="w-3 h-3 inline mr-1" />{a.actionType}
                  </span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${a.statut === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                    {a.statut}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvelle automatisation</h2>
            <div className="space-y-4">
              <input type="text" value={newAuto.nom} onChange={e => setNewAuto({ ...newAuto, nom: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Nom" />
              <textarea value={newAuto.description} onChange={e => setNewAuto({ ...newAuto, description: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" placeholder="Description" />
              <div>
                <label className="block text-xs text-gray-500 mb-1">Déclencheur</label>
                <select value={newAuto.triggerType} onChange={e => setNewAuto({ ...newAuto, triggerType: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                  {TRIGGER_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">Action</label>
                <select value={newAuto.actionType} onChange={e => setNewAuto({ ...newAuto, actionType: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                  {ACTION_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
                className="px-4 py-2 rounded-xl bg-cyan-500 text-white text-sm font-medium hover:bg-cyan-600 flex items-center gap-2">
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
