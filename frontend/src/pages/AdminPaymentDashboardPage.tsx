import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import api from '@/lib/api';
import { useI18n } from '@/i18n/index';
import {
  Loader2,
  BarChart3,
  TrendingUp,
  CheckCircle2,
  Clock,
  XCircle,
  Trash2,
  Repeat,
  Smartphone,
  Target,
  DollarSign,
  Activity,
  Calendar,
  ArrowUpRight,
  ArrowDownRight,
} from 'lucide-react';

interface DashboardData {
  pending: number;
  confirmed: number;
  failed: number;
  cancelled: number;
  total: number;
  confirmationRate: number;
  avgAmount: number;
  maxAmount: number;
  minAmount: number;
  byOperator: { operator: string; label: string; total: number; count: number }[];
  byProvider: { provider: string; total: number; count: number }[];
  byPurpose: { purpose: string; total: number; count: number }[];
  monthlyTrend: { month: string; total: number }[];
  dailyTrend: { date: string; total: number; count: number }[];
  recurring: {
    activeCount: number;
    monthlyCommitment: number;
    avgCommitment: number;
    totalProcessed: number;
    totalRecurringDonated: number;
  };
  recurringByFrequency: { frequency: string; label: string; total: number; count: number }[];
  recurringByOperator: { operator: string; label: string; total: number; count: number }[];
}

const PURPOSE_LABELS: Record<string, string> = {
  DIME: 'giving.purpose.dime',
  OFFRANDE: 'giving.purpose.offrande',
  PROMESSE: 'giving.purpose.promesse',
  PROJET_SPECIAL: 'giving.purpose.projet',
  DON_DIASPORA: 'giving.purpose.diaspora',
};

