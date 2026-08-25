import { useQuery } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import { BarChart3, TrendingUp, TrendingDown, Target, ArrowUpRight } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface DepartmentKpi {
  id: string;
  departmentId: number;
  name: string;
  description?: string;
  targetValue?: number;
  currentValue?: number;
  unit?: string;
  period?: string;
}

export default function DepartmentKPIsPage() {
  const { t } = useI18n();

  const { data: kpis = [], isLoading, error } = useQuery({
    queryKey: ['department-kpis'],
    queryFn: async () => (await api.get('/department-kpis')).data as DepartmentKpi[],
    retry: false,
  });

  const current = (k: DepartmentKpi) => k.currentValue ?? 0;
  const target = (k: DepartmentKpi) => k.targetValue ?? 0;
  const trend = (k: DepartmentKpi) =>
    target(k) === 0 ? 'stable' : current(k) >= target(k) ? 'up' : 'down';
  const progressPercent = (k: DepartmentKpi) => Math.min(Math.round((current(k) / (target(k) || 1)) * 100), 100);
  const progressColor = (pct: number) => pct >= 80 ? 'bg-green-500' : pct >= 50 ? 'bg-amber-500' : 'bg-red-500';

  const departmentsCount = new Set(kpis.map(k => k.departmentId)).size;
  const upCount = kpis.filter(k => trend(k) === 'up')
    .reduce((a, k) => a + (current(k) >= target(k) && target(k) > 0 ? 1 : 0), 0);
  const attainedCount = kpis.filter(k => progressPercent(k) >= 80).length;
  const downCount = kpis.filter(k => target(k) > 0 && current(k) < target(k)).length;

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="p-6 text-red-500 dark:text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
          <BarChart3 className="w-7 h-7 text-teal-500" />
          {t('nav.departmentKpis') ?? 'KPIs Départementaux'}
        </h1>
        <p className="text-sm text-gray-500 mt-1">Suivi des objectifs par département</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">Départements suivis</p>
          <p className="text-2xl font-bold text-gray-900 dark:text-white mt-1">{departmentsCount}</p>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">KPIs atteints (≥80%)</p>
          <p className="text-2xl font-bold text-teal-500 mt-1">{attainedCount}</p>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">KPIs à la cible</p>
          <p className="text-2xl font-bold text-green-500 mt-1">{upCount}</p>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <p className="text-xs text-gray-500">KPIs en baisse</p>
          <p className="text-2xl font-bold text-red-500 mt-1">{downCount}</p>
        </div>
      </div>

      {kpis.length === 0 ? (
        <EmptyState
          icon={<BarChart3 className="w-6 h-6 text-teal-400" />}
          title="Aucun KPI départemental"
          message="Les KPIs mis en place par département apparaîtront ici."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {kpis.map((kpi) => {
            const pct = progressPercent(kpi);
            const tr = trend(kpi);
            return (
              <div key={kpi.id} className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-xs text-gray-500 font-medium">Département #{kpi.departmentId}</p>
                    <h3 className="font-semibold text-gray-900 dark:text-white mt-1">{kpi.name}</h3>
                  </div>
                  <span className={`flex items-center gap-1 text-xs font-medium ${tr === 'up' ? 'text-green-500' : tr === 'down' ? 'text-red-500' : 'text-gray-400'}`}>
                    {tr === 'up' ? <TrendingUp className="w-3 h-3" /> : tr === 'down' ? <TrendingDown className="w-3 h-3" /> : <Target className="w-3 h-3" />}
                  </span>
                </div>
                <div className="mt-4">
                  <div className="flex items-baseline gap-1">
                    <span className="text-2xl font-bold text-gray-900 dark:text-white">{current(kpi).toLocaleString()}</span>
                    <span className="text-xs text-gray-400">/ {target(kpi).toLocaleString()} {kpi.unit || ''}</span>
                  </div>
                  <div className="mt-2 h-2 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
                    <div className={`h-full rounded-full transition-all ${progressColor(pct)}`} style={{ width: `${pct}%` }} />
                  </div>
                  <p className="text-xs text-gray-400 mt-1 text-right flex items-center justify-end gap-1">{pct}% <ArrowUpRight className="w-3 h-3" /></p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
