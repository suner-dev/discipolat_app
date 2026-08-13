import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import type { DictionaryEntry, DictionariesMap } from '@/types';
import {
  BookOpen, Plus, Save, Loader2, X, Trash2, Pencil,
  RotateCcw, Sparkles, ChevronDown, ChevronUp, ToggleLeft, ToggleRight,
} from 'lucide-react';
import toast from 'react-hot-toast';

/* ----------------------------------------------------------------------------
 * Libellés humains des dictionnaires (présentation admin uniquement — les
 * données elles-mêmes sont 100 % configurables en base).
 * -------------------------------------------------------------------------- */
const DICT_LABELS: Record<string, string> = {
  EVENT_TYPE: 'Types d\'événements',
  EVENT_STATUS: 'Statuts d\'événements',
  SOUL_TYPE: 'Types de disciples',
  SOUL_STATUS: 'Statuts d\'âme',
  ABSENCE_RAISON: 'Raisons d\'absence',
  EXIT_MOTIF: 'Motifs de sortie',
  DIFFICULTE_CATEGORIE: 'Catégories de difficultés',
  SITUATION_FAMILIALE: 'Situations familiales',
  PRAYER_CATEGORIE: 'Catégories de prières',
  PRAYER_PRIORITE: 'Priorités de prières',
  PRAYER_VISIBILITE: 'Visibilités des prières',
  DOCUMENT_CATEGORIE: 'Catégories de documents',
  FOLLOWUP_RAISON: 'Raisons de suivi parallèle',
  CULTE: 'Cultes & programmes de présence',
  INTERACTION_TYPE: 'Types d\'interactions',
  MEMBER_REQUEST_TYPE: 'Types de demandes membres',
  MEMBER_REQUEST_TARGET: 'Cibles des demandes membres',
  MEMBER_REQUEST_STATUS: 'Statuts des demandes membres',
  USER_ROLE: 'Rôles utilisateurs',
  USER_CHARGE: 'Charge de travail',
  FEEDBACK_CATEGORIE: 'Catégories de feedback',
  FEEDBACK_STATUS: 'Statuts de feedback',
  TRANSFER_TYPE: 'Types de transfert',
  TRANSFER_STATUS: 'Statuts de transfert',
  TRANSFER_DECISION: 'Décisions de validation',
  TRANSFER_PRIORITE: 'Priorités de transfert',
  EVALUATION_CATEGORIE: 'Catégories d\'évaluations',
  SPIRITUAL_LEVEL: 'États spirituels',
  DISCIPLINE_CATEGORIE: 'Catégories de discipline',
  AUDIT_ENTITY: 'Entités du journal d\'audit',
  APPOINTMENT_MOTIF: 'Motifs de rendez-vous',
  GRATITUDE_CATEGORIE: 'Catégories d\'actions de grâce',
  REPORT_STATUS: 'Statuts de rapports',
  ALERT_TYPE: 'Types d\'alertes',
  ALERT_CIBLE: 'Cibles d\'alertes',
  ALERT_PRIORITE: 'Priorités d\'alertes',
  ALERT_STATUS: 'Statuts d\'alertes',
  NOTIFICATION_TYPE: 'Types de notifications',
  NOTIFICATION_CANAL: 'Canaux de notification',
  DISCIPLINE_TYPE: 'Types d\'événements disciplinaires',
  DISCIPLINE_GRAVITE: 'Gravité disciplinaire',
  FEEDBACK_PRIORITE: 'Priorités de feedback',
  INTERACTION_CANAL: 'Canaux d\'interaction CRM',
};

interface EntryForm {
  code: string;
  label: string;
  description: string;
  color: string;
  ordre: number;
  actif: boolean;
}

const emptyForm = (): EntryForm => ({
  code: '', label: '', description: '', color: '#8b5cf6', ordre: 0, actif: true,
});

