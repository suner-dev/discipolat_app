/**
 * Console de gouvernance — SPEC_ORGANISATION_DENOMINATION_V2 §7.2 / T-W3.
 *
 * <p><b>F3 — plus de `window.prompt`.</b> Motif de bannissement, message
 * d'avertissement, objet de litige et résolution passaient par des boîtes de
 * dialogue système : non testable, non stylable, et surtout
 * <b>non contraignables</b> — un bannissement partait sur un clic avec un
 * motif vide. Le backend refuse désormais un motif vide (F11) ; l'IHM doit
 * donc l'exiger avant d'envoyer, sinon l'utilisateur reçoit un 400 opaque.
 *
 * <p><b>F27 — `plan` affiché.</b> La colonne affichait `{t.plan}` alors que la
 * réponse ne le contenait pas : l'écran rendait « slug · undefined ». Le plan
 * est désormais renvoyé par le backend.
 *
 * <p><b>F27 — pagination.</b> La liste était non paginée : elle chargeait
 * toutes les églises d'un coup. La requête accepte maintenant `page`/`size`.
 */
import { useCallback, useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertTriangle, Ban, CheckCircle2, Gavel, Loader2, Megaphone, RefreshCw, ShieldOff, X,
} from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { tText } from '@/i18n';

type TenantRow = { id: string; name: string; slug: string; status: string; plan: string };
type TenantPage = { content: TenantRow[]; total: number; page: number; size: number; totalPages: number };
type Warning = { id: string; message: string; severity: string; createdAt: string };
type Dispute = {
  id: string; subject: string; description?: string;
  status: string; resolution?: string; createdAt: string;
};

type StatusAction = 'block' | 'unblock' | 'ban' | 'unban';

const STATUS_STYLES: Record<string, string> = {
  ACTIVE: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20',
  SUSPENDED: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20',
  CANCELLED: 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/20',
  PENDING_SETUP: 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-500/20',
};

const SEVERITIES = ['INFO', 'FORMAL', 'FINAL'] as const;

