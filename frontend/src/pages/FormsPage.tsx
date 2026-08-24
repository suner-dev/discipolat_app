import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  FileText, Plus, Loader2, RefreshCw, Eye, Edit3, Trash2, Send,
  BarChart3, Users, Clock, CheckCircle, Archive, X, ChevronDown,
  GripVertical, Type, Calendar, CheckSquare, List, Upload,
} from 'lucide-react';

interface FormTemplate {
  id: string;
  title: string;
  description: string;
  category: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  fields: FormField[];
  responseCount: number;
  createdAt: string;
  updatedAt: string;
}

interface FormField {
  id: string;
  type: 'TEXT' | 'TEXTAREA' | 'SELECT' | 'CHECKBOX' | 'DATE' | 'FILE' | 'SIGNATURE' | 'NUMBER';
  label: string;
  required: boolean;
  options?: string[];
  placeholder?: string;
}

const FIELD_TYPES = [
  { type: 'TEXT', label: 'Texte', icon: Type },
  { type: 'TEXTAREA', label: 'Paragraphe', icon: FileText },
  { type: 'SELECT', label: 'Choix unique', icon: List },
  { type: 'CHECKBOX', label: 'Cases à cocher', icon: CheckSquare },
  { type: 'DATE', label: 'Date', icon: Calendar },
  { type: 'FILE', label: 'Fichier', icon: Upload },
  { type: 'NUMBER', label: 'Nombre', icon: BarChart3 },
] as const;

