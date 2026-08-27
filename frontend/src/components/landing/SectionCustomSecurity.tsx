import { Sparkles } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { CUSTOMIZATION, SECURITY } from '@/components/landing/landingData';

export function Customization() {
  const { t } = useI18n();
  return (
    <section id="customization" className="py-24 sm:py-32 scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid lg:grid-cols-2 gap-10 lg:gap-16 items-center">
          <div>
            <SectionHeading
              eyebrow={t('landing.custom.eyebrow')}
              title={t('landing.custom.title')}
              titleAccent={t('landing.custom.titleAccent')}
              subtitle={t('landing.custom.desc')}
              align="left"
              tone="gold"
            />
            <Reveal>
              <p className="flex items-center gap-2 text-sm font-medium text-gold-600 dark:text-gold-400 -mt-6">
                <Sparkles className="w-4 h-4" /> {t('landing.custom.strap')}
              </p>
            </Reveal>
          </div>
          <Reveal delay={120}>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {CUSTOMIZATION.map((c, i) => (
                <div key={i} className="flex items-center gap-3 p-4 rounded-xl glass-card hover:-translate-y-0.5 transition-all duration-200">
                  <div className="w-10 h-10 rounded-xl bg-gold-500/[0.08] flex items-center justify-center flex-shrink-0">
                    <c.icon className="w-5 h-5 text-gold-600 dark:text-gold-400" />
                  </div>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-200">{t(c.textKey)}</span>
                </div>
              ))}
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}

export function Security() {
  const { t } = useI18n();
  return (
    <section id="security" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.security.eyebrow')}
          title={t('landing.security.title')}
          titleAccent={t('landing.security.titleAccent')}
          subtitle={t('landing.security.subtitle')}
          tone="violet"
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {SECURITY.map((s, i) => (
            <Reveal key={s.titleKey} delay={i * 70}>
              <div className="glass-card p-6 h-full group hover:-translate-y-1 transition-all duration-300">
                <div className={`w-11 h-11 rounded-xl bg-gradient-to-br ${s.gradient} text-white flex items-center justify-center mb-4 shadow-lg shadow-black/5 transition-transform duration-300 group-hover:scale-110`}>
                  <s.icon className="w-5 h-5" />
                </div>
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">{t(s.titleKey)}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{t(s.descKey)}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}