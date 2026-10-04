import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, KeyRound, Loader2, Plus, RefreshCw, ShieldOff, Users } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';

/**
 * SPEC_ONBOARDING_FLOWS (FE-3) — console tenant : gestion des codes d'entrée
 * (église principale + sous-églises, D3) et file d'approbation des demandes de
 * rejointure (mode APPROVAL). Réservé TENANT_OWNER / TENANT_ADMIN.
 */
type JoinCode = {
  id: string;
  code: string;
  label?: string | null;
  joinMode: string;
  isActive: boolean;
  orgNodeId?: string | null;
  createdAt: string;
};

type JoinRequest = {
  id: string;
  code: string;
  email: string;
  userId: string;
  status: string;
  createdAt: string;
};

export default function TenantJoinManagementPage() {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const { data: codes, isLoading } = useQuery<JoinCode[]>({
    queryKey: ['tenant', 'join-codes'],
    queryFn: async () => (await api.get('/tenant/join-codes')).data,
  });

  const { data: requests } = useQuery<JoinRequest[]>({
    queryKey: ['tenant', 'join-requests'],
    queryFn: async () => (await api.get('/tenant/join-requests')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      const label = window.prompt(tText('Libellé du code (ex : Bethel, Campus Nord)')) ?? '';
      const mode = (window.prompt(tText('Mode : OPEN (entrée directe) ou APPROVAL (validation)'), 'OPEN') ?? 'OPEN').toUpperCase();
      return api.post('/tenant/join-codes', { label: label || undefined, joinMode: mode === 'APPROVAL' ? 'APPROVAL' : 'OPEN' });
    },
    onSuccess: () => {
      setNotice(tText('Code généré.'));
      setError('');
      queryClient.invalidateQueries({ queryKey: ['tenant', 'join-codes'] });
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const rotateMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/tenant/join-codes/${id}/rotate`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tenant', 'join-codes'] }),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const toggleMutation = useMutation({
    mutationFn: async ({ id, isActive }: { id: string; isActive: boolean }) =>
      api.patch(`/tenant/join-codes/${id}`, { isActive: !isActive }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tenant', 'join-codes'] }),
    onError: (err) => setError(getErrorMessage(err)),
  });

  const decideMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'approve' | 'reject' }) =>
      api.post(`/tenant/join-requests/${id}/${action}`),
    onSuccess: (_d, vars) => {
      setNotice(vars.action === 'approve' ? tText('Demande approuvée.') : tText('Demande rejetée.'));
      queryClient.invalidateQueries({ queryKey: ['tenant', 'join-requests'] });
      queryClient.invalidateQueries({ queryKey: ['tenant', 'join-codes'] });
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-16"><Loader2 className="h-7 w-7 animate-spin text-primary-500" /></div>;
  }

  return (
    <main className="p-6 space-y-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display flex items-center gap-2">
            <KeyRound className="w-6 h-6 text-primary-500" /> {tText('Codes d’entrée & rejointure')}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Chaque église ou sous-église a son propre code. Un membre ne le saisit qu’une fois.')}
          </p>
        </div>
        <button
          type="button"
          onClick={() => createMutation.mutate()}
          disabled={createMutation.isPending}
          className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-3.5 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
        >
          {createMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
          {tText('Nouveau code')}
        </button>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/20">{error}</div>}
      {notice && <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/20">{notice}</div>}

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100">
          {tText('Codes actifs')} ({codes?.length ?? 0})
        </header>
        <div className="divide-y divide-gray-100 dark:divide-white/5">
          {codes?.length ? codes.map((c) => (
            <div key={c.id} className="px-5 py-3 flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className={`font-mono font-bold text-sm ${c.isActive ? 'text-gray-900 dark:text-white' : 'text-gray-400 line-through'}`}>{c.code}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {c.label || tText('Sans libellé')} · {c.joinMode === 'APPROVAL' ? tText('sur validation') : tText('entrée directe')}
                </p>
              </div>
              <div className="flex items-center gap-1.5 shrink-0">
                <button type="button" title={tText('Régénérer')} onClick={() => rotateMutation.mutate(c.id)} disabled={rotateMutation.isPending}
                  className="rounded-md border px-2 py-1.5 text-xs hover:bg-gray-50 dark:hover:bg-white/5 disabled:opacity-50">
                  <RefreshCw className="h-3.5 w-3.5" />
                </button>
                <button type="button" title={c.isActive ? tText('Désactiver') : tText('Réactiver')} onClick={() => toggleMutation.mutate({ id: c.id, isActive: c.isActive })} disabled={toggleMutation.isPending}
                  className={`rounded-md border px-2 py-1.5 text-xs disabled:opacity-50 ${c.isActive ? 'text-amber-600 hover:bg-amber-500/10' : 'text-emerald-600 hover:bg-emerald-500/10'}`}>
                  {c.isActive ? <ShieldOff className="h-3.5 w-3.5" /> : <CheckCircle2 className="h-3.5 w-3.5" />}
                </button>
              </div>
            </div>
          )) : <p className="px-5 py-8 text-center text-sm text-gray-400">{tText('Aucun code. Créez-en un pour accueillir vos membres.')}</p>}
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
        <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100 flex items-center gap-2">
          <Users className="w-4 h-4" /> {tText('Demandes en attente')} ({requests?.length ?? 0})
        </header>
        <div className="divide-y divide-gray-100 dark:divide-white/5">
          {requests?.length ? requests.map((r) => (
            <div key={r.id} className="px-5 py-3 flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="font-medium text-sm text-gray-900 dark:text-white truncate">{r.email || r.userId || tText('Compte')}</p>
                <p className="text-xs font-mono text-gray-400">{r.code} · {new Date(r.createdAt).toLocaleDateString()}</p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button type="button" onClick={() => decideMutation.mutate({ id: r.id, action: 'approve' })} disabled={decideMutation.isPending}
                  className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-500 disabled:opacity-50">
                  {tText('Approuver')}
                </button>
                <button type="button" onClick={() => decideMutation.mutate({ id: r.id, action: 'reject' })} disabled={decideMutation.isPending}
                  className="rounded-lg border px-3 py-1.5 text-xs font-medium text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-white/5 disabled:opacity-50">
                  {tText('Rejeter')}
                </button>
              </div>
            </div>
          )) : <p className="px-5 py-8 text-center text-sm text-gray-400">{tText('Aucune demande en attente.')}</p>}
        </div>
      </section>
    </main>
  );
}
