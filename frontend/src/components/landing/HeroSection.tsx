import { useState } from 'react';
import {
  Church,
  Sparkles,
  ArrowRight,
  Play,
  Crown,
  Building2,
  Users,
  HandHeart,
  Heart,
  BarChart3,
  Bell,
} from 'lucide-react';
import { useSettings } from '@/contexts/SettingsContext';

const ECOSYSTEM_NODES = [
  { icon: Crown, label: 'Pasteur', color: 'from-emerald-400 to-green-600', x: 50, y: 6 },
  { icon: Building2, label: 'Responsable', color: 'from-amber-400 to-orange-600', x: 20, y: 28 },
  { icon: Users, label: 'Chef de famille', color: 'from-yellow-400 to-amber-600', x: 80, y: 28 },
  { icon: HandHeart, label: 'Faiseur', color: 'from-emerald-400 to-teal-600', x: 10, y: 55 },
  { icon: Heart, label: 'Disciple', color: 'from-rose-400 to-pink-600', x: 50, y: 55 },
  { icon: Sparkles, label: 'Membre', color: 'from-sky-400 to-blue-600', x: 90, y: 55 },
];

const STATS_MINI = [
  { icon: Users, value: '6', label: 'Espaces' },
  { icon: BarChart3, value: '100+', label: 'Modules' },
  { icon: Bell, value: 'Auto', label: 'Alertes' },
];

