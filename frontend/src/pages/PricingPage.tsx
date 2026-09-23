import { useI18n } from '@/i18n';
import { tText } from '@/i18n';
import { Check, X, ArrowRight, Zap, Shield, Bot } from 'lucide-react';

const PLANS = [
  {
    id: 'discovery',
    name: 'Découverte',
    description: 'Parfait pour démarrer et tester la plateforme',
    price: 0,
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
    price: 9,
    period: '9 €/mois',
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
    price: 29,
    period: '29 €/mois',
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
    price: 79,
    period: '79 €/mois',
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

export default function PricingPage() {
  const { t } = useI18n();

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-white dark:from-gray-950 dark:to-gray-900">
      {/* Header */}
      <div className="py-16 px-4 text-center">
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
          {PLANS.map((plan) => (
            <div
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
                  {plan.name}
                </h3>
                <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                  {plan.description}
                </p>
                <div className="mt-4 flex items-baseline gap-1">
                  <span className="text-4xl font-bold text-gray-900 dark:text-white">
                    {plan.price === 0 ? '0' : plan.price}
                  </span>
                  <span className="text-gray-500 dark:text-gray-400">
                    {plan.price === 0 ? '' : '/mois'}
                  </span>
                </div>
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
              <button
                className={`w-full py-3 px-6 rounded-xl font-semibold transition-all ${
                  plan.popular
                    ? 'bg-violet-500 hover:bg-violet-600 text-white shadow-lg shadow-violet-500/25'
                    : 'bg-gray-100 hover:bg-gray-200 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-900 dark:text-white'
                }`}
              >
                Choisir {plan.name}
                <ArrowRight className="inline ml-2 w-4 h-4" />
              </button>
            </div>
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
