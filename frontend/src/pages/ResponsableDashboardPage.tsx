import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useState } from 'react';
import {
  Building2, Users, UserPlus, Calendar, UserCheck, FileText, Activity,
  BookOpen, Star, ChevronRight, Cake, CheckCircle, Clock, UserX,
  Filter, RefreshCw, Heart,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

const formatDate = (d?: string) => {
  if (!d) return '—';
  return new Date(d + 'T00:00:00').toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
};

export default function ResponsableDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [selectedDeptId, setSelectedDeptId] = useState<string | undefined>(undefined);

  const { data: dashboard, isLoading, refetch } = useQuery({
    queryKey: ['dashboard', 'responsable', selectedDeptId],
    queryFn: async () => {
      const res = await api.get('/dashboard/responsable', {
        params: selectedDeptId ? { deptId: selectedDeptId } : undefined,
      });
      return res.data as any;
    },
  });

  const stats = dashboard?.statistiques ?? {};
  const departements = dashboard?.departements ?? [];
  const deptDetail = dashboard?.departement ?? {};
  const anniversaires = deptDetail?.anniversaires ?? [];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Building2 className="w-5 h-5 text-amber-500" />
            <span className="text-sm font-medium text-amber-600 dark:text-amber-400 uppercase tracking-wider">
              {getGreeting()}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            Espace{' '}
            <span className="text-gradient font-display">Responsable</span>
          </h1>
          <p className="page-subtitle">
            Gestion des membres de votre département · {new Date().toLocaleDateString('fr-FR', {
              weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
            })}
          </p>
        </div>

        {/* Department selector (multi-departments) */}
        {departements?.length > 1 && (
          <div className="flex items-center gap-2 mt-3 animate-fade-in">
            <Filter className="w-4 h-4 text-gray-400" />
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Département :</span>
            <select
              value={selectedDeptId ?? dashboard?.selectedDeptId ?? ''}
              onChange={(e) => setSelectedDeptId(e.target.value || undefined)}
              className="px-3 py-1.5 rounded-xl text-sm font-medium bg-white/70 dark:bg-gray-800/70 border border-gray-200 dark:border-gray-700 text-gray-800 dark:text-gray-200 focus:outline-none focus:ring-2 focus:ring-primary-500/40"
            >
              {departements.map((d: any) => (
                <option key={d.id} value={d.id}>{d.nom}</option>
              ))}
            </select>
            <button
              onClick={() => refetch()}
              className="p-2 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
              title="Rafraîchir"
            >
              <RefreshCw className="w-4 h-4 text-gray-400" />
            </button>
          </div>
        )}
      </div>

      {dashboard?.message ? (
        <div className="glass-card p-12 text-center animate-fade-in">
          <Building2 className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-600 dark:text-gray-400 mb-2">{dashboard.message}</h2>
          <p className="text-sm text-gray-400">Contactez le pasteur pour être affecté à un département.</p>
        </div>
      ) : isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="glass-card p-5 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="skeleton h-4 w-24 mb-3 rounded" />
              <div className="skeleton h-8 w-20 rounded" />
            </div>
          ))}
        </div>
      ) : (
        <>
          {/* Active department banner */}
          <div className="glass-card p-4 mb-6 animate-slide-up flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                <Building2 className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs text-gray-400 uppercase tracking-wider">Département actif</p>
                <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100">{dashboard?.selectedDeptNom}</h2>
              </div>
            </div>
            <Link to={`/departments/${dashboard?.selectedDeptId}`} className="btn-ghost btn-xs">
              Voir le détail <ChevronRight className="w-3 h-3 ml-1" />
            </Link>
          </div>

          {/* Member Stats Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
            <div className="stat-card animate-slide-up">
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Membres</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                  <Users className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalMembres ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">dans {dashboard?.selectedDeptNom}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '60ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-green-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Membres actifs</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-green-500 text-white shadow-lg">
                  <CheckCircle className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-emerald-500">{stats.totalActifs ?? 0}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '120ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-500 to-indigo-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Nouveaux membres</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 text-white shadow-lg">
                  <UserPlus className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-blue-500">{stats.nouveauxMembres ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">30 derniers jours</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '180ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-violet-500 to-purple-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Taux de présence</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-violet-500 to-purple-500 text-white shadow-lg">
                  <Activity className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-violet-500">{stats.tauxPresence ?? 0}%</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '240ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-teal-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Rapports reçus</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-500 text-white shadow-lg">
                  <FileText className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.rapportsSoumis ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">/ {stats.rapportsAttendus ?? 0} attendus</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '300ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Anniversaires</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-pink-500 to-rose-500 text-white shadow-lg">
                  <Cake className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-pink-500">{anniversaires.length}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">ce mois-ci</span>
            </div>
          </div>

          {/* Reports progress */}
          <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '360ms' }}>
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-medium text-gray-600 dark:text-gray-400">Progression des rapports</span>
              <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">
                {stats.rapportsSoumis ?? 0} / {stats.rapportsAttendus ?? 0}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-3">
                <div
                  className="h-3 rounded-full bg-gradient-to-r from-amber-500 to-emerald-500 transition-all duration-500"
                  style={{ width: `${Math.min(stats.tauxCompletion ?? 0, 100)}%` }}
                />
              </div>
              <span className="text-xs font-bold text-gray-700 dark:text-gray-300">{stats.tauxCompletion ?? 0}%</span>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Members list */}
            <div className="glass-card p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Users className="w-4 h-4 text-primary-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Membres du département</h3>
                </div>
                <Link to={`/departments/${dashboard?.selectedDeptId}/members`} className="text-[10px] font-medium text-primary-600">
                  Voir tout
                </Link>
              </div>
              <div className="space-y-2 max-h-80 overflow-y-auto">
                {(deptDetail?.membres ?? []).slice(0, 10).map((m: any) => (
                  <Link
                    key={m.id}
                    to={`/souls/${m.id}`}
                    className="flex items-center justify-between p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/40 transition-all"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white shrink-0
                        ${m.statut === 'ACTIF' ? 'bg-emerald-500' : m.statut === 'EN_INTEGRATION' ? 'bg-blue-500' : m.statut === 'EN_VEILLE' ? 'bg-amber-500' : 'bg-red-500'}`}>
                        {m.nom?.split(' ').map((p: string) => p?.[0]).join('').slice(0, 2) || '?'}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{m.nom}</p>
                        <p className="text-[10px] text-gray-400 truncate">
                          {m.familleNom ? `Famille ${m.familleNom}` : 'Sans famille'}
                          {m.faiseurNom ? ` · ${m.faiseurNom}` : ''}
                        </p>
                      </div>
                    </div>
                    <ChevronRight className="w-3.5 h-3.5 text-gray-300 flex-shrink-0" />
                  </Link>
                ))}
                {(deptDetail?.membres ?? []).length === 0 && (
                  <div className="text-center py-8">
                    <UserX className="w-8 h-8 text-gray-300 mx-auto mb-2" />
                    <p className="text-sm text-gray-400">Aucun membre dans ce département</p>
                  </div>
                )}
              </div>
            </div>

            {/* Birthdays + stats */}
            <div className="space-y-6">
              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
                <div className="flex items-center gap-2 mb-4">
                  <Cake className="w-4 h-4 text-pink-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Anniversaires du mois</h3>
                </div>
                <div className="space-y-2">
                  {anniversaires.length === 0 && (
                    <p className="text-sm text-gray-400 text-center py-4">Aucun anniversaire ce mois-ci</p>
                  )}
                  {anniversaires.map((a: any) => (
                    <div key={a.id} className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-pink-500 to-rose-500 flex items-center justify-center text-white">
                          <Cake className="w-4 h-4" />
                        </div>
                        <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{a.nom}</span>
                      </div>
                      <span className="text-xs font-semibold text-pink-500">{formatDate(a.dateNaissance)}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Status distribution */}
              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '150ms' }}>
                <div className="flex items-center gap-2 mb-4">
                  <Activity className="w-4 h-4 text-primary-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Répartition des membres</h3>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: 'Actifs', value: deptDetail.actifs ?? 0, color: 'text-emerald-500' },
                    { label: 'En intégration', value: deptDetail.enIntegration ?? 0, color: 'text-blue-500' },
                    { label: 'En veille', value: deptDetail.enVeille ?? 0, color: 'text-amber-500' },
                    { label: 'Décrochés', value: deptDetail.decroches ?? 0, color: 'text-red-500' },
                  ].map((s) => (
                    <div key={s.label} className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <p className={`text-xl font-bold ${s.color}`}>{s.value}</p>
                      <p className="text-[9px] text-gray-400">{s.label}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6 animate-slide-up">
            <Link to={`/events`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <Calendar className="w-5 h-5 text-primary-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Événements</span>
            </Link>
            <Link to={`/reports`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <FileText className="w-5 h-5 text-amber-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Rapports</span>
            </Link>
            <Link to={`/prayers`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <BookOpen className="w-5 h-5 text-violet-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Prières</span>
            </Link>
            <Link to={`/souls/new`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <UserPlus className="w-5 h-5 text-emerald-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Ajouter un membre</span>
            </Link>
          </div>
        </>
      )}
    </div>
  );
}
