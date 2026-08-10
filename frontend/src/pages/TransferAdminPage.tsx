import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import type { WorkflowConfig, TransferType, ValidationMode, UserRole } from '@/types';
import { TRANSFER_TYPE_LABELS, ROLES } from '@/types';
import {
  Loader2, Plus, Trash2, Save, Pencil, X, Workflow, Power, PowerOff, ChevronDown, ChevronUp, GripVertical,
} from 'lucide-react';

interface StepDraft {
  etapeOrdre: number;
  rolesValidateurs: UserRole[];
  label: string;
  description: string;
  requis: boolean;
}

interface ConfigDraft {
  id?: string;
  transferType: TransferType | '';
  label: string;
  description: string;
  actif: boolean;
  rolesInitiateurs: UserRole[];
  modeValidation: ValidationMode;
  nombreValidationsRequises: number;
  delaiTraitementHeures: number;
  notificationsAuto: boolean;
  modeleMessageDemande: string;
  modeleMessageValidation: string;
  modeleMessageRefus: string;
  modeleMessageExecution: string;
  reglesExecution: string;
  steps: StepDraft[];
}

const emptyDraft = (): ConfigDraft => ({
  transferType: '',
  label: '',
  description: '',
  actif: true,
  rolesInitiateurs: ['PASTEUR'],
  modeValidation: 'SEQUENTIEL',
  nombreValidationsRequises: 1,
  delaiTraitementHeures: 72,
  notificationsAuto: true,
  modeleMessageDemande: '',
  modeleMessageValidation: '',
  modeleMessageRefus: '',
  modeleMessageExecution: '',
  reglesExecution: '{}',
  steps: [{ etapeOrdre: 1, rolesValidateurs: ['PASTEUR'], label: 'Validation du pasteur', description: '', requis: true }],
});

const toDraft = (c: WorkflowConfig): ConfigDraft => ({
  id: c.id,
  transferType: c.transferType,
  label: c.label,
  description: c.description ?? '',
  actif: c.actif,
  rolesInitiateurs: c.rolesInitiateurs as UserRole[],
  modeValidation: c.modeValidation,
  nombreValidationsRequises: c.nombreValidationsRequises,
  delaiTraitementHeures: c.delaiTraitementHeures,
  notificationsAuto: c.notificationsAuto,
  modeleMessageDemande: c.modeleMessageDemande ?? '',
  modeleMessageValidation: c.modeleMessageValidation ?? '',
  modeleMessageRefus: c.modeleMessageRefus ?? '',
  modeleMessageExecution: c.modeleMessageExecution ?? '',
  reglesExecution: c.reglesExecution ? JSON.stringify(c.reglesExecution, null, 2) : '{}',
  steps: c.steps.map(s => ({
    etapeOrdre: s.etapeOrdre,
    rolesValidateurs: s.rolesValidateurs as UserRole[],
    label: s.label,
    description: s.description ?? '',
    requis: s.requis,
  })),
});

function RolePicker({ value, onChange }: { value: UserRole[]; onChange: (v: UserRole[]) => void }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {ROLES.map(r => (
        <button
          key={r}
          type="button"
          onClick={() => onChange(value.includes(r) ? value.filter(x => x !== r) : [...value, r])}
          className={`px-2 py-1 rounded-lg text-[11px] font-medium border transition-all ${
            value.includes(r)
              ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-300'
              : 'border-gray-200 dark:border-gray-700 text-gray-400 hover:border-gray-300'
          }`}
        >
          {r}
        </button>
      ))}
    </div>
  );
}

