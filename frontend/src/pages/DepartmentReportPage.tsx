import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import api from '@/lib/api';
import {
  ArrowLeft, BarChart3, FileText, Users, Loader2, CheckCircle2,
  XCircle, TrendingUp, TrendingDown, Minus,
} from 'lucide-react';

export default function DepartmentReportPage() {
  const { id } = useParams<{ id: string }>();
  const today = new Date().toISOString().split('T')[0];
  const [semaine, setSemaine] = useState(today);

  const { data: dept } = useQuery({
    queryKey: ['department', id],
    queryFn: async () => {
      const res = await api.get(`/departments/${id}/detail`);
      return res.data as any;
    },
    enabled: !!id,
  });

  const { data: report, isLoading } = useQuery({
    queryKey: ['department', id, 'report', semaine],
    queryFn: async () => {
      const res = await api.get(`/departments/${id}/report?semaine=${semaine}`);
      return res.data as any;
    },
    enabled: !!id,
  });

  const { data: kpi } = useQuery({
    queryKey: ['department', id, 'kpi'],
    queryFn: async () => {
      const res = await api.get(`/departments/${id}/kpi`);
      return res.data as any;
    },
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <Link to={`/departments/${id}`} className="btn-ghost btn-sm mb-2 inline-flex">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </Link>
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
              <FileText className="w-6 h-6" />
            </div>
            <div>
              <h1 className="page-title">Rapport du département</h1>
              <p className="page-subtitle">{dept?.nom || 'Chargement...'}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-gray-500">Semaine:</span>
            <input
              type="date"
              value={semaine}
              onChange={(e) => setSemaine(e.target.value)}
              className="input py-1.5 text-sm w-auto"
            />
          </div>
        </div>
      </div>

      {report && (
        <>
          {/* Summary cards */}
          <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
            {[
              { label: 'Familles', value: report.totalFamilles, icon: Users, color: 'from-violet-500 to-purple-500' },
              { label: 'Rapports soumis', value: `${report.familyReportsSoumis}/${report.totalFamilles}`, icon: FileText, color: report.familyReportsSoumis === report.totalFamilles ? 'from-green-500 to-emerald-500' : 'from-amber-500 to-orange-500' },
              { label: 'Présents', value: report.totalPresents, icon: CheckCircle2, color: 'from-green-500 to-emerald-500' },
              { label: 'Absents', value: report.totalAbsents, icon: XCircle, color: 'from-red-500 to-rose-500' },
              { label: 'Présence', value: `${report.presenceMoyenne || 0}%`, icon: BarChart3, color: 'from-blue-500 to-indigo-500' },
            ].map((stat, i) => (
              <div key={stat.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
                <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${stat.color} opacity-60`} />
                <div className="flex items-start justify-between mb-2">
                  <span className="stat-label text-[10px]">{stat.label}</span>
                  <div className={`p-1.5 rounded-lg bg-gradient-to-br ${stat.color} text-white shadow-sm`}>
                    <stat.icon className="w-3.5 h-3.5" />
                  </div>
                </div>
                <p className="stat-value text-xl">{String(stat.value)}</p>
              </div>
            ))}
          </div>

          {/* Additional stats */}
          {report.totalSorties > 0 || report.totalMaintenus > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
              <div className="stat-card">
                <span className="stat-label">Sorties</span>
                <div className="flex items-center gap-2 mt-1">
                  <TrendingDown className="w-5 h-5 text-red-500" />
                  <p className="stat-value text-xl text-red-600">{report.totalSorties || 0}</p>
                </div>
              </div>
              <div className="stat-card">
                <span className="stat-label">Maintenus</span>
                <div className="flex items-center gap-2 mt-1">
                  <TrendingUp className="w-5 h-5 text-green-500" />
                  <p className="stat-value text-xl text-green-600">{report.totalMaintenus || 0}</p>
                </div>
              </div>
            </div>
          )}

          {/* Per-family breakdown */}
          {report.statsParFamille && Object.keys(report.statsParFamille).length > 0 && (
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <BarChart3 className="w-5 h-5 text-primary-500" />
                Détail par famille
              </h2>
              <div className="overflow-x-auto">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Famille</th>
                      <th>Statut</th>
                      <th>Présence</th>
                      <th>Présents</th>
                      <th>Absents</th>
                      <th>Sorties</th>
                      <th>Maintenus</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(report.statsParFamille).map(([famId, stats]: [string, any]) => (
                      <tr key={famId} className="hover:bg-gray-50 dark:hover:bg-gray-800/30">
                        <td className="font-medium text-gray-900 dark:text-gray-100">
                          <Link to={`/families/${stats.familleId}`} className="text-primary-600 hover:underline">
                            {stats.familleNom}
                          </Link>
                        </td>
                        <td>
                          {stats.soumis ? (
                            <span className="badge-success text-xs"><CheckCircle2 className="w-3 h-3 mr-1" />Soumis</span>
                          ) : (
                            <span className="badge-warning text-xs"><Minus className="w-3 h-3 mr-1" />Non soumis</span>
                          )}
                        </td>
                        <td className="font-semibold">{stats.presenceMoyenne != null ? `${stats.presenceMoyenne}%` : '-'}</td>
                        <td>{stats.totalPresents ?? '-'}</td>
                        <td>{stats.totalAbsents ?? '-'}</td>
                        <td className="text-red-600">{stats.totalSorties ?? '-'}</td>
                        <td className="text-green-600">{stats.totalMaintenus ?? '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}

      {!report && !isLoading && (
        <div className="glass-card p-10 text-center">
          <FileText className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500">Aucun rapport disponible pour cette semaine</p>
        </div>
      )}

      {/* KPI reference */}
      {kpi && (
        <div className="glass-card p-5 mt-6">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-primary-500" />
            Indicateurs de la semaine
          </h3>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <span className="text-xs text-gray-500">Taux de soumission</span>
              <p className="text-lg font-bold">{kpi.tauxSoumission}%</p>
              <div className="progress-bar mt-1">
                <div className="progress-bar-fill" style={{ width: `${kpi.tauxSoumission}%` }} />
              </div>
            </div>
            <div>
              <span className="text-xs text-gray-500">Taux de présence</span>
              <p className="text-lg font-bold">{kpi.tauxPresence}%</p>
              <div className="progress-bar mt-1">
                <div className="progress-bar-fill bg-green-500" style={{ width: `${kpi.tauxPresence}%` }} />
              </div>
            </div>
            <div>
              <span className="text-xs text-gray-500">Rapports soumis</span>
              <p className="text-lg font-bold">{kpi.rapportsSoumisSemaine}/{kpi.rapportsAttendusSemaine}</p>
            </div>
            <div>
              <span className="text-xs text-gray-500">Faiseurs actifs</span>
              <p className="text-lg font-bold">{kpi.totalFaiseurs}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
