import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Wrench, Plus, Trash2, Loader2, ToggleLeft, ToggleRight, Zap, Settings, Save, Sliders } from 'lucide-react';
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

interface WorkflowConfigItem {
  key: string;
  label: string;
  description?: string;
  enabled: boolean;
  rules: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

interface ConfigDraft {
  label: string;
  description: string;
  rules: string;
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

  // ======================== WORKFLOW CONFIG CRUD ========================
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [draft, setDraft] = useState<ConfigDraft>({ label: '', description: '', rules: '{}' });

  const { data: configs = [], isLoading: configsLoading } = useQuery({
    queryKey: ['workflow-configs'],
    queryFn: async () => (await api.get<WorkflowConfigItem[]>('/workflows')).data,
  });

  const configsInvalidate = () => queryClient.invalidateQueries({ queryKey: ['workflow-configs'] });

  const saveConfigMutation = useMutation({
    mutationFn: async () => {
      if (!editingKey) return;
      let rules: Record<string, unknown> | undefined;
      const trimmed = draft.rules.trim();
      if (trimmed) {
        try {
          rules = JSON.parse(trimmed);
        } catch {
          toast.error('Règles : JSON invalide');
          throw new Error('invalid rules');
        }
      }
      await api.put(`/workflows/${editingKey}`, {
        label: draft.label || undefined,
        description: draft.description || undefined,
        rules,
      });
    },
    onSuccess: () => {
      toast.success('Configuration mise à jour');
      configsInvalidate();
      setEditingKey(null);
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'invalid rules') toast.error(getErrorMessage(e)); },
  });

  const toggleConfigMutation = useMutation({
    mutationFn: async (key: string) => api.post(`/workflows/${key}/toggle`),
    onSuccess: () => { toast.success('Statut mis à jour'); configsInvalidate(); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const openEditConfig = (c: WorkflowConfigItem) => {
    setEditingKey(c.key);
    setDraft({
      label: c.label || '',
      description: c.description || '',
      rules: c.rules && Object.keys(c.rules).length > 0 ? JSON.stringify(c.rules, null, 2) : '{}',
    });
  };

  const closeEditConfig = () => {
    setEditingKey(null);
    setDraft({ label: '', description: '', rules: '{}' });
  };

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

      {/* ======================= WORKFLOW CONFIGS (CRUD) ======================= */}
      <div className="mt-10">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-gradient-to-br from-orange-500 to-amber-600 text-white">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">Configurations de workflow</h2>
              <p className="text-xs text-gray-500">Seuils d'escalade, rappels et modèles de notification paramétrables</p>
            </div>
          </div>
        </div>

        {configsLoading ? <SkeletonLoader lines={3} variant="card" /> :
          configs.length === 0 ? (
            <EmptyState icon={<Sliders className="w-8 h-8 text-gray-400" />}
              title="Aucune configuration"
              message="Les configurations de workflow apparaîtront ici" />
          ) : (
            <div className="space-y-3">
              {configs.map((c) => (
                <div key={c.key} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                  {editingKey === c.key ? (
                    <div className="space-y-3">
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Nom</label>
                        <input className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                          value={draft.label} onChange={(e) => setDraft({ ...draft, label: e.target.value })} />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Description</label>
                        <input className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                          value={draft.description} onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Règles (JSON)</label>
                        <textarea rows={3}
                          className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm font-mono resize-none"
                          value={draft.rules} onChange={(e) => setDraft({ ...draft, rules: e.target.value })} />
                      </div>
                      <div className="flex justify-end gap-3">
                        <button onClick={closeEditConfig} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
                        <button onClick={() => saveConfigMutation.mutate()} disabled={saveConfigMutation.isPending}
                          className="px-4 py-2 rounded-xl bg-orange-500 text-white text-sm font-medium hover:bg-orange-600 flex items-center gap-2">
                          {saveConfigMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                          Enregistrer
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex items-center gap-4">
                        <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${c.enabled ? 'bg-gradient-to-br from-primary-500 to-primary-700 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'}`}>
                          <Sliders className="w-5 h-5" />
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{c.label || c.key}</h3>
                            <span className="font-mono text-[10px] text-gray-400">{c.key}</span>
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-medium ${c.enabled ? 'bg-green-100 text-green-700 dark:bg-green-500/20 dark:text-green-400' : 'bg-gray-100 text-gray-500 dark:bg-gray-500/20'}`}>
                              {c.enabled ? 'Activé' : 'Désactivé'}
                            </span>
                          </div>
                          {c.description && <p className="text-xs text-gray-500 mt-0.5">{c.description}</p>}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <button onClick={() => openEditConfig(c)} className="btn-icon text-gray-400 hover:text-orange-600" title="Modifier">
                          <Settings className="w-4 h-4" />
                        </button>
                        <button onClick={() => toggleConfigMutation.mutate(c.key)}
                          className={`transition ${c.enabled ? 'text-green-500' : 'text-gray-400'}`} title="Activer/désactiver">
                          {c.enabled ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
      </div>

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
