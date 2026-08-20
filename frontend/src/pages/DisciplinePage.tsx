import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { Gavel, Filter, AlertTriangle, CheckCircle, Clock } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';

type DisciplineEvent = {
  id: string;
  soulNom?: string;
  soulId?: string;
  categorie: string;
  typeEvenement: string;
  gravite: string;
  description: string;
  statut: string;
  createdAt: string;
  resolvedAt?: string;
};

const CATEGORIE_LABELS: Record<string, string> = {
  COMPORTEMENT: 'Comportement',
  ASSIDUITE: 'Assiduité',
  CAPACITE: 'Capacité',
  PROGRESSION: 'Progression',
  INCIDENT: 'Incident',
  DISCIPLINE: 'Discipline',
};

const GRAVITE_COLORS: Record<string, string> = {
  FAIBLE: 'badge-info',
  MOYENNE: 'badge-warning',
  GRAVE: 'badge-danger',
  CRITIQUE: 'badge-danger',
};

export default function DisciplinePage() {
  const [filterCategorie, setFilterCategorie] = useState('all');
  const [filterStatut, setFilterStatut] = useState('all');

  const { data: souls = [] } = useQuery({
    queryKey: ['souls', 'for-discipline'],
    queryFn: async () => {
      const res = await api.get('/souls', { params: { size: 200 } });
      return (res.data?.content ?? res.data ?? []) as any[];
    },
  });

  // Fetch discipline events for each soul (up to 50)
  const soulIds = (souls || []).slice(0, 50).map((s: any) => s.id).filter(Boolean);

  const { data: events = [], isLoading } = useQuery({
    queryKey: ['discipline', 'all', soulIds.join(',')],
    queryFn: async () => {
      const all: DisciplineEvent[] = [];
      for (const soulId of soulIds) {
        try {
          const res = await api.get(`/souls/${soulId}/discipline`, { params: { size: 50 } });
          const items = (res.data?.content ?? res.data ?? []) as any[];
          const soul = souls.find((s: any) => s.id === soulId);
          for (const item of items) {
            all.push({ ...item, soulNom: soul?.nom, soulId });
          }
        } catch (_) {}
      }
      all.sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
      return all;
    },
    enabled: soulIds.length > 0,
  });

  const filtered = events.filter((e) => {
    if (filterCategorie !== 'all' && e.categorie !== filterCategorie) return false;
    if (filterStatut !== 'all' && e.statut !== filterStatut) return false;
    return true;
  });

  const enCours = events.filter(e => e.statut === 'EN_COURS').length;
  const resolus = events.filter(e => e.statut === 'RESOLU').length;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Gavel className="w-5 h-5 text-amber-500" />
            <span className="text-sm font-medium text-amber-600 dark:text-amber-400 uppercase tracking-wider">
              Suivi disciplinaire
            </span>
          </div>
          <h1 className="page-title">
            <span className="text-gradient font-display">Discipline</span>
          </h1>
          <p className="page-subtitle">Événements disciplinaires du département</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        {[
          { label: 'Total', value: events.length, icon: Gavel, color: 'text-blue-500', bg: 'bg-blue-500/10' },
          { label: 'En cours', value: enCours, icon: Clock, color: 'text-amber-500', bg: 'bg-amber-500/10' },
          { label: 'Résolus', value: resolus, icon: CheckCircle, color: 'text-emerald-500', bg: 'bg-emerald-500/10' },
        ].map((s) => (
          <div key={s.label} className="glass-card p-4 text-center animate-slide-up">
            <div className={`p-2 rounded-xl ${s.bg} inline-block mb-2`}>
              <s.icon className={`w-4 h-4 ${s.color}`} />
            </div>
            <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
            <p className="text-[10px] text-gray-400">{s.label}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex items-center gap-2 mb-4 flex-wrap">
        <Filter className="w-4 h-4 text-gray-400" />
        <select
          value={filterCategorie}
          onChange={(e) => setFilterCategorie(e.target.value)}
          className="px-3 py-1.5 rounded-xl text-xs font-medium bg-white/70 dark:bg-gray-800/70 border border-gray-200 dark:border-gray-700"
        >
          <option value="all">Toutes catégories</option>
          {Object.entries(CATEGORIE_LABELS).map(([k, v]) => (
            <option key={k} value={k}>{v}</option>
          ))}
        </select>
        <select
          value={filterStatut}
          onChange={(e) => setFilterStatut(e.target.value)}
          className="px-3 py-1.5 rounded-xl text-xs font-medium bg-white/70 dark:bg-gray-800/70 border border-gray-200 dark:border-gray-700"
        >
          <option value="all">Tous statuts</option>
          <option value="EN_COURS">En cours</option>
          <option value="RESOLU">Résolu</option>
        </select>
      </div>

      {/* Events list */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2, 3].map(i => <div key={i} className="skeleton h-24 w-full rounded-xl" />)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass-card p-12 text-center animate-fade-in">
          <Gavel className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-600 dark:text-gray-400 mb-2">Aucun événement</h2>
          <p className="text-sm text-gray-400">Aucun événement disciplinaire ne correspond aux filtres.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((ev, i) => (
            <div
              key={ev.id}
              className="glass-card p-4 animate-slide-up"
              style={{ animationDelay: `${i * 30}ms` }}
            >
              <div className="flex items-start gap-3">
                <div className={`p-2 rounded-xl shrink-0 ${
                  ev.statut === 'RESOLU' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-amber-500/10 text-amber-500'
                }`}>
                  {ev.statut === 'RESOLU' ? <CheckCircle className="w-5 h-5" /> : <AlertTriangle className="w-5 h-5" />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
                      {ev.soulNom ?? '—'}
                    </p>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className={`badge text-[10px] ${GRAVITE_COLORS[ev.gravite] || 'badge-gray'}`}>
                        {ev.gravite}
                      </span>
                      <span className={`badge text-[10px] ${ev.statut === 'RESOLU' ? 'badge-success' : 'badge-warning'}`}>
                        {ev.statut === 'RESOLU' ? 'Résolu' : 'En cours'}
                      </span>
                    </div>
                  </div>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                    {CATEGORIE_LABELS[ev.categorie] || ev.categorie}
                    {ev.typeEvenement ? ` · ${ev.typeEvenement}` : ''}
                  </p>
                  {ev.description && (
                    <p className="text-xs text-gray-600 dark:text-gray-300 mt-2">{ev.description}</p>
                  )}
                  <div className="flex items-center gap-3 mt-2">
                    {ev.soulId && (
                      <Link to={`/souls/${ev.soulId}`} className="text-[10px] text-primary-600 hover:underline">
                        Voir la fiche
                      </Link>
                    )}
                    <span className="text-[10px] text-gray-400">
                      {ev.createdAt?.length > 10 ? ev.createdAt.substring(0, 10) : ev.createdAt}
                    </span>
                    {ev.resolvedAt && (
                      <span className="text-[10px] text-emerald-500">
                        Résolu {ev.resolvedAt.length > 10 ? ev.resolvedAt.substring(0, 10) : ev.resolvedAt}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
