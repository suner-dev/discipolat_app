import { Check, HelpCircle, Minus, Star } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useI18n, type Locale } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import { usePricing } from '@/features/pricing/usePricing';
import { formatMinor } from '@/lib/money';
import type { PublicPlanView } from '@/features/pricing/types';
import { planBlurb } from './pricingCopy';

/**
 * Section tarifs de la landing — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T7.3.
 *
 * <p><b>Correction de la faille F4.</b> Cette section affichait trois tiers
 * codés en dur (« Essentiel », « Pro », « Église ») tous annoncés « Sur devis »,
 * pendant que la page {@code /pricing} affichait quatre plans avec de vrais prix.
 * Deux vérités contradictoires sur le même produit, et les modifications faites
 * par le super-admin ne se reflète ici sous aucune forme.</p>
 *
 * <p>La section lit désormais le catalogue. Le mode {@code STATIC} est conservé
 * pour qui veut garder les trois tiers d'origine — mais il devient un réglage
 * explicite du super-admin, et non un comportement subi.</p>
 *
 * <p>Un plan sans prix dans la devise résolue affiche « Sur devis » pour
 * <b>lui seul</b> : c'est la seule situation où cette formule reste honnête.
 * Ailleurs, un prix existe et il s'affiche.</p>
 */
export default function Pricing() {
  const { t } = useI18n();
  const { locale } = useI18n();
  const { config, plans, currency, setCurrency } = usePricing();

  const landing = config?.landing;
  const source = landing?.source ?? 'API';
  const maxPlans = Math.max(1, landing?.maxPlans ?? 4);
  const showPrices = landing?.showPrices !== false;

  // Mode STATIC : on conserve exactement le rendu historique (3 tiers, « Sur devis »).
  const staticTiers = [
    { name: 'Essentiel', tagline: 'Pour démarrer votre suivi', featured: false },
    { name: 'Pro', tagline: 'Pour piloter votre église', featured: true },
    { name: 'Église', tagline: 'Pour les grandes communautés', featured: false },
  ];
  const useStatic = source === 'STATIC';

  const visiblePlans = useStatic ? [] : plans.slice(0, maxPlans);
  const ctaLabel = landing?.ctaLabel ?? t('landing.nav.start');

  // ── Mode statique historique ──────────────────────────────────────────────
  if (useStatic) {
    return (
      <section id="pricing" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <StaticGrid tiers={staticTiers} ctaLabel={ctaLabel} />
        </div>
      </section>
    );
  }

  // ── Mode catalogue ────────────────────────────────────────────────────────
  return (
    <section id="pricing" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-14">
          <span className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full border bg-gold-500/10 border-gold-500/20 text-gold-600 dark:text-gold-400 text-xs font-medium mb-4">
            <span className="w-1.5 h-1.5 rounded-full bg-gold-500" /> {t('landing.nav.pricing')}
          </span>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
            <span className="text-gradient-gold">{t('pricing.landing.title')}</span>
          </h2>
          <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-2xl mx-auto">
            {t('pricing.landing.subtitle')}
          </p>

          {/* Sélecteur de devise : rendu uniquement si le super-admin l'autorise,
              et seulement s'il existe réellement plusieurs devises à proposer. */}
          {config?.allowCurrencyChoice && config.enabledCurrencies.length > 1 && (
            <div className="mt-7 inline-flex items-center gap-2 rounded-full border border-gray-200 dark:border-gray-800 bg-white/70 dark:bg-gray-900/60 px-2 py-1.5 backdrop-blur">
              <label htmlFor="landing-currency" className="sr-only">
                {t('pricing.currency')}
              </label>
              <select
                id="landing-currency"
                value={currency}
                onChange={(event) => setCurrency(event.target.value)}
                className="appearance-none bg-transparent pl-2 pr-7 text-sm font-medium text-gray-700 dark:text-gray-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded-full"
              >
                {config.enabledCurrencies.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.symbol} · {option.code}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {visiblePlans.length === 0 ? (
          <p className="text-center text-sm text-gray-400 dark:text-gray-500">{t('pricing.loading')}</p>
        ) : (
          <div
            className={`grid gap-5 mx-auto ${
              visiblePlans.length <= 2
                ? 'sm:grid-cols-2 max-w-3xl'
                : visiblePlans.length === 3
                  ? 'md:grid-cols-3 max-w-5xl'
                  : 'md:grid-cols-2 lg:grid-cols-4 max-w-7xl'
            }`}
          >
            {visiblePlans.map((plan, index) => (
              <Reveal key={plan.key} delay={index * 80}>
                <PlanCard
                  plan={plan}
                  showPrices={showPrices}
                  locale={locale}
                  featured={index === 1}
                  ctaLabel={ctaLabel}
                />
              </Reveal>
            ))}
          </div>
        )}

        <Reveal delay={160}>
          <div className="mt-8 flex flex-col items-center justify-center gap-4 text-sm text-gray-400 dark:text-gray-500">
            <p className="flex items-center justify-center gap-1.5">
              <HelpCircle className="w-4 h-4 flex-shrink-0" /> {t('pricing.landing.footnote')}
            </p>
            {landing?.showToggle !== false && (
              <Link
                to="/pricing"
                className="inline-flex items-center gap-1.5 font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
              >
                {t('pricing.viewAll')}
              </Link>
            )}
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function PlanCard({
  plan,
  showPrices,
  locale,
  featured,
  ctaLabel,
}: {
  plan: PublicPlanView;
  showPrices: boolean;
  locale: Locale;
  featured: boolean;
  ctaLabel: string;
}) {
  const { t } = useI18n();
  const display = plan.display;

  // « Sur devis » uniquement pour CE plan, et seulement s'il n'a réellement pas
  // de prix dans la devise affichée. Un catalogue sans prix ne doit pas faire
  // croire que tout est gratuit — ni que tout est sur devis.
  const quoteOnRequest = !display?.available;
  const price = display?.available && display.amountMinor != null ? display.amountMinor : null;
  const meta = { code: display?.currency ?? 'XAF', decimals: display?.decimals ?? 2 };

  const blurb = planBlurb(plan.key, locale as Locale) ?? plan.description;

  return (
    <div
      className={`rounded-3xl p-7 h-full flex flex-col relative ${
        featured
          ? 'glass-strong shadow-glow-lg ring-1 ring-primary-500/40'
          : 'glass-card'
      }`}
    >
      {featured && (
        <span className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-primary-600 text-white shadow">
          ★ {t('pricing.popular')}
        </span>
      )}
      <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-1">{plan.name}</h3>
      {blurb && <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">{blurb}</p>}

      <div className="mb-6">
        {quoteOnRequest || !showPrices || price === null ? (
          <div className="text-4xl font-bold text-gray-900 dark:text-white font-display">
            {t('pricing.surDevis')}
          </div>
        ) : (
          <>
            <div className="text-4xl font-bold text-gray-900 dark:text-white font-display">
              {formatMinor(price, meta, locale)}
            </div>
            <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {t('pricing.perMonth')}
            </div>
            {display?.chargedInDifferentCurrency && display.checkoutCurrency && (
              <div className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                {t('pricing.chargedIn', { currency: display.checkoutCurrency })}
              </div>
            )}
          </>
        )}
      </div>

      <div className="space-y-2.5 mt-2 mb-7 flex-1">
        <CardLine label={t('pricing.feature.seats', { count: String(plan.seatsLimit ?? '—') })} included={plan.seatsLimit != null && plan.seatsLimit > 0} />
        <CardLine
          label={t('pricing.feature.storage', { size: formatStorage(plan.storageLimitMb, locale) })}
          included={plan.storageLimitMb != null && plan.storageLimitMb > 0}
        />
        <CardLine
          label={t('pricing.feature.aiCredits', { count: String(plan.aiCreditsLimit ?? '—') })}
          included={plan.aiCreditsLimit != null && plan.aiCreditsLimit > 0}
        />
        <CardLine label={t('pricing.feature.modules', { count: String(plan.modules.length) })} included />
      </div>

      <Link to={`/pricing?plan=${encodeURIComponent(plan.key)}`} className="btn btn-primary w-full justify-center">
        {ctaLabel}
      </Link>
    </div>
  );
}

function CardLine({ label, included }: { label: string; included: boolean }) {
  const Icon = included ? Check : Minus;
  return (
    <div className="flex items-center gap-2.5 text-sm text-gray-600 dark:text-gray-300">
      <Icon
        className={`w-4 h-4 flex-shrink-0 ${included ? 'text-primary-500' : 'text-gray-300 dark:text-gray-600'}`}
        aria-hidden="true"
      />
      <span className={included ? '' : 'text-gray-400 dark:text-gray-500'}>{label}</span>
    </div>
  );
}

function StaticGrid({
  tiers,
  ctaLabel,
}: {
  tiers: Array<{ name: string; tagline: string; featured: boolean }>;
  ctaLabel: string;
}) {
  const { t } = useI18n();
  const common = [t('pricing.static.common1'), t('pricing.static.common2'), t('pricing.static.common3')];
  return (
    <>
      <div className="text-center mb-14">
        <span className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full border bg-gold-500/10 border-gold-500/20 text-gold-600 dark:text-gold-400 text-xs font-medium mb-4">
          <span className="w-1.5 h-1.5 rounded-full bg-gold-500" /> {t('landing.nav.pricing')}
        </span>
        <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
          {t('pricing.landing.title')}
        </h2>
        <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-2xl mx-auto">
          {t('pricing.landing.subtitle')}
        </p>
      </div>
      <div className="grid md:grid-cols-3 gap-5 max-w-5xl mx-auto">
        {tiers.map((tier, index) => (
          <Reveal key={tier.name} delay={index * 80}>
            <div
              className={`rounded-3xl p-7 h-full flex flex-col relative ${
                tier.featured ? 'glass-strong shadow-glow-lg ring-1 ring-primary-500/40' : 'glass-card'
              }`}
            >
              {tier.featured && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-primary-600 text-white shadow">
                  <Star className="w-2.5 h-2.5 inline" aria-hidden="true" />
                </span>
              )}
              <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-1">{tier.name}</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">{tier.tagline}</p>
              <div className="text-4xl font-bold text-gray-900 dark:text-white font-display mb-2">
                {t('pricing.surDevis')}
              </div>
              <div className="space-y-2.5 mt-4 mb-7 flex-1">
                {common.map((label) => (
                  <CardLine key={label} label={label} included />
                ))}
              </div>
              <Link to="/login" className="btn btn-primary w-full justify-center">
                {ctaLabel}
              </Link>
            </div>
          </Reveal>
        ))}
      </div>
      <Reveal delay={160}>
        <p className="mt-8 text-center text-sm text-gray-400 dark:text-gray-500 flex items-center justify-center gap-1.5">
          <HelpCircle className="w-4 h-4 flex-shrink-0" /> {t('pricing.landing.footnote')}
        </p>
      </Reveal>
    </>
  );
}

function formatStorage(megabytes: number | null, locale: string): string {
  if (megabytes == null) return '—';
  if (megabytes >= 1024) {
    return `${new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(megabytes / 1024)} Go`;
  }
  return `${new Intl.NumberFormat(locale).format(megabytes)} Mo`;
}