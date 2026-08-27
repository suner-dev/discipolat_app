import { useState } from 'react';
import { CheckCircle2, X, ArrowLeftRight } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { BEFORE, AFTER } from '@/components/landing/landingData';

export default function BeforeAfter() {
  const { t } = useI18n();
  const [flip, setFlip] = useState(false);

  return (
    <section id="before-after" className="py-24 sm:py-32 scroll-mt-20 bg-gradient-to-b from-white to-gray-50/50 dark:from-gray-950 dark:to-gray-900/50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.beforeAfter.eyebrow')}
          title={t('landing.beforeAfter.title')}
          titleAccent={t('landing.beforeAfter.titleAccent')}
          tone="primary"
        />

        <div className="grid lg:grid-cols-2 gap-6 max-w-5xl mx-auto">
          <Reveal delay={80}>
            <div className="rounded-3xl p-7 sm:p-8 bg-red-500/[0.04] dark:bg-red-500/[0.06] border border-red-500/10 h-full">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center">
                  <X className="w-5 h-5 text-red-500" />
                </div>
                <h3 className="text-lg font-bold text-gray-900 dark:text-white">{t('landing.beforeAfter.beforeTitle')}</h3>
              </div>
              <div className="space-y-3">
                {BEFORE.map((item, i) => (
                  <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/60 dark:bg-gray-800/40">
                    <X className="w-4 h-4 text-red-400 flex-shrink-0" />
                    <span className="text-sm text-gray-600 dark:text-gray-300">{t(item)}</span>
                  </div>
                ))}
              </div>
            </div>
          </Reveal>

          <Reveal delay={160}>
            <div className="rounded-3xl p-7 sm:p-8 bg-primary-500/[0.04] dark:bg-primary-500/[0.06] border border-primary-500/10 h-full relative overflow-hidden">
              <div className="absolute -top-12 -right-12 w-40 h-40 rounded-full bg-primary-500/[0.06] blur-3xl" />
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-primary-500/10 flex items-center justify-center">
                  <CheckCircle2 className="w-5 h-5 text-primary-500" />
                </div>
                <h3 className="text-lg font-bold text-gray-900 dark:text-white">{t('landing.beforeAfter.afterTitle')}</h3>
              </div>
              <div className="space-y-3">
                {AFTER.map((item, i) => (
                  <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/60 dark:bg-gray-800/40">
                    <CheckCircle2 className="w-4 h-4 text-primary-500 flex-shrink-0" />
                    <span className="text-sm text-gray-700 dark:text-gray-200">{t(item)}</span>
                  </div>
                ))}
              </div>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}