// B6 — Page « Abonnement & quotas » du tenant (constat F4).
//
// Consomme EXACTEMENT le contrat réel de `SubscriptionController`
// (`/api/v1/admin/subscription`, protégé classe par `@authz.isTenantAdmin()`) :
//   GET  /current          -> { hasSubscription, subscription, quotas }
//   GET  /plans            -> [ { key, name, description, priceMonthly, priceYearly,
//                                 currency, limits, features, isActive } ]
//   GET  /usage            -> même forme que /current (réutilisé par le backend)
//   POST /change-plan      -> { planKey }     (downgrade refusé : 400 QUOTA_EXCEEDS_DOWNGRADE_LIMIT)
//   POST /cancel           -> { atPeriodEnd } -> { message }
//   POST /reactivate       -> { message }
//
// Aucune donnée mockée, aucun champ inventé. Les 5 états (§5.0.2) sont traités :
// chargement, vide (aucun abonnement), erreur (dont 403), succès, et les issues
// métier (rétrogradation refusée, fin de période, annulation programmée).
import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import toast from 'react-hot-toast';
import {
  Wallet, RefreshCw, AlertTriangle, CheckCircle2, Loader2,
  CalendarClock, Ban, RotateCcw, ShieldCheck, CircleDollarSign,
} from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { getI18nLocale, tText } from '@/i18n';
import { QuotaUsageCards } from '@/components/admin/QuotaUsageCards';
import { normalizeQuotaUsage } from '@/types/quota';
import { EmptyState, SkeletonDashboard, VisuallyHidden, ConfirmDialog } from '@/components/ui/UXComponents';

// ── Formes du contrat (typées strictement, alignées champ par champ sur le backend) ──
interface SubscriptionPlanInfo {
  name?: string;
  priceMonthly?: number;
  priceYearly?: number;
}

interface SubscriptionInfo {
  id: string;
  planKey: string;
  status: string;
  billingCycle: string;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd?: boolean;
  canceledAt?: string | null;
  trialEndsAt?: string | null;
  plan?: SubscriptionPlanInfo | null;
}

interface CurrentSubscriptionResponse {
  hasSubscription: boolean;
  subscription?: SubscriptionInfo;
  quotas?: unknown;
}

interface CatalogPlan {
  key: string;
  name: string;
  description?: string;
  priceMonthly?: number;
  priceYearly?: number;
  currency?: string;
  isActive?: boolean;
}

/**
 * Libellés connus des statuts d'abonnement (`SubscriptionStatus`).
 * Un statut inconnu est affiché BRUT, jamais ramené à « Actif » : mentir sur un
 * état de facturation est plus grave qu'afficher une valeur technique.
 */
const STATUS_TONES: Record<string, { label: string; className: string }> = {
  ACTIVE: { label: 'Actif', className: 'badge-success' },
  TRIALING: { label: "Période d'essai", className: 'badge-info' },
  PAST_DUE: { label: 'Paiement en souffrance', className: 'badge-warning' },
  CANCELLED: { label: 'Annulé', className: 'badge-error' },
  INCOMPLETE: { label: 'Incomplet', className: 'badge-gray' },
};

const statusInfo = (status: string): { label: string; className: string } => {
  const known = STATUS_TONES[status];
  return known ?? { label: status, className: 'badge-gray' };
};

/** Prix : nombre → Intl currency. devise absente/illisible → aucune valeur forcée, « — ». */
const formatPrice = (value: unknown, currency: string | undefined): string => {
  const amount = typeof value === 'number' && Number.isFinite(value) ? value : null;
  if (amount === null) return '—';
  const code = typeof currency === 'string' && /^[A-Z]{3}$/.test(currency) ? currency : undefined;
  try {
    return new Intl.NumberFormat(getI18nLocale(), code ? { style: 'currency', currency: code } : undefined)
      .format(amount);
  } catch {
    // Devise inconnue de Intl : on affiche le nombre sans symbole plutôt qu'un symbole faux.
    return new Intl.NumberFormat(getI18nLocale()).format(amount);
  }
};

const formatDate = (value: unknown): string => {
  if (typeof value !== 'string' || value.trim() === '') return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString(getI18nLocale(), { day: 'numeric', month: 'long', year: 'numeric' });
};

