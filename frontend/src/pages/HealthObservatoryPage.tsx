import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { Loader2, HeartPulse, AlertTriangle, ShieldCheck, Users } from 'lucide-react';

interface Observatory {
  healthScore: number;
  totalSouls: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  predictionHorizon: string;
  soulsAtRisk: {
    soulId: string;
    nom: string;
    statut: string;
    riskScore: number;
    riskLabel: string;
    intervention: string;
  }[];
  familiesAtRisk: { familyId: string; avgRisk: number; members: number }[];
}

const RISK_STYLES: Record<string, string> = {
  CRITIQUE: 'badge-danger',
  ELEVE: 'badge-warning',
  MOYEN: 'badge-info',
  FAIBLE: 'badge-success',
};

/** Observatoire de la Santé Spirituelle — IA prédictive de décrochage. */
export default function HealthObservatoryPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['health-observatory'],
    queryFn: async () => (await api.get<Observatory>('/health-observatory')).data,
  });

  if (isLoading || !data) {
    return <Loader2 className="w-8 h-8 animate-spin text-primary-500 mx-auto mt-20" />;
  }

  const stats = [
    { label: 'Critique', value: data.criticalCount, icon: AlertTriangle, color: 'from-red-500 to-rose-600' },
    { label: 'Élevé', value: data.highCount, icon: AlertTriangle, color: 'from-orange-500 to-amber-600' },
    { label: 'Moyen', value: data.mediumCount, icon: Users, color: 'from-yellow-500 to-yellow-600' },
    { label: 'Faible', value: data.lowCount, icon: ShieldCheck, color: 'from-green-500 to-emerald-600' },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
          <HeartPulse className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Observatoire Santé Spirituelle</h1>
          <p className="page-subtitle">
            Prédiction de décrochage à {data.predictionHorizon} — interventions recommandées
          </p>
        </div>
      </div>

      {/* Score global */}
      <div className="glass-card p-6 mb-6 animate-slide-up flex items-center gap-6">
        <div className="relative w-28 h-28">
          <svg viewBox="0 0 100 100" className="w-full h-full -rotate-90">
            <circle cx="50" cy="50" r="42" fill="none" strokeWidth="10"
              className="stroke-gray-200 dark:stroke-gray-700" />
            <circle cx="50" cy="50" r="42" fill="none" strokeWidth="10" strokeLinecap="round"
              strokeDasharray={`${(data.healthScore / 100) * 264} 264`}
              className={data.healthScore >= 70 ? 'stroke-green-500' : data.healthScore >= 45 ? 'stroke-amber-500' : 'stroke-red-500'} />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-2xl font-bold text-gray-900 dark:text-gray-100">{data.healthScore}</span>
          </div>
        </div>
        <div>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Santé pastorale globale</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {data.totalSouls} âmes suivies — score = inverse du risque moyen
          </p>
        </div>
      </div>

      {/* Distribution des risques */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        {stats.map((s) => (
          <div key={s.label} className={`stat-card bg-gradient-to-br ${s.color} animate-slide-up`}>
            <s.icon className="w-5 h-5 opacity-80" />
            <p className="stat-value">{s.value}</p>
            <p className="text-xs opacity-80">{s.label}</p>
          </div>
        ))}
      </div>

      {/* Âmes à risque */}
      <div className="glass-card mb-6 divide-y divide-gray-100 dark:divide-gray-800">
        <div className="px-5 py-4">
          <h3 className="font-semibold text-gray-900 dark:text-gray-100">Âmes à risque prioritaire</h3>
        </div>
        {(data.soulsAtRisk ?? []).map((s) => (
          <div key={s.soulId} className="flex flex-col md:flex-row md:items-center justify-between gap-2 px-5 py-4">
            <div>
              <p className="font-medium text-gray-900 dark:text-gray-100">
                {s.nom}
                <span className={`badge ${RISK_STYLES[s.riskLabel] ?? 'badge-info'} ml-2`}>
                  {s.riskLabel} · {s.riskScore}/100
                </span>
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Statut : {s.statut}</p>
            </div>
            <p className="text-sm text-primary-600 dark:text-primary-400 md:max-w-md">{s.intervention}</p>
          </div>
        ))}
        {(data.soulsAtRisk ?? []).length === 0 && (
          <p className="text-center text-sm text-gray-500 py-8">Aucune âme à risque détectée. Continuez ainsi !</p>
        )}
      </div>

      {/* Familles à risque */}
      <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
        <div className="px-5 py-4">
          <h3 className="font-semibold text-gray-900 dark:text-gray-100">Familles en danger</h3>
        </div>
        {(data.familiesAtRisk ?? []).map((f) => (
          <div key={f.familyId} className="flex items-center justify-between px-5 py-4">
            <span className="text-sm text-gray-700 dark:text-gray-300">
              Famille · {f.members} membres
            </span>
            <span className="badge badge-warning">Risque moyen : {f.avgRisk}/100</span>
          </div>
        ))}
        {(data.familiesAtRisk ?? []).length === 0 && (
          <p className="text-center text-sm text-gray-500 py-8">Toutes les familles sont en bonne santé.</p>
        )}
      </div>
    </div>
  );
}
