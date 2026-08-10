import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { Boxes, Plus, Pencil, Trash2, Loader2, Power } from 'lucide-react';
import type { PlatformModule } from '@/types';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import { MENU_ICON_KEYS, resolveIcon } from '@/lib/menuIcons';

interface ModuleForm {
  key: string;
  label: string;
  description: string;
  icon: string;
  section: string;
  ordre: number;
  enabled: boolean;
}

const EMPTY_FORM: ModuleForm = { key: '', label: '', description: '', icon: 'Boxes', section: 'Général', ordre: 0, enabled: true };

export default function PlatformModulesPage() {
  const queryClient = useQueryClient();
  const { refetch } = usePlatformConfig();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<ModuleForm | null>(null);
  const [form, setForm] = useState<ModuleForm>(EMPTY_FORM);

  const { data: modules = [], isLoading } = useQuery({
    queryKey: ['platform', 'modules', 'admin'],
    queryFn: async () => {
      const res = await api.get('/platform/modules');
      return res.data as PlatformModule[];
    },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['platform', 'modules'] });
    refetch();
  };

  const toggleMutation = useMutation({
    mutationFn: async ({ key, enabled }: { key: string; enabled: boolean }) => {
      await api.put(`/platform/modules/${key}`, { enabled });
    },
    onSuccess: () => { invalidate(); toast.success('Module mis à jour'); },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const saveMutation = useMutation({
    mutationFn: async (payload: ModuleForm) => {
      if (editing) {
        await api.put(`/platform/modules/${editing.key}/edit`, payload);
      } else {
        await api.post('/platform/modules', payload);
      }
    },
    onSuccess: () => {
      invalidate();
      setCreateOpen(false);
      setEditing(null);
      toast.success(editing ? 'Module modifié' : 'Module créé');
    },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { detail?: string } } }).response?.data?.detail;
      toast.error(msg || 'Erreur lors de l’enregistrement');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (key: string) => {
      await api.delete(`/platform/modules/${key}`);
    },
    onSuccess: () => { invalidate(); toast.success('Module supprimé'); },
    onError: (e: unknown) => {
      const msg = (e as { response?: { data?: { detail?: string } } }).response?.data?.detail;
      toast.error(msg || 'Impossible de supprimer ce module');
    },
  });

  const grouped = modules.reduce<Record<string, PlatformModule[]>>((acc, m) => {
    (acc[m.section] = acc[m.section] || []).push(m);
    return acc;
  }, {});

  const openCreate = () => { setEditing(null); setForm(EMPTY_FORM); setCreateOpen(true); };
  const openEdit = (m: PlatformModule) => {
    setEditing({ key: m.key, label: m.label, description: m.description || '', icon: m.icon || 'Boxes', section: m.section, ordre: m.ordre, enabled: m.enabled });
    setCreateOpen(true);
  };

  if (isLoading) {
    return <div className="min-h-[40vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;
  }

  return (
    <div className="page-container max-w-4xl">
      <div className="page-header">
        <div>
          <h1 className="page-title">Modules</h1>
          <p className="page-subtitle">
            Activez ou désactivez les grands modules de la plateforme. Un module désactivé est
            masqué des menus et son API est bloquée côté serveur.
          </p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate}>
            <Plus className="w-4 h-4" /> Nouveau module
          </button>
        </div>
      </div>

      {Object.keys(grouped).length === 0 && (
        <div className="empty-state glass-card"><Boxes className="empty-state-icon" /><p className="text-gray-500 dark:text-gray-400">Aucun module.</p></div>
      )}

      <div className="space-y-6">
        {Object.entries(grouped).map(([section, items]) => (
          <section key={section} className="glass-card overflow-hidden">
            <div className="card-header flex items-center justify-between">
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">{section}</h3>
              <span className="text-xs text-gray-400">{items.length} module(s)</span>
            </div>
            <div className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
              {items.sort((a, b) => a.ordre - b.ordre).map((m) => {
                const Icon = resolveIcon(m.icon);
                return (
                  <div key={m.key} className="px-5 py-4 flex items-center gap-4">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${m.enabled ? 'bg-gradient-to-br from-primary-500 to-primary-700 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'}`}>
                      <Icon className="w-5 h-5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{m.label}</p>
                        <span className="badge badge-gray font-mono text-[10px]">{m.key}</span>
                      </div>
                      {m.description && <p className="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">{m.description}</p>}
                    </div>
                    <div className="flex items-center gap-1.5">
                      <button className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70 dark:hover:bg-gray-800/40" onClick={() => openEdit(m)} aria-label={`Modifier ${m.label}`}>
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20" onClick={() => { if (confirm(`Supprimer le module « ${m.label} » ?`)) deleteMutation.mutate(m.key); }} aria-label={`Supprimer ${m.label}`}>
                        <Trash2 className="w-4 h-4" />
                      </button>
                      <button
                        role="switch"
                        aria-checked={m.enabled}
                        aria-label={`Activer ${m.label}`}
                        onClick={() => toggleMutation.mutate({ key: m.key, enabled: !m.enabled })}
                        className={`relative ml-2 w-11 h-6 rounded-full transition-colors duration-200 ${m.enabled ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                      >
                        <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform duration-200 ${m.enabled ? 'translate-x-5' : ''}`} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      {/* Modal création / édition */}
      {createOpen && (
        <div className="modal-overlay" onClick={() => setCreateOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{editing ? 'Modifier le module' : 'Nouveau module'}</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setCreateOpen(false)}>×</button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">Clé (unique)</label>
                <input className="input font-mono" value={form.key} disabled={!!editing} onChange={(e) => setForm({ ...form, key: e.target.value.toUpperCase() })} placeholder="EXEMPLE" />
              </div>
              <div>
                <label className="label">Libellé</label>
                <input className="input" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} />
              </div>
              <div>
                <label className="label">Description</label>
                <input className="input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Icône</label>
                  <select className="input" value={form.icon} onChange={(e) => setForm({ ...form, icon: e.target.value })}>
                    {MENU_ICON_KEYS.map((k) => <option key={k} value={k}>{k}</option>)}
                  </select>
                </div>
                <div>
                  <label className="label">Section</label>
                  <input className="input" value={form.section} onChange={(e) => setForm({ ...form, section: e.target.value })} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Ordre</label>
                  <input className="input" type="number" value={form.ordre} onChange={(e) => setForm({ ...form, ordre: Number(e.target.value) })} />
                </div>
                <div className="flex items-end">
                  <button
                    role="switch"
                    aria-checked={form.enabled}
                    onClick={() => setForm({ ...form, enabled: !form.enabled })}
                    className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${form.enabled ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                  >
                    <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform duration-200 ${form.enabled ? 'translate-x-5' : ''}`} />
                  </button>
                  <span className="ml-2 text-sm text-gray-500 dark:text-gray-400">{form.enabled ? 'Actif' : 'Désactivé'}</span>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setCreateOpen(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => saveMutation.mutate(form)} disabled={saveMutation.isPending}>
                {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Power className="w-4 h-4" />}
                {editing ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}