export default function PlatformGovernancePage() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  // Modales in-app (F3)
  const [statusModal, setStatusModal] = useState<StatusAction | null>(null);
  const [statusReason, setStatusReason] = useState('');
  const [warningModal, setWarningModal] = useState(false);
  const [warningText, setWarningText] = useState('');
  const [warningSeverity, setWarningSeverity] = useState<(typeof SEVERITIES)[number]>('INFO');
  const [disputeModal, setDisputeModal] = useState(false);
  const [disputeSubject, setDisputeSubject] = useState('');
  const [disputeDescription, setDisputeDescription] = useState('');
  const [resolutionModal, setResolutionModal] = useState<string | null>(null);
  const [resolutionText, setResolutionText] = useState('');

  const { data, isLoading } = useQuery<TenantPage>({
    queryKey: ['platform', 'tenants', search, statusFilter, page],
    queryFn: async () => (await api.get('/platform/tenants', {
      params: { search: search || undefined, status: statusFilter || undefined, page, size: 25 },
    })).data,
  });

  const tenants = data?.content ?? [];
  const tenant = tenants.find((t) => t.id === selectedId) ?? null;

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

  const closeModals = () => {
    setStatusModal(null); setStatusReason('');
    setWarningModal(false); setWarningText(''); setWarningSeverity('INFO');
    setDisputeModal(false); setDisputeSubject(''); setDisputeDescription('');
    setResolutionModal(null); setResolutionText('');
  };

  const statusMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: StatusAction }) => {
      // Le backend REFUSE un motif vide (F11 / D18) : on ne part pas si il est
      // absent, pour ne pas exposer l'utilisateur à un 400 incompréhensible.
      if (!statusReason.trim()) {
        throw new Error(tText('Le motif est obligatoire : il est conservé dans l’historique.'));
      }
      return api.post(`/platform/tenants/${id}/${action}`, { reason: statusReason.trim() });
    },
    onSuccess: (_d, vars) => {
      const labels: Record<StatusAction, string> = {
        block: 'bloquée', unblock: 'réactivée', ban: 'bannie', unban: 'réintégrée',
      };
      setNotice(tText(`Église ${labels[vars.action]}. Le motif est tracé.`));
      setError('');
      closeModals();
      refresh();
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const warnMutation = useMutation({
    mutationFn: () => {
      if (!warningText.trim()) throw new Error(tText('Le message est obligatoire.'));
      return api.post(`/platform/tenants/${selectedId}/warnings`, {
        message: warningText.trim(),
        severity: warningSeverity,
      });
    },
    onSuccess: () => {
      setNotice(tText('Avertissement émis.'));
      setError('');
      closeModals();
      queryClient.invalidateQueries({ queryKey: ['platform', 'tenants', selectedId, 'warnings'] });
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const disputeMutation = useMutation({
    mutationFn: () => {
      if (!disputeSubject.trim()) throw new Error(tText('L’objet du litige est obligatoire.'));
      return api.post(`/platform/tenants/${selectedId}/disputes`, {
        subject: disputeSubject.trim(),
        description: disputeDescription.trim() || undefined,
      });
    },
    onSuccess: () => {
      setNotice(tText('Litige ouvert.'));
      setError('');
      closeModals();
      queryClient.invalidateQueries({ queryKey: ['platform', 'tenants', selectedId, 'disputes'] });
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  const closeDisputeMutation = useMutation({
    mutationFn: () => {
      if (!resolutionModal) return Promise.resolve(null);
      if (!resolutionText.trim()) throw new Error(tText('Indiquez la résolution apportée.'));
      return api.patch(`/platform/tenants/disputes/${resolutionModal}`, {
        status: 'CLOSED',
        resolution: resolutionText.trim(),
      });
    },
    onSuccess: () => {
      setNotice(tText('Litige clôturé.'));
      setError('');
      closeModals();
      queryClient.invalidateQueries({ queryKey: ['platform', 'tenants', selectedId, 'disputes'] });
    },
    onError: (err) => setError(getErrorMessage(err)),
  });

  useEffect(() => { setPage(0); }, [search, statusFilter]);

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-7 w-7 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <main className="p-6 space-y-6 max-w-6xl mx-auto">
      <header className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display">
            {tText('Gouvernance des églises')}
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {tText('Blocages, bannissements, avertissements et litiges — chaque action est motivée et tracée.')}
          </p>
        </div>
        <button
          type="button"
          onClick={refresh}
          className="inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm hover:bg-gray-50 dark:hover:bg-white/5"
        >
          <RefreshCw className="h-4 w-4" /> {tText('Actualiser')}
        </button>
      </header>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/20">
          {error}
        </div>
      )}
      {notice && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/20">
          {notice}
        </div>
      )}

      <div className="grid lg:grid-cols-2 gap-6">
        <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 overflow-hidden">
          <header className="px-5 py-3 border-b font-semibold text-sm text-gray-800 dark:text-gray-100 flex flex-col gap-3">
            <span>{tText('Églises de la plateforme')} ({data?.total ?? 0})</span>
            <div className="flex flex-wrap gap-2">
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder={tText('Rechercher')}
                className="flex-1 min-w-[140px] rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-1.5 text-sm text-gray-900 dark:text-gray-100"
              />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-2 py-1.5 text-sm text-gray-900 dark:text-gray-100"
              >
                <option value="">{tText('Tous les statuts')}</option>
                {Object.keys(STATUS_STYLES).map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
          </header>
          <div className="divide-y divide-gray-100 dark:divide-white/5 max-h-[60vh] overflow-y-auto">
            {tenants.length ? tenants.map((t) => (
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
            )) : (
              <p className="px-5 py-8 text-center text-sm text-gray-400">{tText('Aucune église ne correspond.')}</p>
            )}
          </div>
          {(data?.totalPages ?? 0) > 1 && (
            <footer className="px-5 py-3 border-t flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-md border px-2.5 py-1 disabled:opacity-40"
              >
                {tText('Précédent')}
              </button>
              <span>{tText('Page')} {page + 1} / {data?.totalPages}</span>
              <button
                type="button"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= (data?.totalPages ?? 1)}
                className="rounded-md border px-2.5 py-1 disabled:opacity-40"
              >
                {tText('Suivant')}
              </button>
            </footer>
          )}
        </section>

        <section className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 p-5 space-y-5">
          {!tenant ? (
            <p className="text-sm text-gray-400 py-10 text-center">{tText('Sélectionnez une église pour la gouverner.')}</p>
          ) : (
            <>
              <div>
                <h2 className="font-semibold text-gray-900 dark:text-white">{tenant.name}</h2>
                <p className="text-xs font-mono text-gray-400">{tenant.slug} — {tenant.status} — {tenant.plan}</p>
              </div>

              <div className="flex flex-wrap gap-2">
                {tenant.status !== 'SUSPENDED' && tenant.status !== 'CANCELLED' && (
                  <ActionBtn icon={ShieldOff} tone="amber" label={tText('Bloquer')} busy={statusMutation.isPending}
                    onClick={() => { setStatusReason(''); setStatusModal('block'); }} />
                )}
                {tenant.status === 'SUSPENDED' && (
                  <ActionBtn icon={CheckCircle2} tone="emerald" label={tText('Débloquer')} busy={statusMutation.isPending}
                    onClick={() => { setStatusReason(''); setStatusModal('unblock'); }} />
                )}
                {tenant.status !== 'CANCELLED' && (
                  <ActionBtn icon={Ban} tone="red" label={tText('Bannir')} busy={statusMutation.isPending}
                    onClick={() => { setStatusReason(''); setStatusModal('ban'); }} />
                )}
                {tenant.status === 'CANCELLED' && (
                  <ActionBtn icon={CheckCircle2} tone="emerald" label={tText('Réintégrer')} busy={statusMutation.isPending}
                    onClick={() => { setStatusReason(''); setStatusModal('unban'); }} />
                )}
              </div>

              <div className="border-t pt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-100 flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-amber-500" /> {tText('Avertissements')}
                  </h3>
                  <button type="button" onClick={() => { setWarningText(''); setWarningModal(true); }}
                    className="rounded-lg border border-amber-500/30 px-3 py-1.5 text-xs font-medium text-amber-700 dark:text-amber-300 hover:bg-amber-500/10">
                    {tText('Émettre')}
                  </button>
                </div>
                <ul className="space-y-2 max-h-40 overflow-y-auto">
                  {warnings?.length ? warnings.map((w) => (
                    <li key={w.id} className="rounded-lg border border-gray-100 dark:border-white/5 p-2.5 text-xs">
                      <span className="font-semibold mr-2">{w.severity}</span>
                      <span className="text-gray-600 dark:text-gray-300">{w.message}</span>
                      <span className="block text-[10px] text-gray-400 mt-1">
                        {new Date(w.createdAt).toLocaleString()}
                      </span>
                    </li>
                  )) : <li className="text-xs text-gray-400">{tText('Aucun avertissement.')}</li>}
                </ul>
              </div>

              <div className="border-t pt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-100 flex items-center gap-2">
                    <Gavel className="w-4 h-4 text-violet-500" /> {tText('Litiges')}
                  </h3>
                  <button type="button" onClick={() => { setDisputeSubject(''); setDisputeDescription(''); setDisputeModal(true); }}
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
                        <button
                          type="button"
                          onClick={() => { setResolutionModal(d.id); setResolutionText(d.resolution ?? ''); }}
                          className="shrink-0 rounded-md border px-2 py-1 text-[11px] hover:bg-gray-50 dark:hover:bg-white/5"
                        >
                          {tText('Clôturer')}
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

      {/* ---- Modales in-app (F3 : zéro window.prompt) ---- */}
      {statusModal && tenant && (
        <Modal
          title={statusModal === 'ban' ? tText('Bannir cette église')
            : statusModal === 'block' ? tText('Bloquer cette église')
              : statusModal === 'unban' ? tText('Réintégrer cette église') : tText('Débloquer cette église')}
          onClose={closeModals}
          footer={
            <button
              type="button"
              onClick={() => statusMutation.mutate({ id: tenant.id, action: statusModal })}
              disabled={statusMutation.isPending || !statusReason.trim()}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
            >
              {statusMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Confirmer')}
            </button>
          }
        >
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {tText(
              statusModal === 'ban'
                ? 'Un bannissement est quasi irréversible : l’église perd son accès. Le motif est conservé.'
                : 'Le motif est conservé dans l’historique de gouvernance et dans le journal d’audit.',
            )}
          </p>
          <label className="block text-sm">
            <span className="text-xs font-medium text-gray-600 dark:text-gray-300">
              {tText('Motif (obligatoire)')}
            </span>
            <textarea
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
              rows={3}
              className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
            />
          </label>
        </Modal>
      )}

      {warningModal && (
        <Modal
          title={tText('Émettre un avertissement')}
          onClose={closeModals}
          footer={
            <button
              type="button"
              onClick={() => warnMutation.mutate()}
              disabled={warnMutation.isPending || !warningText.trim()}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
            >
              {warnMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Émettre')}
            </button>
          }
        >
          <label className="block text-sm">
            <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{tText('Message')}</span>
            <textarea
              value={warningText}
              onChange={(e) => setWarningText(e.target.value)}
              rows={3}
              className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
            />
          </label>
          <fieldset className="mt-3">
            <legend className="text-xs font-medium text-gray-600 dark:text-gray-300">{tText('Gravité')}</legend>
            <div className="flex gap-3 mt-1">
              {SEVERITIES.map((s) => (
                <label key={s} className="flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-300">
                  <input type="radio" name="severity" checked={warningSeverity === s}
                    onChange={() => setWarningSeverity(s)} className="h-4 w-4" />
                  {s}
                </label>
              ))}
            </div>
          </fieldset>
        </Modal>
      )}

      {disputeModal && (
        <Modal
          title={tText('Ouvrir un litige')}
          onClose={closeModals}
          footer={
            <button
              type="button"
              onClick={() => disputeMutation.mutate()}
              disabled={disputeMutation.isPending || !disputeSubject.trim()}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
            >
              {disputeMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Ouvrir')}
            </button>
          }
        >
          <label className="block text-sm">
            <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{tText('Objet (obligatoire)')}</span>
            <input
              value={disputeSubject}
              onChange={(e) => setDisputeSubject(e.target.value)}
              className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
            />
          </label>
          <label className="block text-sm mt-3">
            <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{tText('Description')}</span>
            <textarea
              value={disputeDescription}
              onChange={(e) => setDisputeDescription(e.target.value)}
              rows={3}
              className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
            />
          </label>
        </Modal>
      )}

      {resolutionModal && (
        <Modal
          title={tText('Clôturer le litige')}
          onClose={closeModals}
          footer={
            <button
              type="button"
              onClick={() => closeDisputeMutation.mutate()}
              disabled={closeDisputeMutation.isPending || !resolutionText.trim()}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50"
            >
              {closeDisputeMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : tText('Clôturer')}
            </button>
          }
        >
          <label className="block text-sm">
            <span className="text-xs font-medium text-gray-600 dark:text-gray-300">
              {tText('Résolution apportée (obligatoire)')}
            </span>
            <textarea
              value={resolutionText}
              onChange={(e) => setResolutionText(e.target.value)}
              rows={3}
              className="mt-1 w-full rounded-lg border border-gray-300 dark:border-white/15 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100"
            />
          </label>
        </Modal>
      )}
    </main>
  );
}

function Modal({
  title, children, onClose, footer,
}: {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
  footer: React.ReactNode;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-white/10 p-5 space-y-4 shadow-xl">
        <div className="flex items-start justify-between gap-3">
          <h2 className="font-semibold text-gray-900 dark:text-white">{title}</h2>
          <button type="button" onClick={onClose} aria-label={tText('Fermer')} className="text-gray-400 hover:text-gray-600">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="space-y-3">{children}</div>
        <div className="flex justify-end gap-2 pt-1">{footer}</div>
      </div>
    </div>
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
    <button
      type="button"
      onClick={onClick}
      disabled={busy}
      className={`inline-flex items-center gap-1.5 rounded-lg border px-3.5 py-2 text-sm font-medium disabled:opacity-50 ${tones[tone]}`}
    >
      <Icon className="w-4 h-4" /> {label}
    </button>
  );
}