export default function AdminPaymentDashboardPage() {
  const { t, locale } = useI18n();

  const { data, isLoading } = useQuery({
    queryKey: ['payments', 'dashboard'],
    queryFn: async () => (await api.get<DashboardData>('/payments/dashboard')).data,
    refetchInterval: 30000,
  });

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  if (!data) return null;

  const fmt = (n: number) => Number(n).toLocaleString(locale);
  const fmtCurrency = (n: number) =>
    `${Number(n).toLocaleString(locale)} XOF`;

  const kpis = [
    {
      label: t('paymentDashboard.totalPayments'),
      value: data.total,
      icon: BarChart3,
      color: 'from-blue-500 to-cyan-600',
      textColor: 'text-blue-600 dark:text-blue-400',
    },
    {
      label: t('paymentDashboard.confirmed'),
      value: data.confirmed,
      icon: CheckCircle2,
      color: 'from-green-500 to-emerald-600',
      textColor: 'text-green-600 dark:text-green-400',
      extra: `${data.confirmationRate}% ${t('paymentDashboard.confirmationRate')}`,
    },
    {
      label: t('paymentDashboard.pending'),
      value: data.pending,
      icon: Clock,
      color: 'from-amber-500 to-orange-600',
      textColor: 'text-amber-600 dark:text-amber-400',
    },
    {
      label: t('paymentDashboard.failed'),
      value: data.failed,
      icon: XCircle,
      color: 'from-red-500 to-rose-600',
      textColor: 'text-red-600 dark:text-red-400',
    },
  ];

  const maxOperator = Math.max(...(data.byOperator ?? []).map((o) => Number(o.total)), 1);
  const maxPurpose = Math.max(...(data.byPurpose ?? []).map((o) => Number(o.total)), 1);
  const maxMonthly = Math.max(...(data.monthlyTrend ?? []).map((o) => Number(o.total)), 1);

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 text-white shadow-lg">
          <BarChart3 className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">{t('paymentDashboard.title')}</h1>
          <p className="page-subtitle">{t('paymentDashboard.subtitle')}</p>
        </div>
        <div className="ml-auto flex gap-2">
          <Link to="/admin/webhook-logs" className="btn-secondary btn-sm flex items-center gap-1.5">
            <Activity className="w-4 h-4" /> Logs webhooks
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6 animate-slide-up">
        {kpis.map((kpi) => (
          <div key={kpi.label} className="glass-card p-4">
            <div className="flex items-center gap-3 mb-2">
              <div className={`p-2 rounded-lg bg-gradient-to-br ${kpi.color} text-white`}>
                <kpi.icon className="w-4 h-4" />
              </div>
              <span className="text-xs font-medium text-gray-500 dark:text-gray-400">
                {kpi.label}
              </span>
            </div>
            <p className={`text-2xl font-bold ${kpi.textColor}`}>{fmt(kpi.value)}</p>
            {kpi.extra && (
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">{kpi.extra}</p>
            )}
          </div>
        ))}
      </div>

      {/* Amount Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6 animate-slide-up">
        <div className="glass-card p-4 text-center">
          <DollarSign className="w-5 h-5 mx-auto text-primary-500 mb-1" />
          <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.avgAmount')}</p>
          <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{fmtCurrency(data.avgAmount)}</p>
        </div>
        <div className="glass-card p-4 text-center">
          <ArrowUpRight className="w-5 h-5 mx-auto text-green-500 mb-1" />
          <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.maxAmount')}</p>
          <p className="text-lg font-bold text-green-600 dark:text-green-400">{fmtCurrency(data.maxAmount)}</p>
        </div>
        <div className="glass-card p-4 text-center">
          <ArrowDownRight className="w-5 h-5 mx-auto text-amber-500 mb-1" />
          <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.minAmount')}</p>
          <p className="text-lg font-bold text-amber-600 dark:text-amber-400">{fmtCurrency(data.minAmount)}</p>
        </div>
      </div>

      <div className="grid lg:grid-cols-2 gap-6 mb-6">
        {/* Operator Breakdown */}
        <div className="glass-card p-6 animate-slide-up">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Smartphone className="w-5 h-5 text-blue-500" /> {t('paymentDashboard.byOperator')}
          </h2>
          {(data.byOperator ?? []).length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">{t('paymentDashboard.noData')}</p>
          ) : (
            <div className="space-y-3">
              {data.byOperator!.map((o) => (
                <div key={o.operator}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-700 dark:text-gray-300">{o.label}</span>
                    <span className="font-medium text-gray-900 dark:text-gray-100">
                      {fmtCurrency(o.total)} ({o.count})
                    </span>
                  </div>
                  <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-blue-500 to-cyan-500 rounded-full transition-all duration-500"
                      style={{ width: `${(Number(o.total) / maxOperator) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Purpose Breakdown */}
        <div className="glass-card p-6 animate-slide-up">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Target className="w-5 h-5 text-purple-500" /> {t('paymentDashboard.byPurpose')}
          </h2>
          {(data.byPurpose ?? []).length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">{t('paymentDashboard.noData')}</p>
          ) : (
            <div className="space-y-3">
              {data.byPurpose!.map((p) => (
                <div key={p.purpose}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-700 dark:text-gray-300">
                      {t(PURPOSE_LABELS[p.purpose] ?? 'giving.purpose.offrande')}
                    </span>
                    <span className="font-medium text-gray-900 dark:text-gray-100">
                      {fmtCurrency(p.total)} ({p.count})
                    </span>
                  </div>
                  <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full transition-all duration-500"
                      style={{ width: `${(Number(p.total) / maxPurpose) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Monthly Trend */}
      <div className="glass-card p-6 mb-6 animate-slide-up">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <TrendingUp className="w-5 h-5 text-green-500" /> {t('paymentDashboard.monthlyTrend')}
        </h2>
        {(data.monthlyTrend ?? []).length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-4">{t('paymentDashboard.noData')}</p>
        ) : (
          <div className="flex items-end gap-1 h-40">
            {data.monthlyTrend!.slice(0, 12).reverse().map((m) => (
              <div key={m.month} className="flex-1 flex flex-col items-center gap-1">
                <div
                  className="w-full bg-gradient-to-t from-green-500 to-emerald-400 rounded-t-md transition-all duration-500 min-h-[2px]"
                  style={{ height: `${(Number(m.total) / maxMonthly) * 100}%` }}
                  title={`${m.month}: ${fmtCurrency(m.total)}`}
                />
                <span className="text-[9px] text-gray-400 dark:text-gray-500 truncate w-full text-center">
                  {String(m.month).slice(5)}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Provider Breakdown */}
      {(data.byProvider ?? []).length > 0 && (
        <div className="glass-card p-6 mb-6 animate-slide-up">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Activity className="w-5 h-5 text-cyan-500" /> {t('paymentDashboard.byProvider')}
          </h2>
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
            {data.byProvider!.map((p) => (
              <div key={p.provider} className="p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{p.provider}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {fmtCurrency(p.total)} · {p.count} {t('paymentDashboard.payments')}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recurring Donations Section */}
      <div className="glass-card p-6 mb-6 animate-slide-up">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <Repeat className="w-5 h-5 text-purple-500" /> {t('paymentDashboard.recurring')}
        </h2>

        {/* Recurring KPIs */}
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-4">
          <div className="text-center p-3 rounded-lg bg-purple-50 dark:bg-purple-900/20">
            <p className="text-2xl font-bold text-purple-600 dark:text-purple-400">
              {data.recurring.activeCount}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.activeRecurring')}</p>
          </div>
          <div className="text-center p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20">
            <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">
              {fmtCurrency(data.recurring.monthlyCommitment)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.monthlyCommitment')}</p>
          </div>
          <div className="text-center p-3 rounded-lg bg-green-50 dark:bg-green-900/20">
            <p className="text-2xl font-bold text-green-600 dark:text-green-400">
              {fmtCurrency(data.recurring.avgCommitment)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.avgCommitment')}</p>
          </div>
          <div className="text-center p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20">
            <p className="text-2xl font-bold text-amber-600 dark:text-amber-400">
              {fmt(data.recurring.totalProcessed)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.totalProcessed')}</p>
          </div>
          <div className="text-center p-3 rounded-lg bg-emerald-50 dark:bg-emerald-900/20">
            <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
              {fmtCurrency(data.recurring.totalRecurringDonated)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">{t('paymentDashboard.totalRecurringDonated')}</p>
          </div>
        </div>

        <div className="grid lg:grid-cols-2 gap-4">
          {/* By Frequency */}
          <div>
            <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-1">
              <Calendar className="w-4 h-4" /> {t('paymentDashboard.recurringByFrequency')}
            </h3>
            {(data.recurringByFrequency ?? []).length === 0 ? (
              <p className="text-xs text-gray-400 text-center py-2">{t('paymentDashboard.noData')}</p>
            ) : (
              <div className="space-y-2">
                {data.recurringByFrequency!.map((f) => (
                  <div key={f.frequency} className="flex items-center justify-between p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                    <span className="text-sm text-gray-700 dark:text-gray-300">{f.label}</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {fmtCurrency(f.total)} ({f.count})
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* By Operator */}
          <div>
            <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-1">
              <Smartphone className="w-4 h-4" /> {t('paymentDashboard.recurringByOperator')}
            </h3>
            {(data.recurringByOperator ?? []).length === 0 ? (
              <p className="text-xs text-gray-400 text-center py-2">{t('paymentDashboard.noData')}</p>
            ) : (
              <div className="space-y-2">
                {data.recurringByOperator!.map((o) => (
                  <div key={o.operator} className="flex items-center justify-between p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                    <span className="text-sm text-gray-700 dark:text-gray-300">{o.label}</span>
                    <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {fmtCurrency(o.total)} ({o.count})
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
