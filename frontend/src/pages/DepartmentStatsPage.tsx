import { useQuery } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '@/lib/api';
import {
  ArrowLeft, BarChart3, Users, Activity, ListTodo, Gavel, Building2,
  Briefcase, UserCheck, Loader2, TrendingUp,
} from 'lucide-react';
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip, Legend, CartesianGrid,
} from 'recharts';

const PIE_COLORS = ['#f59e0b', '#3b82f6', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#64748b'];

export default function DepartmentStatsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: stats, isLoading } = useQuery({
    queryKey: ['department', id, 'stats'],
    queryFn: async () => (await api.get(`/departments/${id}/stats`)).data as any,
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="page-container flex items-center justify-center min-h-[60vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  const e = stats?.effectif ?? {};
  const presence = stats?.presence ?? {};
  const taches = stats?.taches ?? {};
  const equipes = stats?.equipes ?? {};
  const affectations = stats?.affectations ?? {};
  const charge: any[] = stats?.chargeParMembre ?? [];

  return (
    <div className="page-container">
      <div className="page-header">
        <button onClick={() => navigate(`/departments/${id}`)} className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </button>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg">
            <BarChart3 className="w-6 h-6" />
          </div>
          <div className="flex-1">
            <h1 className="page-title">Statistiques du département</h1>
            <p className="page-subtitle">Données réelles — effectif, présence, tâches, discipline, charge de travail</p>
          </div>
        </div>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
        <div className="stat-card p-3 text-center">
          <Users className="w-4 h-4 text-amber-500 mx-auto mb-1" />
          <p className="stat-value text-xl">{e.total ?? 0}</p>
          <span className="stat-label text-[10px]">Membres</span>
        </div>
        <div className="stat-card p-3 text-center">
          <UserCheck className="w-4 h-4 text-emerald-500 mx-auto mb-1" />
          <p className="stat-value text-xl text-emerald-500">{e.actifs ?? 0}</p>
          <span className="stat-label text-[10px]">Actifs</span>
        </div>
        <div className="stat-card p-3 text-center">
          <TrendingUp className="w-4 h-4 text-blue-500 mx-auto mb-1" />
          <p className="stat-value text-xl text-blue-500">{e.nouveaux30j ?? 0}</p>
          <span className="stat-label text-[10px]">Nouveaux (30 j)</span>
        </div>
        <div className="stat-card p-3 text-center">
          <Activity className="w-4 h-4 text-violet-500 mx-auto mb-1" />
          <p className="stat-value text-xl text-violet-500">{presence.taux ?? 0}%</p>
          <span className="stat-label text-[10px]">Taux de présence</span>
        </div>
        <div className="stat-card p-3 text-center">
          <ListTodo className="w-4 h-4 text-red-500 mx-auto mb-1" />
          <p className="stat-value text-xl text-red-500">{taches.enRetard ?? 0}</p>
          <span className="stat-label text-[10px]">Tâches en retard</span>
        </div>
        <div className="stat-card p-3 text-center">
          <Building2 className="w-4 h-4 text-orange-500 mx-auto mb-1" />
          <p className="stat-value text-xl">{equipes.actives ?? 0}</p>
          <span className="stat-label text-[10px]">Équipes actives</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Évolution effectif */}
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <TrendingUp className="w-4 h-4 text-amber-500" /> Évolution de l'effectif (12 mois)
          </h3>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={stats?.evolutionEffectif ?? []}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.2)" />
              <XAxis dataKey="mois" tick={{ fontSize: 10 }} />
              <YAxis tick={{ fontSize: 10 }} />
              <Tooltip />
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Bar dataKey="ajoutes" name="Arrivées" fill="#10b981" radius={[3, 3, 0, 0]} />
              <Bar dataKey="sortis" name="Sorties" fill="#ef4444" radius={[3, 3, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Présence mensuelle */}
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Activity className="w-4 h-4 text-violet-500" /> Taux de présence mensuel (6 mois)
          </h3>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={stats?.evolutionPresence ?? []}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.2)" />
              <XAxis dataKey="mois" tick={{ fontSize: 10 }} />
              <YAxis tick={{ fontSize: 10 }} domain={[0, 100]} />
              <Tooltip />
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Line type="monotone" dataKey="taux" name="Taux (%)" stroke="#8b5cf6" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Statuts des membres */}
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Users className="w-4 h-4 text-blue-500" /> Répartition par statut
          </h3>
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie
                data={[
                  { name: 'Actifs', value: e.actifs ?? 0 },
                  { name: 'En intégration', value: e.enIntegration ?? 0 },
                  { name: 'En veille', value: e.enVeille ?? 0 },
                  { name: 'Décrochés', value: e.decroches ?? 0 },
                ].filter((d) => d.value > 0)}
                dataKey="value" nameKey="name" innerRadius={50} outerRadius={85} paddingAngle={2}
              >
                {PIE_COLORS.map((c) => <Cell key={c} fill={c} />)}
              </Pie>
              <Tooltip />
              <Legend wrapperStyle={{ fontSize: 11 }} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Tâches par statut */}
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <ListTodo className="w-4 h-4 text-emerald-500" /> Tâches par statut
          </h3>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart
              data={Object.entries(taches.parStatut ?? {}).map(([k, v]) => ({ name: k.replace('_', ' '), value: v }))}
              layout="vertical"
            >
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.2)" />
              <XAxis type="number" tick={{ fontSize: 10 }} />
              <YAxis type="category" dataKey="name" width={90} tick={{ fontSize: 10 }} />
              <Tooltip />
              <Bar dataKey="value" name="Tâches" fill="#10b981" radius={[0, 3, 3, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Discipline par catégorie */}
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Gavel className="w-4 h-4 text-red-500" /> Discipline par catégorie
          </h3>
          {Object.keys(stats?.disciplineParCategorie ?? {}).length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-10">Aucun événement disciplinaire</p>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie
                  data={Object.entries(stats.disciplineParCategorie).map(([k, v]) => ({ name: k.replace('_', ' '), value: v }))}
                  dataKey="value" nameKey="name" outerRadius={85} paddingAngle={2}
                >
                  {PIE_COLORS.map((c) => <Cell key={c} fill={c} />)}
                </Pie>
                <Tooltip />
                <Legend wrapperStyle={{ fontSize: 11 }} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Charge de travail */}
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Briefcase className="w-4 h-4 text-orange-500" /> Charge de travail par membre
          </h3>
          {charge.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-10">Aucune tâche ouverte assignée</p>
          ) : (
            <div className="space-y-2">
              {charge.map((c) => (
                <div key={c.memberId} className="flex items-center gap-3 p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{c.memberNom || '—'}</p>
                    <div className="flex items-center gap-2 mt-1">
                      <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                        <div
                          className={`h-2 rounded-full ${c.enRetard > 0 ? 'bg-red-500' : 'bg-amber-500'}`}
                          style={{ width: `${Math.min((c.tachesOuvertes / (charge[0]?.tachesOuvertes || 1)) * 100, 100)}%` }}
                        />
                      </div>
                      <span className="text-[10px] font-semibold text-gray-500">{c.tachesOuvertes} tâches</span>
                    </div>
                  </div>
                  {c.enRetard > 0 && <span className="badge text-[9px] badge-danger">{c.enRetard} en retard</span>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Synthèse organisation */}
      <div className="glass-card p-5 mb-6">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Synthèse de l'organisation</h3>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: 'Équipes actives', value: equipes.actives ?? 0 },
            { label: 'Équipes archivées', value: equipes.archivees ?? 0 },
            { label: 'Postes actifs', value: stats?.postesActifs ?? 0 },
            { label: 'Affectations actives', value: affectations.actives ?? 0 },
            { label: 'Membres affectés', value: affectations.membresAffectes ?? 0 },
            { label: "Taux d'affectation", value: `${affectations.tauxAffectation ?? 0}%` },
            { label: 'Événements à venir', value: stats?.evenements?.length ?? 0 },
            { label: 'Absences (fiches)', value: presence.absents ?? 0 },
          ].map((s) => (
            <div key={s.label} className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{String(s.value)}</p>
              <p className="text-[9px] text-gray-400">{s.label}</p>
            </div>
          ))}
        </div>
      </div>

      <div className="flex justify-end">
        <Link to={`/departments/${id}`} className="btn-ghost btn-sm">
          <ArrowLeft className="w-4 h-4" /> Retour au département
        </Link>
      </div>
    </div>
  );
}
