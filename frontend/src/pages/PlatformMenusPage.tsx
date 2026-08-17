import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { Menu, Plus, Pencil, Trash2, Loader2, ArrowUp, ArrowDown, Save } from 'lucide-react';
import type { MenuEntry, PlatformModule } from '@/types';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import { useDictionaries } from '@/hooks/useDictionaries';
import { MENU_ICON_KEYS, resolveIcon } from '@/lib/menuIcons';
import ConfigRevisionHistory from '@/components/ConfigRevisionHistory';

const ROLES = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'];

interface MenuForm {
  key: string;
  label: string;
  href: string;
  icon: string;
  section: string;
  ordre: number;
  roles: string[];
  moduleKey: string;
  enabled: boolean;
}

const EMPTY_FORM: MenuForm = { key: '', label: '', href: '/', icon: 'Menu', section: 'Général', ordre: 0, roles: [], moduleKey: '', enabled: true };

export default function PlatformMenusPage() {
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const { refetch } = usePlatformConfig();
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<MenuForm | null>(null);
  const [form, setForm] = useState<MenuForm>(EMPTY_FORM);

  const { data: menus = [], isLoading } = useQuery({
    queryKey: ['platform', 'admin', 'menus'],
    queryFn: async () => {
      const res = await api.get('/platform/admin/menus');
      return res.data as MenuEntry[];
    },
  });

  const { data: modules = [] } = useQuery({
    queryKey: ['platform', 'modules', 'admin'],
    queryFn: async () => {
      const res = await api.get('/platform/modules');
      return res.data as PlatformModule[];
    },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['platform', 'admin', 'menus'] });
    queryClient.invalidateQueries({ queryKey: ['platform', 'menus'] });
    refetch();
  };

  const [editId, setEditId] = useState<string | null>(null);

  const saveFn = async (payload: MenuForm) => {
    if (editId) {
      // Mise à jour par id (UUID) ; on envoie l'objet complet.
      await api.put(`/platform/menus/${editId}`, payload);
    } else {
      await api.post('/platform/menus', payload);
    }
  };

  const [savePending, setSavePending] = useState(false);
  const handleSave = async () => {
    setSavePending(true);
    try {
      await saveFn(form);
      invalidate();
      setCreateOpen(false);
      setEditing(null);
      setEditId(null);
      toast.success('Menu enregistré');
    } catch {
      toast.error('Erreur');
    } finally {
      setSavePending(false);
    }
  };

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/platform/menus/${id}`); },
    onSuccess: () => { invalidate(); toast.success('Menu supprimé'); },
    onError: () => toast.error('Impossible de supprimer ce menu'),
  });

  const toggleMutation = useMutation({
    mutationFn: async ({ id, enabled }: { id: string; enabled: boolean }) => {
      const menu = menus.find((m) => m.id === id)!;
      await api.put(`/platform/menus/${id}`, { ...menu, enabled });
    },
    onSuccess: () => { invalidate(); },
    onError: () => toast.error('Erreur'),
  });

  const reorderMutation = useMutation({
    mutationFn: async (items: { id: string; ordre: number; section: string }[]) => {
      await api.post('/platform/menus/reorder', items);
    },
    onSuccess: () => { invalidate(); toast.success('Ordre mis à jour'); },
    onError: () => toast.error('Erreur de réordonnancement'),
  });

  const moveUp = (idx: number, section: string) => {
    if (idx === 0) return;
    const sectionItems = grouped[section] || [];
    const items = [...sectionItems];
    [items[idx - 1], items[idx]] = [items[idx], items[idx - 1]];
    reorderMutation.mutate(items.map((m, i) => ({ id: m.id, ordre: i, section })));
  };

  const moveDown = (idx: number, section: string) => {
    const sectionItems = grouped[section] || [];
    if (idx >= sectionItems.length - 1) return;
    const items = [...sectionItems];
    [items[idx], items[idx + 1]] = [items[idx + 1], items[idx]];
    reorderMutation.mutate(items.map((m, i) => ({ id: m.id, ordre: i, section })));
  };

  const openCreate = () => { setEditId(null); setEditing(null); setForm(EMPTY_FORM); setCreateOpen(true); };
  const openEdit = (m: MenuEntry) => {
    setEditId(m.id);
    setEditing({ key: m.key, label: m.label, href: m.href, icon: m.icon || 'Menu', section: m.section, ordre: m.ordre, roles: [...m.roles], moduleKey: m.moduleKey || '', enabled: m.enabled });
    setCreateOpen(true);
  };

  const grouped = menus.reduce<Record<string, MenuEntry[]>>((acc, m) => {
    (acc[m.section] = acc[m.section] || []).push(m);
    return acc;
  }, {});
  Object.values(grouped).forEach((items) => items.sort((a, b) => a.ordre - b.ordre));

  if (isLoading) {
    return <div className="min-h-[40vh] flex items-center justify-center"><div className="spinner h-8 w-8" /></div>;
  }

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div>
          <h1 className="page-title">Menus</h1>
          <p className="page-subtitle">Personnalisez les entrées de navigation : ordre, libellé, icônes, rôles visibles et activation.</p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate}><Plus className="w-4 h-4" /> Nouveau menu</button>
        </div>
      </div>

      <div className="mb-6">
        <ConfigRevisionHistory entityType="PLATFORM_MENU" />
      </div>

      {Object.keys(grouped).length === 0 && (
        <div className="empty-state glass-card"><Menu className="empty-state-icon" /><p className="text-gray-500 dark:text-gray-400">Aucun menu.</p></div>
      )}

      <div className="space-y-6">
        {Object.entries(grouped).map(([section, items]) => (
          <section key={section} className="glass-card overflow-hidden">
            <div className="card-header flex items-center justify-between">
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">{section}</h3>
              <span className="text-xs text-gray-400">{items.length} entrée(s)</span>
            </div>
            <div className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
              {items.map((m, idx) => {
                const Icon = resolveIcon(m.icon);
                return (
                  <div key={m.id} className="px-5 py-3 flex items-center gap-3">
                    <div className="flex flex-col gap-0.5">
                      <button className="p-0.5 rounded text-gray-300 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-30" disabled={idx === 0} onClick={() => moveUp(idx, section)} aria-label="Monter"><ArrowUp className="w-3.5 h-3.5" /></button>
                      <button className="p-0.5 rounded text-gray-300 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-30" disabled={idx >= items.length - 1} onClick={() => moveDown(idx, section)} aria-label="Descendre"><ArrowDown className="w-3.5 h-3.5" /></button>
                    </div>
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${m.enabled ? 'bg-gradient-to-br from-primary-500 to-primary-700 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-400'}`}>
                      <Icon className="w-4.5 h-4.5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{m.label}</p>
                        <span className="text-xs text-gray-400 font-mono">{m.href}</span>
                        {m.moduleKey && <span className="badge badge-info text-[10px]">{m.moduleKey}</span>}
                      </div>
                      <div className="flex items-center gap-1.5 mt-0.5 flex-wrap">
                        {m.roles.map((r) => <span key={r} className="px-1.5 py-0.5 text-[10px] font-medium rounded-full border border-primary-200 dark:border-primary-800 text-primary-700 dark:text-primary-300 bg-primary-50 dark:bg-primary-900/30">{dictionaries.label('USER_ROLE', r) || r}</span>)}
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      <button className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70 dark:hover:bg-gray-800/40" onClick={() => openEdit(m)} aria-label={`Modifier ${m.label}`}>
                        <Pencil className="w-4 h-4" />
                      </button>
                      <button className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20" onClick={() => { if (confirm(`Supprimer le menu « ${m.label} » ?`)) deleteMutation.mutate(m.id); }} aria-label={`Supprimer ${m.label}`}>
                        <Trash2 className="w-4 h-4" />
                      </button>
                      <button
                        role="switch"
                        aria-checked={m.enabled}
                        onClick={() => toggleMutation.mutate({ id: m.id, enabled: !m.enabled })}
                        className={`relative ml-2 w-10 h-5 rounded-full transition-colors duration-200 ${m.enabled ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                        aria-label={`Activer ${m.label}`}
                      >
                        <span className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform duration-200 ${m.enabled ? 'translate-x-5' : ''}`} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      {/* Modal */}
      {createOpen && (
        <div className="modal-overlay" onClick={() => { setCreateOpen(false); setEditId(null); }}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{editId ? 'Modifier le menu' : 'Nouveau menu'}</h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => { setCreateOpen(false); setEditId(null); }}>×</button>
            </div>
            <div className="modal-body space-y-4 max-h-[60vh] overflow-y-auto">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Clé</label>
                  <input className="input font-mono" value={form.key} disabled={!!editId} onChange={(e) => setForm({ ...form, key: e.target.value })} placeholder="mon-menu" />
                </div>
                <div>
                  <label className="label">Libellé</label>
                  <input className="input" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} />
                </div>
              </div>
              <div>
                <label className="label">URL</label>
                <input className="input font-mono" value={form.href} onChange={(e) => setForm({ ...form, href: e.target.value })} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Icône</label>
                  <select className="input" value={form.icon} onChange={(e) => setForm({ ...form, icon: e.target.value })}>
                    {MENU_ICON_KEYS.map((k) => <option key={k} value={k}>{k}</option>)}
                  </select>
                </div>
                <div>
                  <label className="label">Module</label>
                  <select className="input" value={form.moduleKey} onChange={(e) => setForm({ ...form, moduleKey: e.target.value })}>
                    <option value="">Aucun</option>
                    {modules.map((m) => <option key={m.key} value={m.key}>{m.label}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Section</label>
                  <input className="input" value={form.section} onChange={(e) => setForm({ ...form, section: e.target.value })} />
                </div>
                <div>
                  <label className="label">Ordre</label>
                  <input className="input" type="number" value={form.ordre} onChange={(e) => setForm({ ...form, ordre: Number(e.target.value) })} />
                </div>
              </div>
              <div>
                <label className="label">Rôles visibles</label>
                <div className="flex flex-wrap gap-2">
                  {ROLES.map((r) => (
                    <button
                      key={r}
                      onClick={() => setForm({ ...form, roles: form.roles.includes(r) ? form.roles.filter((x) => x !== r) : [...form.roles, r] })}
                      className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all border ${form.roles.includes(r) ? 'bg-primary-500/15 border-primary-500/30 text-primary-700 dark:text-primary-400' : 'border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400 hover:border-gray-300 dark:hover:border-gray-600'}`}
                    >
                      {dictionaries.label('USER_ROLE', r) || r}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  role="switch"
                  aria-checked={form.enabled}
                  onClick={() => setForm({ ...form, enabled: !form.enabled })}
                  className={`relative w-11 h-6 rounded-full transition-colors duration-200 ${form.enabled ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'}`}
                >
                  <span className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform duration-200 ${form.enabled ? 'translate-x-5' : ''}`} />
                </button>
                <span className="text-sm text-gray-500 dark:text-gray-400">{form.enabled ? 'Visibilité active' : 'Masqué'}</span>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => { setCreateOpen(false); setEditId(null); }}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={handleSave} disabled={savePending}>
                {savePending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                {editId ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}