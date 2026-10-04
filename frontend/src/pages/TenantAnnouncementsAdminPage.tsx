import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Loader2, Megaphone, Plus, Send, Trash2 } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';

/**
 * SPEC_ONBOARDING_FLOWS (FE-3) — annonces publiques d'une église.
 * L'admin crée une annonce (brouillon) puis la soumet à la modération
 * plateforme ; une fois approuvée, elle apparaît sur le landing.
 */
type Announcement = {
  id: string;
  title: string;
  description?: string;
  imageUrl?: string;
  city?: string;
  country?: string;
  eventAt?: string;
  linkUrl?: string;
  accessRef?: string;
  status: string;
  moderationNote?: string;
  expiresAt?: string;
  createdAt: string;
};

const STATUS_TONE: Record<string, string> = {
  DRAFT: 'bg-gray-500/10 text-gray-600 dark:text-gray-300 border-gray-500/20',
  PENDING_MODERATION: 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20',
  PUBLISHED: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
  REJECTED: 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20',
  EXPIRED: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
};

export default function TenantAnnouncementsAdminPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState({ title: '', description: '', city: '', country: '', eventAt: '', linkUrl: '', accessRef: '', imageUrl: '' });

  const { data: items, isLoading } = useQuery<Announcement[]>({
    queryKey: ['tenant', 'announcements'],
    queryFn: async () => (await api.get('/tenant/announcements')).data,
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['tenant', 'announcements'] });

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!form.title.trim()) throw new Error(tText('Le titre est requis.'));
      return api.post('/tenant/announcements', {
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        imageUrl: form.imageUrl.trim() || undefined,
        city: form.city.trim() || undefined,
        country: form.country.trim() || undefined,
        eventAt: form.eventAt ? new Date(form.eventAt).toISOString() : undefined,
        linkUrl: form.linkUrl.trim() || undefined,
        accessRef: form.accessRef.trim() || undefined,
      });
    },
    onSuccess: () => {
      setError('');
      setFormOpen(false);
      setForm({ title: '', description: '', city: '', country: '', eventAt: '', linkUrl: '', accessRef: '', imageUrl: '' });
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err) || (err instanceof Error ? err.message : '')),
  });

  const submitMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/tenant/announcements/${id}/submit`),
    onSuccess: refresh,
    onError: (err) => setError(getErrorMessage(err)),
  });

  const unpublishMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/tenant/announcements/${id}/unpublish`),
    onSuccess: refresh,
    onError: (err) => setError(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/tenant/announcements/${id}`),
    onSuccess: refresh,
    onError: (err) => setError(getErrorMessage(err)),
  });

  const field = (key: keyof typeof form, label: string, type = 'text', placeholder = '') => (
    <div>
      <label className="block text-xs font-medium text-gray-600 dark:text-gray-300 mb-1">{label}</label>
      <input
        type={type}
        value={form[key]}
        onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
        placeholder={placeholder}
        className="w-full rounded-lg border border-gray-200 dark:border-white/10 bg-gray-50/60 dark:bg-white/5 px-3 py-2 text-sm text-gray-900 dark:text-white focus:outline-none focus:border-primary-500/50"
      />
    </div>
  );

  return (
    <main className="p-6 space-y-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display flex items-center gap-2">
            <Megaphone className="w-6 h-6 text-primary-500" /> {tText('Annonces publiques')}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Annoncez un événement : après validation de la plateforme, il s’affiche sur la page d’accueil.')}
          </p>
        </div>
        <button type="button" onClick={() => setFormOpen((v) => !v)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-primary-500">
          <Plus className="h-4 w-4" /> {tText('Nouvelle annonce')}
        </button>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/20">{error}</div>}

      {formOpen && (
        <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 p-5 space-y-3 animate-slide-up">
          {field('title', tText('Titre *'), 'text', tText('Veillée de prière nationale'))}
          {field('description', tText('Description'), 'text', tText('Rejoignez-nous pour…'))}
          <div className="grid grid-cols-2 gap-3">
            {field('city', tText('Ville'))}
            {field('country', tText('Pays'))}
          </div>
          {field('eventAt', tText('Date de l’événement'), 'datetime-local')}
          <div className="grid grid-cols-2 gap-3">
            {field('accessRef', tText('Code / lien d’accès'), 'text', tText('BETHEL-7K2M'))}
            {field('linkUrl', tText('URL (facultatif)'), 'url')}
          </div>
          {field('imageUrl', tText('Image (URL)'), 'url')}
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={() => setFormOpen(false)} className="rounded-lg border px-3.5 py-2 text-sm hover:bg-gray-50 dark:hover:bg-white/5">{tText('Annuler')}</button>
            <button type="button" onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
              className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50">
              {createMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null} {tText('Enregistrer')}
            </button>
          </div>
        </section>
      )}

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100">
          {tText('Mes annonces')} ({items?.length ?? 0})
        </header>
        {isLoading ? (
          <div className="flex justify-center py-10"><Loader2 className="h-6 w-6 animate-spin text-primary-500" /></div>
        ) : (
          <div className="divide-y divide-gray-100 dark:divide-white/5">
            {items?.length ? items.map((a) => (
              <div key={a.id} className="px-5 py-3.5 flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-sm text-gray-900 dark:text-white truncate">{a.title}</p>
                    <span className={`shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-semibold ${STATUS_TONE[a.status] ?? ''}`}>{a.status}</span>
                  </div>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                    {[a.city, a.country].filter(Boolean).join(', ')}
                    {a.eventAt ? ` · ${new Date(a.eventAt).toLocaleDateString('fr-FR')}` : ''}
                  </p>
                  {a.status === 'REJECTED' && a.moderationNote && (
                    <p className="text-[11px] text-red-500 mt-1">{tText('Motif')} : {a.moderationNote}</p>
                  )}
                </div>
                <div className="flex items-center gap-1.5 shrink-0">
                  {(a.status === 'DRAFT' || a.status === 'REJECTED') && (
                    <button type="button" title={tText('Soumettre à modération')} onClick={() => submitMutation.mutate(a.id)} disabled={submitMutation.isPending}
                      className="rounded-md border px-2 py-1.5 text-xs text-sky-600 hover:bg-sky-500/10 disabled:opacity-50"><Send className="h-3.5 w-3.5" /></button>
                  )}
                  {a.status === 'PUBLISHED' && (
                    <button type="button" title={tText('Retirer')} onClick={() => unpublishMutation.mutate(a.id)} disabled={unpublishMutation.isPending}
                      className="rounded-md border px-2 py-1.5 text-xs text-amber-600 hover:bg-amber-500/10 disabled:opacity-50"><Megaphone className="h-3.5 w-3.5" /></button>
                  )}
                  <button type="button" title={tText('Supprimer')} onClick={() => deleteMutation.mutate(a.id)} disabled={deleteMutation.isPending}
                    className="rounded-md border px-2 py-1.5 text-xs text-red-600 hover:bg-red-500/10 disabled:opacity-50"><Trash2 className="h-3.5 w-3.5" /></button>
                </div>
              </div>
            )) : <p className="px-5 py-8 text-center text-sm text-gray-400">{tText('Aucune annonce pour le moment.')}</p>}
          </div>
        )}
      </section>
    </main>
  );
}