/**
 * Normalise le catalogue de plans : le backend peut renvoyer un tableau OU un
 * `PageResponse { content }`. On accepte les deux sans `as` massif, et on écarte
 * les lignes sans clé exploitable.
 */
const readPlans = (payload: unknown): CatalogPlan[] => {
  const rows = Array.isArray(payload)
    ? payload
    : Array.isArray((payload as { content?: unknown } | null)?.content)
      ? (payload as { content: unknown[] }).content
      : [];
  return rows.reduce<CatalogPlan[]>((acc, row) => {
    const record = (row ?? {}) as Record<string, unknown>;
    if (typeof record.key !== 'string' || record.key.trim() === '') return acc;
    acc.push({
      key: record.key,
      name: typeof record.name === 'string' && record.name.trim() !== '' ? record.name : record.key,
      description: typeof record.description === 'string' ? record.description : undefined,
      priceMonthly: typeof record.priceMonthly === 'number' ? record.priceMonthly : undefined,
      priceYearly: typeof record.priceYearly === 'number' ? record.priceYearly : undefined,
      currency: typeof record.currency === 'string' ? record.currency : undefined,
      isActive: typeof record.isActive === 'boolean' ? record.isActive : undefined,
    });
    return acc;
  }, []);
};

export default function TenantAdminSubscriptionPage() {
  const queryClient = useQueryClient();
  const [pendingChange, setPendingChange] = useState<CatalogPlan | null>(null);
  const [cancelOpen, setCancelOpen] = useState(false);

  const {
    data: current,
    isLoading,
    isError: currentFailed,
    error: currentError,
    refetch,
  } = useQuery<CurrentSubscriptionResponse>({
    queryKey: ['tenant', 'subscription', 'current'],
    queryFn: async () => {
      const res = await api.get('/admin/subscription/current');
      return res.data as CurrentSubscriptionResponse;
    },
    retry: false,
  });

  const { data: plansPayload } = useQuery({
    queryKey: ['tenant', 'subscription', 'plans'],
    queryFn: async () => {
      const res = await api.get('/admin/subscription/plans');
      return res.data as unknown;
    },
    retry: false,
  });

  const plans = useMemo(() => readPlans(plansPayload), [plansPayload]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['tenant', 'subscription'] });
  };

  /**
   * 403 = non-admin du tenant ou session expirée, pas un bug front. On le nomme
   * pour ne pas envoyer l'utilisateur chercher un défaut inexistant.
   */
  const describeError = (error: unknown): string => {
    const status = axios.isAxiosError(error) ? error.response?.status : undefined;
    if (status === 403) {
      return tText('Accès refusé : cette page est réservée aux administrateurs de l’église.');
    }
    return getErrorMessage(error);
  };

  const changePlanMutation = useMutation({
    mutationFn: async (planKey: string) => {
      const res = await api.post('/admin/subscription/change-plan', { planKey });
      return res.data as { message?: string };
    },
    onSuccess: () => {
      invalidate();
      setPendingChange(null);
      // Le backend renvoie un message en français ; on affiche notre propre clé
      // i18n pour que le libellé soit traduit dans les 6 locales.
      toast.success(tText('Changement de plan enregistré'));
    },
    // Un refus de rétrogradation (QUOTA_EXCEEDS_DOWNGRADE_LIMIT) arrive en 400
    // avec le motif dans `detail` : on l'affiche tel quel, c'est une information
    // métier utile, pas une erreur technique à masquer.
    onError: (err: unknown) => toast.error(describeError(err)),
  });

  const cancelMutation = useMutation({
    mutationFn: async () => {
      // `atPeriodEnd: true` : on ne coupe jamais l'accès immédiatement sans le
      // demander explicitement — l'annulation prend effet en fin de période payée.
      const res = await api.post('/admin/subscription/cancel', { atPeriodEnd: true });
      return res.data as { message?: string };
    },
    onSuccess: () => {
      invalidate();
      setCancelOpen(false);
      toast.success(tText('Abonnement résilié en fin de période'));
    },
    onError: (err: unknown) => toast.error(describeError(err)),
  });

  const reactivateMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/admin/subscription/reactivate');
      return res.data as { message?: string };
    },
    onSuccess: () => {
      invalidate();
      toast.success(tText('Abonnement réactivé'));
    },
    onError: (err: unknown) => toast.error(describeError(err)),
  });

  // ── État chargement (§5.0.2) : squelette + annonce pour lecteur d'écran ──
  if (isLoading) {
    return (
      <div className="page-container max-w-5xl" role="status" aria-busy="true" aria-live="polite">
        <VisuallyHidden>{tText('Chargement de l’abonnement…')}</VisuallyHidden>
        <SkeletonDashboard />
      </div>
    );
  }

  // ── État erreur : message actionnable + retry, jamais un écran blanc ──
  if (currentFailed) {
    return (
      <div className="page-container max-w-5xl">
        <EmptyState
          icon={<AlertTriangle className="w-8 h-8 text-red-400" />}
          title={tText('Impossible de charger l’abonnement')}
          description={describeError(currentError)}
          action={{ label: tText('Réessayer'), onClick: () => { void refetch(); } }}
        />
      </div>
    );
  }

  const subscription = current?.subscription;
  const metrics = current?.quotas ? normalizeQuotaUsage(current.quotas).metrics : [];

  return (
    <div className="page-container max-w-5xl">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Wallet className="w-5 h-5 text-primary-500" />
            {tText('Abonnement & quotas')}
          </h1>
          <p className="page-subtitle">
            {tText('Consultez le plan de votre église, gérez votre cycle de facturation et suivez votre consommation.')}
          </p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => { void refetch(); }} className="btn-ghost btn-sm">
            <RefreshCw className="w-4 h-4" /> {tText('Actualiser')}
          </button>
        </div>
      </div>

      {/* ── État vide : aucun abonnement actif. Le catalogue reste en référence,
            aucune action forcée (on n'invente pas de parcours de souscription). ── */}
      {!current?.hasSubscription || !subscription ? (
        <EmptyState
          icon={<CircleDollarSign className="w-8 h-8 text-gray-400" />}
          title={tText('Aucun abonnement actif')}
          description={tText("Cette église n'a pas encore d'abonnement enregistré. Contactez la plateforme pour l'activer.")}
        />
      ) : (
        <>
          {/* Bandeau d'annulation programmée (issue métier, lisible avant tout) */}
          {subscription.cancelAtPeriodEnd && (
            <div
              role="status"
              className="mb-4 flex items-start gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-xs text-amber-700 dark:text-amber-300"
            >
              <CalendarClock className="w-4 h-4 shrink-0 mt-0.5" />
              <span>
                {tText('Cet abonnement prendra fin le')} {formatDate(subscription.currentPeriodEnd)}.
              </span>
            </div>
          )}

          {/* Carte : abonnement courant */}
          <div className="glass-card p-5 mb-6 animate-slide-up">
            <div className="flex items-start justify-between gap-4 flex-wrap">
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-lg font-bold text-gray-900 dark:text-gray-100">
                    {subscription.plan?.name || subscription.planKey}
                  </span>
                  <span className={`badge text-[10px] ${statusInfo(subscription.status).className}`}>
                    {statusInfo(subscription.status).label}
                  </span>
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  {tText('Cycle')} : {subscription.billingCycle === 'yearly' ? tText('Annuel') : subscription.billingCycle === 'monthly' ? tText('Mensuel') : subscription.billingCycle}
                </p>
              </div>
              <div className="text-end">
                <p className="text-[10px] uppercase font-semibold text-gray-400">{tText('Prochaine échéance')}</p>
                <p className="text-sm text-gray-900 dark:text-gray-100">{formatDate(subscription.currentPeriodEnd)}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mt-4">
              <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                <p className="text-[10px] uppercase font-semibold text-gray-400">{tText('Plan')}</p>
                <p className="text-sm font-mono text-gray-900 dark:text-gray-100">{subscription.planKey}</p>
              </div>
              <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                <p className="text-[10px] uppercase font-semibold text-gray-400">{tText('Tarif mensuel')}</p>
                <p className="text-sm text-gray-900 dark:text-gray-100">{formatPrice(subscription.plan?.priceMonthly, undefined)}</p>
              </div>
              <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                <p className="text-[10px] uppercase font-semibold text-gray-400">{tText('Début de période')}</p>
                <p className="text-sm text-gray-900 dark:text-gray-100">{formatDate(subscription.currentPeriodStart)}</p>
              </div>
            </div>

            {/* Actions d'état : réactiver si résilié, sinon résilier en fin de période. */}
            <div className="flex items-center gap-2 mt-5 flex-wrap">
              {subscription.status === 'CANCELLED' || subscription.cancelAtPeriodEnd ? (
                <button
                  type="button"
                  className="btn-secondary btn-sm"
                  disabled={reactivateMutation.isPending}
                  onClick={() => reactivateMutation.mutate()}
                >
                  {reactivateMutation.isPending
                    ? <Loader2 className="w-4 h-4 animate-spin" />
                    : <RotateCcw className="w-4 h-4" />}
                  {tText('Réactiver l’abonnement')}
                </button>
              ) : (
                <button
                  type="button"
                  className="btn-ghost btn-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                  disabled={cancelMutation.isPending}
                  onClick={() => setCancelOpen(true)}
                >
                  <Ban className="w-4 h-4" />
                  {tText('Résilier en fin de période')}
                </button>
              )}
            </div>
          </div>

          {/* Quotas réels : réutilisation de QuotaUsageCards (§5.0.1 — pas de doublon) */}
          <div className="mb-6">
            <QuotaUsageCards metrics={metrics} title={tText('Consommation et limites')} />
          </div>

          {/* Catalogue des plans publiés : changement de plan */}
          <div className="glass-card p-5 animate-slide-up">
            <div className="flex items-center gap-2 mb-1">
              <ShieldCheck className="w-4 h-4 text-primary-500" />
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300">{tText('Changer de plan')}</h2>
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
              {tText('Un changement de plan prend effet à la prochaine période. Une rétrogradation est refusée si votre consommation dépasse la nouvelle limite.')}
            </p>

            {plans.length === 0 ? (
              <div className="flex items-start gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-xs text-amber-700 dark:text-amber-300">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{tText('Catalogue des plans indisponible pour le moment.')}</span>
              </div>
            ) : (
              <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3">
                {plans.map((plan) => {
                  const isCurrent = plan.key === subscription.planKey;
                  return (
                    <div
                      key={plan.key}
                      className={`rounded-xl border p-4 flex flex-col transition-all ${
                        isCurrent
                          ? 'border-primary-500 bg-primary-50/50 dark:bg-primary-900/20 ring-2 ring-primary-500/20'
                          : 'border-gray-200 dark:border-gray-700'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{plan.name}</span>
                        {isCurrent && (
                          <span className="badge text-[10px] badge-primary">
                            <CheckCircle2 className="w-3 h-3" /> {tText('Actuel')}
                          </span>
                        )}
                      </div>
                      {plan.description && (
                        <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">{plan.description}</p>
                      )}
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">
                        <span className="font-semibold text-gray-900 dark:text-gray-100">
                          {formatPrice(plan.priceMonthly, plan.currency)}
                        </span> / {tText('mois')}
                      </p>
                      <p className="text-[11px] text-gray-400">
                        {formatPrice(plan.priceYearly, plan.currency)} / {tText('an')}
                      </p>
                      {!isCurrent && (
                        <button
                          type="button"
                          className="btn-primary btn-sm mt-3 justify-center"
                          disabled={changePlanMutation.isPending}
                          onClick={() => setPendingChange(plan)}
                        >
                          {tText('Choisir ce plan')}
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}

      {/* Confirmation de changement de plan */}
      <ConfirmDialog
        open={pendingChange !== null}
        title={tText('Changer de plan')}
        message={`${tText('Passer au plan')} « ${pendingChange?.name ?? ''} » ? ${tText('Le changement sera appliqué à la prochaine période.')}`}
        confirmLabel={tText('Confirmer le changement')}
        cancelLabel={tText('Annuler')}
        variant="info"
        onConfirm={() => { if (pendingChange) changePlanMutation.mutate(pendingChange.key); }}
        onCancel={() => setPendingChange(null)}
      />

      {/* Confirmation de résiliation */}
      <ConfirmDialog
        open={cancelOpen}
        title={tText('Résilier l’abonnement')}
        message={`${tText('L’accès reste actif jusqu’à la fin de la période payée')} (${formatDate(subscription?.currentPeriodEnd)}). ${tText('Vous pourrez réactiver à tout moment.')}`}
        confirmLabel={tText('Confirmer la résiliation')}
        cancelLabel={tText('Annuler')}
        variant="warning"
        onConfirm={() => cancelMutation.mutate()}
        onCancel={() => setCancelOpen(false)}
      />
    </div>
  );
}
