import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import {
  Users, Heart, UserCheck, FileText, Activity, Bell, Calendar,
  BookOpen, Star, AlertTriangle, TrendingUp, TrendingDown,
  Loader2, Sparkles, ChevronRight, Church, Eye, CheckCircle,
  Clock, UserX, XCircle, Search, GitBranch, BarChart3,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from 'recharts';

const COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899'];

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

export default function ChefFamilleDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const familleId = user?.familleGereeId;

  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard', 'chef-famille', familleId],
    queryFn: async () => {
      const res = await api.get('/dashboard/chef-famille', {
        params: familleId ? { familleId } : undefined,
      });
      return res.data as any;
    },
    enabled: !!user,
  });

  // Charge de travail des faiseurs de SA famille (scopée côté serveur).
  const { data: workload } = useQuery({
    queryKey: ['users', 'workload', familleId],
    queryFn: async () => {
      const res = await api.get('/users/faiseur-workload', {
        params: familleId ? { familleId } : undefined,
      });
      return res.data as { faiseurId: string; faiseurName: string; soulCount: number; charge?: string }[];
    },
    enabled: !!familleId,
  });

  const famille = dashboard?.famille ?? {};
  const faiseurs = dashboard?.faiseurs ?? [];
  const disciples = dashboard?.disciples ?? [];
  const stats = dashboard?.statistiques ?? {};

  // Disciples by status for pie chart
  const disciplesByStatut = [
    { name: 'Actifs', value: stats.actifs ?? 0, color: '#22c55e' },
    { name: 'En intégration', value: stats.enIntegration ?? 0, color: '#f59e0b' },
    { name: 'En veille', value: stats.enVeille ?? 0, color: '#3b82f6' },
    { name: 'Décrochés', value: stats.decroches ?? 0, color: '#ef4444' },
  ].filter(d => d.value > 0);

  // Build network view: faiseurs -> disciples
  const networkNodes = faiseurs.map((f: any) => ({
    ...f,
    disciples: disciples.filter((d: any) => d.faiseurId === f.id),
  }));

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Users className="w-5 h-5 text-gold-500" />
            <span className="text-sm font-medium text-gold-600 dark:text-gold-400 uppercase tracking-wider">
              {getGreeting()}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            Famille{' '}
            <span className="text-gradient font-display">{famille.nom || 'De disciples'}</span>
          </h1>
          <p className="page-subtitle">
            Supervision des faiseurs et des disciples · {new Date().toLocaleDateString('fr-FR', {
              weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
            })}
          </p>
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="glass-card p-5 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="skeleton h-4 w-24 mb-3 rounded" />
              <div className="skeleton h-8 w-20 rounded" />
            </div>
          ))}
        </div>
      ) : !dashboard?.famille?.id ? (
        <div className="glass-card p-12 text-center animate-fade-in">
          <Church className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-600 dark:text-gray-400 mb-2">Aucune famille assignée</h2>
          <p className="text-sm text-gray-400">Vous n'êtes pas encore chef d'une famille de disciples.</p>
        </div>
      ) : (
        <>
          {/* Stats Cards — cliquables : liste des disciples ou rapport */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <button
              type="button"
              onClick={() => document.getElementById('liste-disciples-famille')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              title="Voir tous les disciples de votre famille"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-gold-500 to-amber-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Disciples</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-gold-500 to-amber-500 text-white shadow-lg">
                  <Heart className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalDisciples ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">Cliquer pour voir la liste</span>
            </button>
            <button
              type="button"
              onClick={() => navigate('/families')}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              style={{ animationDelay: '60ms' }}
              title="Ouvrir la gestion des familles"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-green-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Faiseurs</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-green-500 text-white shadow-lg">
                  <UserCheck className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalFaiseurs ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">Gérer les faiseurs</span>
            </button>
            <button
              type="button"
              onClick={() => document.getElementById('liste-disciples-famille')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              style={{ animationDelay: '120ms' }}
              title="Voir les disciples actifs de votre famille"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-teal-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Actifs</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-500 text-white shadow-lg">
                  <CheckCircle className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-emerald-500">{stats.actifs ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">Cliquer pour voir la liste</span>
            </button>
            <button
              type="button"
              onClick={() => navigate('/reports/family')}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              style={{ animationDelay: '180ms' }}
              title="Ouvrir le rapport de famille"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Rapports semaine</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                  <FileText className="w-4 h-4" />
                </div>
              </div>
              <span className={`stat-value ${(stats.rapportsSoumisSemaine ?? 0) > 0 ? 'text-emerald-500' : 'text-amber-500'}`}>
                {stats.rapportsSoumisSemaine ?? 0}
                <span className="text-xs text-gray-400 ml-1">/ {stats.totalDisciples ?? 0}</span>
              </span>
              <span className="text-[10px] text-gray-400 mt-1 block">Rapport de famille</span>
            </button>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* Status Distribution Pie */}
            <div className="glass-card p-6 animate-slide-up">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Répartition des disciples</h3>
              </div>
              <div className="h-64">
                {disciplesByStatut.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={disciplesByStatut} cx="50%" cy="50%" innerRadius={60} outerRadius={90}
                        paddingAngle={3} dataKey="value" startAngle={90} endAngle={-270}>
                        {disciplesByStatut.map((entry, i) => (
                          <Cell key={i} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend formatter={(v: string) => <span className="text-xs text-gray-600 dark:text-gray-400">{v}</span>} />
                    </PieChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex items-center justify-center h-full text-gray-400 text-sm">Aucune donnée</div>
                )}
              </div>
            </div>

            {/* Reports Status */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '60ms' }}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Rapports de la semaine</h3>
                <Link to="/reports" className="text-[10px] font-medium text-primary-600">Détail</Link>
              </div>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={[
                    { name: 'Soumis', value: stats.rapportsSoumisSemaine ?? 0, color: '#22c55e' },
                    { name: 'En attente', value: stats.rapportsEnAttente ?? 0, color: '#f59e0b' },
                  ]}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 12 }} stroke="rgba(128,128,128,0.3)" />
                    <YAxis tick={{ fontSize: 12 }} stroke="rgba(128,128,128,0.3)" />
                    <Tooltip />
                    <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                      <Cell fill="#22c55e" />
                      <Cell fill="#f59e0b" />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Charge de travail des faiseurs (US-14) */}
          {workload && workload.length > 0 && (
            <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '110ms' }}>
              <div className="flex items-center gap-2 mb-1">
                <BarChart3 className="w-4 h-4 text-primary-500" />
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Charge de travail des Faiseurs</h3>
              </div>
              <p className="text-xs text-gray-400 mb-4">Répartition des disciples suivis par faiseur</p>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
                {workload.map((w) => {
                  const style =
                    w.charge === 'LEGER' ? 'bg-green-100/80 dark:bg-green-900/30 text-green-700 dark:text-green-400 border-green-200/60 dark:border-green-700/40'
                    : w.charge === 'SURCHARGÉ' ? 'bg-red-100/80 dark:bg-red-900/30 text-red-700 dark:text-red-300 border-red-200/60 dark:border-red-700/40'
                    : 'bg-blue-100/80 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border-blue-200/60 dark:border-blue-700/40';
                  const label =
                    w.charge === 'LEGER' ? 'Léger'
                    : w.charge === 'SURCHARGÉ' ? 'Surchargé'
                    : 'Normal';
                  return (
                    <div key={w.faiseurId} className="p-3 rounded-xl bg-gradient-to-br from-gray-50 to-white dark:from-gray-800/50 dark:to-gray-800/30 border border-gray-100 dark:border-gray-700/50">
                      <p className="text-xs text-gray-500 dark:text-gray-400 truncate" title={w.faiseurName}>{w.faiseurName}</p>
                      <p className="text-lg font-bold text-gray-900 dark:text-gray-100 mt-1">{w.soulCount}</p>
                      <p className="text-[10px] text-gray-400">âmes suivies</p>
                      <span className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[9px] font-semibold border mt-1 ${style}`}>
                        {label}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Network View: Faiseurs */}
          <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
            <div className="flex items-center gap-2 mb-4">
              <GitBranch className="w-4 h-4 text-primary-500" />
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Vue réseau — Faiseurs & Disciples</h3>
            </div>

            {networkNodes.length > 0 ? (
              <div className="space-y-4">
                {networkNodes.map((faiseur: any) => (
                  <div key={faiseur.id} className="border border-gray-100 dark:border-gray-700/30 rounded-xl overflow-hidden">
                    {/* Faiseur header */}
                    <div className="flex items-center justify-between p-3 bg-gradient-to-r from-primary-500/5 to-primary-600/5 border-b border-gray-100 dark:border-gray-700/30">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-xs font-bold text-white">
                          {faiseur.nom?.charAt(0)}
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{faiseur.nom}</p>
                          <p className="text-[10px] text-gray-400">{faiseur.totalAmes} disciples</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${faiseur.rapportSoumis ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'}`}>
                          {faiseur.rapportSoumis ? 'Rapport OK' : 'En attente'}
                        </span>
                        <Link to={`/souls?faiseurId=${faiseur.id}`} className="text-[10px] text-primary-500 hover:underline">
                          Voir
                        </Link>
                      </div>
                    </div>

                    {/* Disciples */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 p-3">
                      {faiseur.disciples?.slice(0, 6).map((soul: any) => (
                        <div key={soul.id}
                          className="flex items-center gap-2 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer transition-colors"
                          onClick={() => navigate(`/souls/${soul.id}`)}
                        >
                          <div className={`w-2 h-2 rounded-full flex-shrink-0 ${
                            soul.statut === 'ACTIF' ? 'bg-green-500' :
                            soul.statut === 'EN_INTEGRATION' ? 'bg-amber-500' :
                            soul.statut === 'EN_VEILLE' ? 'bg-blue-500' : 'bg-red-500'
                          }`} />
                          <div className="flex-1 min-w-0">
                            <p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{soul.nom}</p>
                            <p className="text-[9px] text-gray-400 truncate">
                              {soul.type === 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : 'Nouvel arrivant'}
                              {soul.dateDernierContact ? ` · ${soul.dateDernierContact}` : ''}
                            </p>
                          </div>
                          <span className={`text-[8px] font-semibold px-1.5 py-0.5 rounded-full ${
                            soul.etatSpirituel === 'EN_DIFFICULTE' ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400' :
                            'bg-gray-100 dark:bg-gray-800/50 text-gray-500'
                          }`}>
                            Niv.{soul.niveauCroissance}
                          </span>
                        </div>
                      ))}
                      {faiseur.disciples?.length > 6 && (
                        <div className="flex items-center justify-center p-2 text-[10px] text-primary-500">
                          +{faiseur.disciples.length - 6} autres
                        </div>
                      )}
                      {(!faiseur.disciples || faiseur.disciples.length === 0) && (
                        <p className="text-xs text-gray-400 p-2">Aucun disciple</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <Users className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                <p className="text-sm text-gray-400">Aucun faiseur dans cette famille</p>
              </div>
            )}
          </div>

          {/* All Disciples List */}
          <div id="liste-disciples-famille" className="glass-card p-6 animate-slide-up scroll-mt-24" style={{ animationDelay: '180ms' }}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Heart className="w-4 h-4 text-primary-500" />
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Tous les disciples</h3>
              </div>
              <Link to={`/souls?familleId=${dashboard?.famille?.id}`} className="text-[10px] font-medium text-primary-600">
                Voir tout
              </Link>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[10px] text-gray-400 uppercase tracking-wider border-b border-gray-100 dark:border-gray-700/30">
                    <th className="pb-2 font-medium">Nom</th>
                    <th className="pb-2 font-medium">Faiseur</th>
                    <th className="pb-2 font-medium">Statut</th>
                    <th className="pb-2 font-medium">Type</th>
                    <th className="pb-2 font-medium">Niveau</th>
                    <th className="pb-2 font-medium">Rapport</th>
                    <th className="pb-2 font-medium">Dernier contact</th>
                  </tr>
                </thead>
                <tbody>
                  {disciples.map((soul: any, i: number) => (
                    <tr key={soul.id}
                      className="border-b border-gray-50 dark:border-gray-800/30 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 cursor-pointer transition-colors"
                      onClick={() => navigate(`/souls/${soul.id}`)}
                    >
                      <td className="py-2.5 font-medium text-gray-900 dark:text-gray-100">{soul.nom}</td>
                      <td className="py-2.5 text-gray-500 text-xs">{soul.faiseurNom}</td>
                      <td className="py-2.5">
                        <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded-full ${
                          soul.statut === 'ACTIF' ? 'badge-success' :
                          soul.statut === 'EN_INTEGRATION' ? 'badge-warning' :
                          soul.statut === 'EN_VEILLE' ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400' :
                          'badge-error'
                        }`}>
                          {soul.statut === 'ACTIF' ? 'Actif' : soul.statut === 'EN_INTEGRATION' ? 'Intégration' : soul.statut === 'EN_VEILLE' ? 'Veille' : 'Décroché'}
                        </span>
                      </td>
                      <td className="py-2.5 text-gray-500 text-xs">{soul.type === 'NOUVEAU_CONVERTI' ? 'Nv. converti' : 'Nv. arrivant'}</td>
                      <td className="py-2.5">
                        <span className="flex items-center gap-1">
                          {[1,2,3,4,5].map(i => (
                            <Star key={i} className={`w-2.5 h-2.5 ${i <= (soul.niveauCroissance || 1) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                          ))}
                        </span>
                      </td>
                      <td className="py-2.5">
                        <span className={`text-[9px] font-semibold ${soul.rapportSemaine ? 'text-green-500' : 'text-amber-500'}`}>
                          {soul.rapportSemaine ? 'Soumis' : 'En attente'}
                        </span>
                      </td>
                      <td className="py-2.5 text-[10px] text-gray-400">{soul.dateDernierContact || '—'}</td>
                    </tr>
                  ))}
                  {disciples.length === 0 && (
                    <tr><td colSpan={7} className="py-6 text-center text-gray-400">Aucun disciple</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