export default function FormsPage() {
  const qc = useQueryClient();
  const [showBuilder, setShowBuilder] = useState(false);
  const [editingForm, setEditingForm] = useState<FormTemplate | null>(null);
  const [form, setForm] = useState({ title: '', description: '', category: 'GENERIC', fields: [] as FormField[] });

  const { data: forms = [], isLoading, refetch } = useQuery({
    queryKey: ['forms'],
    queryFn: async () => {
      const res = await api.get('/forms');
      return res.data as FormTemplate[];
    },
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/forms', form);
    },
    onSuccess: () => {
      toast.success('Formulaire créé');
      qc.invalidateQueries({ queryKey: ['forms'] });
      setShowBuilder(false);
      setForm({ title: '', description: '', category: 'GENERIC', fields: [] });
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const publishMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/forms/${id}/publish`); },
    onSuccess: () => { toast.success('Formulaire publié'); qc.invalidateQueries({ queryKey: ['forms'] }); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const archiveMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/forms/${id}/archive`); },
    onSuccess: () => { toast.success('Formulaire archivé'); qc.invalidateQueries({ queryKey: ['forms'] }); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const addField = (type: FormField['type']) => {
    setForm({ ...form, fields: [...form.fields, { id: crypto.randomUUID(), type, label: '', required: false, options: type === 'SELECT' ? ['Option 1'] : undefined }] });
  };

  const updateField = (index: number, updates: Partial<FormField>) => {
    const newFields = [...form.fields];
    newFields[index] = { ...newFields[index], ...updates };
    setForm({ ...form, fields: newFields });
  };

  const removeField = (index: number) => {
    setForm({ ...form, fields: form.fields.filter((_, i) => i !== index) });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PUBLISHED': return 'badge-success';
      case 'DRAFT': return 'badge-warning';
      case 'ARCHIVED': return 'badge-error';
      default: return 'badge-info';
    }
  };

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <FileText className="w-5 h-5 text-primary-500" />
            Formulaires intelligents
          </h1>
          <p className="page-subtitle">Créez des formulaires avec validation IA, conditions logiques et export de données.</p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => refetch()} className="btn-ghost btn-sm"><RefreshCw className="w-4 h-4" /></button>
          <button className="btn-primary btn-sm" onClick={() => { setShowBuilder(true); setEditingForm(null); setForm({ title: '', description: '', category: 'GENERIC', fields: [] }); }}>
            <Plus className="w-4 h-4" /> Nouveau formulaire
          </button>
        </div>
      </div>

      {/* Forms list */}
      {forms.length === 0 ? (
        <div className="glass-card p-10 text-center animate-scale-in">
          <FileText className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucun formulaire.</p>
          <button className="text-primary-500 hover:underline text-sm mt-2" onClick={() => setShowBuilder(true)}>Créer le premier formulaire</button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {forms.map((f, i) => (
            <div key={f.id} className="glass-card p-5 animate-slide-up hover:shadow-lg transition-shadow" style={{ animationDelay: `${i * 60}ms` }}>
              <div className="flex items-start justify-between mb-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-white">
                  <FileText className="w-5 h-5" />
                </div>
                <span className={`badge text-[10px] ${getStatusColor(f.status)}`}>{f.status}</span>
              </div>
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-1">{f.title}</h3>
              <p className="text-xs text-gray-400 mb-3 line-clamp-2">{f.description}</p>
              <div className="flex items-center gap-3 text-[10px] text-gray-400 mb-3">
                <span className="flex items-center gap-1"><Type className="w-3 h-3" /> {f.fields?.length || 0} champs</span>
                <span className="flex items-center gap-1"><Users className="w-3 h-3" /> {f.responseCount} réponses</span>
                <span className="flex items-center gap-1"><Clock className="w-3 h-3" /> {new Date(f.updatedAt).toLocaleDateString('fr-FR')}</span>
              </div>
              <div className="flex items-center gap-1">
                {f.status === 'DRAFT' && (
                  <button onClick={() => publishMutation.mutate(f.id)} disabled={publishMutation.isPending} className="btn-icon text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20" title="Publier">
                    <Send className="w-4 h-4" />
                  </button>
                )}
                <button onClick={() => { setEditingForm(f); setForm({ title: f.title, description: f.description, category: f.category, fields: f.fields || [] }); setShowBuilder(true); }} className="btn-icon text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20" title="Modifier">
                  <Edit3 className="w-4 h-4" />
                </button>
                {f.status === 'PUBLISHED' && (
                  <button onClick={() => archiveMutation.mutate(f.id)} className="btn-icon text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20" title="Archiver">
                    <Archive className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Builder Modal */}
      {showBuilder && (
        <div className="modal-overlay" onClick={() => setShowBuilder(false)}>
          <div className="modal-content max-w-2xl max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{editingForm ? 'Modifier le formulaire' : 'Nouveau formulaire'}</h3>
              <button className="btn-icon" onClick={() => setShowBuilder(false)}><X className="w-5 h-5" /></button>
            </div>
            <div className="modal-body space-y-4">
              <div><label className="label">Titre</label><input className="input" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} placeholder="Ex: Inscription événement" /></div>
              <div><label className="label">Description</label><textarea className="input min-h-[60px]" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Description du formulaire..." /></div>
              <div><label className="label">Catégorie</label>
                <select className="input" value={form.category} onChange={e => setForm({ ...form, category: e.target.value })}>
                  <option value="GENERIC">Général</option><option value="EVENT">Événement</option><option value="FEEDBACK">Feedback</option><option value="INSSCRIPTION">Inscription</option>
                </select>
              </div>
              {/* Field builder */}
              <div>
                <label className="label">Champs ({form.fields.length})</label>
                {form.fields.map((field, idx) => (
                  <div key={field.id} className="flex items-center gap-2 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 mb-2">
                    <GripVertical className="w-4 h-4 text-gray-400 cursor-grab" />
                    <input className="input flex-1 text-sm" value={field.label} onChange={e => updateField(idx, { label: e.target.value })} placeholder="Libellé du champ" />
                    <select className="input w-auto text-sm" value={field.type} onChange={e => updateField(idx, { type: e.target.value as FormField['type'] })}>
                      {FIELD_TYPES.map(ft => <option key={ft.type} value={ft.type}>{ft.label}</option>)}
                    </select>
                    <label className="flex items-center gap-1 text-xs"><input type="checkbox" checked={field.required} onChange={e => updateField(idx, { required: e.target.checked })} /> Obligatoire</label>
                    <button onClick={() => removeField(idx)} className="btn-icon text-red-400 hover:text-red-500"><Trash2 className="w-3.5 h-3.5" /></button>
                  </div>
                ))}
                <div className="flex flex-wrap gap-2 mt-2">
                  {FIELD_TYPES.map(ft => (
                    <button key={ft.type} onClick={() => addField(ft.type)} className="flex items-center gap-1 px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 text-xs hover:border-primary-500 transition">
                      <ft.icon className="w-3 h-3" /> {ft.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowBuilder(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => createMutation.mutate()} disabled={createMutation.isPending || !form.title}>
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                {editingForm ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
