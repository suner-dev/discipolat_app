import { Link } from 'react-router-dom';
import { ArrowRight, Church, CheckCircle2, Sparkles } from 'lucide-react';
import { useI18n } from '@/i18n';
import { useSettings } from '@/contexts/SettingsContext';
import Reveal from '@/components/shared/Reveal';

interface CTAFooterProps { onDemo: () => void; }

export function CTAFinal({ onDemo }: CTAFooterProps) {
  const { t } = useI18n();
  return (
    <section id="cta" className="py-20 sm:py-28 scroll-mt-20">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <Reveal>
          <div className="relative">
            <div className="absolute -inset-1 rounded-[2rem] bg-gradient-to-br from-primary-500/30 via-transparent to-gold-500/30 opacity-60 blur-xl animate-pulse-soft" />
            <div className="relative glass-strong rounded-[2rem] p-10 sm:p-16 text-center overflow-hidden">
              <div className="absolute -top-20 -right-20 w-64 h-64 rounded-full bg-primary-500/[0.06] blur-3xl" />
              <div className="absolute -bottom-24 -left-16 w-72 h-72 rounded-full bg-gold-500/[0.06] blur-3xl" />

              <div className="relative">
                <div className="w-16 h-16 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-glow animate-float">
                  <CheckCircle2 className="w-8 h-8 text-white" />
                </div>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight text-balance">
                  {t('landing.cta.title')}
                </h2>
                <p className="mt-5 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                  {t('landing.cta.subtitle')}
                </p>
                <div className="mt-8 flex flex-wrap items-center justify-center gap-3.5">
                  <Link to="/login" className="btn-primary btn-lg group">
                    {t('landing.cta.primary')}
                    <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
                  </Link>
                  <button onClick={onDemo} className="btn-secondary btn-lg group">
                    <Sparkles className="w-4 h-4 transition-transform duration-300 group-hover:rotate-12" />
                    {t('landing.cta.secondary')}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

export function Footer() {
  const { t } = useI18n();
  const { branding } = useSettings();
  const year = new Date().getFullYear();
  const cols = [
    { title: t('landing.footer.product'), links: [
      [t('landing.nav.features'), '#features'], [t('landing.nav.platform'), '#modules'],
      [t('landing.nav.roles'), '#roles'], [t('landing.nav.pricing'), '#pricing'],
    ] },
    { title: t('landing.footer.company'), links: [
      [t('landing.nav.about'), '#about'], [t('landing.footer.contact'), 'mailto:' + (branding.email || 'demo@discipolat.app')],
    ] },
    { title: t('landing.footer.legal'), links: [
      [t('landing.footer.privacy'), '#security'], [t('landing.footer.terms'), '#security'],
    ] },
  ];

  return (
    <footer id="about" className="relative z-10 border-t border-gray-200/60 dark:border-gray-800/60 backdrop-blur-xl bg-white/40 dark:bg-gray-950/40 scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14">
        <div className="grid gap-10 md:grid-cols-[1.2fr_1fr_1fr_1fr]">
          <div>
            <div className="flex items-center gap-2.5 mb-4">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
                {branding.logoUrl ? <img src={branding.logoUrl} alt="" className="w-5 h-5 object-contain" /> : <Church className="w-5 h-5 text-white" />}
              </div>
              <span className="text-lg font-bold text-gray-900 dark:text-white font-display">{branding.platformName || 'Discipolat'}</span>
            </div>
            <p className="text-sm text-gray-500 dark:text-gray-400 max-w-xs">{t('landing.footer.tagline')}</p>
            {(branding.phone || branding.email || branding.website) && (
              <div className="mt-4 space-y-1 text-xs text-gray-400 dark:text-gray-500">
                {branding.phone && <p>{branding.phone}</p>}
                {branding.email && <p>{branding.email}</p>}
                {branding.website && <p className="text-primary-600 dark:text-primary-400">{branding.website}</p>}
              </div>
            )}
          </div>
          {cols.map((col) => (
            <div key={col.title}>
              <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">{col.title}</h4>
              <ul className="space-y-2.5">
                {col.links.map(([label, href], idx) => (
                  <li key={col.title + '-' + idx}>
                    <a href={href} className="text-sm text-gray-500 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400 transition-colors">
                      {label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="mt-12 pt-6 border-t border-gray-200/60 dark:border-gray-800/60 flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-xs text-gray-400 dark:text-gray-500">© {year} {branding.platformName || 'Discipolat'}. {t('landing.footer.rights')}</p>
          <div className="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
            <span className="inline-flex items-center gap-1.5"><span className="w-1.5 h-1.5 rounded-full bg-primary-500" /> Discipolat</span>
          </div>
        </div>
      </div>
    </footer>
  );
}