import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Workflow, Plus, Trash2, Loader2, Save, X, Power, PowerOff,
  ChevronDown, ChevronUp, GripVertical, ArrowRight, CheckCircle2,
  Circle, AlertTriangle, Settings, Users, Bell, Zap, Eye, EyeOff,
} from 'lucide-react';
import type { WorkflowConfig, TransferType, ValidationMode, UserRole, WorkflowStep } from '@/types';
import { TRANSFER_TYPE_LABELS, ROLES } from '@/types';
import { useDictionaries } from '@/hooks/useDictionaries';
import ConfigRevisionHistory from '@/components/ConfigRevisionHistory';

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
  steps: [{ etapeOrdre: 1, rolesValidateurs: ['PASTEUR'], label: 'Validation', description: '', requis: true }],
});

/**
 * Les règles d'exécution sont stockées côté API comme objet JSON.
 * L'éditeur travaille sur une chaîne ; on re-sérialise au chargement.
 */
const toReglesExecutionDraft = (value?: Record<string, unknown>): string =>
  value && Object.keys(value).length > 0 ? JSON.stringify(value, null, 2) : '{}';

/** Conversion inverse vers l'API : chaîne JSON valide → objet, sinon `undefined`. */
const parseReglesExecution = (value: string): Record<string, unknown> | undefined => {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  try {
    const parsed = JSON.parse(trimmed);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : undefined;
  } catch {
    throw new Error('Règles d\'exécution : JSON invalide. Vérifiez la syntaxe (ex. { "transfererAmes": true }).');
  }
};

const MODE_VALIDATION_LABELS: Record<ValidationMode, string> = {
  SEQUENTIEL: 'Séquentiel',
  PARALLELE: 'Parallèle',
  N_VALIDATIONS_REQUISES: 'N validations requises',
};

const MODE_VALIDATION_DESCRIPTIONS: Record<ValidationMode, string> = {
  SEQUENTIEL: 'Les validateurs sont sollicités un par un, dans l\'ordre.',
  PARALLELE: 'Tous les validateurs reçoivent la demande simultanément.',
  N_VALIDATIONS_REQUISES: 'Un nombre fixe de validations suffit (quel que soit le validateur).',
};