export default function TransferAdminPage() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<ConfigDraft | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const { data: configs, isLoading } = useQuery({
    queryKey: ['admin-transfer-workflows'],
    queryFn: async () => {
      const res = await api.get('/admin/transfers/workflows');
      return res.data as WorkflowConfig[];
    },
  });

  const saveMutation = useMutation({
    mutationFn: async (draft: ConfigDraft) => {
      const body = {
        transferType: draft.transferType,
        label: draft.label,
        description: draft.description || undefined,
        actif: draft.actif,
        rolesInitiateurs: draft.rolesInitiateurs,
        modeValidation: draft.modeValidation,
        nombreValidationsRequises: draft.nombreValidationsRequises,
        delaiTraitementHeures: draft.delaiTraitementHeures,
        notificationsAuto: draft.notificationsAuto,
        modeleMessageDemande: draft.modeleMessageDemande || undefined,
        modeleMessageValidation: draft.modeleMessageValidation || undefined,
        modeleMessageRefus: draft.modeleMessageRefus || undefined,
        modeleMessageExecution: draft.modeleMessageExecution || undefined,
        reglesExecution: draft.reglesExecution.trim() ? JSON.parse(draft.reglesExecution) : undefined,
        steps: draft.steps.map((s, i) => ({
          etapeOrdre: s.etapeOrdre || i + 1,
          rolesValidateurs: s.rolesValidateurs,
          label: s.label || `Validation étape ${i + 1}`,
          description: s.description || undefined,
          requis: s.requis,
        })),
      };
      if (draft.id) {
        await api.put(`/admin/transfers/workflows/${draft.id}`, body);
      } else {
        await api.post('/admin/transfers/workflows', body);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-transfer-workflows'] });
      toast.success('Configuration enregistrée');
      setEditing(null);
      setIsNew(false);
    },
    onError: (err) => {
      if (err instanceof SyntaxError) toast.error('JSON des règles d\u2019exécution invalide');
      else toast.error(getErrorMessage(err));
    },
  });

  const toggleMutation = useMutation({
    mutationFn: async ({ id, actif }: { id: string; actif: boolean }) => {
      await api.patch(`/admin/transfers/workflows/${id}/toggle`, { actif });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-transfer-workflows'] });
      toast.success('Statut mis à jour');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/admin/transfers/workflows/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-transfer-workflows'] });
      toast.success('Configuration supprimée');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  useEffect(() => { setSaving(saveMutation.isPending); }, [saveMutation.isPending]);

  const usedTypes = new Set((configs ?? []).map(c => c.transferType));

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
            <Workflow className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Configuration du workflow de transfert</h1>
            <p className="page-subtitle">
              Le circuit de validation évolue ici, sans modification de code — types autorisés, étapes, rôles, délais, notifications
            </p>
          </div>
        </div>
        <button
          onClick={() => { setEditing(emptyDraft()); setIsNew(true); }}
          className="btn-primary btn-sm"
        >
          <Plus className="w-4 h-4" /> Nouvelle configuration
        </button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : !configs || configs.length === 0 ? (
        <div className="glass-card p-12 text-center text-gray-500">Aucune configuration — les transferts sont exécutés immédiatement sans validation.</div>
      ) : (
        <div className="space-y-3">
          {configs.map(c => (
            <div key={c.id} className={`glass-card overflow-hidden transition-all ${c.actif ? '' : 'opacity-70'}`}>
              <div className="p-4 flex items-center justify-between gap-4 cursor-pointer" onClick={() => setExpanded(expanded === c.id ? null : c.id)}>
                <div className="flex items-center gap-3 min-w-0">
                  <div className={`p-2.5 rounded-xl ${c.actif ? 'bg-emerald-100 dark:bg-emerald-900/30' : 'bg-gray-100 dark:bg-gray-800'}`}>
                    {c.actif ? <Power className="w-5 h-5 text-emerald-600 dark:text-emerald-400" /> : <PowerOff className="w-5 h-5 text-gray-400" />}
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-gray-900 dark:text-gray-100">{c.label}</p>
                    <p className="text-xs text-gray-500 truncate">
                      {TRANSFER_TYPE_LABELS[c.transferType]} · {c.modeValidation} · {c.delaiTraitementHeures}h · {c.steps.length} étape(s)
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className={`badge text-[10px] ${c.actif ? 'badge-success' : 'badge-gray'}`}>{c.actif ? 'Actif' : 'Inactif'}</span>
                  {expanded === c.id ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </div>
              </div>

              {expanded === c.id && (
                <div className="px-4 pb-4 space-y-4 border-t border-gray-100 dark:border-gray-800 pt-4">
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                    <div className="p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-gray-400 mb-0.5">Initiateurs</p>
                      <p className="font-medium text-gray-700 dark:text-gray-300">{c.rolesInitiateurs.join(', ')}</p>
                    </div>
                    <div className="p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-gray-400 mb-0.5">Validations requises</p>
                      <p className="font-medium text-gray-700 dark:text-gray-300">{c.nombreValidationsRequises}</p>
                    </div>
                    <div className="p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-gray-400 mb-0.5">Délai</p>
                      <p className="font-medium text-gray-700 dark:text-gray-300">{c.delaiTraitementHeures} heures</p>
                    </div>
                    <div className="p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-gray-400 mb-0.5">Notifications auto</p>
                      <p className="font-medium text-gray-700 dark:text-gray-300">{c.notificationsAuto ? 'Oui' : 'Non'}</p>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button onClick={() => { setEditing(toDraft(c)); setIsNew(false); }} className="btn-primary btn-sm">
                      <Pencil className="w-4 h-4" /> Modifier
                    </button>
                    <button
                      onClick={() => toggleMutation.mutate({ id: c.id, actif: !c.actif })}
                      className="btn-secondary btn-sm"
                    >
                      {c.actif ? <PowerOff className="w-4 h-4" /> : <Power className="w-4 h-4" />}
                      {c.actif ? 'Désactiver' : 'Activer'}
                    </button>
                    <button
                      onClick={() => { if (confirm(`Supprimer la configuration « ${c.label} » ?`)) deleteMutation.mutate(c.id); }}
                      className="btn-secondary btn-sm text-red-600 border-red-200 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" /> Supprimer
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Éditeur */}
      {editing && (
        <div className="modal-overlay" onClick={() => { setEditing(null); setIsNew(false); }}>
          <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-3xl animate-slide-up max-h-[92vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="sticky top-0 bg-white dark:bg-gray-800 z-10 flex items-center justify-between p-5 border-b border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center text-white">
                  <Workflow className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {isNew ? 'Nouvelle configuration' : `Modifier : ${editing.label}`}
                  </h3>
                  <p className="text-xs text-gray-500">Paramétrage du circuit de validation</p>
                </div>
              </div>
              <button onClick={() => { setEditing(null); setIsNew(false); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            <div className="p-5 space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="sm:col-span-2">
                  <label className="label">Type de transfert *</label>
                  <select
                    className="input"
                    value={editing.transferType}
                    disabled={!isNew}
                    onChange={(e) => {
                      const t = e.target.value as TransferType;
                      setEditing({ ...editing, transferType: t, label: editing.label || TRANSFER_TYPE_LABELS[t] });
                    }}
                  >
                    <option value="">Sélectionner...</option>
                    {(Object.keys(TRANSFER_TYPE_LABELS) as TransferType[])
                      .filter(t => isNew ? !usedTypes.has(t) : t === editing.transferType)
                      .map(t => <option key={t} value={t}>{TRANSFER_TYPE_LABELS[t]}</option>)}
                  </select>
                </div>
                <div>
                  <label className="label">Libellé</label>
                  <input className="input" value={editing.label} onChange={(e) => setEditing({ ...editing, label: e.target.value })} />
                </div>
                <div>
                  <label className="label">Description</label>
                  <input className="input" value={editing.description} onChange={(e) => setEditing({ ...editing, description: e.target.value })} />
                </div>
                <div className="sm:col-span-2">
                  <label className="label">Rôles autorisés à initier</label>
                  <RolePicker value={editing.rolesInitiateurs} onChange={(v) => setEditing({ ...editing, rolesInitiateurs: v })} />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
                <div>
                  <label className="label">Mode de validation</label>
                  <select
                    className="input"
                    value={editing.modeValidation}
                    onChange={(e) => setEditing({ ...editing, modeValidation: e.target.value as ValidationMode })}
                  >
                    <option value="SEQUENTIEL">Séquentiel</option>
                    <option value="PARALLELE">Parallèle</option>
                    <option value="N_VALIDATIONS_REQUISES">N validations requises</option>
                  </select>
                </div>
                <div>
                  <label className="label">Validations requises</label>
                  <input
                    type="number" min={1} className="input"
                    value={editing.nombreValidationsRequises}
                    onChange={(e) => setEditing({ ...editing, nombreValidationsRequises: Number(e.target.value) || 1 })}
                  />
                </div>
                <div>
                  <label className="label">Délai (heures)</label>
                  <input
                    type="number" min={1} className="input"
                    value={editing.delaiTraitementHeures}
                    onChange={(e) => setEditing({ ...editing, delaiTraitementHeures: Number(e.target.value) || 72 })}
                  />
                </div>
                <div className="flex items-end pb-1">
                  <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300 cursor-pointer">
                    <input
                      type="checkbox" className="rounded"
                      checked={editing.notificationsAuto}
                      onChange={(e) => setEditing({ ...editing, notificationsAuto: e.target.checked })}
                    />
                    Notifications auto
                  </label>
                </div>
              </div>

              {/* Étapes */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="label mb-0">Étapes du circuit de validation</label>
                  <button
                    type="button"
                    onClick={() => setEditing({
                      ...editing,
                      steps: [...editing.steps, {
                        etapeOrdre: editing.steps.length + 1,
                        rolesValidateurs: ['PASTEUR'],
                        label: `Validation étape ${editing.steps.length + 1}`,
                        description: '',
                        requis: true,
                      }],
                    })}
                    className="btn-secondary btn-sm"
                  >
                    <Plus className="w-3.5 h-3.5" /> Ajouter une étape
                  </button>
                </div>
                <div className="space-y-3">
                  {editing.steps.map((s, i) => (
                    <div key={i} className="p-3 rounded-xl border border-gray-200 dark:border-gray-700 space-y-2">
                      <div className="flex items-center gap-2">
                        <GripVertical className="w-4 h-4 text-gray-300" />
                        <span className="badge badge-gray">Étape {i + 1}</span>
                        <input
                          className="input text-sm flex-1"
                          value={s.label}
                          onChange={(e) => {
                            const steps = [...editing.steps];
                            steps[i] = { ...s, label: e.target.value };
                            setEditing({ ...editing, steps });
                          }}
                          placeholder="Libellé de l'étape"
                        />
                        <button
                          type="button"
                          onClick={() => setEditing({
                            ...editing,
                            steps: editing.steps.filter((_, j) => j !== i).map((st, j) => ({ ...st, etapeOrdre: j + 1 })),
                          })}
                          className="p-1.5 rounded-lg text-red-500 hover:bg-red-50"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400 mb-1">Rôles validateurs</p>
                        <RolePicker
                          value={s.rolesValidateurs}
                          onChange={(v) => {
                            const steps = [...editing.steps];
                            steps[i] = { ...s, rolesValidateurs: v };
                            setEditing({ ...editing, steps });
                          }}
                        />
                      </div>
                      <div className="flex items-center gap-3">
                        <input
                          className="input text-sm flex-1"
                          value={s.description}
                          onChange={(e) => {
                            const steps = [...editing.steps];
                            steps[i] = { ...s, description: e.target.value };
                            setEditing({ ...editing, steps });
                          }}
                          placeholder="Description (optionnel)"
                        />
                        <label className="flex items-center gap-1.5 text-xs text-gray-500 cursor-pointer">
                          <input
                            type="checkbox" className="rounded"
                            checked={s.requis}
                            onChange={(e) => {
                              const steps = [...editing.steps];
                              steps[i] = { ...s, requis: e.target.checked };
                              setEditing({ ...editing, steps });
                            }}
                          />
                          Requise
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Modèles de messages */}
              <div>
                <label className="label">{'Modèles de messages — placeholders : {type}, {personne}, {cible}'}</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input className="input text-sm" placeholder="Message à la création" value={editing.modeleMessageDemande}
                    onChange={(e) => setEditing({ ...editing, modeleMessageDemande: e.target.value })} />
                  <input className="input text-sm" placeholder="Message lors d'une validation" value={editing.modeleMessageValidation}
                    onChange={(e) => setEditing({ ...editing, modeleMessageValidation: e.target.value })} />
                  <input className="input text-sm" placeholder="Message en cas de refus" value={editing.modeleMessageRefus}
                    onChange={(e) => setEditing({ ...editing, modeleMessageRefus: e.target.value })} />
                  <input className="input text-sm" placeholder="Message à l'exécution" value={editing.modeleMessageExecution}
                    onChange={(e) => setEditing({ ...editing, modeleMessageExecution: e.target.value })} />
                </div>
              </div>

              {/* Règles d'exécution */}
              <div>
                <label className="label">Règles d'exécution (JSON — ex : {'{ "transfererAmes": true }'})</label>
                <textarea
                  className="input font-mono text-xs"
                  rows={3}
                  value={editing.reglesExecution}
                  onChange={(e) => setEditing({ ...editing, reglesExecution: e.target.value })}
                />
              </div>
            </div>

            <div className="sticky bottom-0 bg-white dark:bg-gray-800 flex justify-end gap-3 p-5 border-t border-gray-200 dark:border-gray-700">
              <button onClick={() => { setEditing(null); setIsNew(false); }} className="btn-secondary">Annuler</button>
              <button
                onClick={() => saveMutation.mutate(editing)}
                disabled={saving || !editing.transferType || !editing.label.trim() || editing.steps.length === 0}
                className="btn-primary"
              >
                {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
