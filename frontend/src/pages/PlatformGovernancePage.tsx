import { useCallback, useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';
import { AlertTriangle, Ban, CheckCircle2, Gavel, Loader2, Megaphone, RefreshCw, ShieldOff } from 'lucide-react';

/**
 * SPEC_ONBOARDING_FLOWS (FE-1/BE-5) — gouvernance des tenants par le Super
 * Admin : blocage/déblocage, bannissement/réintégration, avertissements et
 * litiges. Console plateforme uniquement.
 */
type TenantRow = { id: string; name: string; slug: string; status: string; plan: string };
type Warning = { id: string; message: string; severity: string; createdAt: string };
type Dispute = { id: string; subject: string; description?: string; status: string; resolution?: string; createdAt: string };

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
  SUSPENDED: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
  CANCELLED: 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20',
  PENDING_SETUP: 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20',
};

export default function PlatformGovernancePage() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const { data: tenants, isLoading } = useQuery<TenantRow[]>({
    queryKey: ['platform', 'tenants'],
    queryFn: async () => (await api.get('/platform/tenants')).data,
  });

  const tenant = tenants?.find((t) => t.id === selectedId) ?? null;

  const { data: warnings } = useQuery<Warning[]>({
    queryKey: ['platform', 'tenants', selectedId, 'warnings'],
    enabled: !!selectedId,
    queryFn: async () => (await api.get(`/platform/tenants/${selectedId}/warnings`)).data,
  });

  const { data: disputes } = useQuery<Dispute[]>({
    queryKey: ['platform', 'tenants', selectedId, 'disputes'],
    enabled: !!selectedId,
    queryFn: async () => (await api.get(`/platform/tenants/${selectedId}/disputes`)).data,
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['platform', 'tenants'] });
  }, [queryClient]);

  const statusMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'block' | 'unblock' | 'ban' | 'unban' }) => {
      const reason = window.prompt(tText('Motif (traçé dans l’audit)')) ?? '';
      return api.post(`/platform/tenants/${id}/${action}`, { reason });
    },
    onSuccess: (_d, vars) => {
      setNotice(tText(`Tenant ${vars.action === 'block' ? 'bloqué' : vars.action === 'unblock' ? 'débloqué' : vars.action === 'ban' ? 'banni' : 'réintégré'}.`));
      setError('');
      refresh();
    },
    onError: (err) => { setError(getErrorMessage(err)); setNotice(''); },
  });

  const warnMutation = useMutation({
    mutationFn: async () => {
      const message = window.prompt(tText('Message de l’avertissement'));
      if (!message) throw new Error('annulé');
      const severity = window.prompt(tText('Sévérité : INFO, FORMAL ou FINAL'), 'INFO') ?? 'INFO';
      return api.post(`/platform/tenants/${selectedId}/warnings`, { message, severity: severity.toUpperCase() });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['platform', 'tenants', selectedId, 'warnings'] }); },
    onError: (err) => { const m = getErrorMessage(err); if (m !== 'annulé' && !(err instanceof Error && err.message === 'annulé')) setError(m); },
  });

  const disputeMutation = useMutation({
    mutationFn: async () => {
      const subject = window.prompt(tText('Objet du litige'));
      if (!subject) throw new Error('annulé');
      const description = window.prompt(tText('Description (facultative)')) ?? '';
      return api.post(`/platform/tenants/${selectedId}/disputes`, { subject, description });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['platform', 'tenants', selectedId, 'disputes'] }); },
    onError: (err) => { const m = getErrorMessage(err); if (!(err instanceof Error && err.message === 'annulé')) setError(m); },
  });

  const closeDisputeMutation = useMutation({
    mutationFn: async (disputeId: string) => {
      const resolution = window.prompt(tText('Résolution apportée')) ?? '';
      return api.patch(`/platform/tenants/disputes/${disputeId}`, { status: 'CLOSED', resolution });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['platform', 'tenants', selectedId, 'disputes'] }); },
    onError: (err) => setError(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="flex justify-center py-16"><Loader2 className="h-7 w-7 animate-spin text-primary-500" /></div>;
  }

  return (
    <main className="p-6 space-y-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display">{tText('Gouvernance des églises')}</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Blocages, bannissements, avertissements et litiges — chaque action est auditée.')}
          </p>
        </div>
        <button type="button" onClick={refresh} className="inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm hover:bg-gray-50 dark:hover:bg-white/5">
          <RefreshCw className="h-4 w-4" /> {tText('Actualiser')}
        </button>
      </div>

      {error && <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/20">{error}</div>}
      {notice && <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/20">{notice}</div>}

      <div className="grid lg:grid-cols-2 gap-6">
        {/* Liste des tenants */}
        <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
          <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100">
            {tText('Églises de la plateforme')} ({tenants?.length ?? 0})
          </header>
          <div className="divide-y divide-gray-100 dark:divide-white/5 max-h-[60vh] overflow-y-auto">
            {tenants?.map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => setSelectedId(t.id)}
                className={`w-full text-left px-5 py-3 flex items-center justify-between gap-3 hover:bg-gray-50 dark:hover:bg-white/5 ${selectedId === t.id ? 'bg-primary-500/5' : ''}`}
              >
                <div className="min-w-0">
                  <p className="font-medium text-sm text-gray-900 dark:text-white truncate">{t.name}</p>
                  <p className="text-xs font-mono text-gray-400">{t.slug} · {t.plan}</p>
                </div>
                <span className={`shrink-0 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold ${STATUS_STYLES[t.status] ?? ''}`}>
                  {t.status}
                </span>
              </button>
            ))}
          </div>
        </section>

        {/* Panneau actions du tenant sélectionné */}
        <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 p-5 space-y-5">
          {!tenant ? (
            <p className="text-sm text-gray-400 py-10 text-center">{tText('Sélectionnez une église pour la gouverner.')}</p>
          ) : (
            <>
              <div>
                <h2 className="font-semibold text-gray-900 dark:text-white">{tenant.name}</h2>
                <p className="text-xs font-mono text-gray-400">{tenant.slug} — statut : {tenant.status}</p>
              </div>

              <div className="flex flex-wrap gap-2">
                {tenant.status !== 'SUSPENDED' && tenant.status !== 'CANCELLED' && (
                  <ActionBtn icon={ShieldOff} tone="amber" label={tText('Bloquer')} busy={statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ id: tenant.id, action: 'block' })} />
                )}
                {tenant.status === 'SUSPENDED' && (
                  <ActionBtn icon={CheckCircle2} tone="emerald" label={tText('Débloquer')} busy={statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ id: tenant.id, action: 'unblock' })} />
                )}
                {tenant.status !== 'CANCELLED' && (
                  <ActionBtn icon={Ban} tone="red" label={tText('Bannir')} busy={statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ id: tenant.id, action: 'ban' })} />
                )}
                {tenant.status === 'CANCELLED' && (
                  <ActionBtn icon={CheckCircle2} tone="emerald" label={tText('Réintégrer')} busy={statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ id: tenant.id, action: 'unban' })} />
                )}
              </div>

              <div className="border-t pt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-100 flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-amber-500" /> {tText('Avertissements')}
                  </h3>
                  <button type="button" onClick={() => warnMutation.mutate()} disabled={warnMutation.isPending}
                    className="rounded-lg border border-amber-500/30 px-3 py-1.5 text-xs font-medium text-amber-700 dark:text-amber-300 hover:bg-amber-500/10">
                    {tText('Émettre')}
                  </button>
                </div>
                <ul className="space-y-2 max-h-40 overflow-y-auto">
                  {warnings?.length ? warnings.map((w) => (
                    <li key={w.id} className="rounded-lg border border-gray-100 dark:border-white/5 p-2.5 text-xs">
                      <span className="font-semibold mr-2">{w.severity}</span>
                      <span className="text-gray-600 dark:text-gray-300">{w.message}</span>
                      <span className="block text-[10px] text-gray-400 mt-1">{new Date(w.createdAt).toLocaleString()}</span>
                    </li>
                  )) : <li className="text-xs text-gray-400">{tText('Aucun avertissement.')}</li>}
                </ul>
              </div>

              <div className="border-t pt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-100 flex items-center gap-2">
                    <Gavel className="w-4 h-4 text-violet-500" /> {tText('Litiges')}
                  </h3>
                  <button type="button" onClick={() => disputeMutation.mutate()} disabled={disputeMutation.isPending}
                    className="rounded-lg border border-violet-500/30 px-3 py-1.5 text-xs font-medium text-violet-700 dark:text-violet-300 hover:bg-violet-500/10">
                    {tText('Ouvrir')}
                  </button>
                </div>
                <ul className="space-y-2 max-h-40 overflow-y-auto">
                  {disputes?.length ? disputes.map((d) => (
                    <li key={d.id} className="rounded-lg border border-gray-100 dark:border-white/5 p-2.5 text-xs flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="font-medium text-gray-800 dark:text-gray-100">{d.subject}</p>
                        <p className="text-[10px] text-gray-400">{d.status} · {new Date(d.createdAt).toLocaleDateString()}</p>
                        {d.resolution && <p className="text-[11px] text-gray-500 mt-1">{d.resolution}</p>}
                      </div>
                      {d.status !== 'CLOSED' && (
                        <button type="button" onClick={() => closeDisputeMutation.mutate(d.id)} disabled={closeDisputeMutation.isPending}
                          className="shrink-0 rounded-md border px-2 py-1 text-[11px] hover:bg-gray-50 dark:hover:bg-white/5">
                          <Megaphone className="hidden" />{tText('Clôturer')}
                        </button>
                      )}
                    </li>
                  )) : <li className="text-xs text-gray-400">{tText('Aucun litige.')}</li>}
                </ul>
              </div>
            </>
          )}
        </section>
      </div>
    </main>
  );
}

function ActionBtn({ icon: Icon, label, tone, busy, onClick }: {
  icon: typeof Ban; label: string; tone: 'amber' | 'red' | 'emerald'; busy: boolean; onClick: () => void;
}) {
  const tones = {
    amber: 'border-amber-500/30 text-amber-700 dark:text-amber-300 hover:bg-amber-500/10',
    red: 'border-red-500/30 text-red-700 dark:text-red-300 hover:bg-red-500/10',
    emerald: 'border-emerald-500/30 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-500/10',
  } as const;
  return (
    <button type="button" onClick={onClick} disabled={busy}
      className={`inline-flex items-center gap-1.5 rounded-lg border px-3.5 py-2 text-sm font-medium disabled:opacity-50 ${tones[tone]}`}>
      <Icon className="w-4 h-4" /> {label}
    </button>
  );
}
