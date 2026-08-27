import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, CheckCircle2 } from 'lucide-react';
import { useI18n } from '@/i18n';
import Reveal from '@/components/shared/Reveal';
import SectionHeading from '@/components/landing/SectionHeading';
import { ROLES } from '@/components/landing/landingData';
import { useReducedMotion } from '@/components/ui/UXComponents';

export default function RoleExplorer() {
  const { t } = useI18n();
  const [active, setActive] = useState(0);
  const reduced = useReducedMotion();
  const role = ROLES[active];

  return (
    <section id="roles" className="relative py-24 sm:py-32 overflow-hidden scroll-mt-20">
      <div className="absolute -top-24 right-0 w-96 h-96 rounded-full bg-primary-500/[0.06] blur-[120px]" />
      <div className="absolute -bottom-24 left-0 w-96 h-96 rounded-full bg-gold-500/[0.06] blur-[120px]" />

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow={t('landing.roles.eyebrow')}
          title={t('landing.roles.title')}
          titleAccent={t('landing.roles.titleAccent')}
          subtitle={t('landing.roles.subtitle')}
          tone="primary"
        />

        {/* Sélecteur de rôle */}
        <Reveal delay={80}>
          <div className="flex flex-wrap justify-center gap-2 mb-4">
            <span className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-400 dark:text-gray-500 uppercase tracking-wider">
              {t('landing.roles.iAm')}
            </span>
            {ROLES.map((r, i) => (
              <button
                key={r.id}
                onClick={() => setActive(i)}
                aria-pressed={active === i}
                className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-300 ${
                  active === i
                    ? 'glass-strong text-gray-900 dark:text-white shadow-glow ring-1 ring-primary-500/30'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-white/60 dark:hover:bg-gray-800/40'
                }`}
              >
                <span className={`w-2 h-2 rounded-full ${active === i ? 'bg-primary-500' : 'bg-gray-400 dark:bg-gray-600'}`} />
                {t(r.roleKey)}
              </button>
            ))}
          </div>
        </Reveal>

        {/* Détail du rôle actif */}
        <Reveal delay={120}>
          <div className="relative" key={role.id}>
            <div className="absolute -inset-px rounded-[1.75rem] bg-gradient-to-br from-primary-500/20 via-transparent to-gold-500/20 opacity-40" />
            <div className={`relative grid lg:grid-cols-2 gap-8 glass-strong rounded-[1.75rem] p-8 sm:p-10 lg:p-12 overflow-hidden transition-all duration-500 ${reduced ? '' : 'animate-slide-up'}`}>
              <div className="absolute -top-20 -right-20 w-64 h-64 rounded-full" style={{ background: 'radial-gradient(circle, rgba(34,197,94,0.08), transparent 70%)' }} />
              <div className="relative">
                <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${role.gradient} text-white flex items-center justify-center shadow-xl mb-6 transition-transform duration-500`}>
                  {(() => { const I = role.icon; return <I className="w-8 h-8" />; })()}
                </div>
                <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border border-gray-200 dark:border-white/10 text-gray-600 dark:text-gray-300 bg-white/60 dark:bg-gray-800/40 mb-4">
                  {t(role.roleKey)}
                </span>
                <h3 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white font-display mb-4">
                  {t(role.roleKey)}
                </h3>
                <p className="text-base text-gray-500 dark:text-gray-400 leading-relaxed mb-8">{t(role.descKey)}</p>

                <div className="space-y-3">
                  {role.featKeys.map((fk, fi) => (
                    <div key={fk} className="flex items-center gap-3 p-3 rounded-xl bg-white/60 dark:bg-gray-800/40 border border-white/60 dark:border-white/[0.05]" style={{ transitionDelay: `${fi * 40}ms` }}>
                      <CheckCircle2 className="w-5 h-5 text-primary-500 flex-shrink-0" />
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-200">{t(fk)}</span>
                    </div>
                  ))}
                </div>

                <Link to="/login" className={`btn-primary btn-lg group mt-8 bg-gradient-to-r ${role.gradient} border-0 hover:brightness-110`}>
                  {t('landing.roles.openSpace')}
                  <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
                </Link>
              </div>

              <div className="relative hidden sm:block">
                <RoleDashboard roleId={role.id} gradient={role.gradient} />
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function RoleDashboard({ roleId, gradient }: { roleId: string; gradient: string }) {
  const byRole: Record<string, { kpis: [string, string][]; bars: number[]; label: string }> = {
    pasteur: {
      kpis: [['Âmes', '248'], ['Familles', '21'], ['Présence', '94%']],
      bars: [55, 70, 62, 82, 74, 92],
      label: 'Vue du pasteur',
    },
    responsable: {
      kpis: [['Dépts', '6'], ['Membres', '58'], ['Rapports', '12']],
      bars: [40, 55, 80, 65, 70, 85],
      label: 'Vue du responsable',
    },
    chef: {
      kpis: [['Famille', '9'], ['Disciples', '24'], ['Rapports', '4']],
      bars: [60, 45, 72, 58, 66, 80],
      label: 'Vue du chef de famille',
    },
    faiseur: {
      kpis: [['Disciples', '6'], ['Notes', '18'], ['Suivis', '5/5']],
      bars: [50, 66, 58, 74, 70, 88],
      label: 'Espace du faiseur',
    },
    membre: {
      kpis: [['Parcours', '62%'], ['Événements', '3'], ['Prières', '5']],
      bars: [70, 60, 78, 66, 84, 90],
      label: 'Mon espace membre',
    },
  };
  const d = byRole[roleId] ?? byRole.pasteur;
  const max = Math.max(...d.bars);

  return (
    <div className="glass-strong rounded-2xl p-5 shadow-xl animate-float-slow" key={roleId}>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <span className={`w-2.5 h-2.5 rounded-full bg-gradient-to-br ${gradient}`} />
          <p className="text-xs font-semibold text-gray-800 dark:text-gray-100">{d.label}</p>
        </div>
        <span className="text-[10px] font-medium text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full">● En direct</span>
      </div>

      <div className="grid grid-cols-3 gap-2 mb-5">
        {d.kpis.map(([k, v]) => (
          <div key={k} className="rounded-xl bg-white/60 dark:bg-gray-800/50 border border-white/60 dark:border-white/[0.05] p-2.5 text-center">
            <p className="text-lg font-bold text-gray-900 dark:text-white font-mono">{v}</p>
            <p className="text-[9px] text-gray-400 dark:text-gray-500 uppercase tracking-wide">{k}</p>
          </div>
        ))}
      </div>

      <div className="flex items-end gap-1.5 h-24">
        {d.bars.map((b, i) => (
          <div key={i} className="flex-1 rounded-t-md bg-gradient-to-t from-gray-200 to-gray-300 dark:from-gray-700 dark:to-gray-600 overflow-hidden">
            <div
              className={`w-full bg-gradient-to-t ${gradient} transition-all duration-700 ease-out`}
              style={{ height: `${(b / max) * 100}%` }}
            />
          </div>
        ))}
      </div>
    </div>
  );
}