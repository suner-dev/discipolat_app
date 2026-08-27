import { Check, HelpCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';

const TIERS = [
  { name: 'Essentiel', tagline: 'Pour démarrer votre suivi', featured: false },
  { name: 'Pro', tagline: 'Pour piloter votre église', featured: true },
  { name: 'Église', tagline: 'Pour les grandes communautés', featured: false },
];

const COMMON = ['Tous les modules de base', 'Accompagnement à la mise en place', 'Support réactif'];

export default function Pricing() {
  const { t } = useI18n();
  return (
    <section id="pricing" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-14">
          <span className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full border bg-gold-500/10 border-gold-500/20 text-gold-600 dark:text-gold-400 text-xs font-medium mb-4">
            <span className="w-1.5 h-1.5 rounded-full bg-gold-500" /> {t('landing.nav.pricing')}
          </span>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
            Un modèle <span className="text-gradient-gold">pensé pour les églises</span>
          </h2>
          <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-2xl mx-auto">
            Tarification transparente, adaptée à la taille de votre communauté. Contactez-nous pour un devis personnalisé.
          </p>
        </div>
        <div className="grid md:grid-cols-3 gap-5 max-w-5xl mx-auto">
          {TIERS.map((c, i) => (
            <Reveal key={c.name} delay={i * 80}>
              <div className={`rounded-3xl p-7 h-full flex flex-col relative ${c.featured ? 'glass-strong shadow-glow-lg ring-1 ring-primary-500/40' : 'glass-card'}`}>
                {c.featured && (
                  <span className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider bg-primary-600 text-white shadow">★</span>
                )}
                <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-1">{c.name}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">{c.tagline}</p>
                <div className="text-4xl font-bold text-gray-900 dark:text-white font-display mb-2">Sur devis</div>
                <div className="space-y-2.5 mt-4 mb-7 flex-1">
                  {COMMON.map((f) => (
                    <div key={f} className="flex items-center gap-2.5 text-sm text-gray-600 dark:text-gray-300">
                      <Check className="w-4 h-4 text-primary-500 flex-shrink-0" /> {f}
                    </div>
                  ))}
                </div>
                <Link to="/login" className={`btn ${c.featured ? 'btn-primary' : 'btn-secondary'} w-full justify-center`}>
                  {t('landing.nav.start')}
                </Link>
              </div>
            </Reveal>
          ))}
        </div>
        <Reveal delay={160}>
          <p className="mt-8 text-center text-sm text-gray-400 dark:text-gray-500 flex items-center justify-center gap-1.5">
            <HelpCircle className="w-4 h-4 flex-shrink-0" /> Tarification finale discutée selon la taille de votre église et les besoins de votre équipe.
          </p>
        </Reveal>
      </div>
    </section>
  );
}