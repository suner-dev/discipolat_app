import { ChevronRight } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { ECOSYSTEM } from '@/components/landing/landingData';

export default function Ecosystem() {
  const { t } = useI18n();
  return (
    <section id="ecosystem" className="relative py-24 sm:py-32 overflow-hidden scroll-mt-20">
      <div className="absolute inset-0 bg-white/40 dark:bg-gray-900/40 backdrop-blur-sm" />
      <div className="absolute -bottom-24 -right-24 w-80 h-80 rounded-full bg-emerald-500/[0.05] blur-[110px]" />
      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.ecosystem.eyebrow')}
          title={t('landing.ecosystem.title')}
          titleAccent={t('landing.ecosystem.titleAccent')}
          subtitle={t('landing.ecosystem.subtitle')}
          tone="emerald"
        />
        <Reveal delay={100}>
          <div className="max-w-3xl mx-auto">
            {ECOSYSTEM.map((item, i, arr) => (
              <div key={item.levelKey} className="relative">
                <div className="flex items-center gap-4 sm:gap-6 p-4 sm:p-5 rounded-2xl glass-card hover:-translate-y-0.5 transition-all duration-200 group">
                  <div className={`w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-gradient-to-br ${item.color} text-white flex items-center justify-center shadow-lg transition-transform duration-300 group-hover:scale-110 flex-shrink-0`}>
                    <item.icon className="w-6 h-6" />
                  </div>
                  <div className="flex-1">
                    <h4 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white">{t(item.levelKey)}</h4>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{t(item.descKey)}</p>
                  </div>
                  <ChevronRight className="w-5 h-5 text-gray-300 dark:text-gray-600 hidden sm:block" />
                </div>
                {i < arr.length - 1 && (
                  <div className="flex justify-center py-1">
                    <div className="w-0.5 h-6 bg-gradient-to-b from-primary-500/30 to-primary-500/10 rounded-full" />
                  </div>
                )}
              </div>
            ))}
          </div>
        </Reveal>
      </div>
    </section>
  );
}