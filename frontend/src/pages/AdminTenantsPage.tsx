import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Plus, Pencil, Building2, Loader2, Save, Globe, Calendar,
} from 'lucide-react';
import type { Tenant, TenantStatus } from '@/types';

const STATUS_OPTIONS: { value: TenantStatus; label: string; color: string }[] = [
  { value: 'ACTIVE', label: 'Active', color: 'badge-success' },
  { value: 'SUSPENDED', label: 'Suspendue', color: 'badge-warning' },
  { value: 'CANCELLED', label: 'Annulée', color: 'badge-error' },
  { value: 'PENDING_SETUP', label: 'En attente', color: 'badge-info' },
];

const PLAN_OPTIONS = ['free', 'starter', 'pro', 'enterprise'];

interface TenantForm {
  name: string;
  slug: string;
  plan: string;
  status?: TenantStatus;
}

const EMPTY_FORM: TenantForm = { name: '', slug: '', plan: 'free' };

export default function AdminTenantsPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState<TenantForm>(EMPTY_FORM);

  const { data: tenants = [], isLoading } = useQuery({
    queryKey: ['admin', 'tenants'],
    queryFn: async () => {
      const res = await api.get('/tenants');
      return res.data as Tenant[];
    },
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['admin', 'tenants'] });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editId) {
        await api.put(`/tenants/${editId}`, { name: form.name, status: form.status, plan: form.plan });
      } else {
        await api.post('/tenants', { name: form.name, slug: form.slug, plan: form.plan });
      }
    },
    onSuccess: () => {
      invalidate();
      setModalOpen(false);
      setEditId(null);
      toast.success(editId ? 'Tenant mis à jour' : 'Tenant créé');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const openCreate = () => { setEditId(null); setForm(EMPTY_FORM); setModalOpen(true); };
  const openEdit = (t: Tenant) => {
    setEditId(t.id);
    setForm({ name: t.name, slug: t.slug, plan: t.plan, status: t.status });
    setModalOpen(true);
  };

  const autoSlug = (name: string) =>
    name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

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
          <h1 className="page-title">Églises (Tenants)</h1>
          <p className="page-subtitle">
            Gérez les églises de la plateforme — chaque église possède ses propres
            données et utilisateurs isolés.
          </p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate}>
            <Plus className="w-4 h-4" /> Nouvelle église
          </button>
        </div>
      </div>

      {/* Stats bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        {STATUS_OPTIONS.map((s) => {
          const count = tenants.filter((t) => t.status === s.value).length;
          return (
            <div key={s.value} className="glass-card p-4 text-center animate-slide-up">
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{count}</p>
              <p className="text-[10px] text-gray-400 mt-1">{s.label}</p>
            </div>
          );
        })}
      </div>

      {/* Tenant list */}
      {tenants.length === 0 ? (
        <div className="empty-state glass-card">
          <Building2 className="w-10 h-10 text-gray-300 mb-3" />
          <p className="text-gray-500">Aucune église configurée.</p>
          <button className="text-primary-500 hover:underline text-sm mt-2" onClick={openCreate}>
            Créer la première église
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {tenants.map((t, i) => {
            const statusInfo = STATUS_OPTIONS.find((s) => s.value === t.status) ?? STATUS_OPTIONS[0];
            return (
              <div
                key={t.id}
                className="glass-card px-5 py-4 flex items-center gap-4 animate-slide-up"
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
                  {t.name.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t.name}</span>
                    <span className={`badge text-[10px] ${statusInfo.color}`}>{statusInfo.label}</span>
                    <span className="badge badge-gray text-[10px]">{t.plan}</span>
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                    <span className="flex items-center gap-1">
                      <Globe className="w-3 h-3" /> {t.slug}
                    </span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {new Date(t.createdAt).toLocaleDateString('fr-FR')}
                    </span>
                  </div>
                </div>
                <button
                  className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70"
                  onClick={() => openEdit(t)}
                >
                  <Pencil className="w-4 h-4" />
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* Create / Edit Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold">
                {editId ? 'Modifier l\'église' : 'Nouvelle église'}
              </h3>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setModalOpen(false)}>
                ×
              </button>
            </div>
            <div className="modal-body space-y-4 max-h-[60vh] overflow-y-auto">
              <div>
                <label className="label">Nom de l'église</label>
                <input
                  className="input"
                  value={form.name}
                  onChange={(e) => {
                    const name = e.target.value;
                    setForm({
                      ...form,
                      name,
                      slug: editId ? form.slug : autoSlug(name),
                    });
                  }}
                  placeholder="Ex. : Église de la Grâce"
                />
              </div>
              <div>
                <label className="label">Slug (identifiant URL)</label>
                <input
                  className="input font-mono"
                  value={form.slug}
                  onChange={(e) => setForm({ ...form, slug: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '') })}
                  disabled={!!editId}
                  placeholder="eglise-de-la-grace"
                />
                <p className="text-[10px] text-gray-400 mt-1">
                  {editId ? 'Le slug ne peut pas être modifié.' : 'Minuscules, chiffres et tirets uniquement.'}
                </p>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Plan</label>
                  <select
                    className="input"
                    value={form.plan}
                    onChange={(e) => setForm({ ...form, plan: e.target.value })}
                  >
                    {PLAN_OPTIONS.map((p) => (
                      <option key={p} value={p}>{p.charAt(0).toUpperCase() + p.slice(1)}</option>
                    ))}
                  </select>
                </div>
                {editId && (
                  <div>
                    <label className="label">Statut</label>
                    <select
                      className="input"
                      value={form.status}
                      onChange={(e) => setForm({ ...form, status: e.target.value as TenantStatus })}
                    >
                      {STATUS_OPTIONS.map((s) => (
                        <option key={s.value} value={s.value}>{s.label}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setModalOpen(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => saveMutation.mutate()}
                disabled={saveMutation.isPending || !form.name.trim() || (!editId && !form.slug.trim())}
              >
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
