import { useI18n } from '@/i18n';
import { BarChart3, TrendingUp, TrendingDown, Target, ArrowUpRight } from 'lucide-react';

interface DepartmentKpi {
  id: string;
  department: string;
  name: string;
  target: number;
  current: number;
  unit: string;
  trend: 'up' | 'down' | 'stable';
}

const MOCK_KPIS: DepartmentKpi[] = [
  { id: '1', department: 'Évangélisation', name: 'Nouvelles conversions', target: 20, current: 14, unit: 'personnes', trend: 'up' },
  { id: '2', department: 'Louange', name: 'Heures de répétition', target: 8, current: 6, unit: 'heures/semaine', trend: 'stable' },
  { id: '3', department: 'Jeunesse', name: 'Participants activité', target: 50, current: 42, unit: 'jeunes', trend: 'up' },
  { id: '4', department: 'Diaconie', name: 'Visites à domicile', target: 30, current: 18, unit: 'visites', trend: 'down' },
  { id: '5', department: 'Missions', name: 'Dons collectés', target: 500000, current: 320000, unit: 'FCFA', trend: 'up' },
  { id: '6', department: 'Enseignement', name: 'Sessions de formation', target: 12, current: 10, unit: 'sessions', trend: 'stable' },
];

export default function DepartmentKPIsPage() {
  const { t } = useI18n();

  const progressPercent = (current: number, target: number) => Math.min(Math.round((current / target) * 100), 100);
  const progressColor = (pct: number) => pct >= 80 ? 'bg-green-500' : pct >= 50 ? 'bg-amber-500' : 'bg-red-500';

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
          <BarChart3 className="w-7 h-7 text-teal-500" />
          {t('nav.departmentKpis') ?? 'KPIs Départementaux'}
        </h1>
        <p className="text-sm text-gray-500 mt-1">Suivi des objectifs par département</p>
      </div>

      {/* Overview cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">Départements suivis</p>
          <p className="text-2xl font-bold text-gray-900 dark:text-white mt-1">{new Set(MOCK_KPIS.map(k => k.department)).size}</p>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">KPIs en hausse</p>
          <p className="text-2xl font-bold text-green-500 mt-1">{MOCK_KPIS.filter(k => k.trend === 'up').length}</p>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">Objectifs atteints (≥80%)</p>
          <p className="text-2xl font-bold text-teal-500 mt-1">{MOCK_KPIS.filter(k => progressPercent(k.current, k.target) >= 80).length}</p>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">KPIs en baisse</p>
          <p className="text-2xl font-bold text-red-500 mt-1">{MOCK_KPIS.filter(k => k.trend === 'down').length}</p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {MOCK_KPIS.map((kpi) => {
          const pct = progressPercent(kpi.current, kpi.target);
          return (
            <div key={kpi.id} className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs text-gray-500 font-medium">{kpi.department}</p>
                  <h3 className="font-semibold text-gray-900 dark:text-white mt-1">{kpi.name}</h3>
                </div>
                <span className={`flex items-center gap-1 text-xs font-medium ${kpi.trend === 'up' ? 'text-green-500' : kpi.trend === 'down' ? 'text-red-500' : 'text-gray-400'}`}>
                  {kpi.trend === 'up' ? <TrendingUp className="w-3 h-3" /> : kpi.trend === 'down' ? <TrendingDown className="w-3 h-3" /> : <Target className="w-3 h-3" />}
                </span>
              </div>
              <div className="mt-4">
                <div className="flex items-baseline gap-1">
                  <span className="text-2xl font-bold text-gray-900 dark:text-white">{kpi.current.toLocaleString()}</span>
                  <span className="text-xs text-gray-400">/ {kpi.target.toLocaleString()} {kpi.unit}</span>
                </div>
                <div className="mt-2 h-2 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
                  <div className={`h-full rounded-full transition-all ${progressColor(pct)}`} style={{ width: `${pct}%` }} />
                </div>
                <p className="text-xs text-gray-400 mt-1 text-right">{pct}%</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
