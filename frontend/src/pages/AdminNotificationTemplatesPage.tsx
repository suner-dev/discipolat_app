import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Plus, Pencil, Trash2, Bell, Loader2, Save, Inbox, Megaphone, Users2,
} from 'lucide-react';
import type { CanalNotification, NotificationEventInfo, NotificationTemplateAdmin } from '@/types';

const CANAL_OPTIONS: { value: CanalNotification; label: string }[] = [
  { value: 'IN_APP', label: 'In-app' },
  { value: 'PUSH', label: 'Push' },
  { value: 'EMAIL', label: 'Email' },
];

const ROLE_OPTIONS = ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'];

interface TemplateForm {
  event: string;
  titre: string;
  message: string;
  canaux: CanalNotification[];
  roles: string;
  actif: boolean;
}

const EMPTY_FORM: TemplateForm = {
  event: '', titre: '', message: '',
  canaux: ['IN_APP'], roles: '', actif: true,
};

export default function AdminNotificationTemplatesPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState<TemplateForm>(EMPTY_FORM);

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['admin', 'notifications', 'templates'],
    queryFn: async () => {
      const res = await api.get('/admin/notifications/templates');
      return res.data as NotificationTemplateAdmin[];
    },
  });

  const { data: catalog = [], isLoading: loadingCatalog } = useQuery({
    queryKey: ['admin', 'notifications', 'events'],
    queryFn: async () => {
      const res = await api.get('/admin/notifications/events');
      return res.data as NotificationEventInfo[];
    },
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['admin', 'notifications'] });

  const configuredEvents = useMemo(() => new Set(templates.map((t) => t.event)), [templates]);
  const eventLabel = (event: string) => catalog.find((e) => e.event === event)?.label || event;

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        event: form.event,
        titre: form.titre,
        message: form.message,
        canaux: form.canaux,
        rolesDestinataires: form.roles.split(',').map((r) => r.trim().toUpperCase()).filter(Boolean),
        actif: form.actif,
      };
      if (editId) {
        await api.put(`/admin/notifications/templates/${editId}`, body);
      } else {
        await api.post('/admin/notifications/templates', body);
      }
    },
    onSuccess: () => {
      invalidate();
      setModalOpen(false);
      setEditId(null);
      toast.success(editId ? 'Modèle mis à jour' : 'Modèle créé');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const toggleMutation = useMutation({
    mutationFn: async ({ id, actif }: { id: string; actif: boolean }) =>
      api.patch(`/admin/notifications/templates/${id}/toggle`, { actif }),
    onSuccess: () => { invalidate(); toast.success('Statut du modèle mis à jour'); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/admin/notifications/templates/${id}`),
    onSuccess: () => { invalidate(); toast.success('Modèle supprimé'); },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const openCreate = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEdit = (t: NotificationTemplateAdmin) => {
    setEditId(t.id);
    setForm({
      event: t.event,
      titre: t.titre || '',
      message: t.message || '',
      canaux: t.canaux.length > 0 ? t.canaux : ['IN_APP'],
      roles: t.rolesDestinataires.join(', '),
      actif: t.actif,
    });
    setModalOpen(true);
  };

  const onSelectEvent = (event: string) => {
    const info = catalog.find((e) => e.event === event);
    setForm({
      ...form,
      event,
      titre: info?.defaultTitre || '',
      message: info?.defaultMessage || '',
      canaux: info?.canauxSuggestes?.length ? info.canauxSuggestes : ['IN_APP'],
    });
  };

  const toggleCanal = (canal: CanalNotification) => {
    setForm((f) => ({
      ...f,
      canaux: f.canaux.includes(canal)
        ? f.canaux.filter((c) => c !== canal)
        : [...f.canaux, canal],
    }));
  };

  const availableEvents = catalog.filter((e) => !configuredEvents.has(e.event));
  const activeCount = templates.filter((t) => t.actif).length;

  if (isLoading || loadingCatalog) {
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
          <h1 className="page-title">Notifications</h1>
          <p className="page-subtitle">
            Personnalisez les modèles de notification de votre église : titres,
            messages, canaux de diffusion et rôles destinataires. Les variables{' '}
            <code className="font-mono text-[10px] bg-gray-100 dark:bg-gray-800 px-1 py-0.5 rounded">&#123;&#123;type&#125;&#125;</code>{' '}
            et{' '}
            <code className="font-mono text-[10px] bg-gray-100 dark:bg-gray-800 px-1 py-0.5 rounded">&#123;&#123;entiteType&#125;&#125;</code>{' '}
            sont remplacées à l'émission.
          </p>
        </div>
        <div className="page-header-actions">
          <button className="btn-primary btn-sm" onClick={openCreate} disabled={availableEvents.length === 0}>
            <Plus className="w-4 h-4" /> Nouveau modèle
          </button>
        </div>
      </div>

      {/* Stats bar */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-6">
        <div className="stat-card p-4 text-center">
          <span className="stat-label text-[10px]">Modèles actifs</span>
          <p className="stat-value text-xl">{activeCount}</p>
        </div>
        <div className="stat-card p-4 text-center">
          <span className="stat-label text-[10px]">Modèles configurés</span>
          <p className="stat-value text-xl">{templates.length}</p>
        </div>
        <div className="stat-card p-4 text-center">
          <span className="stat-label text-[10px]">Événements couverts</span>
          <p className="stat-value text-xl">{configuredEvents.size} / {catalog.length}</p>
        </div>
      </div>

      {templates.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Inbox className="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-1">
            Aucun modèle configuré
          </h3>
          <p className="text-sm text-gray-400 mb-4">
            Sans modèle, les notifications utilisent les textes par défaut de la plateforme.
            Créez votre premier modèle pour personnaliser un événement.
          </p>
          {availableEvents.length > 0 ? (
            <button className="btn-primary btn-sm" onClick={openCreate}>
              <Plus className="w-4 h-4" /> Créer un modèle
            </button>
          ) : (
            <p className="text-xs text-gray-400">Tous les événements sont déjà configurés.</p>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {templates.map((t) => (
            <div key={t.id} className={`glass-card p-5 ${t.actif ? '' : 'opacity-70'}`}>
              <div className="flex items-start gap-4 flex-wrap">
                <div className={`p-2.5 rounded-xl ${t.actif ? 'bg-primary-500/10 text-primary-600' : 'bg-gray-500/10 text-gray-400'}`}>
                  <Megaphone className="w-5 h-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{eventLabel(t.event)}</span>
                    <span className="font-mono text-[9px] badge badge-gray">{t.event}</span>
                    {t.actif ? (
                      <span className="badge text-[9px] badge-success">Actif</span>
                    ) : (
                      <span className="badge text-[9px] badge-inactive">Inactif</span>
                    )}
                  </div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200 mt-1">{t.titre || '—'}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 line-clamp-2">{t.message || '—'}</p>
                  <div className="flex items-center gap-2 mt-2 flex-wrap">
                    {t.canaux.map((c) => (
                      <span key={c} className="badge text-[9px] badge-info">{c}</span>
                    ))}
                    {t.rolesDestinataires.length > 0 && (
                      <span className="flex items-center gap-1 text-[10px] text-gray-400">
                        <Users2 className="w-3 h-3" />
                        {t.rolesDestinataires.join(', ')}
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    role="switch"
                    aria-checked={t.actif}
                    aria-label={`Activer ${eventLabel(t.event)}`}
                    onClick={() => toggleMutation.mutate({ id: t.id, actif: !t.actif })}
                    disabled={toggleMutation.isPending}
                    className={`w-9 h-5 rounded-full transition-colors relative cursor-pointer ${t.actif ? 'bg-primary-500' : 'bg-gray-300 dark:bg-gray-600'}`}
                    title={t.actif ? 'Désactiver' : 'Activer'}
                  >
                    <span className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-all ${t.actif ? 'left-[18px]' : 'left-0.5'}`} />
                  </button>
                  <button
                    onClick={() => openEdit(t)}
                    aria-label={`Modifier ${eventLabel(t.event)}`}
                    className="p-2 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-500/10 transition-all cursor-pointer"
                  >
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => { if (window.confirm(`Supprimer le modèle « ${eventLabel(t.event)} » ?`)) deleteMutation.mutate(t.id); }}
                    aria-label={`Supprimer ${eventLabel(t.event)}`}
                    className="p-2 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modale création / édition */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" onClick={() => setModalOpen(false)}>
          <div className="glass-card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-2">
                <Bell className="w-4 h-4 text-primary-500" />
                <h3 className="text-base font-bold">
                  {editId ? 'Modifier le modèle' : 'Nouveau modèle'}
                </h3>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setModalOpen(false)}>
                ×
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">Événement</label>
                <select
                  className="input"
                  value={form.event}
                  disabled={!!editId}
                  aria-label="Événement"
                  onChange={(e) => onSelectEvent(e.target.value)}
                >
                  <option value="">— Choisir un événement —</option>
                  {(editId ? catalog : availableEvents).map((e) => (
                    <option key={e.event} value={e.event}>{e.label} ({e.event})</option>
                  ))}
                </select>
                {!editId && (
                  <p className="text-[10px] text-gray-400 mt-1">
                    Un seul modèle par événement — les événements déjà configurés sont masqués.
                  </p>
                )}
              </div>

              <div>
                <label className="label">Titre (avec variables &#123;&#123;...&#125;&#125;)</label>
                <input
                  className="input"
                  value={form.titre}
                  aria-label="Titre du modèle (avec variables)"
                  onChange={(e) => setForm({ ...form, titre: e.target.value })}
                  placeholder="Ex : {{type}} — information importante"
                />
              </div>

              <div>
                <label className="label">Message</label>
                <textarea
                  className="input min-h-[80px]"
                  value={form.message}
                  onChange={(e) => setForm({ ...form, message: e.target.value })}
                  placeholder="Ex : Un événement approche : {{entiteType}}."
                />
              </div>

              <div>
                <label className="label">Canaux de diffusion</label>
                <div className="flex flex-wrap gap-3">
                  {CANAL_OPTIONS.map((c) => (
                    <label key={c.value} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={form.canaux.includes(c.value)}
                        onChange={() => toggleCanal(c.value)}
                        className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 cursor-pointer"
                      />
                      {c.label} <span className="font-mono text-[9px] text-gray-400">{c.value}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="label">Rôles destinataires</label>
                <div className="flex flex-wrap gap-2 mb-2">
                  {ROLE_OPTIONS.map((r) => {
                    const selected = form.roles.split(',').map((x) => x.trim().toUpperCase()).filter(Boolean).includes(r);
                    return (
                      <button
                        key={r}
                        type="button"
                        onClick={() => {
                          const current = form.roles.split(',').map((x) => x.trim().toUpperCase()).filter(Boolean);
                          const next = selected ? current.filter((x) => x !== r) : [...current, r];
                          setForm({ ...form, roles: next.join(', ') });
                        }}
                        className={`px-2.5 py-1 rounded-lg text-[10px] font-medium border transition-all cursor-pointer ${
                          selected
                            ? 'bg-primary-500/10 border-primary-500 text-primary-600'
                            : 'border-gray-200 dark:border-gray-700 text-gray-400 hover:border-gray-300'
                        }`}
                      >
                        {r}
                      </button>
                    );
                  })}
                </div>
                <input
                  className="input font-mono text-xs"
                  value={form.roles}
                  onChange={(e) => setForm({ ...form, roles: e.target.value })}
                  placeholder="PASTEUR, RESPONSABLE"
                  aria-label="Rôles destinataires"
                />
                <p className="text-[10px] text-gray-400 mt-1">
                  Recommandation — les rôles sont informés côté modèle, l'émission reste pilotée par le métier.
                </p>
              </div>

              <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.actif}
                  onChange={(e) => setForm({ ...form, actif: e.target.checked })}
                  className="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 cursor-pointer"
                />
                Modèle actif (appliqué aux nouvelles notifications)
              </label>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setModalOpen(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => saveMutation.mutate()}
                disabled={saveMutation.isPending || !form.event || !form.titre.trim() || form.canaux.length === 0}
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