export default function AdminWorkflowBuilderPage() {
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [editing, setEditing] = useState<string | null>(null);
  const [draft, setDraft] = useState<ConfigDraft>(emptyDraft());
  const [showNewForm, setShowNewForm] = useState(false);

  const { data: workflows = [], isLoading } = useQuery({
    queryKey: ['admin-transfer-workflows'],
    queryFn: async () => {
      const res = await api.get('/admin/transfers/workflows');
      return res.data as WorkflowConfig[];
    },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin-transfer-workflows'] });
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        transferType: draft.transferType,
        label: draft.label,
        description: draft.description,
        actif: draft.actif,
        rolesInitiateurs: draft.rolesInitiateurs,
        modeValidation: draft.modeValidation,
        nombreValidationsRequises: draft.nombreValidationsRequises,
        delaiTraitementHeures: draft.delaiTraitementHeures,
        notificationsAuto: draft.notificationsAuto,
        modeleMessageDemande: draft.modeleMessageDemande,
        modeleMessageValidation: draft.modeleMessageValidation,
        modeleMessageRefus: draft.modeleMessageRefus,
        modeleMessageExecution: draft.modeleMessageExecution,
        reglesExecution: parseReglesExecution(draft.reglesExecution),
        steps: draft.steps,
      };
      if (editing) {
        await api.put(`/admin/transfers/workflows/${editing}`, body);
      } else {
        await api.post('/admin/transfers/workflows', body);
      }
    },
    onSuccess: () => {
      invalidate();
      toast.success(editing ? 'Workflow mis à jour' : 'Workflow créé');
      setEditing(null);
      setShowNewForm(false);
      setDraft(emptyDraft());
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const toggleMutation = useMutation({
    mutationFn: async ({ id, actif }: { id: string; actif: boolean }) =>
      api.patch(`/admin/transfers/workflows/${id}/toggle`, { actif }),
    onSuccess: () => { invalidate(); toast.success('Workflow mis à jour'); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/admin/transfers/workflows/${id}`),
    onSuccess: () => { invalidate(); toast.success('Workflow supprimé'); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const openEdit = (wf: WorkflowConfig) => {
    setEditing(wf.id);
    setDraft({
      id: wf.id,
      transferType: wf.transferType,
      label: wf.label || '',
      description: wf.description || '',
      actif: wf.actif,
      rolesInitiateurs: wf.rolesInitiateurs || ['PASTEUR'],
      modeValidation: wf.modeValidation || 'SEQUENTIEL',
      nombreValidationsRequises: wf.nombreValidationsRequises || 1,
      delaiTraitementHeures: wf.delaiTraitementHeures || 72,
      notificationsAuto: wf.notificationsAuto ?? true,
      modeleMessageDemande: wf.modeleMessageDemande || '',
      modeleMessageValidation: wf.modeleMessageValidation || '',
      modeleMessageRefus: wf.modeleMessageRefus || '',
      modeleMessageExecution: wf.modeleMessageExecution || '',
      reglesExecution: toReglesExecutionDraft(wf.reglesExecution),
      steps: (wf.steps || []).map((s: WorkflowStep) => ({
        etapeOrdre: s.etapeOrdre,
        rolesValidateurs: s.rolesValidateurs || ['PASTEUR'],
        label: s.label || '',
        description: s.description || '',
        requis: s.requis ?? true,
      })),
    });
    setShowNewForm(true);
  };

  const activeCount = workflows.filter((w) => w.actif).length;

  const addStep = () => {
    setDraft((d) => ({
      ...d,
      steps: [
        ...d.steps,
        { etapeOrdre: d.steps.length + 1, rolesValidateurs: ['PASTEUR'], label: '', description: '', requis: true },
      ],
    }));
  };

  const removeStep = (idx: number) => {
    setDraft((d) => ({
      ...d,
      steps: d.steps.filter((_, i) => i !== idx).map((s, i) => ({ ...s, etapeOrdre: i + 1 })),
    }));
  };

  const updateStep = (idx: number, patch: Partial<StepDraft>) => {
    setDraft((d) => ({
      ...d,
      steps: d.steps.map((s, i) => (i === idx ? { ...s, ...patch } : s)),
    }));
  };

  const toggleInitiator = (role: UserRole) => {
    setDraft((d) => ({
      ...d,
      rolesInitiateurs: d.rolesInitiateurs.includes(role)
        ? d.rolesInitiateurs.filter((r) => r !== role)
        : [...d.rolesInitiateurs, role],
    }));
  };

  const toggleStepValidator = (stepIdx: number, role: UserRole) => {
    setDraft((d) => ({
      ...d,
      steps: d.steps.map((s, i) =>
        i === stepIdx
          ? {
              ...s,
              rolesValidateurs: s.rolesValidateurs.includes(role)
                ? s.rolesValidateurs.filter((r) => r !== role)
                : [...s.rolesValidateurs, role],
            }
          : s
      ),
    }));
  };

  if (isLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div>
          <h1 className="page-title">Constructeur de workflows</h1>
          <p className="page-subtitle">
            Configurez les circuits de validation : demandes, validations multi-étapes,
            actions automatiques et notifications. Chaque workflow définit qui demande,
            qui valide et quelle action est exécutée.
          </p>
        </div>
        <div className="page-header-actions">
          <button
            className="btn-primary btn-sm"
            onClick={() => { setEditing(null); setDraft(emptyDraft()); setShowNewForm(true); }}
          >
            <Plus className="w-4 h-4" /> Nouveau workflow
          </button>
        </div>
      </div>

      <div className="mb-6">
        <ConfigRevisionHistory entityType="PLATFORM_WORKFLOW" title="Historique des workflows" />
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="stat-card p-4 text-center">
          <span className="stat-label text-[10px]">Workflows actifs</span>
          <p className="stat-value text-xl">{activeCount}</p>
        </div>
        <div className="stat-card p-4 text-center">
          <span className="stat-label text-[10px]">Total configurés</span>
          <p className="stat-value text-xl">{workflows.length}</p>
        </div>
        <div className="stat-card p-4 text-center">
          <span className="stat-label text-[10px]">Types couverts</span>
          <p className="stat-value text-xl">{new Set(workflows.map((w) => w.transferType)).size}</p>
        </div>
      </div>

      {workflows.length === 0 && !showNewForm && (
        <div className="glass-card p-10 text-center">
          <Workflow className="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-1">
            Aucun workflow configuré
          </h3>
          <p className="text-sm text-gray-400 mb-4">
            Créez votre premier workflow pour définir un circuit de validation automatisé.
          </p>
          <button className="btn-primary btn-sm" onClick={() => { setEditing(null); setDraft(emptyDraft()); setShowNewForm(true); }}>
            <Plus className="w-4 h-4" /> Créer un workflow
          </button>
        </div>
      )}

      {/* Workflow list */}
      <div className="space-y-4">
        {workflows.map((wf) => {
          const isExpanded = expandedId === wf.id;
          return (
            <div key={wf.id} className={`glass-card overflow-hidden transition-all ${!wf.actif ? 'opacity-60' : ''}`}>
              <button
                className="w-full p-5 flex items-center gap-4 hover:bg-white/40 dark:hover:bg-gray-800/30 transition-colors"
                onClick={() => setExpandedId(isExpanded ? null : wf.id)}
              >
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${wf.actif ? 'bg-gradient-to-br from-primary-500 to-primary-700 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'}`}>
                  <Workflow className="w-5 h-5" />
                </div>
                <div className="flex-1 min-w-0 text-left">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{wf.label || TRANSFER_TYPE_LABELS[wf.transferType] || wf.transferType}</p>
                    <span className="font-mono text-[10px] text-gray-400">{wf.transferType}</span>
                    <span className={`badge text-[10px] ${wf.actif ? 'badge-success' : 'badge-gray'}`}>
                      {wf.actif ? 'Actif' : 'Inactif'}
                    </span>
                    <span className="badge badge-info text-[10px]">{wf.steps?.length || 0} étape(s)</span>
                  </div>
                  {wf.description && <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 truncate">{wf.description}</p>}
                </div>
                <div className="flex items-center gap-1">
                  <button
                    className="btn-icon text-gray-400 hover:text-primary-600"
                    onClick={(e) => { e.stopPropagation(); openEdit(wf); }}
                    aria-label={`Modifier ${wf.label}`}
                  >
                    <Settings className="w-4 h-4" />
                  </button>
                  <button
                    className="btn-icon text-gray-400 hover:text-red-500"
                    onClick={(e) => { e.stopPropagation(); if (confirm(`Supprimer le workflow « ${wf.label || wf.transferType} » ?`)) deleteMutation.mutate(wf.id); }}
                    aria-label={`Supprimer ${wf.label}`}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                  <button
                    role="switch"
                    aria-checked={wf.actif}
                    onClick={(e) => { e.stopPropagation(); toggleMutation.mutate({ id: wf.id, actif: !wf.actif }); }}
                    className={`relative ml-2 w-10 h-5 rounded-full transition-colors ${wf.actif ? 'bg-primary-500' : 'bg-gray-300 dark:bg-gray-600'}`}
                  >
                    <span className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${wf.actif ? 'left-[18px]' : 'left-0.5'}`} />
                  </button>
                  {isExpanded ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </div>
              </button>

              {isExpanded && (
                <div className="px-5 pb-5 border-t border-gray-200/60 dark:border-gray-700/60">
                  <div className="mt-4 space-y-4">
                    {/* Workflow visual pipeline */}
                    <div className="flex items-center gap-2 overflow-x-auto pb-2">
                      {/* Initiator */}
                      <div className="flex-shrink-0 px-3 py-2 rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200/60 dark:border-blue-700/30 text-center min-w-[100px]">
                        <Users className="w-4 h-4 text-blue-500 mx-auto mb-1" />
                        <p className="text-[10px] font-semibold text-blue-700 dark:text-blue-300">Initiateur</p>
                        <p className="text-[9px] text-blue-500">{(wf.rolesInitiateurs || []).join(', ')}</p>
                      </div>
                      <ArrowRight className="w-4 h-4 text-gray-300 flex-shrink-0" />
                      {/* Steps */}
                      {(wf.steps || []).map((step, i) => (
                        <div key={i} className="flex items-center gap-2 flex-shrink-0">
                          <div className="px-3 py-2 rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200/60 dark:border-primary-700/30 text-center min-w-[100px]">
                            <CheckCircle2 className="w-4 h-4 text-primary-500 mx-auto mb-1" />
                            <p className="text-[10px] font-semibold text-primary-700 dark:text-primary-300">
                              {step.label || `Étape ${step.etapeOrdre}`}
                            </p>
                            <p className="text-[9px] text-primary-500">{(step.rolesValidateurs || []).join(', ')}</p>
                          </div>
                          <ArrowRight className="w-4 h-4 text-gray-300 flex-shrink-0" />
                        </div>
                      ))}
                      {/* Action */}
                      <div className="flex-shrink-0 px-3 py-2 rounded-xl bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200/60 dark:border-emerald-700/30 text-center min-w-[80px]">
                        <Zap className="w-4 h-4 text-emerald-500 mx-auto mb-1" />
                        <p className="text-[10px] font-semibold text-emerald-700 dark:text-emerald-300">Action</p>
                        <p className="text-[9px] text-emerald-500">Exécution</p>
                      </div>
                      {wf.notificationsAuto && (
                        <>
                          <ArrowRight className="w-4 h-4 text-gray-300 flex-shrink-0" />
                          <div className="flex-shrink-0 px-3 py-2 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200/60 dark:border-amber-700/30 text-center min-w-[80px]">
                            <Bell className="w-4 h-4 text-amber-500 mx-auto mb-1" />
                            <p className="text-[10px] font-semibold text-amber-700 dark:text-amber-300">Notification</p>
                            <p className="text-[9px] text-amber-500">Auto</p>
                          </div>
                        </>
                      )}
                    </div>

                    {/* Config details */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                        <p className="text-[10px] text-gray-400 uppercase">Mode</p>
                        <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">{MODE_VALIDATION_LABELS[wf.modeValidation]}</p>
                      </div>
                      <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                        <p className="text-[10px] text-gray-400 uppercase">Délai</p>
                        <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">{wf.delaiTraitementHeures}h</p>
                      </div>
                      <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                        <p className="text-[10px] text-gray-400 uppercase">Validations</p>
                        <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">{wf.nombreValidationsRequises}</p>
                      </div>
                      <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                        <p className="text-[10px] text-gray-400 uppercase">Notifications</p>
                        <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">{wf.notificationsAuto ? 'Activées' : 'Désactivées'}</p>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Create/Edit modal */}
      {showNewForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" onClick={() => { setShowNewForm(false); setEditing(null); }}>
          <div className="glass-card max-w-3xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-2">
                <Workflow className="w-4 h-4 text-primary-500" />
                <h3 className="text-base font-bold">{editing ? 'Modifier le workflow' : 'Nouveau workflow'}</h3>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => { setShowNewForm(false); setEditing(null); }}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-5">
              {/* General */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="label">Type de workflow</label>
                  <select
                    className="input"
                    value={draft.transferType}
                    disabled={!!editing}
                    onChange={(e) => setDraft({ ...draft, transferType: e.target.value as TransferType })}
                  >
                    <option value="">— Choisir —</option>
                    {Object.entries(TRANSFER_TYPE_LABELS).map(([k, v]) => (
                      <option key={k} value={k}>{v}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label">Libellé</label>
                  <input className="input" value={draft.label} onChange={(e) => setDraft({ ...draft, label: e.target.value })} placeholder="Ex : Transfert de membre" />
                </div>
              </div>
              <div>
                <label className="label">Description</label>
                <input className="input" value={draft.description} onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
              </div>

              {/* Initiators */}
              <div>
                <label className="label">Rôles initiateurs (qui peuvent lancer le workflow)</label>
                <div className="flex flex-wrap gap-2">
                  {ROLES.map((r) => (
                    <button
                      key={r}
                      type="button"
                      onClick={() => toggleInitiator(r as UserRole)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                        draft.rolesInitiateurs.includes(r as UserRole)
                          ? 'bg-primary-500/15 border-primary-500/30 text-primary-700 dark:text-primary-400'
                          : 'border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400 hover:border-gray-300'
                      }`}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>

              {/* Validation mode */}
              <div>
                <label className="label">Mode de validation</label>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {(Object.entries(MODE_VALIDATION_LABELS) as [ValidationMode, string][]).map(([key, label]) => (
                    <button
                      key={key}
                      type="button"
                      onClick={() => setDraft({ ...draft, modeValidation: key })}
                      className={`p-3 rounded-xl border text-left transition-all ${
                        draft.modeValidation === key
                          ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-2 ring-primary-500/20'
                          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                      }`}
                    >
                      <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">{label}</p>
                      <p className="text-[10px] text-gray-500 dark:text-gray-400 mt-0.5">{MODE_VALIDATION_DESCRIPTIONS[key]}</p>
                    </button>
                  ))}
                </div>
              </div>

              {/* Timing */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Délai de traitement (heures)</label>
                  <input type="number" className="input" value={draft.delaiTraitementHeures} onChange={(e) => setDraft({ ...draft, delaiTraitementHeures: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="label">Validations requises</label>
                  <input type="number" className="input" value={draft.nombreValidationsRequises} onChange={(e) => setDraft({ ...draft, nombreValidationsRequises: Number(e.target.value) })} />
                </div>
              </div>

              {/* Steps */}
              <div>
                <div className="flex items-center justify-between mb-3">
                  <label className="label !mb-0">Étapes de validation</label>
                  <button type="button" className="btn-ghost btn-sm" onClick={addStep}>
                    <Plus className="w-3.5 h-3.5" /> Ajouter une étape
                  </button>
                </div>
                <div className="space-y-3">
                  {draft.steps.map((step, i) => (
                    <div key={i} className="p-4 rounded-xl border border-gray-200/70 dark:border-gray-700/50 bg-white/40 dark:bg-gray-900/30">
                      <div className="flex items-center gap-2 mb-3">
                        <span className="badge badge-primary text-[10px]">Étape {step.etapeOrdre}</span>
                        <input
                          className="input flex-1 !py-1"
                          value={step.label}
                          onChange={(e) => updateStep(i, { label: e.target.value })}
                          placeholder="Label de l'étape"
                        />
                        <button type="button" className="btn-icon btn-icon-sm text-gray-400 hover:text-red-500" onClick={() => removeStep(i)}>
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                      <div>
                        <label className="text-[10px] text-gray-500 uppercase">Validateurs</label>
                        <div className="flex flex-wrap gap-1.5 mt-1">
                          {ROLES.map((r) => (
                            <button
                              key={r}
                              type="button"
                              onClick={() => toggleStepValidator(i, r as UserRole)}
                              className={`px-2 py-0.5 rounded text-[10px] font-medium border transition-all ${
                                step.rolesValidateurs.includes(r as UserRole)
                                  ? 'bg-primary-500/15 border-primary-500/30 text-primary-700'
                                  : 'border-gray-200 dark:border-gray-700 text-gray-400 hover:border-gray-300'
                              }`}
                            >
                              {r}
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Notifications */}
              <div className="flex items-center gap-3">
                <button
                  role="switch"
                  aria-checked={draft.notificationsAuto}
                  onClick={() => setDraft({ ...draft, notificationsAuto: !draft.notificationsAuto })}
                  className={`relative w-11 h-6 rounded-full transition-colors ${draft.notificationsAuto ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                >
                  <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform ${draft.notificationsAuto ? 'translate-x-5' : ''}`} />
                </button>
                <span className="text-sm text-gray-500 dark:text-gray-400">Notifications automatiques</span>
              </div>

              {/* Message templates */}
              {draft.notificationsAuto && (
                <div className="space-y-3 p-4 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 border border-gray-200/60 dark:border-gray-700/60">
                  <p className="text-xs font-semibold text-gray-500 uppercase">Modèles de messages</p>
                  <div>
                    <label className="label text-[10px]">Message de demande</label>
                    <input className="input" value={draft.modeleMessageDemande} onChange={(e) => setDraft({ ...draft, modeleMessageDemande: e.target.value })} placeholder="{{demandeur}} a soumis une demande de {{type}}…" />
                  </div>
                  <div>
                    <label className="label text-[10px]">Message de validation</label>
                    <input className="input" value={draft.modeleMessageValidation} onChange={(e) => setDraft({ ...draft, modeleMessageValidation: e.target.value })} placeholder="Votre demande a été validée par {{validateur}}." />
                  </div>
                  <div>
                    <label className="label text-[10px]">Message de refus</label>
                    <input className="input" value={draft.modeleMessageRefus} onChange={(e) => setDraft({ ...draft, modeleMessageRefus: e.target.value })} placeholder="Votre demande a été refusée." />
                  </div>
                  <div>
                    <label className="label text-[10px]">Message d'exécution</label>
                    <input className="input" value={draft.modeleMessageExecution} onChange={(e) => setDraft({ ...draft, modeleMessageExecution: e.target.value })} placeholder="La demande a été exécutée avec succès." />
                  </div>
                </div>
              )}

              {/* Execution rules */}
              <div>
                <label className="label">Règles d'exécution</label>
                <textarea
                  className="input min-h-[60px]"
                  value={draft.reglesExecution}
                  onChange={(e) => setDraft({ ...draft, reglesExecution: e.target.value })}
                  placeholder="Actions automatiques exécutées après validation complète…"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => { setShowNewForm(false); setEditing(null); }}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => saveMutation.mutate()}
                disabled={saveMutation.isPending || !draft.transferType || !draft.label.trim()}
              >
                {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                {editing ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
