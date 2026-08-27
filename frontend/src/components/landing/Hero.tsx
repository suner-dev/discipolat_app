import { useRef, useState } from 'react';
import {
  ArrowRight, Sparkles, Play, Users, Bell, ChevronDown, LayoutDashboard,
  Crown, Heart, HandHeart, BellRing, BarChart3, TrendingUp, Building2,
} from 'lucide-react';
import { useI18n } from '@/i18n';
import { useReducedMotion } from '@/components/ui/UXComponents';

interface HeroProps { onNavigate: (id: string) => void; onDemo: () => void; }

const STATS_MINI = [
  { icon: Users, value: '6', key: 'landing.hero.statSpaces' },
  { icon: LayoutDashboard, value: '100+', key: 'landing.hero.statModules' },
  { icon: Bell, value: 'Auto', key: 'landing.hero.statAlerts' },
];

export default function Hero({ onNavigate, onDemo }: HeroProps) {
  const { t } = useI18n();
  const reduced = useReducedMotion();
  const frame = useRef<HTMLDivElement>(null);
  const [tilt, setTilt] = useState({ rx: 0, ry: 0 });

  const handleMove = (e: React.MouseEvent) => {
    if (reduced) return;
    const el = frame.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    setTilt({ rx: ((e.clientY - r.top) / r.height - 0.5) * -8, ry: ((e.clientX - r.left) / r.width - 0.5) * 10 });
  };

  return (
    <section id="hero" className="relative min-h-screen flex flex-col overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-gray-50 via-white to-gray-100 dark:from-emerald-950/40 dark:via-gray-950 dark:to-gray-950" />
      <div className="absolute inset-0 bg-gradient-mesh animate-gradient-shift bg-[length:200%_200%]" />
      <div className="absolute -top-40 -right-32 w-[520px] h-[520px] rounded-full bg-primary-500/[0.10] blur-[110px] animate-pulse-soft" />
      <div className="absolute top-1/3 -left-44 w-[440px] h-[440px] rounded-full bg-gold-500/[0.08] blur-[110px] animate-pulse-soft" style={{ animationDelay: '1.4s' }} />
      <div className="absolute -bottom-40 right-1/4 w-[400px] h-[400px] rounded-full bg-sky-500/[0.07] blur-[110px] animate-pulse-soft" style={{ animationDelay: '2.8s' }} />

      <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden>
        {[...Array(14)].map((_, i) => (
          <div key={i} className="particle" style={{
            width: `${2 + (i % 4) * 1.5}px`, height: `${2 + (i % 4) * 1.5}px`,
            left: `${(i * 7.1) % 100}%`, bottom: '-10px',
            animation: `particleDrift ${14 + (i % 6) * 3}s linear infinite`, animationDelay: `${(i * 1.7) % 12}s`,
          }} />
        ))}
      </div>

      <div className="relative z-10 flex-1 flex items-center">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 lg:py-0 w-full">
          <div className="grid lg:grid-cols-2 gap-12 lg:gap-10 items-center">
            <div className="text-center lg:text-left">
              <div className="reveal reveal-visible inline-flex items-center gap-2 px-4 py-1.5 rounded-full glass-strong text-primary-600 dark:text-primary-400 text-xs font-medium animate-float mb-6">
                <Sparkles className="w-3.5 h-3.5" /> {t('landing.hero.eyebrow')}
              </div>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl xl:text-[4.25rem] font-bold text-gray-900 dark:text-white font-display tracking-tight leading-[1.06] text-balance">
                {t('landing.hero.headline')}{' '}
                <span className="relative inline-block">
                  <span className="text-gradient">{t('landing.hero.headlineAccent')}</span>
                  <span className="absolute -bottom-1.5 left-0 right-0 h-[3px] rounded-full bg-gradient-to-r from-primary-500 via-primary-400 to-gold-500 opacity-70" aria-hidden />
                </span>
              </h1>
              <p className="mt-6 max-w-xl mx-auto lg:mx-0 text-base sm:text-lg text-gray-500 dark:text-gray-400 leading-relaxed">
                {t('landing.hero.subtitle')}{' '}
                <span className="text-gray-900 dark:text-white font-medium">{t('landing.hero.subtitleEmphasis')}</span>
              </p>
              <div className="mt-8 flex flex-wrap items-center justify-center lg:justify-start gap-3.5">
                <button onClick={() => onNavigate('ecosystem')} className="btn-primary btn-lg group">
                  {t('landing.hero.ctaDiscover')}
                  <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
                </button>
                <button onClick={onDemo} className="btn-secondary btn-lg group">
                  <Play className="w-4 h-4 transition-transform duration-300 group-hover:scale-110" /> {t('landing.hero.ctaDemo')}
                </button>
              </div>
              <div className="mt-10 flex items-center justify-center lg:justify-start gap-6 sm:gap-8">
                {STATS_MINI.map((s) => (
                  <div key={s.key} className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-primary-500/[0.08] dark:bg-primary-500/[0.12] flex items-center justify-center">
                      <s.icon className="w-4 h-4 text-primary-600 dark:text-primary-400" />
                    </div>
                    <div>
                      <p className="text-lg font-bold text-gray-900 dark:text-white font-mono">{s.value}</p>
                      <p className="text-[10px] text-gray-400 dark:text-gray-500 font-medium uppercase tracking-wider">{t(s.key)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div ref={frame} onMouseMove={handleMove} onMouseLeave={() => setTilt({ rx: 0, ry: 0 })} className="relative hidden lg:block [perspective:1200px]">
              <HeroVisual tilt={tilt} reduced={reduced} />
            </div>
          </div>
        </div>
      </div>

      <button onClick={() => onNavigate('problem')} className="relative z-10 mx-auto mb-6 pb-2 flex flex-col items-center gap-1.5 text-gray-400 dark:text-gray-500 hover:text-primary-500 transition-colors" aria-label={t('landing.hero.scroll')}>
        <span className="text-[10px] uppercase tracking-[0.2em] font-medium">{t('landing.hero.scroll')}</span>
        <ChevronDown className="w-4 h-4 animate-bounce" />
      </button>
    </section>
  );
}

function HeroVisual({ tilt, reduced }: { tilt: { rx: number; ry: number }; reduced: boolean }) {
  const frameStyle: React.CSSProperties = reduced
    ? {}
    : { transform: `rotateX(${tilt.rx}deg) rotateY(${tilt.ry}deg)`, transition: 'transform 0.15s ease-out' };

  return (
    <div className="relative w-full aspect-[4/5] max-w-[480px] mx-auto" style={frameStyle}>
      {/* Halo */}
      <div className="absolute inset-[12%] rounded-full bg-primary-500/[0.07] dark:bg-primary-500/[0.12] blur-3xl animate-pulse-soft" />

      {/* Lignes de connexion */}
      <svg className="absolute inset-0 w-full h-full" viewBox="0 0 100 100" aria-hidden>
        <line x1="50" y1="18" x2="14" y2="38" className="stroke-primary-500/25" strokeWidth="0.25" strokeDasharray="1.5 1.5" />
        <line x1="50" y1="18" x2="86" y2="38" className="stroke-primary-500/25" strokeWidth="0.25" strokeDasharray="1.5 1.5" />
        <line x1="14" y1="40" x2="28" y2="70" className="stroke-gold-500/20" strokeWidth="0.2" strokeDasharray="1.5 1.5" />
        <line x1="86" y1="40" x2="70" y2="70" className="stroke-gold-500/20" strokeWidth="0.2" strokeDasharray="1.5 1.5" />
        <line x1="28" y1="72" x2="70" y2="72" className="stroke-emerald-500/15" strokeWidth="0.2" strokeDasharray="1.5 1.5" />
      </svg>

      {/* Nœud central — Pasteur / vision */}
      <div className="absolute left-1/2 top-0 -translate-x-1/2 flex flex-col items-center">
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-500 to-emerald-600 text-white flex items-center justify-center shadow-xl shadow-primary-500/30 animate-float ring-4 ring-white/40 dark:ring-white/10">
          <Crown className="w-7 h-7" />
        </div>
        <p className="mt-2 text-xs font-semibold text-gray-700 dark:text-gray-200">Pasteur</p>
      </div>

      {/* Cartes flottantes satellites */}
      <FloatingCard icon={Building2} color="from-amber-500 to-orange-600" className="absolute left-0 top-[30%]" label="Responsables" />
      <FloatingCard icon={HandHeart} color="from-emerald-500 to-teal-600" className="absolute right-0 top-[30%]" label="Faiseurs" />
      <FloatingCard icon={Heart} color="from-rose-500 to-pink-600" className="absolute left-[6%] bottom-[18%]" label="Disciples" />
      <FloatingCard icon={Users} color="from-sky-500 to-blue-600" className="absolute right-[6%] bottom-[10%]" label="Membres" />

      {/* Carte dashboard centrale */}
      <div className="absolute left-1/2 top-[44%] -translate-x-1/2 w-[66%] glass-strong rounded-2xl p-4 shadow-glow-lg animate-float-slow" style={{ animationDelay: '0.4s' }}>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-primary-500/15 flex items-center justify-center"><BarChart3 className="w-4 h-4 text-primary-600 dark:text-primary-400" /></div>
            <p className="text-xs font-semibold text-gray-800 dark:text-gray-100">Vue d&apos;ensemble</p>
          </div>
          <span className="inline-flex items-center gap-1 text-[10px] font-medium text-emerald-600 dark:text-emerald-400">
            <TrendingUp className="w-3 h-3" /> +12%
          </span>
        </div>
        <div className="grid grid-cols-3 gap-2">
          {[['Souls', '248'], ['Familles', '21'], ['Présents', '94%']].map(([k, v]) => (
            <div key={k} className="rounded-xl bg-white/60 dark:bg-gray-800/50 border border-white/50 dark:border-white/[0.06] p-2 text-center">
              <p className="text-base font-bold text-gray-900 dark:text-white font-mono">{v}</p>
              <p className="text-[9px] text-gray-400 dark:text-gray-500 uppercase tracking-wide">{k}</p>
            </div>
          ))}
        </div>
        <div className="mt-3 h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
          <div className="h-full w-3/4 rounded-full bg-gradient-to-r from-primary-500 to-gold-500" />
        </div>
      </div>

      {/* Notification flottante */}
      <div className="absolute right-0 bottom-[38%] glass-strong rounded-xl px-3 py-2 flex items-center gap-2 shadow-lg animate-float" style={{ animationDelay: '1.2s' }}>
        <span className="w-7 h-7 rounded-lg bg-rose-500/15 flex items-center justify-center"><BellRing className="w-3.5 h-3.5 text-rose-500" /></span>
        <div>
          <p className="text-[10px] font-medium text-gray-800 dark:text-gray-100">Alerte · Décrochage</p>
          <p className="text-[9px] text-gray-400 dark:text-gray-500">Famille Bethel</p>
        </div>
      </div>

      {/* Anneaux orbitaux */}
      <div className="absolute inset-[10%] rounded-full border border-primary-500/[0.08] dark:border-primary-500/[0.12] animate-spin-slow" />
      <div className="absolute inset-[24%] rounded-full border border-gold-500/[0.06] dark:border-gold-500/[0.1] animate-spin-reverse" />
    </div>
  );
}

function FloatingCard({ icon: Icon, label, color, className }: { icon: any; label: string; color: string; className: string }) {
  return (
    <div className={`absolute flex flex-col items-center ${className} group cursor-default animate-float`} style={{ animationDelay: `${Math.random() * 1.5}s` }}>
      <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${color} text-white flex items-center justify-center shadow-lg shadow-black/10 group-hover:scale-110 transition-transform duration-300`}>
        <Icon className="w-5 h-5" />
      </div>
      <p className="mt-1.5 text-[10px] font-medium text-gray-500 dark:text-gray-400">{label}</p>
    </div>
  );
}