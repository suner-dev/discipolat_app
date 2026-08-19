import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import {
  Heart, Users, FileText, Activity, Bell, Star, TrendingUp,
  TrendingDown, Loader2, Sparkles, ChevronRight, AlertTriangle,
  UserCheck, CheckCircle, Clock, UserX, XCircle, MessageSquare,
  Calendar, Phone, Mail, MapPin, BookOpen, Target,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from 'recharts';

const COLORS = ['#22c55e', '#f59e0b', '#3b82f6', '#ef4444', '#8b5cf6'];

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

export default function CrmFaiseurPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [filterDifficulte, setFilterDifficulte] = useState(false);

  const scrollToDisciples = (status: string, difficulte = false) => {
    setFilterStatus(status);
    setFilterDifficulte(difficulte);
    document.getElementById('liste-disciples')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const { data: crm, isLoading } = useQuery({
    queryKey: ['crm', 'faiseur'],
    queryFn: async () => {
      const res = await api.get('/dashboard/crm-faiseur');
      return res.data as any;
    },
  });

  const disciples = crm?.disciples ?? [];
  const stats = crm?.statistiques ?? {};
  const alertes = crm?.alertes ?? [];

  const filteredDisciples = disciples.filter((d: any) =>
    (filterStatus === 'all' || d.statut === filterStatus)
    && (!filterDifficulte || d.etatSpirituel === 'EN_DIFFICULTE')
  );

  const pieData = [
    { name: 'Actifs', value: stats.actifs ?? 0, color: '#22c55e' },
    { name: 'Intégration', value: stats.enIntegration ?? 0, color: '#f59e0b' },
    { name: 'Veille', value: stats.enVeille ?? 0, color: '#3b82f6' },
    { name: 'Décrochés', value: stats.decroches ?? 0, color: '#ef4444' },
  ].filter(d => d.value > 0);

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Users className="w-5 h-5 text-emerald-500" />
            <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400 uppercase tracking-wider">
              {getGreeting()}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            CRM{' '}
            <span className="text-gradient font-display">Faiseur</span>
          </h1>
          <p className="page-subtitle">
            Suivi complet de vos disciples · {new Date().toLocaleDateString('fr-FR', {
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
          {/* Stats Cards — cliquables : filtrent la liste des disciples */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <button
              type="button"
              onClick={() => scrollToDisciples('all', false)}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              title="Voir tous vos disciples"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-500 to-green-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Disciples</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-500 to-green-500 text-white shadow-lg">
                  <Heart className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value">{stats.totalDisciples ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">Cliquer pour voir la liste</span>
            </button>
            <button
              type="button"
              onClick={() => scrollToDisciples('ACTIF', false)}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              style={{ animationDelay: '60ms' }}
              title="Voir vos disciples actifs"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-green-500 to-teal-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Actifs</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-green-500 to-teal-500 text-white shadow-lg">
                  <CheckCircle className="w-4 h-4" />
                </div>
              </div>
              <span className="stat-value text-emerald-500">{stats.actifs ?? 0}</span>
              <span className="text-[10px] text-gray-400 mt-1 block">Cliquer pour filtrer</span>
            </button>
            <button
              type="button"
              onClick={() => navigate('/reports/maker')}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
              style={{ animationDelay: '120ms' }}
              title="Ouvrir le rapport hebdomadaire"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-500 to-indigo-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">Rapports soumis</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-500 text-white shadow-lg">
                  <FileText className="w-4 h-4" />
                </div>
              </div>
              <span className={`stat-value ${(stats.rapportsSoumisSemaine ?? 0) > 0 ? 'text-emerald-500' : 'text-amber-500'}`}>
                {stats.rapportsSoumisSemaine ?? 0}
                <span className="text-xs text-gray-400 ml-1">/ {stats.totalDisciples ?? 0}</span>
              </span>
              <span className="text-[10px] text-gray-400 mt-1 block">Rapport hebdomadaire</span>
            </button>
            <button
              type="button"
              onClick={() => scrollToDisciples('all', true)}
              className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${filterDifficulte ? 'ring-2 ring-red-400/40' : ''}`}
              style={{ animationDelay: '180ms' }}
              title="Voir vos disciples en difficulté"
            >
              <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-red-500 to-rose-500 opacity-60" />
              <div className="flex items-start justify-between mb-3">
                <span className="stat-label">En difficulté</span>
                <div className="p-2 rounded-xl bg-gradient-to-br from-red-500 to-rose-500 text-white shadow-lg">
                  <AlertTriangle className="w-4 h-4" />
                </div>
              </div>
              <span className={`stat-value ${(stats.enDifficulte ?? 0) > 0 ? 'text-red-500' : 'text-green-500'}`}>
                {stats.enDifficulte ?? 0}
              </span>
              <span className="text-[10px] text-gray-400 mt-1 block">Cliquer pour filtrer</span>
            </button>
          </div>

          {/* Chart + Alerts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
            {/* Pie chart */}
            <div className="glass-card p-6 animate-slide-up">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Répartition</h3>
              <div className="h-48">
                {pieData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={pieData} cx="50%" cy="50%" innerRadius={40} outerRadius={65}
                        paddingAngle={3} dataKey="value">
                        {pieData.map((entry, i) => (
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

            {/* Alerts */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '60ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Bell className="w-4 h-4 text-amber-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Alertes</h3>
                </div>
                <span className="text-[10px] text-gray-400">{alertes.length}</span>
              </div>
              {alertes.length > 0 ? (
                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {alertes.slice(0, 5).map((alert: any, i: number) => (
                    <div key={i} className={`p-2.5 rounded-xl flex items-center gap-2 cursor-pointer hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors ${
                      alert.priorite === 'HAUTE' ? 'bg-red-50/50 dark:bg-red-900/10 border border-red-200/30 dark:border-red-800/20' : 'bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/30 dark:border-amber-800/20'
                    }`}
                      onClick={() => navigate(`/souls/${alert.soulId}`)}>
                      <AlertTriangle className={`w-4 h-4 flex-shrink-0 ${alert.priorite === 'HAUTE' ? 'text-red-500' : 'text-amber-500'}`} />
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{alert.soulNom}</p>
                        <p className="text-[9px] text-gray-400">{alert.message}</p>
                      </div>
                      <span className={`text-[8px] font-semibold px-1.5 py-0.5 rounded-full ${
                        alert.priorite === 'HAUTE' ? 'bg-red-100 dark:bg-red-900/30 text-red-600' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-600'
                      }`}>{alert.priorite}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8">
                  <CheckCircle className="w-8 h-8 text-green-500 mx-auto mb-2" />
                  <p className="text-xs text-gray-400">Aucune alerte</p>
                </div>
              )}
            </div>

            {/* Quick actions */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Actions rapides</h3>
              <div className="space-y-2">
                <Link to="/reports/maker" className="flex items-center gap-3 p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 hover:bg-gray-100/50 dark:hover:bg-gray-800/50 transition-colors">
                  <FileText className="w-4 h-4 text-primary-500" />
                  <span className="text-sm text-gray-700 dark:text-gray-300">Rapport hebdomadaire</span>
                  <ChevronRight className="w-3 h-3 text-gray-400 ml-auto" />
                </Link>
                <Link to="/souls/new" className="flex items-center gap-3 p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 hover:bg-gray-100/50 dark:hover:bg-gray-800/50 transition-colors">
                  <Heart className="w-4 h-4 text-emerald-500" />
                  <span className="text-sm text-gray-700 dark:text-gray-300">Nouveau disciple</span>
                  <ChevronRight className="w-3 h-3 text-gray-400 ml-auto" />
                </Link>
                <Link to="/prayers" className="flex items-center gap-3 p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 hover:bg-gray-100/50 dark:hover:bg-gray-800/50 transition-colors">
                  <BookOpen className="w-4 h-4 text-violet-500" />
                  <span className="text-sm text-gray-700 dark:text-gray-300">Demande de prière</span>
                  <ChevronRight className="w-3 h-3 text-gray-400 ml-auto" />
                </Link>
              </div>
            </div>
          </div>

          {/* Filter Tabs */}
          <div className="flex items-center gap-2 mb-4 flex-wrap">
            <button onClick={() => setFilterStatus('all')}
              className={`text-xs font-medium px-3 py-1.5 rounded-full transition-all ${
                filterStatus === 'all' ? 'bg-primary-500/20 text-primary-600 dark:text-primary-400' : 'bg-gray-100 dark:bg-gray-800/50 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}>Tous ({disciples.length})</button>
            {[
              { key: 'ACTIF', label: 'Actifs', color: 'text-green-600 bg-green-100 dark:bg-green-900/20' },
              { key: 'EN_INTEGRATION', label: 'Intégration', color: 'text-amber-600 bg-amber-100 dark:bg-amber-900/20' },
              { key: 'EN_VEILLE', label: 'Veille', color: 'text-blue-600 bg-blue-100 dark:bg-blue-900/20' },
              { key: 'DECROCHE', label: 'Décrochés', color: 'text-red-600 bg-red-100 dark:bg-red-900/20' },
            ].map(f => (
              <button key={f.key} onClick={() => setFilterStatus(f.key)}
                className={`text-xs font-medium px-3 py-1.5 rounded-full transition-all ${
                  filterStatus === f.key ? f.color : 'bg-gray-100 dark:bg-gray-800/50 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
                }`}>{f.label}</button>
            ))}
          </div>

          {/* Disciples List */}
          <div id="liste-disciples" className="space-y-3 scroll-mt-24">
            {filteredDisciples.length === 0 ? (
              <div className="glass-card p-12 text-center animate-fade-in">
                <Heart className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                <p className="text-sm text-gray-500">Aucun disciple trouvé</p>
              </div>
            ) : (
              filteredDisciples.map((disciple: any, i: number) => (
                <div key={disciple.id}
                  className="glass-card p-4 animate-slide-up cursor-pointer hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-all duration-200"
                  style={{ animationDelay: `${i * 40}ms` }}
                  onClick={() => navigate(`/souls/${disciple.id}`)}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold text-white flex-shrink-0 ${
                        disciple.statut === 'ACTIF' ? 'bg-gradient-to-br from-emerald-500 to-green-600' :
                        disciple.statut === 'EN_INTEGRATION' ? 'bg-gradient-to-br from-amber-500 to-orange-600' :
                        disciple.statut === 'EN_VEILLE' ? 'bg-gradient-to-br from-blue-500 to-indigo-600' :
                        'bg-gradient-to-br from-red-500 to-rose-600'
                      }`}>
                        {disciple.nom?.charAt(0)?.toUpperCase() || '?'}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{disciple.nom}</p>
                          {disciple.etatSpirituel === 'EN_DIFFICULTE' && (
                            <AlertTriangle className="w-3 h-3 text-red-500 flex-shrink-0" />
                          )}
                        </div>
                        <div className="flex items-center gap-2 mt-0.5 flex-wrap">
                          <span className={`text-[9px] font-medium px-1.5 py-0.5 rounded-full ${
                            disciple.statut === 'ACTIF' ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' :
                            disciple.statut === 'EN_INTEGRATION' ? 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400' :
                            disciple.statut === 'EN_VEILLE' ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400' :
                            'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400'
                          }`}>
                            {disciple.statut === 'ACTIF' ? 'Actif' : disciple.statut === 'EN_INTEGRATION' ? 'Intégration' : disciple.statut === 'EN_VEILLE' ? 'Veille' : 'Décroché'}
                          </span>
                          <span className="flex items-center gap-0.5">
                            {[1,2,3,4,5].map(s => (
                              <Star key={s} className={`w-2.5 h-2.5 ${s <= (disciple.niveauCroissance || 1) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                            ))}
                          </span>
                          {disciple.telephone && (
                            <span className="text-[9px] text-gray-400 flex items-center gap-0.5">
                              <Phone className="w-2.5 h-2.5" /> {disciple.telephone}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Right: status indicators */}
                    <div className="flex flex-col items-end gap-1 flex-shrink-0">
                      <div className="flex items-center gap-1.5">
                        <span className={`text-[9px] font-semibold px-1.5 py-0.5 rounded-full ${
                          disciple.rapportSoumis ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'
                        }`}>
                          {disciple.rapportSoumis ? 'Rapport ✓' : 'En attente'}
                        </span>
                      </div>
                      {disciple.dateDernierContact && (
                        <span className="text-[9px] text-gray-400">
                          Contact: {new Date(disciple.dateDernierContact).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
                        </span>
                      )}
                      {disciple.nbNotes > 0 && (
                        <span className="text-[9px] text-gray-400 flex items-center gap-0.5">
                          <MessageSquare className="w-2.5 h-2.5" /> {disciple.nbNotes} note{disciple.nbNotes > 1 ? 's' : ''}
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Difficultés */}
                  {disciple.difficultes && (
                    <div className="mt-2 p-2 rounded-lg bg-red-50/50 dark:bg-red-900/10 border border-red-200/30 dark:border-red-800/20">
                      <p className="text-[10px] text-red-600 dark:text-red-400">
                        <AlertTriangle className="w-3 h-3 inline mr-1" />
                        {disciple.difficultes}
                      </p>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>

          {/* Upcoming Visits */}
          {crm?.visites && crm.visites.length > 0 && (
            <div className="glass-card p-6 mt-6 animate-slide-up">
              <div className="flex items-center gap-2 mb-3">
                <Calendar className="w-4 h-4 text-teal-500" />
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Prochaines visites ({crm.visites.length})</h3>
              </div>
              <div className="space-y-2">
                {crm.visites.slice(0, 5).map((v: any) => (
                  <div key={v.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
                    <div className="w-8 h-8 rounded-lg bg-teal-50 dark:bg-teal-900/20 flex items-center justify-center"><Calendar className="w-4 h-4 text-teal-500" /></div>
                    <div className="min-w-0 flex-1"><p className="text-xs font-medium text-gray-900 dark:text-gray-100">{v.ameNom || '—'}</p><p className="text-[9px] text-gray-400">{v.datePrevue ? new Date(v.datePrevue).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) : '—'} · {v.typeVisite}</p></div>
                    <span className={`text-[9px] font-semibold px-1.5 py-0.5 rounded-full ${v.statut === 'PLANIFIEE' ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400' : 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'}`}>{v.statut}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Prayers */}
          {crm?.prieres && crm.prieres.length > 0 && (
            <div className="glass-card p-6 mt-6 animate-slide-up" style={{ animationDelay: '40ms' }}>
              <div className="flex items-center gap-2 mb-3">
                <BookOpen className="w-4 h-4 text-indigo-500" />
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Prières de mes disciples ({crm.prieres.length})</h3>
              </div>
              <div className="space-y-2">
                {crm.prieres.slice(0, 5).map((p: any) => (
                  <div key={p.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${p.statut === 'EXAUCEE' ? 'bg-green-50 dark:bg-green-900/20' : 'bg-indigo-50 dark:bg-indigo-900/20'}`}>
                      {p.statut === 'EXAUCEE' ? <CheckCircle className="w-4 h-4 text-green-500" /> : <BookOpen className="w-4 h-4 text-indigo-500" />}
                    </div>
                    <div className="min-w-0 flex-1"><p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{p.titre}</p><p className="text-[9px] text-gray-400">{p.priorite} · {p.auteurNom || '—'}</p></div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Progression */}
          {crm?.progression && (
            <div className="glass-card p-6 mt-6 animate-slide-up" style={{ animationDelay: '80ms' }}>
              <div className="flex items-center gap-2 mb-3">
                <TrendingUp className="w-4 h-4 text-amber-500" />
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Ma progression</h3>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 text-center">
                  <p className="text-xl font-bold text-amber-600 dark:text-amber-400">{crm.progression.totalNotes ?? 0}</p>
                  <p className="text-[10px] text-gray-400">Notes ajoutées</p>
                </div>
                <div className="p-3 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 text-center">
                  <p className="text-xl font-bold text-blue-600 dark:text-blue-400">{crm.progression.visitesRealisees ?? 0}</p>
                  <p className="text-[10px] text-gray-400">Visites réalisées</p>
                </div>
                <div className="p-3 rounded-xl bg-green-50/50 dark:bg-green-900/10 text-center">
                  <p className="text-xl font-bold text-green-600 dark:text-green-400">{crm.progression.rapportsSoumis ?? 0}</p>
                  <p className="text-[10px] text-gray-400">Rapports soumis</p>
                </div>
                <div className="p-3 rounded-xl bg-indigo-50/50 dark:bg-indigo-900/10 text-center">
                  <p className="text-xl font-bold text-indigo-600 dark:text-indigo-400">{crm.progression.prieres ?? 0}</p>
                  <p className="text-[10px] text-gray-400">Prières actives</p>
                </div>
              </div>
            </div>
          )}

          {/* Week info */}
          <div className="text-center mt-6">
            <p className="text-[10px] text-gray-400">
              Semaine du {stats.semaine ? new Date(stats.semaine + 'T00:00:00').toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }) : '—'}
            </p>
          </div>
        </>
      )}
    </div>
  );
}
