import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import {
  Building2, Users, Heart, UserCheck, FileText, Activity, Bell,
  Calendar, BookOpen, Star, AlertTriangle, TrendingUp, TrendingDown,
  Loader2, Sparkles, ChevronRight, Church, Target, MessageSquare,
  FolderOpen, Eye, CheckCircle, Clock, UserX,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';


const COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6'];

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

export default function ResponsableDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard', 'responsable'],
    queryFn: async () => {
      const res = await api.get('/dashboard/responsable');
      return res.data as any;
    },
  });

  const stats = dashboard?.statistiques ?? {};
  const departements = dashboard?.departements ?? [];

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
            Gestion de votre département · {new Date().toLocaleDateString('fr-FR', {
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
      ) : (
        <>
          {/* Global Stats Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
            <div className="stat-card animate-slide-up">
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Total âmes</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                  <Heart className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalAmes ?? 0}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '60ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-green-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Âmes actives</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-green-500 text-white shadow-lg">
                  <CheckCircle className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-emerald-500">{stats.totalActifs ?? 0}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '120ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-500 to-indigo-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Familles</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 text-white shadow-lg">
                  <Users className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalFamilles ?? 0}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '180ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-violet-500 to-purple-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Faiseurs</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-violet-500 to-purple-500 text-white shadow-lg">
                  <UserCheck className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalFaiseurs ?? 0}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '240ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-teal-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Rapports soumis</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-500 text-white shadow-lg">
                  <FileText className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.rapportsSoumis ?? 0}</span>
            </div>
            <div className="stat-card animate-slide-up" style={{ animationDelay: '300ms' }}>
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-500 to-orange-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Taux complétion</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                  <Activity className="w-4 h-4" />
                </div>
              </div>
              <span className={`stat-value ${(stats.tauxCompletion ?? 0) >= 70 ? 'text-emerald-500' : 'text-amber-500'}`}>
                {stats.tauxCompletion ?? 0}%
              </span>
            </div>
          </div>

          {/* Progress bar for reports */}
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

          {/* Departments List */}
          {departements?.length > 0 && (
            <div className="space-y-4">
              {departements.map((dept: any, idx: number) => (
                <div key={dept.id} className="glass-card p-6 animate-slide-up" style={{ animationDelay: `${(idx + 1) * 80}ms` }}>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <div className="p-2 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
                        <Building2 className="w-4 h-4" />
                      </div>
                      <div>
                        <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100">{dept.nom}</h3>
                        {dept.description && (
                          <p className="text-xs text-gray-400">{dept.description}</p>
                        )}
                      </div>
                    </div>
                    <Link to={`/departments/${dept.id}`} className="btn-ghost btn-xs">
                      Détail <ChevronRight className="w-3 h-3 ml-1" />
                    </Link>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
                    <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-xl font-bold text-amber-500">{dept.totalFamilles}</p>
                      <p className="text-[9px] text-gray-400">Familles</p>
                    </div>
                    <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-xl font-bold text-primary-500">{dept.totalAmes}</p>
                      <p className="text-[9px] text-gray-400">Âmes</p>
                    </div>
                    <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-xl font-bold text-violet-500">{dept.totalFaiseurs}</p>
                      <p className="text-[9px] text-gray-400">Faiseurs</p>
                    </div>
                    <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-xl font-bold text-emerald-500">{dept.tauxPresence ?? 0}%</p>
                      <p className="text-[9px] text-gray-400">Présence</p>
                    </div>
                  </div>

                  {/* Mini progress for reports */}
                  <div className="flex items-center justify-between text-[10px] text-gray-400">
                    <span>Rapports</span>
                    <span className="font-medium text-gray-700 dark:text-gray-300">
                      {dept.rapportsSoumis ?? 0} / {dept.rapportsAttendus ?? 0} soumis
                    </span>
                  </div>
                  <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-1.5 mt-1">
                    <div
                      className="h-1.5 rounded-full bg-gradient-to-r from-amber-500 to-emerald-500 transition-all duration-500"
                      style={{ width: `${Math.min((dept.rapportsAttendus ?? 0) > 0 ? (dept.rapportsSoumis ?? 0) / (dept.rapportsAttendus ?? 1) * 100 : 0, 100)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}

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
            <Link to={`/evaluations`} className="glass-card p-4 text-center hover:bg-gray-50/80 dark:hover:bg-gray-800/40 transition-all duration-200">
              <Star className="w-5 h-5 text-gold-500 mx-auto mb-1" />
              <span className="text-[10px] font-medium text-gray-600 dark:text-gray-400">Évaluations</span>
            </Link>
          </div>

          {departements?.length === 0 && (
            <div className="glass-card p-12 text-center animate-fade-in mt-6">
              <Building2 className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <h2 className="text-lg font-semibold text-gray-600 dark:text-gray-400 mb-2">Aucun département assigné</h2>
              <p className="text-sm text-gray-400">Vous n'êtes pas encore responsable d'un département.</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
