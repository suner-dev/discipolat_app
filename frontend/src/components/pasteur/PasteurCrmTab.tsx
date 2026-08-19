import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PasteurKpis } from '@/types';
import {
  Target, Eye, CheckCircle, XCircle, Users, Heart,
  TrendingUp, AlertTriangle, ArrowRight, ChevronDown, ChevronUp,
  Calendar, BookOpen, BarChart3,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell,
} from 'recharts';

export default function PasteurCrmTab() {
  const [expandedFaiseur, setExpandedFaiseur] = useState<string | null>(null);

  const { data: kpis, isLoading } = useQuery({
    queryKey: ['dashboard', 'pasteur', 'kpis'],
    queryFn: async () => {
      const res = await api.get('/dashboard/pasteur/kpis');
      return res.data as PasteurKpis;
    },
  });

  // Fetch CRM data per faiseur when expanded
  const { data: faiseurDetail } = useQuery({
    queryKey: ['crm', 'faiseur', expandedFaiseur],
    queryFn: async () => {
      const res = await api.get('/dashboard/pasteur');
      return res.data;
    },
    enabled: !!expandedFaiseur,
  });

  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Target className="w-5 h-5 text-pink-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">CRM Faiseur</h2>
        </div>
        <Link to="/crm/faiseur" className="btn-primary btn-sm"><Eye className="w-4 h-4" /> Page complète</Link>
      </div>

      {isLoading ? (
        <div className="space-y-4">{[...Array(3)].map((_, i) => <div key={i} className="glass-card p-6"><div className="skeleton h-20 w-full rounded-xl" /></div>)}</div>
      ) : (
        <>
          {/* KPIs globaux — cliquables */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <Link to="/souls" className="glass-card p-4 text-center hover:shadow-lg hover:-translate-y-0.5 transition-all group">
              <Heart className="w-6 h-6 text-rose-500 mx-auto mb-2" />
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{kpis?.resume?.totalAmes || 0}</p>
              <p className="text-[10px] text-gray-400">Total âmes</p>
              <ArrowRight className="w-3 h-3 text-gray-400 mx-auto mt-1 opacity-0 group-hover:opacity-100 transition-opacity" />
            </Link>
            <Link to="/souls?statut=ACTIF" className="glass-card p-4 text-center hover:shadow-lg hover:-translate-y-0.5 transition-all group">
              <CheckCircle className="w-6 h-6 text-green-500 mx-auto mb-2" />
              <p className="text-2xl font-bold text-green-600 dark:text-green-400">{kpis?.resume?.actifs || 0}</p>
              <p className="text-[10px] text-gray-400">Actifs</p>
              <ArrowRight className="w-3 h-3 text-gray-400 mx-auto mt-1 opacity-0 group-hover:opacity-100 transition-opacity" />
            </Link>
            <Link to="/users?role=FAISEUR" className="glass-card p-4 text-center hover:shadow-lg hover:-translate-y-0.5 transition-all group">
              <Users className="w-6 h-6 text-blue-500 mx-auto mb-2" />
              <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">{kpis?.resume?.totalFaiseurs || 0}</p>
              <p className="text-[10px] text-gray-400">Faiseurs</p>
              <ArrowRight className="w-3 h-3 text-gray-400 mx-auto mt-1 opacity-0 group-hover:opacity-100 transition-opacity" />
            </Link>
            <Link to="/alerts" className="glass-card p-4 text-center hover:shadow-lg hover:-translate-y-0.5 transition-all group">
              <AlertTriangle className="w-6 h-6 text-amber-500 mx-auto mb-2" />
              <p className="text-2xl font-bold text-amber-600 dark:text-amber-400">{kpis?.resume?.alertesActives || 0}</p>
              <p className="text-[10px] text-gray-400">Alertes actives</p>
              <ArrowRight className="w-3 h-3 text-gray-400 mx-auto mt-1 opacity-0 group-hover:opacity-100 transition-opacity" />
            </Link>
          </div>

          {/* Charge de travail — cliquable par faiseur */}
          {kpis?.workload && kpis.workload.length > 0 && (
            <div className="glass-card p-6 mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Charge par faiseur — cliquable</h3>
              <div className="h-64 mb-4">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={kpis.workload.slice(0, 10)} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" />
                    <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 10 }} unit="%" stroke="rgba(128,128,128,0.3)" />
                    <YAxis type="category" dataKey="nom" tick={{ fontSize: 10 }} width={120} stroke="rgba(128,128,128,0.3)" />
                    <Tooltip formatter={(v: number) => [`${v}%`, 'Charge']} />
                    <Bar dataKey="charge" radius={[0, 6, 6, 0]}>
                      {kpis.workload.slice(0, 10).map((w, i) => (
                        <Cell key={i} fill={w.charge > 80 ? '#ef4444' : w.charge > 50 ? '#f59e0b' : '#22c55e'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
              {/* Détails par faiseur */}
              <div className="space-y-1 max-h-72 overflow-y-auto">
                {kpis.workload.map((w) => (
                  <div key={w.id}
                    className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors cursor-pointer"
                    onClick={() => setExpandedFaiseur(expandedFaiseur === w.id ? null : w.id)}>
                    <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-[10px] font-bold text-white">
                      {w.nom.charAt(0)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{w.nom}</span>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] text-gray-400">{w.totalAmes} âmes</span>
                          {expandedFaiseur === w.id ? <ChevronUp className="w-3 h-3 text-gray-400" /> : <ChevronDown className="w-3 h-3 text-gray-400" />}
                        </div>
                      </div>
                      <div className="flex items-center gap-2 mt-1">
                        <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-1.5">
                          <div
                            className={`h-1.5 rounded-full transition-all duration-500 ${
                              w.charge > 80 ? 'bg-red-500' : w.charge > 50 ? 'bg-amber-500' : 'bg-emerald-500'
                            }`}
                            style={{ width: `${Math.min(w.charge, 100)}%` }}
                          />
                        </div>
                        <span className="text-[10px] font-semibold text-gray-500 w-8 text-right">{w.charge}%</span>
                      </div>
                    </div>
                    {w.rapportSoumis ? (
                      <span title="Rapport soumis"><CheckCircle className="w-4 h-4 text-emerald-500 flex-shrink-0" /></span>
                    ) : (
                      <span title="Rapport non soumis"><XCircle className="w-4 h-4 text-red-500 flex-shrink-0" /></span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Rapports en retard */}
          {kpis?.overdueReports && kpis.overdueReports.length > 0 && (
            <div className="glass-card p-6 mb-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Rapports en retard</h3>
                <Link to="/reports" className="text-[10px] font-medium text-primary-600">Voir tout</Link>
              </div>
              <div className="space-y-2">
                {kpis.overdueReports.map(r => (
                  <div key={r.faiseurId} className="flex items-center justify-between p-3 rounded-lg bg-red-50/30 dark:bg-red-900/10 border border-red-100/50">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-red-100 dark:bg-red-900/30 flex items-center justify-center"><XCircle className="w-4 h-4 text-red-500" /></div>
                      <div>
                        <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{r.faiseurNom}</p>
                        <p className="text-[10px] text-gray-400">{r.nbAmes} âmes suivies</p>
                      </div>
                    </div>
                    <span className="text-[9px] text-red-500 font-semibold">Non soumis</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Liens rapides vers modules réels */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {[
              { label: 'Âmes', route: '/souls', icon: Heart, color: 'from-rose-500 to-pink-500' },
              { label: 'Rapports', route: '/reports', icon: TrendingUp, color: 'from-emerald-500 to-teal-500' },
              { label: 'Évaluations', route: '/evaluations', icon: Target, color: 'from-yellow-500 to-amber-500' },
              { label: 'Familles', route: '/families', icon: Users, color: 'from-blue-500 to-indigo-500' },
            ].map(l => (
              <Link key={l.route} to={l.route} className="glass-card p-3.5 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 group">
                <div className="flex items-center gap-2.5">
                  <div className={`p-2 rounded-xl bg-gradient-to-br ${l.color} text-white`}><l.icon className="w-4 h-4" /></div>
                  <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">{l.label}</span>
                  <ArrowRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
                </div>
              </Link>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
