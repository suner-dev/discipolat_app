import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import type { ProgramType, ProgramSubType } from '@/types';
import {
  Calendar, Plus, Save, Loader2, X, CheckCircle2, Trash2,
  Pencil, ToggleLeft, ToggleRight, Sparkles, Clock,
} from 'lucide-react';
import toast from 'react-hot-toast';

interface SubTypeForm {
  id?: string;
  label: string;
  heureDebut: string;
  heureFin: string;
  actif: boolean;
  ordre?: number;
}

interface TypeForm {
  code: string;
  label: string;
  description: string;
  aSousProgrammes: boolean;
  couleur: string;
  actif: boolean;
  ordre: number;
  sousProgrammes: SubTypeForm[];
}

const emptyForm = (): TypeForm => ({
  code: '', label: '', description: '', aSousProgrammes: false,
  couleur: '#8b5cf6', actif: true, ordre: 0, sousProgrammes: [],
});

export default function ProgramTypesPage() {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<ProgramType | null>(null);
  const [form, setForm] = useState<TypeForm>(emptyForm());
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);

  const { data: types, isLoading } = useQuery({
    queryKey: ['programs', 'all'],
    queryFn: async () => (await api.get('/programs?all=true')).data as ProgramType[],
  });

  const createMutation = useMutation({
    mutationFn: async (payload: TypeForm) => {
      await api.post('/programs', payload);
    },
    onSuccess: () => {
      toast.success('Type de programme créé');
      queryClient.invalidateQueries({ queryKey: ['programs'] });
      setShowModal(false);
      setForm(emptyForm());
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: TypeForm }) => {
      await api.put(`/programs/${id}`, payload);
    },
    onSuccess: () => {
      toast.success('Type de programme mis à jour');
      queryClient.invalidateQueries({ queryKey: ['programs'] });
      setShowModal(false);
      setEditing(null);
      setForm(emptyForm());
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/programs/${id}`);
    },
    onSuccess: () => {
      toast.success('Type de programme supprimé');
      queryClient.invalidateQueries({ queryKey: ['programs'] });
      setConfirmDelete(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm());
    setShowModal(true);
  };

  const openEdit = (t: ProgramType) => {
    setEditing(t);
    setForm({
      code: t.code, label: t.label, description: t.description || '',
      aSousProgrammes: t.aSousProgrammes, couleur: t.couleur || '#8b5cf6',
      actif: t.actif, ordre: t.ordre ?? 0,
      sousProgrammes: (t.sousProgrammes || []).map((s) => ({
        id: s.id, label: s.label,
        heureDebut: s.heureDebut?.slice(0, 5) || '',
        heureFin: s.heureFin?.slice(0, 5) || '',
        actif: s.actif, ordre: s.ordre ?? 0,
      })),
    });
    setShowModal(true);
  };

  const addSubType = () => {
    setForm((f) => ({
      ...f,
      sousProgrammes: [...f.sousProgrammes, { label: '', heureDebut: '', heureFin: '', actif: true, ordre: f.sousProgrammes.length }],
    }));
  };

  const updateSubType = (i: number, patch: Partial<SubTypeForm>) => {
    setForm((f) => ({
      ...f,
      sousProgrammes: f.sousProgrammes.map((s, idx) => (idx === i ? { ...s, ...patch } : s)),
    }));
  };

  const removeSubType = (i: number) => {
    setForm((f) => ({ ...f, sousProgrammes: f.sousProgrammes.filter((_, idx) => idx !== i) }));
  };

  const handleSubmit = () => {
    if (!form.label.trim() || !form.code.trim()) {
      toast.error('Le nom et le code sont obligatoires');
      return;
    }
    const payload = {
      ...form,
      code: form.code.trim().toUpperCase().replace(/\s+/g, '_'),
      actif: true,
      sousProgrammes: form.sousProgrammes
        .filter((s) => s.label.trim())
        .map((s) => ({ ...s, actif: true })),
    };
    if (editing) updateMutation.mutate({ id: editing.id, payload });
    else createMutation.mutate(payload);
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Calendar className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Types de programmes</h1>
          </div>
          <p className="page-subtitle">
            Configurez les programmes de l'église (dimanche, convention, séminaire…) et leurs sous-programmes — utilisés pour la saisie des présences
          </p>
        </div>
        <button onClick={openCreate} className="btn-primary btn-sm animate-scale-in">
          <Plus className="w-4 h-4" /> Nouveau type
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="glass-card p-5 animate-pulse">
              <div className="skeleton h-5 w-32 mb-3" />
              <div className="skeleton h-3 w-24" />
            </div>
          ))}
        </div>
      ) : types && types.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 animate-slide-up">
          {types.map((t) => (
            <div key={t.id} className={`glass-card p-5 relative overflow-hidden ${!t.actif ? 'opacity-60' : ''}`}>
              <div className="absolute top-0 left-0 w-1 h-full" style={{ backgroundColor: t.couleur || '#8b5cf6' }} />
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div
                    className="w-11 h-11 rounded-xl flex items-center justify-center text-white font-bold shadow"
                    style={{ backgroundColor: t.couleur || '#8b5cf6' }}
                  >
                    {t.label.slice(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t.label}</p>
                    <p className="text-[11px] text-gray-400 uppercase tracking-wider">{t.code}</p>
                  </div>
                </div>
                <div className="flex gap-1">
                  <button
                    onClick={() => setConfirmDelete(t.id)}
                    className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                  >
                    <Trash2 className="w-4 h-4 text-red-500" />
                  </button>
                  <button onClick={() => openEdit(t)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                    <Pencil className="w-4 h-4 text-blue-500" />
                  </button>
                </div>
              </div>
              {t.description && <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">{t.description}</p>}
              <div className="mt-3 flex items-center gap-2 flex-wrap">
                {t.aSousProgrammes ? (
                  <span className="badge-info text-[10px]">
                    {t.sousProgrammes?.length || 0} sous-programme{(t.sousProgrammes?.length || 0) > 1 ? 's' : ''}
                  </span>
                ) : (
                  <span className="badge-gray text-[10px]">Sans sous-programme</span>
                )}
                <span className={`badge text-[10px] ${t.actif ? 'badge-success' : 'badge-gray'}`}>
                  {t.actif ? 'Actif' : 'Inactif'}
                </span>
              </div>
              {t.aSousProgrammes && t.sousProgrammes?.length > 0 && (
                <div className="mt-3 space-y-1">
                  {t.sousProgrammes.slice(0, 4).map((s) => (
                    <div key={s.id} className="flex items-center gap-2 text-xs text-gray-500">
                      <Clock className="w-3 h-3 text-gray-400" />
                      <span className="truncate">{s.label}</span>
                      {s.heureDebut && <span className="text-gray-400 ml-auto">{s.heureDebut.slice(0, 5)}</span>}
                    </div>
                  ))}
                  {(t.sousProgrammes?.length || 0) > 4 && (
                    <p className="text-[10px] text-gray-400">+{(t.sousProgrammes?.length || 0) - 4} autres</p>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <div className="glass-card p-12 text-center">
          <Calendar className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500 dark:text-gray-400">Aucun type de programme configuré</p>
          <p className="text-sm text-gray-400 mt-1">Cliquez sur « Nouveau type » pour commencer</p>
        </div>
      )}

      {/* Create/Edit modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4" onClick={() => setShowModal(false)}>
          <div className="card p-6 w-full max-w-xl mx-4 max-h-[90vh] overflow-y-auto animate-slide-up" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                {editing ? `Modifier : ${editing.label}` : 'Nouveau type de programme'}
              </h3>
              <button onClick={() => setShowModal(false)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Nom *</label>
                  <input className="input" value={form.label}
                    onChange={(e) => setForm({ ...form, label: e.target.value })}
                    placeholder="Ex : Dimanche, Convention" />
                </div>
                <div>
                  <label className="label">Code *</label>
                  <input className="input uppercase" value={form.code}
                    onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
                    placeholder="Ex : DIMANCHE" />
                </div>
              </div>

              <div>
                <label className="label">Description</label>
                <textarea className="input" rows={2} value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Couleur</label>
                  <input type="color" className="input h-10 p-1" value={form.couleur}
                    onChange={(e) => setForm({ ...form, couleur: e.target.value })} />
                </div>
                <div>
                  <label className="label">Ordre d'affichage</label>
                  <input type="number" min={0} className="input" value={form.ordre}
                    onChange={(e) => setForm({ ...form, ordre: parseInt(e.target.value) || 0 })} />
                </div>
              </div>

              <label className="flex items-center gap-2.5 p-3 rounded-xl bg-white/50 dark:bg-gray-800/30 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.aSousProgrammes}
                  onChange={(e) => setForm({ ...form, aSousProgrammes: e.target.checked })}
                  className="w-4 h-4 rounded accent-primary-500"
                />
                <span className="text-sm text-gray-700 dark:text-gray-200">
                  Ce programme a des sous-programmes (ex : premier culte, deuxième culte)
                </span>
              </label>

              {form.aSousProgrammes && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="text-xs font-medium text-gray-500">Sous-programmes</p>
                    <button onClick={addSubType} className="btn-secondary btn-xs">
                      <Plus className="w-3 h-3" /> Ajouter
                    </button>
                  </div>
                  {form.sousProgrammes.map((s, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <input
                        className="input flex-1"
                        placeholder="Nom (ex : Premier culte)"
                        value={s.label}
                        onChange={(e) => updateSubType(i, { label: e.target.value })}
                      />
                      <input
                        type="time"
                        className="input w-32"
                        value={s.heureDebut}
                        onChange={(e) => updateSubType(i, { heureDebut: e.target.value })}
                      />
                      <button onClick={() => removeSubType(i)} className="p-2 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20">
                        <Trash2 className="w-4 h-4 text-red-500" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                <button onClick={() => setShowModal(false)} className="btn-secondary btn-sm">Annuler</button>
                <button onClick={handleSubmit} disabled={isPending || !form.label.trim()} className="btn-primary btn-sm">
                  {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : editing ? <Pencil className="w-4 h-4" /> : <CheckCircle2 className="w-4 h-4" />}
                  {editing ? 'Enregistrer' : 'Créer'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

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
                <p className="text-sm text-gray-500">Ce type de programme sera définitivement supprimé.</p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setConfirmDelete(null)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => deleteMutation.mutate(confirmDelete)}
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
