import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import { STATS } from '@/components/landing/landingData';
import { useCountUp } from '@/hooks/useCountUp';

export default function StatsSection() {
  const { t } = useI18n();
  return (
    <section id="stats" className="py-20 sm:py-28 scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {STATS.map((s, i) => (
            <Reveal key={s.key} delay={i * 80}>
              <div className="glass-card p-6 sm:p-8 text-center group hover:-translate-y-1 transition-all duration-300 relative overflow-hidden">
                <div className="absolute inset-x-0 -bottom-8 h-16 bg-primary-500/[0.05] blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                <div className="relative">
                  <div className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-mono mb-2">
                    <CountUp end={s.value} suffix={s.suffix} />
                  </div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">{t(s.key)}</p>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
        <Reveal delay={120}>
          <p className="mt-6 text-center text-xs text-gray-400 dark:text-gray-500">
            💠 {t('landing.stats.demo')}
          </p>
        </Reveal>
      </div>
    </section>
  );
}

function CountUp({ end, suffix = '' }: { end: number; suffix?: string }) {
  const { value, ref } = useCountUp(end);
  return <span ref={ref}>{value.toLocaleString('fr-FR')}{suffix}</span>;
}