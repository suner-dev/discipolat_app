import { X } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { PROBLEMS } from '@/components/landing/landingData';

export default function Problem() {
  const { t } = useI18n();
  return (
    <section id="problem" className="relative py-24 sm:py-32 overflow-hidden scroll-mt-20">
      <div className="absolute -top-32 -left-32 w-80 h-80 rounded-full bg-rose-500/[0.05] blur-[110px]" />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.problem.eyebrow')}
          title={t('landing.problem.title')}
          titleAccent={t('landing.problem.titleAccent')}
          subtitle={t('landing.problem.subtitle')}
          tone="violet"
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {PROBLEMS.map((p, i) => (
            <Reveal key={p.titleKey} delay={i * 70}>
              <div className="group glass-card p-6 h-full hover:-translate-y-1 transition-all duration-300">
                <div className={`w-11 h-11 rounded-xl bg-gradient-to-br ${p.gradient} text-white flex items-center justify-center mb-4 shadow-lg shadow-black/5 transition-transform duration-300 group-hover:scale-110 group-hover:-rotate-6`}>
                  <X className="w-5 h-5 opacity-90" />
                </div>
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">{t(p.titleKey)}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{t(p.descKey)}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}