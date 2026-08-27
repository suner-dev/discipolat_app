import { ChevronRight, Heart, FileBarChart, BarChart3, Building2, AlertTriangle, Shield } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { MODULES } from '@/components/landing/landingData';

const FEATURES = [
  { icon: Heart, tKey: 'landing.features.discipolat', dKey: 'landing.features.discipolatDesc', g: 'from-rose-500 to-pink-600' },
  { icon: FileBarChart, tKey: 'landing.features.reporting', dKey: 'landing.features.reportingDesc', g: 'from-emerald-500 to-teal-600' },
  { icon: BarChart3, tKey: 'landing.features.dashboard', dKey: 'landing.features.dashboardDesc', g: 'from-blue-500 to-indigo-600' },
  { icon: Building2, tKey: 'landing.features.org', dKey: 'landing.features.orgDesc', g: 'from-amber-500 to-orange-600' },
  { icon: AlertTriangle, tKey: 'landing.features.alerts', dKey: 'landing.features.alertsDesc', g: 'from-red-500 to-rose-600' },
  { icon: Shield, tKey: 'landing.features.security', dKey: 'landing.features.securityDesc', g: 'from-violet-500 to-purple-600' },
];

export function Features() {
  const { t } = useI18n();
  return (
    <section id="features" className="py-24 sm:py-32 scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.features.eyebrow')}
          title={`${t('landing.features.title')} `}
          titleAccent={t('landing.features.titleAccent')}
          subtitle={t('landing.features.subtitle')}
          tone="primary"
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {FEATURES.map((f, i) => (
            <Reveal key={f.tKey} delay={i * 70}>
              <div className="group glass-card-interactive p-7 h-full transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl cursor-pointer">
                <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${f.g} text-white flex items-center justify-center shadow-lg mb-5 transition-transform duration-300 group-hover:scale-110 group-hover:-rotate-6`}>
                  <f.icon className="w-6 h-6" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">{t(f.tKey)}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{t(f.dKey)}</p>
                <div className="mt-5 flex items-center gap-1 text-xs font-medium text-primary-600 dark:text-primary-400 transition-all duration-300 sm:opacity-0 sm:group-hover:opacity-100 sm:translate-x-[-6px] sm:group-hover:translate-x-0">
                  {t('landing.features.discover')} <ChevronRight className="w-3.5 h-3.5" />
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

export function Modules() {
  const { t } = useI18n();
  return (
    <section id="modules" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.modules.eyebrow')}
          title={t('landing.modules.title')}
          titleAccent={t('landing.modules.titleAccent')}
          subtitle={t('landing.modules.subtitle')}
          tone="gold"
        />
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {MODULES.map((m, i) => (
            <Reveal key={m.nameKey} delay={i * 50}>
              <div className="glass-card p-5 h-full text-center group hover:-translate-y-1 transition-all duration-300 cursor-default">
                <div className={`w-11 h-11 rounded-xl bg-gradient-to-br ${m.gradient} text-white flex items-center justify-center mx-auto mb-3 shadow-md shadow-black/5 transition-transform duration-300 group-hover:scale-110`}>
                  <m.icon className="w-5 h-5" />
                </div>
                <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-1">{t(m.nameKey)}</h4>
                <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">{t(m.descKey)}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}