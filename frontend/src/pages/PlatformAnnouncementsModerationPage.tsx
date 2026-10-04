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
    mutationFn: async (id: string) => {
      const note = window.prompt(tText('Motif du rejet (communiqué à l’église)')) ?? '';
      return api.post(`/platform/announcements/${id}/reject`, { note });
    },
    onSuccess: () => { setError(''); refresh(); },
    onError: (err) => setError(getErrorMessage(err)),
  });

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
                  <button type="button" onClick={() => rejectMutation.mutate(a.id)} disabled={rejectMutation.isPending}
                    className="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1.5 text-xs font-medium text-red-600 hover:bg-red-500/10 disabled:opacity-50">
                    <X className="h-3.5 w-3.5" /> {tText('Rejeter')}
                  </button>
                </div>
              </div>
            )) : <p className="px-5 py-10 text-center text-sm text-gray-400">{tText('File vide.')}</p>}
          </div>
        )}
      </section>
    </main>
  );
}
