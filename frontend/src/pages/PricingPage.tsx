import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, X, ArrowRight, Zap, Shield, Bot, AlertCircle } from 'lucide-react';
import { tText, useI18n, type Locale } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';

interface PriceView {
  amount: number;
  currency: string;
}

interface PlanView {
  id: string;
  name: string;
  description: string;
  prices: PriceView[];
  period: string;
  popular: boolean;
  features: Array<{ text: string; included: boolean }>;
}

interface PublicPlan {
  key: string;
  name: string;
  description: string | null;
  priceMonthly: number | null;
  priceEur: number | null;
  priceXaf: number | null;
  priceUsd: number | null;
  currency: string | null;
  seatsLimit: number | null;
  storageLimitMb: number | null;
  aiCreditsLimit: number | null;
  billingPeriod: string | null;
  sortOrder: number | null;
}

const FALLBACK_PLANS: PlanView[] = [
  {
    id: 'discovery',
    name: 'Découverte',
    description: 'Parfait pour démarrer et tester la plateforme',
    prices: [{ amount: 0, currency: 'EUR' }],
    period: 'Gratuit',
    popular: false,
    features: [
      { text: 'Jusqu\'à 50 membres', included: true },
      { text: '1 espace (département/famille)', included: true },
      { text: '100 Mo de stockage', included: true },
      { text: '10 crédits IA/mois', included: true },
      { text: '5 événements/mois', included: true },
      { text: 'Assistant IA basique', included: true },
      { text: 'Support par email', included: true },
      { text: 'API publique', included: false },
      { text: 'Mobile Money intégré', included: false },
      { text: 'Jumeau numérique', included: false },
    ],
  },
  {
    id: 'startup',
    name: 'Démarrage',
    description: 'Idéal pour les églises en croissance',
    prices: [{ amount: 9, currency: 'EUR' }],
    period: '/mois',
    popular: true,
    features: [
      { text: 'Jusqu\'à 250 membres', included: true },
      { text: '10 espaces', included: true },
      { text: '1 Go de stockage', included: true },
      { text: '30 crédits IA/mois', included: true },
      { text: 'Événements illimités', included: true },
      { text: 'Assistant IA complet', included: true },
      { text: 'Support prioritaire', included: true },
      { text: 'API publique', included: true },
      { text: 'Mobile Money intégré', included: true },
      { text: 'Analyse des risques IA', included: true },
    ],
  },
  {
    id: 'growth',
    name: 'Croissance',
    description: 'Pour les églises dynamiques et en expansion',
    prices: [{ amount: 29, currency: 'EUR' }],
    period: '/mois',
    popular: false,
    features: [
      { text: 'Jusqu\'à 2 000 membres', included: true },
      { text: '50 espaces', included: true },
      { text: '20 Go de stockage', included: true },
      { text: '100 crédits IA/mois', included: true },
      { text: 'Événements illimités', included: true },
      { text: 'Assistant IA avancé avec RAG', included: true },
      { text: 'Support dédié', included: true },
      { text: 'API publique complète', included: true },
      { text: 'Mobile Money + Stripe', included: true },
      { text: 'Jumeau numérique IA', included: true },
      { text: 'Prédictions de décrochage', included: true },
      { text: 'Benchmark inter-églises', included: true },
    ],
  },
  {
    id: 'network',
    name: 'Réseau & Campus',
    description: 'Pour les réseaux d\'églises et campuses multinationaux',
    prices: [{ amount: 79, currency: 'EUR' }],
    period: '/mois',
    popular: false,
    features: [
      { text: 'Jusqu\'à 10 000 membres', included: true },
      { text: '150+ espaces', included: true },
      { text: '100 Go de stockage', included: true },
      { text: '500 crédits IA/mois', included: true },
      { text: 'Événements illimités', included: true },
      { text: 'Assistant IA personnalisé', included: true },
      { text: 'Account manager dédié', included: true },
      { text: 'API publique illimitée', included: true },
      { text: 'Mobile Money + Stripe + SEPA', included: true },
      { text: 'Jumeau numérique complet', included: true },
      { text: 'Module Santé/Infrimerie', included: true },
      { text: 'Multi-campus', included: true },
      { text: 'Annuaire inter-églises', included: true },
    ],
  },
];

