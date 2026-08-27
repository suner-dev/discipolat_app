import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { STEPS } from '@/components/landing/landingData';

export default function HowItWorks() {
  const { t } = useI18n();
  return (
    <section id="how-it-works" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.how.eyebrow')}
          title={t('landing.how.title')}
          titleAccent={t('landing.how.titleAccent')}
          subtitle={t('landing.how.subtitle')}
          tone="primary"
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {STEPS.map((s, i) => (
            <Reveal key={s.nameKey} delay={i * 90}>
              <div className="group relative glass-card p-7 h-full hover:-translate-y-1.5 transition-all duration-300 overflow-hidden">
                <div className={`absolute -top-3 -right-3 text-6xl font-black text-gray-900/[0.04] dark:text-white/[0.05] font-display group-hover:text-primary-500/10 transition-colors duration-300`}>
                  {String(i + 1).padStart(2, '0')}
                </div>
                <div className={`relative w-14 h-14 rounded-2xl bg-gradient-to-br ${s.gradient} text-white flex items-center justify-center shadow-lg mb-5 transition-transform duration-300 group-hover:scale-110`}>
                  <s.icon className="w-6 h-6" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">{t(s.nameKey)}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{t(s.descKey)}</p>
                {i < STEPS.length - 1 && (
                  <div className="hidden lg:block absolute top-1/2 -right-4 w-8 border-t border-dashed border-primary-500/30 -translate-y-1/2" />
                )}
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}