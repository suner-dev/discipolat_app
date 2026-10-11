import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AlertCircle, ArrowRight, Bot, Check, CreditCard, Loader2, Minus, Shield, Zap } from 'lucide-react';
import { tText, useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { usePricing } from '@/features/pricing/usePricing';
import { formatMinor } from '@/lib/money';
import type { BillingCycleName, PublicPlanView } from '@/features/pricing/types';
import { CURRENCY_LABEL, PLAN_BLURB } from '@/components/landing/pricingCopy';
import type { Locale } from '@/i18n';

/**
 * Page publique des tarifs — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T7.1.
 *
 * <p><b>Ce qui ne change pas</b> (rien de cassé, conformément à la règle du
 * projet) : la route publique, la gestion de {@code ?checkout=cancelled}, la
 * détection de la caisse Stripe via {@code /public/billing/status}, le CTA
 * réservé aux sessions authentifiées, et le repli visuel si le catalogue est
 * indisponible.</p>
 *
 * <p><b>Ce qui change</b> : la devise affichée et le cycle de facturation
 * deviennent des <b>choix du visiteur</b>, dans la stricte limite de ce que le
 * super-admin a autorisé. Aucun interrupteur n'est codé en dur ici — un
 * sélecteur absent signifie « non autorisé », pas « oublié ».</p>
 *
 * <p>Les montants affichés proviennent tous de {@code display.amountMinor},
 * déjà en unités mineures côté serveur. Le client ne fait aucune conversion.</p>
 */
export default function PricingPage() {
  const { t, locale } = useI18n();
  const { isAuthenticated } = useAuth();
  const [searchParams] = useSearchParams();
  const {
    config,
    plans,
    currency,
    cycle,
    currencies,
    loading,
    error,
    setCurrency,
    setCycle,
  } = usePricing();

  const [checkoutBusy, setCheckoutBusy] = useState<string | null>(null);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const cancelled = searchParams.get('checkout') === 'cancelled';

  // Le CTA Stripe n'existe que si la caisse est réellement activée côté serveur.
  const stripeEnabled = config?.stripeEnabled === true;

  const subscribeWithStripe = async (planKey: string) => {
    try {
      setCheckoutBusy(planKey);
      setCheckoutError(null);
      const { data } = await api.post<{ url: string }>('/billing/stripe/checkout', {
        planKey: planKey.toUpperCase(),
        billingCycle: cycle.toLowerCase(),
        currency,
      });
      if (data?.url) {
        window.location.href = data.url;
      } else {
        setCheckoutError(t('pricing.checkoutUnexpected'));
        setCheckoutBusy(null);
      }
    } catch (err) {
      setCheckoutError(getErrorMessage(err));
      setCheckoutBusy(null);
    }
  };

  if (error && plans.length === 0) {
    return (
      <CatalogUnavailable message={error} onRetry={() => window.location.reload()} />
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white dark:from-gray-950 dark:to-gray-900">
      <div className="py-16 px-4 text-center">
        {loading && plans.length === 0 && (
          <p role="status" className="mb-4 text-sm text-gray-500">
            {t('pricing.loading')}
          </p>
        )}
        {cancelled && (
          <div
            role="status"
            className="mx-auto mb-4 flex max-w-2xl items-center justify-center gap-2 rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800 dark:border-blue-500/30 dark:bg-blue-500/10 dark:text-blue-300"
          >
            {t('pricing.checkoutCancelled')}
          </div>
        )}
        {checkoutError && (
          <div
            role="alert"
            className="mx-auto mb-4 flex max-w-2xl items-center justify-center gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300"
          >
            <AlertCircle className="h-4 w-4 shrink-0" />
            {checkoutError}
          </div>
        )}
        <h1 className="text-4xl md:text-5xl font-bold text-gray-900 dark:text-white">{t('pricing.title')}</h1>
        <p className="mt-4 text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
          {t('pricing.subtitle')}
        </p>

        <PricingSelectors
          config={config}
          currency={currency}
          cycle={cycle}
          currencies={currencies}
          locale={locale}
          onCurrency={setCurrency}
          onCycle={setCycle}
        />
      </div>

      <div className="max-w-6xl mx-auto px-4 pb-20">
        {plans.length === 0 && !loading ? (
          <p className="text-center text-sm text-gray-500">{t('pricing.empty')}</p>
        ) : (
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {plans.map((plan) => (
              <PlanCard
                key={plan.key}
                plan={plan}
                cycle={cycle}
                locale={locale}
                stripeEnabled={stripeEnabled}
                isAuthenticated={isAuthenticated}
                busy={checkoutBusy}
                onCheckout={() => void subscribeWithStripe(plan.key)}
              />
            ))}
          </div>
        )}

        <Highlights />
        <Testimonials />
        <Faq />
      </div>
    </div>
  );
}

// ── Sélecteurs (devise + cycle) ─────────────────────────────────────────────

function PricingSelectors({
  config,
  currency,
  cycle,
  currencies,
  locale,
  onCurrency,
  onCycle,
}: {
  config: ReturnType<typeof usePricing>['config'];
  currency: string;
  cycle: BillingCycleName;
  currencies: ReturnType<typeof usePricing>['currencies'];
  locale: Locale;
  onCurrency: (code: string) => void;
  onCycle: (next: BillingCycleName) => void;
}) {
  const { t } = useI18n();

  // Un sélecteur ne s'affiche que s'il est autorisé ET s'il a quelque chose à
  // proposer. Un menu à une seule option est du bruit, pas une fonction.
  const showCurrency = config?.allowCurrencyChoice === true && currencies.length > 1;
  const showCycle =
    config?.allowCycleChoice === true &&
    (config?.allowMonthly !== false || config?.allowYearly !== false);

  if (!showCurrency && !showCycle) return null;

  return (
    <div className="mt-8 flex flex-wrap items-center justify-center gap-3" role="group" aria-label={t('pricing.selectors')}>
      {showCycle && (
        <div className="inline-flex rounded-full border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 p-1">
          {config?.allowMonthly !== false && (
            <CycleButton active={cycle === 'MONTHLY'} onClick={() => onCycle('MONTHLY')} label={t('pricing.cycle.monthly')} />
          )}
          {config?.allowYearly !== false && (
            <CycleButton active={cycle === 'YEARLY'} onClick={() => onCycle('YEARLY')} label={t('pricing.cycle.yearly')} />
          )}
        </div>
      )}
      {showCurrency && (
        <div className="inline-flex items-center gap-2 rounded-full border border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 px-3 py-2">
          <label htmlFor="pricing-currency" className="text-xs font-medium text-gray-500 dark:text-gray-400">
            {t('pricing.currency')}
          </label>
          <select
            id="pricing-currency"
            value={currency}
            onChange={(event) => onCurrency(event.target.value)}
            className="bg-transparent text-sm font-semibold text-gray-900 dark:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-violet-500 rounded"
          >
            {currencies.map((option) => (
              <option key={option.code} value={option.code}>
                {option.code} — {CURRENCY_LABEL[locale]?.[option.code] ?? option.name}
              </option>
            ))}
          </select>
        </div>
      )}
    </div>
  );
}

function CycleButton({
  active,
  onClick,
  label,
}: {
  active: boolean;
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
        active
          ? 'bg-violet-500 text-white shadow'
          : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
      }`}
    >
      {label}
    </button>
  );
}

// ── Carte de plan ───────────────────────────────────────────────────────────

function PlanCard({
  plan,
  cycle,
  locale,
  stripeEnabled,
  isAuthenticated,
  busy,
  onCheckout,
}: {
  plan: PublicPlanView;
  cycle: BillingCycleName;
  locale: Locale;
  stripeEnabled: boolean;
  isAuthenticated: boolean;
  busy: string | null;
  onCheckout: () => void;
}) {
  const { t } = useI18n();
  const display = plan.display;
  const isFree = plan.seatsLimit != null && plan.seatsLimit > 0 && plan.key.toUpperCase() === 'DISCOVERY';
  const popular = plan.key.toUpperCase() === 'STARTUP';

  // Le prix affiché vient du serveur, déjà résolu pour la devise et la région.
  const priceEntry = useMemo(() => {
    if (display?.available && display.amountMinor != null) {
      return {
        amountMinor: display.amountMinor,
        currency: display.currency,
        decimals: display.decimals,
      };
    }
    return null;
  }, [display]);

  const blurb = PLAN_BLURB[plan.key.toUpperCase()]?.[locale] ?? plan.description ?? t('pricing.genericBlurb');

  return (
    <article
      className={`relative rounded-2xl p-6 border-2 transition-all hover:shadow-xl ${
        popular
          ? 'border-violet-500 bg-gradient-to-b from-violet-50 to-white dark:from-violet-900/20 dark:to-gray-900'
          : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900'
      }`}
    >
      {popular && (
        <div className="absolute -top-3 left-1/2 -translate-x-1/2">
          <span className="px-4 py-1 rounded-full bg-violet-500 text-white text-xs font-semibold">
            {t('pricing.popular')}
          </span>
        </div>
      )}

      <div className="mb-6">
        <h3 className="text-xl font-bold text-gray-900 dark:text-white">{tText(plan.name)}</h3>
        {blurb && <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">{blurb}</p>}

        {priceEntry ? (
          <div className="mt-4">
            <div className="text-3xl font-bold text-gray-900 dark:text-white">
              {formatMinor(priceEntry.amountMinor, { code: priceEntry.currency, decimals: priceEntry.decimals }, locale)}
            </div>
            <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {cycle === 'YEARLY' ? t('pricing.perYear') : t('pricing.perMonth')}
            </div>
            {/* Le débit peut se faire dans une autre devise que l'affichage : le
                dire explicitement vaut mieux qu'une surprise à l'écran de carte. */}
            {display?.chargedInDifferentCurrency && display.checkoutCurrency && (
              <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
                {t('pricing.chargedIn', { currency: display.checkoutCurrency })}
              </p>
            )}
          </div>
        ) : (
          <div className="mt-4">
            <p className="text-lg font-medium text-gray-500 dark:text-gray-400">{t('pricing.priceUnavailable')}</p>
          </div>
        )}
      </div>

      <ul className="space-y-3 mb-8">
        <PlanFeature included label={formatSeats(plan, locale)} />
        <PlanFeature included={plan.storageLimitMb != null} label={formatStorage(plan.storageLimitMb, locale)} />
        <PlanFeature included={plan.aiCreditsLimit != null} label={formatCredits(plan.aiCreditsLimit, locale)} />
      </ul>

      <div className="space-y-2">
        <Link
          to={`/register?plan=${encodeURIComponent(plan.key.toLowerCase())}&currency=${encodeURIComponent(
            display?.currency ?? '',
          )}&cycle=${cycle.toLowerCase()}`}
          className={`inline-flex w-full items-center justify-center py-3 px-6 rounded-xl font-semibold transition-all ${
            popular
              ? 'bg-violet-500 hover:bg-violet-600 text-white shadow-lg shadow-violet-500/25'
              : 'bg-gray-100 hover:bg-gray-200 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-900 dark:text-white'
          }`}
        >
          {t('pricing.choose')}
          <ArrowRight className="inline ml-2 w-4 h-4" />
        </Link>

        {/* Souscription directe : reservee aux visiteurs connectes, aux plans
            payants, et uniquement si la caisse est reellement active. */}
        {stripeEnabled && isAuthenticated && !isFree && display?.selectable && (
          <button
            type="button"
            onClick={onCheckout}
            disabled={busy !== null}
            className="mt-1 inline-flex w-full items-center justify-center py-2.5 px-4 rounded-xl text-sm font-semibold border border-emerald-500/40 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-500/10 disabled:opacity-50 transition-colors"
          >
            {busy === plan.key ? (
              <Loader2 className="mr-2 w-4 h-4 animate-spin" />
            ) : (
              <CreditCard className="mr-2 w-4 h-4" />
            )}
            {t('pricing.subscribeOnline')}
          </button>
        )}

        {!display?.available && (
          <Link
            to="/contact"
            className="inline-flex w-full items-center justify-center py-2.5 px-4 rounded-xl text-sm font-semibold border border-gray-300 dark:border-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
          >
            {t('pricing.contactUs')}
          </Link>
        )}
      </div>
    </article>
  );
}

function PlanFeature({ included, label }: { included: boolean; label: string }) {
  return (
    <li className="flex items-start gap-3 text-sm">
      {included ? (
        <Check className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" aria-hidden="true" />
      ) : (
        <Minus className="w-5 h-5 text-gray-300 dark:text-gray-600 flex-shrink-0 mt-0.5" aria-hidden="true" />
      )}
      <span
        className={
          included ? 'text-gray-700 dark:text-gray-300' : 'text-gray-400 dark:text-gray-500 line-through'
        }
      >
        {label}
      </span>
    </li>
  );
}

// ── Blocs éditoriaux ────────────────────────────────────────────────────────

function Highlights() {
  const { t } = useI18n();
  const items = [
    { key: 'ai', Icon: Bot, tone: 'violet' },
    { key: 'momo', Icon: Zap, tone: 'emerald' },
    { key: 'security', Icon: Shield, tone: 'blue' },
  ] as const;
  return (
    <div className="mt-20 grid md:grid-cols-3 gap-6">
      {items.map(({ key, Icon, tone }) => (
        <div
          key={key}
          className={`p-6 rounded-2xl border ${
            tone === 'violet'
              ? 'bg-gradient-to-br from-violet-500/10 to-purple-500/10 dark:from-violet-500/20 dark:to-purple-500/20 border-violet-200/50 dark:border-violet-500/20'
              : tone === 'emerald'
                ? 'bg-gradient-to-br from-emerald-500/10 to-green-500/10 dark:from-emerald-500/20 dark:to-green-500/20 border-emerald-200/50 dark:border-emerald-500/20'
                : 'bg-gradient-to-br from-blue-500/10 to-cyan-500/10 dark:from-blue-500/20 dark:to-cyan-500/20 border-blue-200/50 dark:border-blue-500/20'
          }`}
        >
          <div className="w-12 h-12 rounded-xl bg-violet-500 text-white flex items-center justify-center mb-4">
            <Icon className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
            {t(`pricing.highlight.${key}.title`)}
          </h3>
          <p className="text-sm text-gray-600 dark:text-gray-400">{t(`pricing.highlight.${key}.body`)}</p>
        </div>
      ))}
    </div>
  );
}

function Testimonials() {
  const { t } = useI18n();
  return (
    <div className="mt-16 text-center">
      <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-8">{t('pricing.testimonials.title')}</h2>
      <div className="grid md:grid-cols-3 gap-6 max-w-4xl mx-auto">
        {[1, 2, 3].map((index) => (
          <div
            key={index}
            className="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700"
          >
            <div className="flex items-center gap-4 mb-4">
              <div
                className={`w-12 h-12 rounded-full bg-gradient-to-br ${index === 1 ? 'from-orange-400 to-pink-500' : index === 2 ? 'from-blue-400 to-purple-500' : 'from-green-400 to-teal-500'} flex items-center justify-center text-white font-bold`}
              >
                {t(`pricing.testimonials.${index}.initial`)}
              </div>
              <div>
                <div className="font-semibold text-gray-900 dark:text-white">
                  {t(`pricing.testimonials.${index}.name`)}
                </div>
                <div className="text-sm text-gray-500">{t(`pricing.testimonials.${index}.role`)}</div>
              </div>
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-400 italic">{t(`pricing.testimonials.${index}.quote`)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function Faq() {
  const { t } = useI18n();
  const entries = ['changePlan', 'paymentMethods', 'trial'] as const;
  return (
    <div className="mt-16 max-w-3xl mx-auto">
      <h2 className="text-2xl font-bold text-gray-900 dark:text-white text-center mb-8">{t('pricing.faq.title')}</h2>
      <div className="space-y-4">
        {entries.map((entry) => (
          <div
            key={entry}
            className="p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700"
          >
            <h3 className="font-semibold text-gray-900 dark:text-white">{t(`pricing.faq.${entry}.title`)}</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{t(`pricing.faq.${entry}.body`)}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function CatalogUnavailable({ message, onRetry }: { message: string; onRetry: () => void }) {
  const { t } = useI18n();
  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white dark:from-gray-950 dark:to-gray-900 flex items-center justify-center px-4">
      <div
        role="alert"
        className="max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-500/30 dark:bg-amber-500/10"
      >
        <AlertCircle className="mx-auto h-8 w-8 text-amber-600 dark:text-amber-400" aria-hidden="true" />
        <h1 className="mt-4 text-lg font-semibold text-amber-900 dark:text-amber-200">{t('pricing.unavailableTitle')}</h1>
        <p className="mt-2 text-sm text-amber-800 dark:text-amber-300">{message}</p>
        <button
          type="button"
          onClick={onRetry}
          className="mt-6 inline-flex items-center gap-2 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          <Loader2 className="h-4 w-4" /> {t('pricing.retry')}
        </button>
      </div>
    </div>
  );
}

// ── Formatage ───────────────────────────────────────────────────────────────

function formatSeats(plan: PublicPlanView, locale: Locale): string {
  if (plan.seatsLimit == null) return '—';
  const count = new Intl.NumberFormat(locale).format(plan.seatsLimit);
  return plan.seatsLimit >= 1000 ? `${count}+` : count;
}

function formatStorage(megabytes: number | null, locale: Locale): string {
  if (megabytes == null) return '—';
  if (megabytes >= 1024) {
    return `${new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(megabytes / 1024)} Go`;
  }
  return `${new Intl.NumberFormat(locale).format(megabytes)} Mo`;
}

function formatCredits(credits: number | null, locale: Locale): string {
  if (credits == null) return '—';
  return new Intl.NumberFormat(locale).format(credits);
}

