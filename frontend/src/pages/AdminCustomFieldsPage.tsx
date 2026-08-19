import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { Plus, Pencil, Trash2, Loader2, Save, GripVertical, Search, Eye, EyeOff, X, Filter, ChevronDown, ChevronUp, BarChart3, Check } from 'lucide-react';
import type { CustomFieldDefinition } from '@/types';
import { useDictionaries } from '@/hooks/useDictionaries';

const ENTITY_TYPES = ['SOUL', 'USER', 'DEPARTMENT', 'FAMILY'];
const ENTITY_LABELS: Record<string, string> = { SOUL: 'Âmes', USER: 'Utilisateurs', DEPARTMENT: 'Départements', FAMILY: 'Familles' };
const ENTITY_ICONS: Record<string, string> = { SOUL: '💕', USER: '👤', DEPARTMENT: '🏢', FAMILY: '👨‍👩‍👧‍👦' };
const FIELD_TYPES = ['TEXTE', 'NOMBRE', 'DATE', 'DATE_HEURE', 'BOOLEEN', 'SELECTION', 'SELECTION_MULTIPLE', 'FICHIER', 'IMAGE', 'TELEPHONE', 'EMAIL', 'URL', 'TEXTAREA'];
const FIELD_TYPE_ICONS: Record<string, string> = { TEXTE: 'Aa', NOMBRE: '#', DATE: '📅', DATE_HEURE: '🕐', BOOLEEN: '✓', SELECTION: '▼', SELECTION_MULTIPLE: '☑', FICHIER: '📎', IMAGE: '🖼', TELEPHONE: '📞', EMAIL: '✉', URL: '🔗', TEXTAREA: '¶' };
const ROLES = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];

interface DefForm {
  code: string;
  label: string;
  type: string;
  obligatoire: boolean;
  ordre: number;
  options: string[];
  placeholder: string;
  defaultValue: string;
  rolesLecture: string[];
  rolesEcriture: string[];
  actif: boolean;
}

const EMPTY_FORM: DefForm = {
  code: '', label: '', type: 'TEXTE', obligatoire: false, ordre: 0,
  options: [], placeholder: '', defaultValue: '', rolesLecture: [], rolesEcriture: [], actif: true,
};

