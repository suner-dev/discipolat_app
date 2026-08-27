import { useState } from 'react';
import { Smartphone, Tablet, Monitor } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';

type Device = 'phone' | 'tablet' | 'desktop';

export default function MultiDevice() {
  const { t } = useI18n();
  const [device, setDevice] = useState<Device>('desktop');
  const devices: { id: Device; icon: typeof Smartphone; key: string }[] = [
    { id: 'phone', icon: Smartphone, key: 'landing.device.phone' },
    { id: 'tablet', icon: Tablet, key: 'landing.device.tablet' },
    { id: 'desktop', icon: Monitor, key: 'landing.device.desktop' },
  ];

  return (
    <section id="device" className="py-20 sm:py-28 scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Reveal>
          <div className="glass-strong rounded-3xl p-8 sm:p-14 relative overflow-hidden">
            <div className="absolute -top-20 -right-20 w-64 h-64 rounded-full bg-primary-500/[0.05] blur-3xl" />
            <div className="absolute -bottom-24 -left-16 w-72 h-72 rounded-full bg-gold-500/[0.05] blur-3xl" />

            <div className="relative">
              <h2 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-gray-900 dark:text-white font-display mb-3 text-balance">
                {t('landing.device.title')}
              </h2>
              <p className="text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto mb-7">
                {t('landing.device.subtitle')}
              </p>

              <div className="inline-flex flex-wrap items-center gap-1 p-1 rounded-2xl bg-gray-100/80 dark:bg-gray-800/60 mb-8" role="tablist" aria-label="Appareil">
                {devices.map((d) => (
                  <button
                    key={d.id}
                    role="tab"
                    aria-selected={device === d.id}
                    onClick={() => setDevice(d.id)}
                    className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-300 ${device === d.id ? 'bg-white dark:bg-gray-900 text-primary-600 dark:text-primary-400 shadow-md' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'}`}
                  >
                    <d.icon className="w-4 h-4" /> {t(d.key)}
                  </button>
                ))}
              </div>

              {/* Mockup animé */}
              <div className="flex justify-center" key={device}>
                <div className={`${device === 'phone' ? 'w-60 sm:w-64' : device === 'tablet' ? 'w-[22rem]' : 'w-full max-w-2xl'} transition-all duration-500`}>
                  <div className={`glass rounded-2xl border border-white/30 dark:border-white/10 shadow-2xl p-4 ${device === 'phone' ? 'rounded-[2rem]' : ''} animate-float-slow`}>
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full bg-primary-500" />
                        <span className="text-xs font-semibold text-gray-800 dark:text-gray-100">Discipolat</span>
                      </div>
                      <span className="text-[10px] text-emerald-500 font-medium">● En ligne</span>
                    </div>
                    <div className="grid grid-cols-3 gap-2 mb-3">
                      {[['Souls', '248'], ['Familles', '21'], ['Rapports', '9']].map(([k, v]) => (
                        <div key={k} className="rounded-xl bg-white/60 dark:bg-gray-800/50 border border-white/50 dark:border-white/[0.06] p-2 text-center">
                          <p className="text-sm font-bold text-gray-900 dark:text-white font-mono">{v}</p>
                          <p className="text-[8px] text-gray-400 dark:text-gray-500 uppercase">{k}</p>
                        </div>
                      ))}
                    </div>
                    <div className="flex items-end gap-1.5 h-16">
                      {[50, 70, 45, 80, 62, 90, 74].map((b, i) => (
                        <div key={i} className="flex-1 rounded-t bg-gradient-to-t from-primary-500 to-emerald-400" style={{ height: `${b}%`, opacity: 0.85 }} />
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}