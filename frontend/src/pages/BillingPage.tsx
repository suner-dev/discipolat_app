import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2, CreditCard, ExternalLink, Loader2, Receipt } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';

interface SubscriptionView {
  planKey: string | null;
  billingCycle: string | null;
  status: string | null;
  cancelAtPeriodEnd: boolean | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  trialEndsAt: string | null;
  hasStripeCustomer: boolean;
}

const STATUS_LABELS: Record<string, string> = {
  TRIAL: 'Essai gratuit',
  ACTIVE: 'Actif',
  PAST_DUE: 'Paiement en souffrance',
  PAUSED: 'En pause',
  CANCELED: 'Résilié',
  EXPIRED: 'Expiré',
  PENDING_CHANGE: 'Changement en cours',
};

const fmtDate = (iso: string | null): string => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
};

/**
 * Facturation du tenant : abonnement courant (synchronisé par les webhooks
 * Stripe) + portail client Stripe (moyen de paiement, factures, résiliation).
 * Les CTA sont masqués tant que STRIPE_SECRET_KEY n'est pas configurée
 * (GET /public/billing/status).
 */
export default function BillingPage() {
  const [searchParams] = useSearchParams();
  const { isPlatformAdmin } = useAuth() as ReturnType<typeof useAuth> & { isPlatformAdmin?: boolean };
  const [subscription, setSubscription] = useState<SubscriptionView | null>(null);
  const [stripeEnabled, setStripeEnabled] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const checkoutSuccess = searchParams.get('checkout') === 'success';

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [statusRes, subRes] = await Promise.all([
        api.get<{ stripeEnabled: boolean }>('/public/billing/status'),
        api.get<{ subscription: SubscriptionView | null }>('/billing/stripe/subscription'),
      ]);
      setStripeEnabled(Boolean(statusRes.data.stripeEnabled));
      setSubscription(subRes.data.subscription ?? null);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const openPortal = async () => {
    try {
      setBusy(true);
      setError('');
      const { data } = await api.post<{ url: string }>('/billing/stripe/portal');
      window.location.href = data.url;
    } catch (err) {
      setError(getErrorMessage(err));
      setBusy(false);
    }
  };

  const status = subscription?.status ? (STATUS_LABELS[subscription.status] ?? subscription.status) : 'Aucun abonnement';

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-4 md:p-8">
      <header>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white font-display">Facturation</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Suivi de votre abonnement Discipolat et gestion du moyen de paiement.
        </p>
      </header>

      {checkoutSuccess && (
        <div className="flex items-start gap-2.5 rounded-xl border border-green-500/20 bg-green-500/10 p-4">
          <CheckCircle2 className="w-5 h-5 text-green-500 shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-green-700 dark:text-green-300">Souscription enregistrée.</p>
            <p className="text-xs text-green-600/80 dark:text-green-400/80">
              L'activation intervient dès confirmation du paiement par Stripe (quelques secondes).
            </p>
          </div>
        </div>
      )}

      {error && (
        <div role="alert" className="flex items-center gap-2 rounded-xl border border-red-500/20 bg-red-500/10 p-3.5 text-sm text-red-600 dark:text-red-300">
          <AlertCircle className="w-4 h-4 shrink-0" /> {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center gap-2 py-16 text-gray-500">
          <Loader2 className="w-5 h-5 animate-spin" /> Chargement…
        </div>
      ) : (
        <section className="rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-gray-900 p-6 shadow-sm">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <div className="flex items-center gap-3">
              <div className="inline-flex h-11 w-11 items-center justify-center rounded-xl bg-primary-500/10 border border-primary-500/20">
                <Receipt className="w-5 h-5 text-primary-500" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Plan actuel</p>
                <p className="text-lg font-bold text-gray-900 dark:text-white">
                  {subscription?.planKey ?? 'DISCOVERY'}
                  <span className="ml-2 rounded-full bg-gray-100 dark:bg-white/10 px-2.5 py-0.5 text-xs font-medium text-gray-600 dark:text-gray-300">
                    {status}
                  </span>
                </p>
              </div>
            </div>
            {subscription?.cancelAtPeriodEnd && (
              <p className="text-xs font-medium text-amber-600 dark:text-amber-400">
                Résiliation programmée en fin de période ({fmtDate(subscription.currentPeriodEnd)})
              </p>
            )}
          </div>

          <dl className="mt-6 grid grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="text-gray-500 dark:text-gray-400">Cycle de facturation</dt>
              <dd className="mt-0.5 font-medium text-gray-900 dark:text-white">
                {subscription?.billingCycle === 'yearly' ? 'Annuel' : subscription?.billingCycle === 'monthly' ? 'Mensuel' : '—'}
              </dd>
            </div>
            <div>
              <dt className="text-gray-500 dark:text-gray-400">Fin de période en cours</dt>
              <dd className="mt-0.5 font-medium text-gray-900 dark:text-white">{fmtDate(subscription?.currentPeriodEnd ?? null)}</dd>
            </div>
            {subscription?.trialEndsAt && (
              <div>
                <dt className="text-gray-500 dark:text-gray-400">Fin de l'essai</dt>
                <dd className="mt-0.5 font-medium text-gray-900 dark:text-white">{fmtDate(subscription.trialEndsAt)}</dd>
              </div>
            )}
          </dl>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            {stripeEnabled && subscription?.hasStripeCustomer && (
              <button
                onClick={() => void openPortal()}
                disabled={busy}
                className="inline-flex items-center gap-2 rounded-xl bg-primary-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-primary-500 disabled:opacity-50 transition-colors shadow-lg shadow-primary-500/25"
              >
                {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : <CreditCard className="w-4 h-4" />}
                Gérer le paiement (portail Stripe)
                <ExternalLink className="w-3.5 h-3.5 opacity-70" />
              </button>
            )}
            <Link
              to="/pricing"
              className="inline-flex items-center gap-2 rounded-xl border border-gray-200 dark:border-white/10 px-4 py-2.5 text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-white/5 transition-colors"
            >
              Changer de plan
            </Link>
          </div>

          {!stripeEnabled && !isPlatformAdmin && (
            <p className="mt-4 text-xs text-gray-400 dark:text-gray-500">
              Le paiement par carte (Stripe) sera activé très prochainement — votre plan actuel reste valable.
            </p>
          )}
        </section>
      )}
    </div>
  );
}
