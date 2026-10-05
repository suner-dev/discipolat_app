import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Check, Loader2, Megaphone, X } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';

/**
 * SPEC_ONBOARDING_FLOWS (FE-3 / D4) — file de modération des annonces
 * publiques. Console Super Admin uniquement : rien n'est publié sans approbation.
 */
type QueueItem = {
  id: string;
  tenantId: string;
  title: string;
  description?: string;
  city?: string;
  country?: string;
  eventAt?: string;
  accessRef?: string;
  status: string;
  createdAt: string;
};

const STATUSES = ['PENDING_MODERATION', 'PUBLISHED', 'REJECTED', ''];

export default function PlatformAnnouncementsModerationPage() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('PENDING_MODERATION');
  const [error, setError] = useState('');

  /**
   * F3 (console plateforme) — le motif de rejet ne passe plus par
   * `window.prompt` : une modale in-app, champs contrôlés, motif
   * obligatoire (c'est la trace d'audit et le texte renvoyé à l'église).
   */
  const [rejecting, setRejecting] = useState<QueueItem | null>(null);
  const [rejectNote, setRejectNote] = useState('');

  const { data: items, isLoading } = useQuery<QueueItem[]>({
    queryKey: ['platform', 'announcements', status],
    queryFn: async () => (await api.get('/platform/announcements', { params: status ? { status } : {} })).data,
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['platform', 'announcements'] });

  const approveMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/platform/announcements/${id}/approve`),
    onSuccess: () => { setError(''); refresh(); },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const rejectMutation = useMutation({
    mutationFn: async ({ id, note }: { id: string; note: string }) =>
      api.post(`/platform/announcements/${id}/reject`, { note }),
    onSuccess: () => { setError(''); setRejecting(null); setRejectNote(''); refresh(); },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const openReject = (item: QueueItem) => {
    setRejecting(item);
    setRejectNote('');
  };

  const confirmReject = () => {
    const note = rejectNote.trim();
    if (!rejecting || !note) return;   // motif obligatoire
    rejectMutation.mutate({ id: rejecting.id, note });
  };

  return (
    <main className="p-6 space-y-6 max-w-4xl mx-auto">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display flex items-center gap-2">
            <Megaphone className="w-6 h-6 text-primary-500" /> {tText('Modération des annonces')}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Aucune annonce n’apparaît sur le landing sans approbation de la plateforme.')}
          </p>
        </div>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="rounded-lg border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 px-3 py-2 text-sm text-gray-700 dark:text-gray-200"
        >
          {STATUSES.map((s) => (
            <option key={s} value={s}>{s === '' ? tText('Toutes') : s}</option>
          ))}
        </select>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/20">{error}</div>}

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary-500" /></div>
        ) : (
          <div className="divide-y divide-gray-100 dark:divide-white/5">
            {items?.length ? items.map((a) => (
              <div key={a.id} className="px-5 py-3.5 flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-sm text-gray-900 dark:text-white truncate">{a.title}</p>
                    <span className="shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-semibold bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20">{a.status}</span>
                  </div>
                  {a.description && <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 line-clamp-2">{a.description}</p>}
                  <p className="text-[11px] text-gray-400 mt-1">
                    {[a.city, a.country].filter(Boolean).join(', ')}
                    {a.eventAt ? ` · ${new Date(a.eventAt).toLocaleDateString('fr-FR')}` : ''}
                    {a.accessRef ? ` · ${a.accessRef}` : ''}
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button type="button" onClick={() => approveMutation.mutate(a.id)} disabled={approveMutation.isPending}
                    className="inline-flex items-center gap-1 rounded-lg bg-emerald-600 px-2.5 py-1.5 text-xs font-medium text-white hover:bg-emerald-500 disabled:opacity-50">
                    <Check className="h-3.5 w-3.5" /> {tText('Publier')}
                  </button>
                  <button type="button" onClick={() => openReject(a)} disabled={rejectMutation.isPending}
                    className="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1.5 text-xs font-medium text-red-600 hover:bg-red-500/10 disabled:opacity-50">
                    <X className="h-3.5 w-3.5" /> {tText('Rejeter')}
                  </button>
                </div>
              </div>
            )) : <p className="px-5 py-10 text-center text-sm text-gray-400">{tText('File vide.')}</p>}
          </div>
        )}
      </section>

      {/* ---- Modale de rejet (F3 : zéro window.prompt) ---- */}
      {rejecting && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-white/10 p-5 space-y-4">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">
              {tText('Rejeter l’annonce')}
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {rejecting.title}
            </p>
            <label className="block text-sm font-medium text-gray-600 dark:text-gray-300">
              {tText('Motif du rejet (communiqué à l’église)')}
            </label>
            <textarea
              value={rejectNote}
              onChange={(e) => setRejectNote(e.target.value)}
              rows={3}
              autoFocus
              className="w-full rounded-xl bg-gray-100/80 dark:bg-white/5 border border-gray-200 dark:border-white/10
                         text-gray-900 dark:text-white px-3 py-2 text-sm focus:outline-none
                         focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20"
            />
            <p className="text-xs text-gray-400">{tText('Le motif est obligatoire.')}</p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => { setRejecting(null); setRejectNote(''); }}
                className="rounded-lg px-3 py-2 text-sm text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-white/5"
              >
                {tText('Annuler')}
              </button>
              <button
                type="button"
                onClick={confirmReject}
                disabled={rejectMutation.isPending || !rejectNote.trim()}
                className="rounded-lg bg-red-600 px-3 py-2 text-sm font-medium text-white hover:bg-red-500 disabled:opacity-50"
              >
                {rejectMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : tText('Rejeter')}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