const formatStorage = (megabytes: number | null, locale: Locale): string => {
  if (megabytes == null) return 'Stockage non précisé';
  if (megabytes >= 1024) return `${new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(megabytes / 1024)} Go`;
  return `${new Intl.NumberFormat(locale).format(megabytes)} Mo`;
};

const isRecord = (value: unknown): value is Record<string, unknown> => (
  typeof value === 'object' && value !== null && !Array.isArray(value)
);

const toNullableNumber = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

const toNullableString = (value: unknown): string | null => (
  typeof value === 'string' && value.trim() ? value.trim() : null
);

const normalizeCurrency = (value: unknown): string | null => {
  const currency = toNullableString(value)?.toUpperCase();
  return currency && /^[A-Z]{3}$/.test(currency) ? currency : null;
};

const parsePublicPlans = (payload: unknown): PublicPlan[] => {
  if (!Array.isArray(payload)) return [];
  return payload.flatMap((item): PublicPlan[] => {
    if (!isRecord(item)) return [];
    const key = toNullableString(item.key);
    const name = toNullableString(item.name);
    if (!key || !name) return [];
    return [{
      key,
      name,
      description: toNullableString(item.description),
      priceMonthly: toNullableNumber(item.priceMonthly),
      priceEur: toNullableNumber(item.priceEur),
      priceXaf: toNullableNumber(item.priceXaf),
      priceUsd: toNullableNumber(item.priceUsd),
      currency: toNullableString(item.currency),
      seatsLimit: toNullableNumber(item.seatsLimit),
      storageLimitMb: toNullableNumber(item.storageLimitMb),
      aiCreditsLimit: toNullableNumber(item.aiCreditsLimit),
      billingPeriod: toNullableString(item.billingPeriod),
      sortOrder: toNullableNumber(item.sortOrder),
    }];
  });
};

const toPlanPrices = (plan: PublicPlan): PriceView[] => {
  const specificPrices: PriceView[] = [
    { amount: plan.priceEur, currency: 'EUR' },
    { amount: plan.priceXaf, currency: 'XAF' },
    { amount: plan.priceUsd, currency: 'USD' },
  ].filter((price): price is PriceView => price.amount !== null);
  if (specificPrices.length > 0) return specificPrices;
  const currency = normalizeCurrency(plan.currency);
  return plan.priceMonthly !== null && currency ? [{ amount: plan.priceMonthly, currency }] : [];
};

const formatPrice = (price: PriceView, locale: Locale): string => (
  new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: price.currency,
    maximumFractionDigits: 0,
  }).format(price.amount)
);

const toPlanView = (plan: PublicPlan, locale: Locale): PlanView => {
  const formatter = new Intl.NumberFormat(locale);
  const seats = plan.seatsLimit === null
    ? 'Utilisateurs non précisés'
    : `Jusqu'à ${formatter.format(plan.seatsLimit)} utilisateurs`;
  const aiCredits = plan.aiCreditsLimit === null
    ? 'Crédits IA non précisés'
    : `${formatter.format(plan.aiCreditsLimit)} crédits IA / mois`;

  return {
    id: plan.key.toLowerCase(),
    name: plan.name,
    description: plan.description ?? 'Offre Discipolat',
    prices: toPlanPrices(plan),
    period: plan.billingPeriod?.toUpperCase() === 'YEARLY' ? '/an' : '/mois',
    popular: plan.sortOrder === 1,
    features: [
      { text: seats, included: true },
      { text: formatStorage(plan.storageLimitMb, locale), included: true },
      { text: aiCredits, included: true },
    ],
  };
};

