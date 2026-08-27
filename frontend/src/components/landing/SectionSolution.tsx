import { CheckCircle2 } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { SOLUTION_FEATURES } from '@/components/landing/landingData';

export default function Solution() {
  const { t } = useI18n();
  return (
    <section id="solutions" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-white/40 to-gray-50/40 dark:from-gray-900/30 dark:to-gray-950/30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid lg:grid-cols-2 gap-10 lg:gap-16 items-center">
          <SectionHeading
            eyebrow={t('landing.solution.eyebrow')}
            title={t('landing.solution.title')}
            titleAccent={t('landing.solution.titleAccent')}
            subtitle={t('landing.solution.subtitle')}
            align="left"
            tone="primary"
          />
          <Reveal delay={100}>
            <div className="glass-strong rounded-3xl p-6 sm:p-8 space-y-3 relative overflow-hidden">
              <div className="absolute -top-16 -right-16 w-48 h-48 rounded-full bg-primary-500/[0.06] blur-3xl" />
              {SOLUTION_FEATURES.map((k, i) => (
                <div key={k} className="flex items-center gap-3 p-3.5 rounded-2xl bg-white/70 dark:bg-gray-800/40 border border-white/60 dark:border-white/[0.05] hover:border-primary-500/30 transition-colors">
                  <span className="w-8 h-8 rounded-xl bg-primary-500/10 flex items-center justify-center flex-shrink-0 text-primary-600 dark:text-primary-400 font-bold text-sm">{i + 1}</span>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-200">{t(k)}</span>
                  <CheckCircle2 className="ml-auto w-5 h-5 text-primary-500 flex-shrink-0" />
                </div>
              ))}
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}