export default function HeroSection({ onNavigate }: { onNavigate: (id: string) => void }) {
  const { branding } = useSettings();
  const [hoveredNode, setHoveredNode] = useState<number | null>(null);

  return (
    <section className="relative min-h-screen flex flex-col overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-gray-50 via-white to-gray-50 dark:from-gray-950 dark:via-gray-900 dark:to-gray-950" />
      <div className="absolute inset-0 bg-gradient-mesh animate-gradient-shift bg-[length:200%_200%]" />

      <div className="absolute -top-32 -right-32 w-[500px] h-[500px] rounded-full bg-primary-500/[0.07] blur-[100px] animate-pulse-soft" />
      <div className="absolute top-1/3 -left-40 w-[400px] h-[400px] rounded-full bg-gold-500/[0.06] blur-[100px] animate-pulse-soft" style={{ animationDelay: '1.5s' }} />
      <div className="absolute -bottom-32 right-1/4 w-[350px] h-[350px] rounded-full bg-sky-500/[0.05] blur-[100px] animate-pulse-soft" style={{ animationDelay: '3s' }} />

      <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden>
        {[...Array(20)].map((_, i) => (
          <div
            key={i}
            className="particle"
            style={{
              width: `${2 + (i % 4) * 1.5}px`,
              height: `${2 + (i % 4) * 1.5}px`,
              left: `${(i * 5.3) % 100}%`,
              bottom: '-10px',
              animation: `particleDrift ${14 + (i % 6) * 3}s linear infinite`,
              animationDelay: `${(i * 1.7) % 12}s`,
            }}
          />
        ))}
      </div>

      <div className="relative z-10 flex-1 flex items-center">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-24 lg:py-0 w-full">
          <div className="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
            <div className="text-center lg:text-left">
              <div className="reveal reveal-visible">
                <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full glass-strong text-primary-600 dark:text-primary-400 text-xs font-medium animate-float mb-6">
                  <Sparkles className="w-3.5 h-3.5" />
                  {branding.slogan || 'La plateforme du discipolat moderne'}
                </div>
              </div>

              <div className="reveal reveal-visible" style={{ transitionDelay: '80ms' }}>
                <h1 className="text-4xl sm:text-5xl lg:text-6xl xl:text-[4.25rem] font-bold text-gray-900 dark:text-white font-display tracking-tight leading-[1.08] text-balance">
                  Le discipolat de votre église,{' '}
                  <span className="relative inline-block">
                    <span className="text-gradient">enfin connecté</span>
                    <span className="absolute -bottom-1.5 left-0 right-0 h-[3px] rounded-full bg-gradient-to-r from-primary-500 via-primary-400 to-gold-500 opacity-70" aria-hidden />
                  </span>
                </h1>
              </div>

              <div className="reveal reveal-visible" style={{ transitionDelay: '160ms' }}>
                <p className="mt-6 max-w-lg mx-auto lg:mx-0 text-base sm:text-lg text-gray-500 dark:text-gray-400 leading-relaxed">
                  Une plateforme centralisée pour accompagner chaque âme, coordonner vos
                  responsables et piloter la croissance spirituelle de votre église.
                  <span className="text-gray-700 dark:text-gray-200 font-medium"> Simple, puissant, humain.</span>
                </p>
              </div>

              <div className="reveal reveal-visible mt-8" style={{ transitionDelay: '240ms' }}>
                <div className="flex flex-wrap items-center justify-center lg:justify-start gap-3.5">
                  <button onClick={() => onNavigate('ecosystem')} className="btn-primary btn-lg group">
                    Découvrir la plateforme
                    <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
                  </button>
                  <button onClick={() => onNavigate('features')} className="btn-secondary btn-lg group">
                    <Play className="w-4 h-4 transition-transform duration-300 group-hover:scale-110" />
                    Voir les fonctionnalités
                  </button>
                </div>
              </div>

              <div className="reveal reveal-visible mt-10" style={{ transitionDelay: '320ms' }}>
                <div className="flex items-center justify-center lg:justify-start gap-6 sm:gap-8">
                  {STATS_MINI.map((s) => (
                    <div key={s.label} className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-xl bg-primary-500/[0.08] dark:bg-primary-500/[0.12] flex items-center justify-center">
                        <s.icon className="w-4 h-4 text-primary-600 dark:text-primary-400" />
                      </div>
                      <div>
                        <p className="text-lg font-bold text-gray-900 dark:text-white font-mono">{s.value}</p>
                        <p className="text-[10px] text-gray-400 dark:text-gray-500 font-medium uppercase tracking-wider">{s.label}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="reveal reveal-visible hidden lg:block" style={{ transitionDelay: '200ms' }}>
              <div className="relative w-full aspect-square max-w-[480px] mx-auto">
                <div className="absolute inset-[15%] rounded-full bg-primary-500/[0.06] dark:bg-primary-500/[0.1] blur-3xl animate-pulse-soft" />

                <svg className="absolute inset-0 w-full h-full" viewBox="0 0 100 100" aria-hidden>
                  <line x1="50" y1="14" x2="22" y2="32" className="stroke-primary-500/20" strokeWidth="0.3" strokeDasharray="2 2" />
                  <line x1="50" y1="14" x2="78" y2="32" className="stroke-primary-500/20" strokeWidth="0.3" strokeDasharray="2 2" />
                  <line x1="22" y1="34" x2="14" y2="57" className="stroke-amber-500/20" strokeWidth="0.3" strokeDasharray="2 2" />
                  <line x1="22" y1="34" x2="50" y2="57" className="stroke-amber-500/20" strokeWidth="0.3" strokeDasharray="2 2" />
                  <line x1="78" y1="34" x2="50" y2="57" className="stroke-yellow-500/20" strokeWidth="0.3" strokeDasharray="2 2" />
                  <line x1="78" y1="34" x2="90" y2="57" className="stroke-yellow-500/20" strokeWidth="0.3" strokeDasharray="2 2" />
                  <line x1="14" y1="59" x2="50" y2="57" className="stroke-emerald-500/15" strokeWidth="0.2" strokeDasharray="1.5 1.5" />
                  <line x1="90" y1="59" x2="50" y2="57" className="stroke-sky-500/15" strokeWidth="0.2" strokeDasharray="1.5 1.5" />
                </svg>

                {ECOSYSTEM_NODES.map((node, i) => (
                  <div
                    key={node.label}
                    className="absolute -translate-x-1/2 -translate-y-1/2 group cursor-pointer"
                    style={{ left: `${node.x}%`, top: `${node.y}%` }}
                    onMouseEnter={() => setHoveredNode(i)}
                    onMouseLeave={() => setHoveredNode(null)}
                  >
                    <div className={`relative w-14 h-14 sm:w-16 sm:h-16 rounded-2xl bg-gradient-to-br ${node.color} text-white flex items-center justify-center shadow-lg transition-all duration-300 ${hoveredNode === i ? 'scale-125 shadow-2xl' : 'hover:scale-110'}`}>
                      <node.icon className="w-6 h-6 sm:w-7 sm:h-7" />
                      {hoveredNode === i && (
                        <div className="absolute -bottom-9 whitespace-nowrap px-3 py-1 rounded-lg bg-gray-900 dark:bg-gray-800 text-white text-xs font-medium shadow-xl animate-fade-in z-10">
                          {node.label}
                        </div>
                      )}
                    </div>
                    <p className="text-center text-[10px] font-medium text-gray-500 dark:text-gray-400 mt-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                      {node.label}
                    </p>
                  </div>
                ))}

                <div className="absolute inset-[8%] rounded-full border border-primary-500/[0.08] dark:border-primary-500/[0.12] animate-spin-slow" />
                <div className="absolute inset-[22%] rounded-full border border-gold-500/[0.06] dark:border-gold-500/[0.1] animate-spin-reverse" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