export default function AdminCustomFieldsPage() {
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const [entityType, setEntityType] = useState('SOUL');
  const [createOpen, setCreateOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState<DefForm>(EMPTY_FORM);
  const [searchTerm, setSearchTerm] = useState('');
  const [previewField, setPreviewField] = useState<CustomFieldDefinition | null>(null);
  const [showInactive, setShowInactive] = useState(false);

  const { data: definitions = [], isLoading } = useQuery({
    queryKey: ['custom-fields', 'definitions', entityType],
    queryFn: async () => {
      const res = await api.get(`/custom-fields/definitions/all?entiteType=${entityType}`);
      return res.data as CustomFieldDefinition[];
    },
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['custom-fields'] });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const payload = { ...form, entiteType: entityType };
      if (editId) {
        await api.put(`/custom-fields/definitions/${editId}`, payload);
      } else {
        await api.post('/custom-fields/definitions', payload);
      }
    },
    onSuccess: () => { invalidate(); setCreateOpen(false); setEditId(null); toast.success('Champ enregistré'); },
    onError: () => toast.error('Erreur'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/custom-fields/definitions/${id}`); },
    onSuccess: () => { invalidate(); toast.success('Champ supprimé'); },
    onError: () => toast.error('Erreur'),
  });

  const toggleActiveMutation = useMutation({
    mutationFn: async (def: CustomFieldDefinition) => {
      await api.put(`/custom-fields/definitions/${def.id}`, { ...def, actif: !def.actif });
    },
    onSuccess: () => { invalidate(); toast.success('Statut mis à jour'); },
    onError: () => toast.error('Erreur'),
  });

  const openCreate = () => { setEditId(null); setForm(EMPTY_FORM); setCreateOpen(true); };
  const openEdit = (d: CustomFieldDefinition) => {
    setEditId(d.id);
    setForm({
      code: d.code, label: d.label, type: d.type, obligatoire: d.obligatoire,
      ordre: d.ordre, options: d.options || [], placeholder: d.placeholder || '',
      defaultValue: d.defaultValue || '', rolesLecture: d.rolesLecture || [],
      rolesEcriture: d.rolesEcriture || [], actif: d.actif,
    });
    setCreateOpen(true);
  };

  const toggleOption = (opt: string) => {
    setForm({ ...form, options: form.options.includes(opt) ? form.options.filter((x) => x !== opt) : [...form.options, opt] });
  };

  // Stats per entity type
  const statsPerEntity = useMemo(() => {
    return ENTITY_TYPES.map((et) => ({
      key: et,
      label: ENTITY_LABELS[et],
      icon: ENTITY_ICONS[et],
      count: definitions.length, // This is only accurate for current tab
    }));
  }, [definitions]);

  const filtered = useMemo(() => {
    let items = definitions;
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      items = items.filter((d) =>
        d.label.toLowerCase().includes(q) ||
        d.code.toLowerCase().includes(q) ||
        d.type.toLowerCase().includes(q)
      );
    }
    if (!showInactive) items = items.filter((d) => d.actif);
    return items.sort((a, b) => a.ordre - b.ordre);
  }, [definitions, searchTerm, showInactive]);

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            Champs personnalisés
          </h1>
          <p className="page-subtitle">Ajoutez des champs supplémentaires aux entités — {definitions.length} champ(s) configuré(s) sur l'entité {ENTITY_LABELS[entityType]}</p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate}><Plus className="w-4 h-4" /> Nouveau champ</button>
        </div>
      </div>

      {/* Entity tabs with counts */}
      <div className="flex items-center gap-2 mb-6 -mx-4 px-4 overflow-x-auto scrollbar-hide">
        {ENTITY_TYPES.map((et) => {
          const isActive = entityType === et;
          return (
            <button
              key={et}
              onClick={() => setEntityType(et)}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all whitespace-nowrap ${
                isActive
                  ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800/50'
              }`}
            >
              <span>{ENTITY_ICONS[et]}</span>
              {ENTITY_LABELS[et]}
            </button>
          );
        })}
      </div>

      {/* Search & filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Rechercher un champ par nom, code ou type..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input pl-9"
            />
          </div>
          <button
            onClick={() => setShowInactive(!showInactive)}
            className={`btn-sm ${showInactive ? 'btn-primary' : 'btn-secondary'}`}
          >
            {showInactive ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
            {showInactive ? 'Tous' : 'Inclus inactifs'}
          </button>
        </div>
      </div>

      {definitions.length === 0 && (
        <div className="glass-card p-10 text-center animate-scale-in">
          <div className="text-4xl mb-3">{ENTITY_ICONS[entityType]}</div>
          <p className="text-gray-500 dark:text-gray-400 font-medium">Aucun champ personnalisé pour {ENTITY_LABELS[entityType]}</p>
          <button className="text-primary-500 hover:underline text-sm mt-2" onClick={openCreate}>Ajouter le premier champ</button>
        </div>
      )}

      <div className="space-y-3">
        {filtered.map((def, i) => (
          <div
            key={def.id}
            className={`glass-card px-5 py-4 flex items-center gap-4 animate-slide-up transition-all hover:shadow-md ${
              !def.actif ? 'opacity-50' : ''
            }`}
            style={{ animationDelay: `${i * 30}ms` }}
          >
            <GripVertical className="w-4 h-4 text-gray-300 flex-shrink-0" />
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500/10 to-primary-600/10 flex items-center justify-center text-lg flex-shrink-0">
              {FIELD_TYPE_ICONS[def.type] || '?'}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{def.label}</span>
                <span className="badge badge-gray text-[10px]">{def.type}</span>
                {def.obligatoire && <span className="text-[10px] font-semibold text-red-500">* Obligatoire</span>}
                {!def.actif && <span className="badge badge-gray">Désactivé</span>}
              </div>
              <div className="text-xs text-gray-400 font-mono mt-0.5">{def.code}</div>
              {def.rolesLecture.length > 0 && (
                <div className="flex items-center gap-1 mt-1 flex-wrap">
                  {def.rolesLecture.map((r) => (
                    <span key={r} className="px-1.5 py-0.5 text-[9px] font-medium rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 border border-primary-200/50 dark:border-primary-700/30">
                      {dictionaries.label('USER_ROLE', r) || r}
                    </span>
                  ))}
                </div>
              )}
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setPreviewField(previewField?.id === def.id ? null : def)}
                className="btn-icon text-gray-400 hover:text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                title="Aperçu"
              >
                <Eye className="w-4 h-4" />
              </button>
              <button
                onClick={() => toggleActiveMutation.mutate(def)}
                className={`btn-icon ${def.actif ? 'text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/20' : 'text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'}`}
                title={def.actif ? 'Désactiver' : 'Activer'}
              >
                {def.actif ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
              </button>
              <button className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70" onClick={() => openEdit(def)} title="Modifier">
                <Pencil className="w-4 h-4" />
              </button>
              <button className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50/50" onClick={() => { if (confirm('Supprimer ce champ ?')) deleteMutation.mutate(def.id); }} title="Supprimer">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Inline preview */}
      {previewField && (
        <div className="glass-card p-6 mt-4 animate-slide-up border-2 border-primary-200/50 dark:border-primary-700/30">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Eye className="w-4 h-4 text-primary-500" />
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Aperçu — {previewField.label}</h3>
            </div>
            <button onClick={() => setPreviewField(null)} className="btn-icon text-gray-400 hover:text-gray-600"><X className="w-4 h-4" /></button>
          </div>
          <div className="max-w-md">
            <label className="label">{previewField.label} {previewField.obligatoire && <span className="text-red-500">*</span>}</label>
            {previewField.type === 'TEXTE' || previewField.type === 'EMAIL' || previewField.type === 'TELEPHONE' || previewField.type === 'URL' ? (
              <input className="input" placeholder={previewField.placeholder || `Saisir ${previewField.label.toLowerCase()}`} defaultValue={previewField.defaultValue || ''} readOnly />
            ) : previewField.type === 'TEXTAREA' ? (
              <textarea className="input" rows={3} placeholder={previewField.placeholder || ''} defaultValue={previewField.defaultValue || ''} readOnly />
            ) : previewField.type === 'NOMBRE' ? (
              <input type="number" className="input" placeholder={previewField.placeholder || '0'} defaultValue={previewField.defaultValue || ''} readOnly />
            ) : previewField.type === 'DATE' ? (
              <input type="date" className="input" readOnly />
            ) : previewField.type === 'DATE_HEURE' ? (
              <input type="datetime-local" className="input" readOnly />
            ) : previewField.type === 'BOOLEEN' ? (
              <div className="flex items-center gap-3 p-3 rounded-xl border border-gray-200 dark:border-gray-700">
                <span className="text-sm text-gray-700 dark:text-gray-300">{previewField.label}</span>
                <button className="relative w-11 h-6 rounded-full bg-gray-300 dark:bg-gray-600">
                  <span className="absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow" />
                </button>
              </div>
            ) : previewField.type === 'SELECTION' ? (
              <select className="input" disabled>
                <option>{previewField.options?.[0] || 'Sélectionner...'}</option>
                {(previewField.options || []).map((o) => <option key={o}>{o}</option>)}
              </select>
            ) : (
              <input className="input" placeholder={previewField.placeholder || ''} defaultValue={previewField.defaultValue || ''} readOnly />
            )}
            {previewField.type === 'SELECTION_MULTIPLE' && previewField.options && previewField.options.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {previewField.options.map((o) => (
                  <label key={o} className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs border border-gray-200 dark:border-gray-700">
                    <input type="checkbox" className="rounded" disabled /> {o}
                  </label>
                ))}
              </div>
            )}
            <p className="text-[10px] text-gray-400 mt-2">Code: <span className="font-mono">{previewField.code}</span> · Type: {previewField.type} · Ordre: {previewField.ordre}</p>
          </div>
        </div>
      )}

      {/* Modal */}
      {createOpen && (
        <div className="modal-overlay" onClick={() => setCreateOpen(false)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-sm">
                  <Plus className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{editId ? 'Modifier le champ' : 'Nouveau champ'}</h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">pour {ENTITY_LABELS[entityType]}</p>
                </div>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setCreateOpen(false)}><X className="w-5 h-5" /></button>
            </div>
            <div className="modal-body space-y-4 max-h-[60vh] overflow-y-auto">
              <div className="grid grid-cols-2 gap-4">
                <div><label className="label">Code</label><input className="input font-mono" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase().replace(/\s/g, '_') })} disabled={!!editId} placeholder="MON_CHAMP" /></div>
                <div><label className="label">Libellé</label><input className="input" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} placeholder="Mon champ" /></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="label">Type</label>
                  <select className="input" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                    {FIELD_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div><label className="label">Ordre</label><input className="input" type="number" value={form.ordre} onChange={(e) => setForm({ ...form, ordre: Number(e.target.value) })} /></div>
              </div>
              <div><label className="label">Placeholder</label><input className="input" value={form.placeholder} onChange={(e) => setForm({ ...form, placeholder: e.target.value })} /></div>
              <div><label className="label">Valeur par défaut</label><input className="input" value={form.defaultValue} onChange={(e) => setForm({ ...form, defaultValue: e.target.value })} /></div>

              {(form.type === 'SELECTION' || form.type === 'SELECTION_MULTIPLE') && (
                <div>
                  <label className="label">Options</label>
                  <div className="flex flex-wrap gap-2 mb-2">
                    {form.options.map((opt) => (
                      <button key={opt} onClick={() => toggleOption(opt)}
                        className="px-2.5 py-1 rounded-lg text-xs font-medium border bg-primary-500/15 border-primary-500/30 text-primary-700 flex items-center gap-1">
                        {opt}
                        <X className="w-3 h-3" />
                      </button>
                    ))}
                  </div>
                  <input
                    className="input"
                    placeholder="Ajouter une option puis Entrée"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        const val = (e.target as HTMLInputElement).value.trim();
                        if (val && !form.options.includes(val)) {
                          setForm({ ...form, options: [...form.options, val] });
                        }
                        (e.target as HTMLInputElement).value = '';
                      }
                    }}
                  />
                </div>
              )}

              <div>
                <label className="label">Rôles pouvant lire</label>
                <div className="flex flex-wrap gap-1.5">
                  {ROLES.map((r) => (
                    <button key={r} onClick={() => setForm({ ...form, rolesLecture: form.rolesLecture.includes(r) ? form.rolesLecture.filter((x) => x !== r) : [...form.rolesLecture, r] })}
                      className={`px-2 py-0.5 rounded text-xs font-medium border ${form.rolesLecture.includes(r) ? 'bg-primary-500/15 border-primary-500/30 text-primary-700' : 'border-gray-200 dark:border-gray-700 text-gray-500'}`}>
                      {dictionaries.label('USER_ROLE', r) || r}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="label">Rôles pouvant modifier</label>
                <div className="flex flex-wrap gap-1.5">
                  {ROLES.map((r) => (
                    <button key={r} onClick={() => setForm({ ...form, rolesEcriture: form.rolesEcriture.includes(r) ? form.rolesEcriture.filter((x) => x !== r) : [...form.rolesEcriture, r] })}
                      className={`px-2 py-0.5 rounded text-xs font-medium border ${form.rolesEcriture.includes(r) ? 'bg-primary-500/15 border-primary-500/30 text-primary-700' : 'border-gray-200 dark:border-gray-700 text-gray-500'}`}>
                      {dictionaries.label('USER_ROLE', r) || r}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2">
                  <button
                    role="switch"
                    aria-checked={form.actif}
                    onClick={() => setForm({ ...form, actif: !form.actif })}
                    className={`relative w-11 h-6 rounded-full transition-colors ${form.actif ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                  >
                    <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform ${form.actif ? 'translate-x-5' : ''}`} />
                  </button>
                  <span className="text-sm text-gray-500">{form.actif ? 'Actif' : 'Désactivé'}</span>
                </div>
                <div className="flex items-center gap-2">
                  <input type="checkbox" id="obligatoire" checked={form.obligatoire} onChange={() => setForm({ ...form, obligatoire: !form.obligatoire })} className="rounded border-gray-300 text-primary-600 focus:ring-primary-500" />
                  <label htmlFor="obligatoire" className="text-sm text-gray-500">Obligatoire</label>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setCreateOpen(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
                {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                {editId ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
