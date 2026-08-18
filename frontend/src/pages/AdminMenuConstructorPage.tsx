import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Plus, Pencil, Trash2, Loader2, Save, GripVertical, Menu as MenuIcon,
} from 'lucide-react';
import type { MenuEntry } from '@/types';

interface MenuForm {
  key: string;
  label: string;
  href: string;
  icon: string;
  section: string;
  ordre: number;
  enabled: boolean;
  roles: string[];
}

const EMPTY_FORM: MenuForm = {
  key: '', label: '', href: '', icon: '', section: 'Général', ordre: 0, enabled: true, roles: [],
};

function shuffleArray(array: string[]) {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

export default function AdminMenuConstructorPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState<MenuForm>(EMPTY_FORM);

  const { data: menus = [], isLoading } = useQuery({
    queryKey: ['admin', 'menus'],
    queryFn: async () => {
      const res = await api.get('/admin/menus');
      return res.data as MenuEntry[];
    },
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin', 'menus'] });

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        key: form.key,
        label: form.label,
        href: form.href,
        icon: form.icon,
        section: form.section,
        ordre: form.ordre,
        enabled: form.enabled,
        roles: form.roles,
      };
      if (editId) {
        await api.put(`/admin/menus/${editId}`, body);
      } else {
        await api.post('/admin/menus', body);
      }
    },
    onSuccess: () => {
      invalidate();
      setModalOpen(false);
      setEditId(null);
      toast.success(editId ? 'Menu mis à jour' : 'Menu créé');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/admin/menus/${id}`),
    onSuccess: () => { invalidate(); toast.success('Menu supprimé'); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEdit = (m: MenuEntry) => {
    setEditId(m.id);
    setForm({
      key: m.key,
      label: m.label || '',
      href: m.href || '',
      icon: m.icon || 'Menu',
      section: m.section || 'Général',
      ordre: m.ordre ?? 0,
      enabled: m.enabled ?? true,
      roles: m.roles ?? [],
    });
    setModalOpen(true);
  };

  if (isLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  const sorted = [...menus].sort((a, b) => (a.ordre ?? 0) - (b.ordre ?? 0));
  const grouped = sorted.reduce<Record<string, MenuEntry[]>>((acc, m) => {
    const sec = m.section || 'Général';
    (acc[sec] = acc[sec] || []).push(m);
    return acc;
  }, {});

  return (
    <div className="page-container max-w-4xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <MenuIcon className="w-6 h-6 text-primary-500" /> Constructeur de menus
          </h1>
          <p className="page-subtitle">Créez, réorganisez et personnalisez les entrées de navigation.</p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate}>
            <Plus className="w-4 h-4" /> Nouveau menu
          </button>
        </div>
      </div>

      {Object.entries(grouped).map(([section, items]) => (
        <div key={section} className="glass-card overflow-hidden mb-6">
          <div className="card-header">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 uppercase tracking-wider">{section}</h3>
          </div>
          <div className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
            {items.map((m) => (
              <div key={m.id} className="flex items-center gap-3 px-5 py-3 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 transition-colors">
                <GripVertical className="w-4 h-4 text-gray-300 dark:text-gray-600 cursor-grab flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{m.label}</span>
                    <span className="font-mono text-[10px] text-gray-400">{m.key}</span>
                    {m.href && <span className="text-[10px] text-gray-400">→ {m.href}</span>}
                  </div>
                  <div className="flex items-center gap-2 mt-0.5">
                    {m.icon && <span className="text-[10px] text-gray-400">icon: {m.icon}</span>}
                    {m.roles && m.roles.length > 0 && (
                      <span className="text-[10px] text-gray-400">
                        rôles: {m.roles.join(', ')}
                      </span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <span className={`badge text-[9px] ${m.enabled ? 'badge-success' : 'badge-warning'}`}>
                    {m.enabled ? 'Actif' : 'Inactif'}
                  </span>
                  <button className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200" onClick={() => openEdit(m)} aria-label={`Modifier ${m.label}`}>
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button className="btn-icon text-gray-400 hover:text-red-500" onClick={() => { if (confirm(`Supprimer « ${m.label} » ?`)) deleteMutation.mutate(m.id); }} aria-label={`Supprimer ${m.label}`}>
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}

      {/* Modal créer / éditer */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">
                {editId ? 'Modifier le menu' : 'Nouveau menu'}
              </h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">Clé (unique)</label>
                <input className="input font-mono" value={form.key} onChange={(e) => setForm({ ...form, key: e.target.value.toUpperCase() })} placeholder="MENU_KEY" disabled={!!editId} />
              </div>
              <div>
                <label className="label">Libellé</label>
                <input className="input" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} />
              </div>
              <div>
                <label className="label">URL / Route</label>
                <input className="input font-mono" value={form.href} onChange={(e) => setForm({ ...form, href: e.target.value })} placeholder="/dashboard" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Icône</label>
                  <input className="input" value={form.icon} onChange={(e) => setForm({ ...form, icon: e.target.value })} placeholder="Home" />
                </div>
                <div>
                  <label className="label">Section</label>
                  <input className="input" value={form.section} onChange={(e) => setForm({ ...form, section: e.target.value })} placeholder="Général" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Ordre</label>
                  <input className="input" type="number" value={form.ordre} onChange={(e) => setForm({ ...form, ordre: parseInt(e.target.value) || 0 })} />
                </div>
                <div className="flex items-end pb-1">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} className="rounded" />
                    <span className="text-sm text-gray-700 dark:text-gray-300">Activé</span>
                  </label>
                </div>
              </div>
              <div>
                <label className="label">Rôles visibles</label>
                <div className="flex flex-wrap gap-2 mt-1">
                  {['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'].map((role) => (
                    <button
                      key={role}
                      type="button"
                      onClick={() => {
                        setForm((prev) => ({
                          ...prev,
                          roles: prev.roles.includes(role)
                            ? prev.roles.filter((r) => r !== role)
                            : [...prev.roles, role],
                        }));
                      }}
                      className={`px-2 py-1 rounded-lg text-xs font-medium transition-all ${
                        form.roles.includes(role)
                          ? 'bg-primary-500 text-white'
                          : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700'
                      }`}
                    >
                      {role}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setModalOpen(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending || !form.key || !form.label}>
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
