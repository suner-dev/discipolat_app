import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { Plus, Pencil, Trash2, Loader2, Save, GripVertical } from 'lucide-react';
import type { CustomFieldDefinition } from '@/types';
import { useDictionaries } from '@/hooks/useDictionaries';

const ENTITY_TYPES = ['SOUL', 'USER', 'DEPARTMENT', 'FAMILY'];
const FIELD_TYPES = ['TEXTE', 'NOMBRE', 'DATE', 'DATE_HEURE', 'BOOLEEN', 'SELECTION', 'SELECTION_MULTIPLE', 'FICHIER', 'IMAGE', 'TELEPHONE', 'EMAIL', 'URL', 'TEXTAREA'];
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

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;

  return (
    <div className="page-container max-w-4xl">
      <div className="page-header">
        <div>
          <h1 className="page-title">Champs personnalisés</h1>
          <p className="page-subtitle">Ajoutez des champs supplémentaires aux entités (âmes, utilisateurs, départements, familles).</p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate}><Plus className="w-4 h-4" /> Nouveau champ</button>
        </div>
      </div>

      {/* Tabs entité */}
      <div className="tabs mb-6">
        {ENTITY_TYPES.map((et) => (
          <button key={et} className={entityType === et ? 'tab-active' : 'tab'} onClick={() => setEntityType(et)}>
            {et === 'SOUL' ? 'Âmes' : et === 'USER' ? 'Utilisateurs' : et === 'DEPARTMENT' ? 'Départements' : 'Familles'}
          </button>
        ))}
      </div>

      {definitions.length === 0 && (
        <div className="empty-state glass-card">
          <p className="text-gray-500 dark:text-gray-400">Aucun champ personnalisé pour cette entité. <button className="text-primary-500 hover:underline" onClick={openCreate}>Ajouter</button></p>
        </div>
      )}

      <div className="space-y-3">
        {definitions.sort((a, b) => a.ordre - b.ordre).map((def) => (
          <div key={def.id} className="glass-card px-5 py-4 flex items-center gap-4">
            <GripVertical className="w-4 h-4 text-gray-300 flex-shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{def.label}</span>
                <span className="badge badge-gray text-[10px]">{def.type}</span>
                {def.obligatoire && <span className="text-[10px] font-semibold text-red-500">* Obligatoire</span>}
                {!def.actif && <span className="badge badge-gray">Désactivé</span>}
              </div>
              <div className="text-xs text-gray-400 font-mono mt-0.5">{def.code} · {def.entiteType}</div>
              {def.rolesLecture.length > 0 && (
                <div className="flex items-center gap-1 mt-1 flex-wrap">
                  {def.rolesLecture.map((r) => <span key={r} className="px-1.5 py-0.5 text-[9px] font-medium rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 border border-primary-200/50 dark:border-primary-700/30">{dictionaries.label('USER_ROLE', r) || r}</span>)}
                </div>
              )}
            </div>
            <div className="flex items-center gap-1">
              <button className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70" onClick={() => openEdit(def)}><Pencil className="w-4 h-4" /></button>
              <button className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50/50" onClick={() => { if (confirm('Supprimer ce champ ?')) deleteMutation.mutate(def.id); }}><Trash2 className="w-4 h-4" /></button>
            </div>
          </div>
        ))}
      </div>

      {/* Modal */}
      {createOpen && (
        <div className="modal-overlay" onClick={() => setCreateOpen(false)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold">{editId ? 'Modifier le champ' : 'Nouveau champ'}</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setCreateOpen(false)}>×</button>
            </div>
            <div className="modal-body space-y-4 max-h-[60vh] overflow-y-auto">
              <div className="grid grid-cols-2 gap-4">
                <div><label className="label">Code</label><input className="input font-mono" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase().replace(/\s/g, '_') })} disabled={!!editId} /></div>
                <div><label className="label">Libellé</label><input className="input" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} /></div>
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
                  <label className="label">Options (sélectionner pour ajouter/retirer)</label>
                  <div className="flex flex-wrap gap-2">
                    {['Option 1', 'Option 2', 'Option 3'].map((opt) => (
                      <button key={opt} onClick={() => toggleOption(opt)}
                        className={`px-2.5 py-1 rounded-lg text-xs font-medium border ${form.options.includes(opt) ? 'bg-primary-500/15 border-primary-500/30 text-primary-700' : 'border-gray-200 dark:border-gray-700 text-gray-500'}`}>
                        {opt}
                      </button>
                    ))}
                    <input
                      className="input !w-28 !py-1 text-xs"
                      placeholder="Ajouter…"
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
                <div className="flex items-center gap-2 ml-4">
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