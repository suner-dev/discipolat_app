// B4 — Page d'administration des invitations d'un tenant (constat F3).
//
// Cette page ne parle plus à l'API : elle compose. Toute la donnée et toutes
// les mutations vivent dans `useInvitations` (contrat validé, clés de cache
// préfixées par tenant) ; le formulaire et la modale de lien sont des
// composants dédiés.
//
// États gérés explicitement (plan §5.0.2) : chargement (squelette), vide
// (EmptyState avec action), erreur (message actionnable + retry), succès, et
// les trois issues métier réelles : email non envoyé, changement d'église
// requis, invitation expirée.

import { useState } from 'react';
import { useTenant } from '@/contexts/TenantContext';
import { useI18n } from '@/i18n';
import EmptyState from '@/components/shared/EmptyState';
import { ConfirmDialog, SkeletonTable } from '@/components/ui/UXComponents';
import InvitationCreateDialog from '@/components/admin/InvitationCreateDialog';
import InvitationLinkDialog from '@/components/admin/InvitationLinkDialog';
import {
  useInvitations,
  useResendInvitation,
  useCancelInvitation,
  type CreateInvitationResult,
  type Invitation,
} from '@/hooks/useInvitations';
import { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';

const PAGE_SIZE = 20;

const STATUSES = ['PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'] as const;

/** Une invitation PENDING dont la date d'expiration est passée est EXPIRED
 *  pour l'utilisateur — le backend ne rebalise pas la ligne. */
const isExpired = (invitation: Invitation, now: number) =>
  invitation.status === 'PENDING' &&
  Boolean(invitation.expiresAt) &&
  new Date(invitation.expiresAt).getTime() < now;

const formatDate = (iso: string, locale: string) => {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(date);
};

const statusBadge = (status: string, expired: boolean) => {
  if (expired) return 'bg-gray-100 text-gray-700 dark:bg-white/10 dark:text-gray-300';
  if (status === 'PENDING') return 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200';
  if (status === 'ACCEPTED') return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200';
  return 'bg-gray-100 text-gray-700 dark:bg-white/10 dark:text-gray-300';
};

export default function TenantAdminInvitationsPage() {
  const { hasPermission } = useTenant();
  const { t, locale } = useI18n();

  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<string>('');
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [linkResult, setLinkResult] = useState<CreateInvitationResult | null>(null);
  const [toCancel, setToCancel] = useState<Invitation | null>(null);

  const canInvite = hasPermission('USER_INVITE');
  const canManage = hasPermission('USER_MANAGE');

  const query = useInvitations({ page, size: PAGE_SIZE, status: status || undefined, q: search });
  const resend = useResendInvitation();
  const cancel = useCancelInvitation();

  const now = Date.now();
  const rows = query.data?.content ?? [];
  const totalPages = query.data?.totalPages ?? 0;

  const onResend = async (invitation: Invitation) => {
    try {
      const result = await resend.mutateAsync(invitation.id);
      if (result.emailSent) {
        toast.success(t('invitations.resent'));
      } else {
        // SMTP absent : ne jamais afficher « renvoyée » comme si c'était parti.
        toast.error(t('invitations.emailNotSent'));
        setLinkResult({
          success: true,
          invitationLink: result.invitationLink,
          emailSent: false,
        });
      }
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  };

  const onCancel = async () => {
    if (!toCancel) return;
    try {
      await cancel.mutateAsync(toCancel.id);
      toast.success(t('invitations.cancelled'));
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setToCancel(null);
    }
  };

  if (!canInvite) {
    return (
      <div className="p-6">
        <EmptyState
          title={t('invitations.forbiddenTitle')}
          message={t('invitations.forbiddenDescription')}
        />
      </div>
    );
  }

  return (
    <div className="page-container">
      <header className="page-header flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{t('invitations.title')}</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">{t('invitations.subtitle')}</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm min-h-[44px]">
          {t('invitations.new')}
        </button>
      </header>

      {/* Filtres */}
      <div className="flex flex-wrap gap-3 mb-4">
        <div className="flex-1 min-w-[220px]">
          <label htmlFor="inv-search" className="sr-only">
            {t('invitations.search')}
          </label>
          <input
            id="inv-search"
            type="search"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            placeholder={t('invitations.search')}
            className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
          />
        </div>
        <div>
          <label htmlFor="inv-status" className="sr-only">
            {t('invitations.status.label')}
          </label>
          <select
            id="inv-status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
            className="rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]"
          >
            <option value="">{t('invitations.status.all')}</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {t(`invitations.status.${s.toLowerCase()}`)}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* États */}
      {query.isLoading ? (
        <SkeletonTable rows={5} cols={4} />
      ) : query.isError ? (
        <EmptyState
          title={t('invitations.errorTitle')}
          message={getErrorMessage(query.error)}
          action={{ label: t('invitations.retry'), onClick: () => void query.refetch() }}
        />
      ) : rows.length === 0 ? (
        <EmptyState
          title={t('invitations.emptyTitle')}
          message={t('invitations.emptyDescription')}
          action={{ label: t('invitations.new'), onClick: () => setShowCreate(true) }}
        />
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-white/10">
            <thead>
              <tr>
                <th scope="col" className="px-4 py-3 text-start text-xs font-medium uppercase text-gray-500">
                  {t('invitations.email')}
                </th>
                <th scope="col" className="px-4 py-3 text-start text-xs font-medium uppercase text-gray-500">
                  {t('invitations.role.label')}
                </th>
                <th scope="col" className="px-4 py-3 text-start text-xs font-medium uppercase text-gray-500">
                  {t('invitations.status.label')}
                </th>
                <th scope="col" className="px-4 py-3 text-start text-xs font-medium uppercase text-gray-500">
                  {t('invitations.expiresAt')}
                </th>
                <th scope="col" className="px-4 py-3 text-end text-xs font-medium uppercase text-gray-500">
                  {t('invitations.actions')}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-white/10">
              {rows.map((invitation) => {
                const expired = isExpired(invitation, now);
                return (
                  <tr key={invitation.id}>
                    <td className="px-4 py-3">
                      <span className="font-medium text-gray-900 dark:text-gray-100">{invitation.email}</span>
                      {invitation.organizationNodeName && (
                        <span className="block text-xs text-gray-500 dark:text-gray-400">
                          {invitation.organizationNodeName}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">{invitation.role}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 text-xs rounded-full ${statusBadge(invitation.status, expired)}`}>
                        {expired ? t('invitations.expired') : t(`invitations.status.${invitation.status.toLowerCase()}`)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                      {formatDate(invitation.expiresAt, locale)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        {invitation.status === 'PENDING' && !expired && canManage && (
                          <button
                            onClick={() => void onResend(invitation)}
                            disabled={resend.isPending}
                            className="btn-ghost btn-sm min-h-[44px]"
                          >
                            {t('invitations.resend')}
                          </button>
                        )}
                        {invitation.status === 'PENDING' && canManage && (
                          <button
                            onClick={() => setToCancel(invitation)}
                            className="btn-ghost btn-sm min-h-[44px] text-red-600 dark:text-red-400"
                          >
                            {t('invitations.cancelAction')}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {query.data && totalPages > 1 && (
        <nav className="flex items-center justify-end gap-3 mt-4" aria-label={t('invitations.pagination')}>
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="btn-ghost btn-sm min-h-[44px]"
          >
            {t('invitations.previous')}
          </button>
          <span className="text-sm text-gray-600 dark:text-gray-300">
            {t('invitations.page')} {query.data.page + 1} / {query.data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={page + 1 >= totalPages}
            className="btn-ghost btn-sm min-h-[44px]"
          >
            {t('invitations.next')}
          </button>
        </nav>
      )}

      {showCreate && (
        <InvitationCreateDialog
          onClose={() => setShowCreate(false)}
          onCreated={(result) => {
            setShowCreate(false);
            setLinkResult(result);
            toast.success(t('invitations.created'));
          }}
        />
      )}

      <InvitationLinkDialog result={linkResult} onClose={() => setLinkResult(null)} />

      <ConfirmDialog
        open={toCancel !== null}
        title={t('invitations.confirmCancelTitle')}
        message={t('invitations.confirmCancelMessage')}
        confirmLabel={t('invitations.cancelAction')}
        cancelLabel={t('common.cancel')}
        variant="danger"
        onConfirm={() => void onCancel()}
        onCancel={() => setToCancel(null)}
      />

    </div>
  );
}