export default function AdminDictionariesPage() {
  const queryClient = useQueryClient();
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [editingKey, setEditingKey] = useState<string | null>(null);
  /** Entrée en cours d'édition (id renseigné) — null = création. */
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<EntryForm>(emptyForm());
  const [confirmDelete, setConfirmDelete] = useState<DictionaryEntry | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-dictionaries'],
    queryFn: async () => (await api.get('/admin/dictionaries')).data as DictionariesMap,
  });

  const createMutation = useMutation({
    mutationFn: async ({ key, payload }: { key: string; payload: EntryForm }) => {
      await api.post(`/admin/dictionaries/${key}`, payload);
    },
    onSuccess: () => {
      toast.success('Entrée créée');
      queryClient.invalidateQueries({ queryKey: ['admin-dictionaries'] });
      queryClient.invalidateQueries({ queryKey: ['dictionaries'] });
      setEditingKey(null);
      setForm(emptyForm());
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: EntryForm }) => {
      await api.put(`/admin/dictionaries/${id}`, payload);
    },
    onSuccess: () => {
      toast.success('Entrée mise à jour');
      queryClient.invalidateQueries({ queryKey: ['admin-dictionaries'] });
      queryClient.invalidateQueries({ queryKey: ['dictionaries'] });
      setEditingKey(null);
      setForm(emptyForm());
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/admin/dictionaries/${id}`);
    },
    onSuccess: () => {
      toast.success('Entrée supprimée');
      queryClient.invalidateQueries({ queryKey: ['admin-dictionaries'] });
      queryClient.invalidateQueries({ queryKey: ['dictionaries'] });
      setConfirmDelete(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const resetMutation = useMutation({
    mutationFn: async () => {
      await api.post('/admin/dictionaries/reset');
    },
    onSuccess: () => {
      toast.success('Dictionnaires restaurés aux valeurs par défaut');
      queryClient.invalidateQueries({ queryKey: ['admin-dictionaries'] });
      queryClient.invalidateQueries({ queryKey: ['dictionaries'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const openCreate = (key: string) => {
    setEditingKey(key);
    setEditingId(null);
    setForm(emptyForm());
    setExpanded((e) => ({ ...e, [key]: true }));
  };

  const openEdit = (entry: DictionaryEntry) => {
    setEditingKey(entry.dictKey);
    setEditingId(entry.id);
    setForm({
      code: entry.code, label: entry.label, description: entry.description || '',
      color: entry.color || '#8b5cf6', ordre: entry.ordre, actif: entry.actif,
    });
    setExpanded((e) => ({ ...e, [entry.dictKey]: true }));
  };

  const submit = (key: string) => {
    if (!form.label.trim()) {
      toast.error('Le libellé est obligatoire');
      return;
    }
    const payload: EntryForm = {
      ...form,
      code: form.code.trim().toUpperCase()
        || form.label.trim().toUpperCase().replace(/\s+/g, '_'),
    };
    if (editingId) {
      updateMutation.mutate({ id: editingId, payload });
      return;
    }
    // Création : un code saisi doit être unique dans le dictionnaire.
    if (data?.[key]?.some((e) => e.code === payload.code)) {
      toast.error('Ce code existe déjà dans ce dictionnaire');
      return;
    }
    createMutation.mutate({ key, payload });
  };

  const toggleActif = (entry: DictionaryEntry) => {
    updateMutation.mutate({
      id: entry.id,
      payload: {
        code: entry.code, label: entry.label, description: entry.description || '',
        color: entry.color || '#8b5cf6', ordre: entry.ordre, actif: !entry.actif,
      },
    });
  };

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  const dictKeys = Object.keys(data || {}).sort((a, b) =>
    (DICT_LABELS[a] || a).localeCompare(DICT_LABELS[b] || b));

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <BookOpen className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Dictionnaires</h1>
          </div>
          <p className="page-subtitle">
            Référentiels de la plateforme : types d&apos;événements, statuts, raisons d&apos;absence,
            catégories… Adaptez chaque liste au vocabulaire de votre église — sans modifier le code.
          </p>
        </div>
        <div className="page-header-actions animate-fade-in">
          <button
            className="btn-ghost btn-sm"
            onClick={() => resetMutation.mutate()}
            disabled={resetMutation.isPending}
          >
            <RotateCcw className="w-4 h-4" /> Restaurer les défauts
          </button>
        </div>
      </div>

      {/* Explication rapide */}
      <div className="glass-card p-4 mb-6 text-sm text-gray-500 dark:text-gray-400 flex items-start gap-3">
        <Sparkles className="w-4 h-4 text-primary-500 mt-0.5 flex-shrink-0" />
        <p>
          Chaque liste peut être <strong className="text-gray-700 dark:text-gray-200">renommée</strong>,
          <strong className="text-gray-700 dark:text-gray-200"> réordonnée</strong> et
          <strong className="text-gray-700 dark:text-gray-200"> recolorée</strong> ; les entrées peuvent être
          ajoutées, désactivées ou supprimées. Les changements sont appliqués immédiatement dans toute
          l&apos;application.
        </p>
      </div>

      <div className="space-y-4 animate-slide-up">
        {dictKeys.map((key) => {
          const entries = (data?.[key] || []).slice().sort((a, b) => a.ordre - b.ordre);
          const isOpen = expanded[key];
          const isEditing = editingKey === key;
          return (
            <div key={key} className="glass-card overflow-hidden">
              <button
                className="w-full flex items-center justify-between p-5 hover:bg-white/40 dark:hover:bg-gray-800/30 transition-colors"
                onClick={() => setExpanded((e) => ({ ...e, [key]: !isOpen }))}
              >
                <div className="flex items-center gap-3 text-left">
                  <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center flex-shrink-0">
                    <BookOpen className="w-4 h-4 text-white" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                      {DICT_LABELS[key] || key}
                    </p>
                    <p className="text-[11px] text-gray-400 font-mono uppercase tracking-wider">{key}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="badge-info text-[10px]">{entries.length} entrée{entries.length > 1 ? 's' : ''}</span>
                  {isOpen ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </div>
              </button>

              {isOpen && (
                <div className="px-5 pb-5 border-t border-gray-200/60 dark:border-gray-700/60">
                  <div className="space-y-2 mt-4">
                    {entries.map((entry) => (
                      <div
                        key={entry.id}
                        className={`flex items-center gap-3 p-3 rounded-xl border border-gray-200/60 dark:border-gray-700/60 ${
                          entry.actif ? 'bg-white/40 dark:bg-gray-800/30' : 'opacity-50 bg-gray-100/40 dark:bg-gray-900/40'
                        }`}
                      >
                        <span
                          className="w-3.5 h-3.5 rounded-full flex-shrink-0"
                          style={{ backgroundColor: entry.color || '#8b5cf6' }}
                          title={entry.color || 'Aucune couleur'}
                        />
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{entry.label}</p>
                          <p className="text-[10px] text-gray-400 font-mono uppercase">{entry.code}</p>
                        </div>
                        <span className={`badge text-[10px] ${entry.actif ? 'badge-success' : 'badge-gray'}`}>
                          {entry.actif ? 'Actif' : 'Inactif'}
                        </span>
                        <button
                          onClick={() => toggleActif(entry)}
                          className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                          title={entry.actif ? 'Désactiver' : 'Activer'}
                        >
                          {entry.actif
                            ? <ToggleRight className="w-4 h-4 text-primary-600" />
                            : <ToggleLeft className="w-4 h-4 text-gray-400" />}
                        </button>
                        <button
                          onClick={() => openEdit(entry)}
                          className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                        >
                          <Pencil className="w-4 h-4 text-blue-500" />
                        </button>
                        <button
                          onClick={() => setConfirmDelete(entry)}
                          className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                        >
                          <Trash2 className="w-4 h-4 text-red-500" />
                        </button>
                      </div>
                    ))}
                    {entries.length === 0 && (
                      <p className="text-sm text-gray-400 text-center py-3">Aucune entrée</p>
                    )}
                  </div>

                  {isEditing ? (
                    <div className="mt-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/30 border border-primary-200/50 dark:border-primary-700/30">
                      <p className="text-xs font-semibold text-gray-500 mb-3">
                        {editingId ? 'Modifier l\'entrée' : 'Nouvelle entrée'}
                      </p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div>
                          <label className="label">Libellé *</label>
                          <input className="input" value={form.label}
                            onChange={(e) => setForm({ ...form, label: e.target.value })}
                            placeholder="Ex : Culte d'adoration" />
                        </div>
                        <div>
                          <label className="label">Code</label>
                          <input className="input font-mono uppercase" value={form.code}
                            onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
                            placeholder="Auto si vide" />
                        </div>
                        <div className="sm:col-span-2">
                          <label className="label">Description</label>
                          <input className="input" value={form.description}
                            onChange={(e) => setForm({ ...form, description: e.target.value })} />
                        </div>
                        <div>
                          <label className="label">Couleur</label>
                          <input type="color" className="input h-10 p-1" value={form.color}
                            onChange={(e) => setForm({ ...form, color: e.target.value })} />
                        </div>
                        <div>
                          <label className="label">Ordre d'affichage</label>
                          <input type="number" min={0} className="input" value={form.ordre}
                            onChange={(e) => setForm({ ...form, ordre: parseInt(e.target.value) || 0 })} />
                        </div>
                      </div>
                      <div className="flex justify-end gap-2 mt-4">
                        <button className="btn-secondary btn-sm" onClick={() => { setEditingKey(null); setEditingId(null); setForm(emptyForm()); }}>
                          Annuler
                        </button>
                        <button
                          className="btn-primary btn-sm"
                          onClick={() => submit(key)}
                          disabled={createMutation.isPending || updateMutation.isPending}
                        >
                          {(createMutation.isPending || updateMutation.isPending)
                            ? <Loader2 className="w-4 h-4 animate-spin" />
                            : <Save className="w-4 h-4" />}
                          Enregistrer
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button
                      className="btn-secondary btn-sm mt-4"
                      onClick={() => openCreate(key)}
                    >
                      <Plus className="w-3.5 h-3.5" /> Ajouter une entrée
                    </button>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Delete confirmation */}
      {confirmDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setConfirmDelete(null)}>
          <div className="card p-6 w-full max-w-sm mx-4 animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                <Trash2 className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h3 className="text-lg font-semibold">Confirmer la suppression</h3>
                <p className="text-sm text-gray-500">
                  « {confirmDelete.label} » ({confirmDelete.code}) sera supprimé du dictionnaire.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setConfirmDelete(null)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => deleteMutation.mutate(confirmDelete.id)}
                disabled={deleteMutation.isPending}
                className="btn-primary btn-sm bg-red-600 hover:bg-red-700"
              >
                {deleteMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                Supprimer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