export default function PricingPage() {
  const { locale } = useI18n();
  const [plans, setPlans] = useState<PlanView[]>(FALLBACK_PLANS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [usingFallback, setUsingFallback] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    api.get<unknown>('/public/plans')
      .then(({ data }) => {
        if (!active) return;
        const publicPlans = parsePublicPlans(data);
        if (publicPlans.length > 0) {
          setPlans(publicPlans.map((plan) => toPlanView(plan, locale)));
          setUsingFallback(false);
        } else {
          setPlans(FALLBACK_PLANS);
          setUsingFallback(true);
        }
      })
      .catch((requestError) => {
        if (!active) return;
        setPlans(FALLBACK_PLANS);
        setError(getErrorMessage(requestError));
        setUsingFallback(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [locale]);

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white dark:from-gray-950 dark:to-gray-900">
      {/* Header */}
      <div className="py-16 px-4 text-center">
        {loading && <p role="status" className="mb-4 text-sm text-gray-500">Chargement du catalogue…</p>}
        {error && (
          <div role="alert" className="mx-auto mb-4 flex max-w-2xl items-center justify-center gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>Catalogue momentanément indisponible : {error}</span>
          </div>
        )}
        {!loading && !error && usingFallback && (
          <p role="status" className="mb-4 text-sm text-amber-700">Catalogue de secours affiché ; les tarifs en vigueur seront confirmés lors de l'inscription.</p>
        )}
        <h1 className="text-4xl md:text-5xl font-bold text-gray-900 dark:text-white">
          Plans et Tarifs
        </h1>
        <p className="mt-4 text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
          Choisissez le plan qui correspond à la taille et aux besoins de votre église.
          Tous les plans incluent l'assistant IA pastoral.
        </p>
      </div>

      {/* Plans Grid */}
      <div className="max-w-6xl mx-auto px-4 pb-20">
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {plans.map((plan) => (
            <article
              key={plan.id}
              className={`relative rounded-2xl p-6 border-2 transition-all hover:shadow-xl ${
                plan.popular
                  ? 'border-violet-500 bg-gradient-to-b from-violet-50 to-white dark:from-violet-900/20 dark:to-gray-900'
                  : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900'
              }`}
            >
              {/* Badge Popular */}
              {plan.popular && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                  <span className="px-4 py-1 rounded-full bg-violet-500 text-white text-xs font-semibold">
                    Le plus populaire
                  </span>
                </div>
              )}

              {/* Plan Name & Price */}
              <div className="mb-6">
                <h3 className="text-xl font-bold text-gray-900 dark:text-white">
                  {tText(plan.name)}
                </h3>
                <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                  {tText(plan.description)}
                </p>
                {plan.prices.length > 0 ? (
                  <div className="mt-4 space-y-1">
                    {plan.prices.map((price) => (
                      <div key={price.currency} className="flex items-baseline gap-1">
                        <span className="text-3xl font-bold text-gray-900 dark:text-white">
                          {formatPrice(price, locale)}
                        </span>
                        <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{price.currency}</span>
                        <span className="text-sm text-gray-500 dark:text-gray-400">{plan.period}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="mt-4 text-lg text-gray-500">Tarif non communiqué</p>
                )}
              </div>

              {/* Features */}
              <ul className="space-y-3 mb-8">
                {plan.features.map((feature, idx) => (
                  <li
                    key={idx}
                    className="flex items-start gap-3 text-sm"
                  >
                    {feature.included ? (
                      <Check className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" />
                    ) : (
                      <X className="w-5 h-5 text-gray-400 flex-shrink-0 mt-0.5" />
                    )}
                    <span
                      className={
                        feature.included
                          ? 'text-gray-700 dark:text-gray-300'
                          : 'text-gray-400 dark:text-gray-500 line-through'
                      }
                    >
                      {feature.text}
                    </span>
                  </li>
                ))}
              </ul>

              {/* CTA */}
              <Link
                to={`/register?plan=${encodeURIComponent(plan.id)}`}
                className={`inline-flex w-full items-center justify-center py-3 px-6 rounded-xl font-semibold transition-all ${
                  plan.popular
                    ? 'bg-violet-500 hover:bg-violet-600 text-white shadow-lg shadow-violet-500/25'
                    : 'bg-gray-100 hover:bg-gray-200 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-900 dark:text-white'
                }`}
              >
                Choisir {tText(plan.name)}
                <ArrowRight className="inline ml-2 w-4 h-4" />
              </Link>
            </article>
          ))}
        </div>

        {/* Feature Highlights */}
        <div className="mt-20 grid md:grid-cols-3 gap-6">
          <div className="p-6 rounded-2xl bg-gradient-to-br from-violet-500/10 to-purple-500/10 dark:from-violet-500/20 dark:to-purple-500/20 border border-violet-200/50 dark:border-violet-500/20">
            <div className="w-12 h-12 rounded-xl bg-violet-500 text-white flex items-center justify-center mb-4">
              <Bot className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              IA Pastorale
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              L'assistant IA est inclus dans tous les plans. Il analyse vos données
              en temps réel et vous aide à prendre des décisions éclairées.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-gradient-to-br from-emerald-500/10 to-green-500/10 dark:from-emerald-500/20 dark:to-green-500/20 border border-emerald-200/50 dark:border-emerald-500/20">
            <div className="w-12 h-12 rounded-xl bg-emerald-500 text-white flex items-center justify-center mb-4">
              <Zap className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              Mobile Money
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Intégration native avec MTN MoMo, Orange Money et M-Pesa pour
              recevoir les dons de vos membres.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-gradient-to-br from-blue-500/10 to-cyan-500/10 dark:from-blue-500/20 dark:to-cyan-500/20 border border-blue-200/50 dark:border-blue-500/20">
            <div className="w-12 h-12 rounded-xl bg-blue-500 text-white flex items-center justify-center mb-4">
              <Shield className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              Sécurisé & Conforme
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Données chiffrées, authentification 2FA, audit trail complet.
              Conforme RGPD pour protéger les données de vos membres.
            </p>
          </div>
        </div>

        {/* Testimonials */}
        <div className="mt-16 text-center">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-8">
            Rejoignez des centaines d'églises qui grandissent avec Discipolat
          </h2>
          <div className="grid md:grid-cols-3 gap-6 max-w-4xl mx-auto">
            <div className="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-4 mb-4">
                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-orange-400 to-pink-500 flex items-center justify-center text-white font-bold">
                  P
                </div>
                <div>
                  <div className="font-semibold text-gray-900 dark:text-white">Pasteur Martin</div>
                  <div className="text-sm text-gray-500">Église de la Grâce, Cameroun</div>
                </div>
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400 italic">
                "Discipolat a transformé notre façon de suivre nos membres.
                L'assistant IA nous aide à identifier les âmes qui ont besoin d'attention."
              </p>
            </div>
            <div className="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-4 mb-4">
                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white font-bold">
                  S
                </div>
                <div>
                  <div className="font-semibold text-gray-900 dark:text-white">Sophie K.</div>
                  <div className="text-sm text-gray-500">Responsable Jeunesse, RDC</div>
                </div>
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400 italic">
                "La gamification motive nos jeunes. Ils s'engagent plus que jamais
                grâce aux quêtes et badges."
              </p>
            </div>
            <div className="p-6 rounded-2xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-4 mb-4">
                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-green-400 to-teal-500 flex items-center justify-center text-white font-bold">
                  A
                </div>
                <div>
                  <div className="font-semibold text-gray-900 dark:text-white">Pastor A.</div>
                  <div className="text-sm text-gray-500">Mission Evangélique, France</div>
                </div>
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400 italic">
                "La gestion des finances et des dons est devenue simple et transparente.
                Nos membres font plus de dons."
              </p>
            </div>
          </div>
        </div>

        {/* FAQ */}
        <div className="mt-16 max-w-3xl mx-auto">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white text-center mb-8">
            Questions Fréquentes
          </h2>
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <h3 className="font-semibold text-gray-900 dark:text-white">
                Puis-je changer de plan à tout moment ?
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Oui, vous pouvez passer à un plan supérieur à tout moment.
                Le changement est effectif immédiatement et vous payez la différence
                au prorata.
              </p>
            </div>
            <div className="p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <h3 className="font-semibold text-gray-900 dark:text-white">
                Quels modes de paiement acceptez-vous ?
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Nous acceptons les cartes bancaires (Stripe), SEPA pour l'Europe,
                et Mobile Money (MTN MoMo, Orange Money, M-Pesa) pour l'Afrique.
              </p>
            </div>
            <div className="p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
              <h3 className="font-semibold text-gray-900 dark:text-white">
                Puis-je essayer gratuitement ?
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Oui ! Le plan Découverte est gratuit à vie avec jusqu'à 50 membres.
                Pas de carte bancaire requise. Passez à un plan payant quand vous
                êtes prêt.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
