import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Search,
  ChevronLeft,
  ChevronRight,
  Target,
  Users,
  History,
  ArrowRight,
  CheckCircle2,
  Sprout,
  Church,
  UserPlus,
  Download,
  BarChart3,
  Trophy,
} from 'lucide-react';
import type {
  EvangelismTrack,
  EvangelismStats,
  EvangelismEtape,
  EvangelismHistoryEntry,
  UpdateEvangelismRequest,
} from '@/types';

const ETAPES: { etape: EvangelismEtape; label: string; icon: 'ame' | 'contact' | 'culte' | 'bapteme' | 'leader' }[] = [
  { etape: 'NOUVELLE_AME', label: 'Nouvelle âme', icon: 'ame' },
  { etape: 'PREMIER_CONTACT', label: 'Premier contact', icon: 'contact' },
  { etape: 'VISITE', label: 'Visite', icon: 'contact' },
  { etape: 'INVITATION', label: 'Invitation', icon: 'contact' },
  { etape: 'PREMIER_CULTE', label: 'Premier culte', icon: 'culte' },
  { etape: 'SUIVI', label: 'Suivi', icon: 'contact' },
  { etape: 'BAPTEME', label: 'Baptême', icon: 'bapteme' },
  { etape: 'DEPARTEMENT', label: 'Département', icon: 'culte' },
  { etape: 'FAMILLE', label: 'Famille', icon: 'culte' },
  { etape: 'DISCIPOLAT', label: 'Discipolat', icon: 'culte' },
  { etape: 'LEADER', label: 'Leader', icon: 'leader' },
];

const ETAPE_COLORS: Record<EvangelismEtape, string> = {
  NOUVELLE_AME: 'bg-emerald-500',
  PREMIER_CONTACT: 'bg-teal-500',
  VISITE: 'bg-cyan-500',
  INVITATION: 'bg-sky-500',
  PREMIER_CULTE: 'bg-blue-500',
  SUIVI: 'bg-indigo-500',
  BAPTEME: 'bg-violet-500',
  DEPARTEMENT: 'bg-purple-500',
  FAMILLE: 'bg-fuchsia-500',
  DISCIPOLAT: 'bg-rose-500',
  LEADER: 'bg-amber-500',
};

function etapeIndex(etape: EvangelismEtape) {
  return ETAPES.findIndex(e => e.etape === etape);
}

function etapeIcon(etape: EvangelismEtape, className = 'w-5 h-5') {
  const icon = ETAPES.find(e => e.etape === etape)?.icon ?? 'ame';
  switch (icon) {
    case 'ame': return <UserPlus className={className} />;
    case 'contact': return <Users className={className} />;
    case 'culte': return <Church className={className} />;
    case 'bapteme': return <CheckCircle2 className={className} />;
    case 'leader': return <Sprout className={className} />;
  }
}

export default function EvangelismPage() {
  const [search, setSearch] = useState('');
  const [selectedEtape, setSelectedEtape] = useState<EvangelismEtape | ''>('');
  const [expanded, setExpanded] = useState<string | null>(null);
  const [historyOpen, setHistoryOpen] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<'etape' | 'date' | 'name'>('etape');
  const queryClient = useQueryClient();

  const statsQuery = useQuery({
    queryKey: ['evangelism', 'stats'],
    queryFn: async () => {
      const res = await api.get('/evangelism/stats');
      return res.data as EvangelismStats;
    },
  });

  const tracksQuery = useQuery({
    queryKey: ['evangelism', 'tracks', selectedEtape, search],
    queryFn: async () => {
      const res = await api.get('/evangelism', {
        params: { etape: selectedEtape || undefined, search: search || undefined },
      });
      return res.data as EvangelismTrack[];
    },
  });

  const historyQuery = useQuery({
    queryKey: ['evangelism', 'history', historyOpen],
    queryFn: async () => {
      const res = await api.get(`/evangelism/souls/${historyOpen}/history`);
      return res.data as EvangelismHistoryEntry[];
    },
    enabled: !!historyOpen,
  });

  const updateMutation = useMutation({
    mutationFn: async ({ soulId, payload }: { soulId: string; payload: UpdateEvangelismRequest }) => {
      const res = await api.put(`/evangelism/souls/${soulId}`, payload);
      return res.data as EvangelismTrack;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['evangelism'] });
      toast.success('Étape du pipeline mise à jour');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const stats = statsQuery.data;
  const tracks = tracksQuery.data ?? [];

  // Export CSV
  const exportCsv = () => {
    const rows = [['Nom', 'Étape', 'Progression', 'Date étape', 'Note']];
    tracks.forEach((t) => {
      const idx = etapeIndex(t.etape);
      rows.push([
        t.soulNom || '',
        ETAPES[idx]?.label || t.etape,
        `${progressionPct(t.etape)}%`,
        new Date(t.dateEtape).toLocaleDateString('fr-FR'),
        t.note || '',
      ]);
    });
    const csv = rows.map(r => r.map(c => `"${c}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `evangelisation_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Sort tracks
  const sortedTracks = [...tracks].sort((a, b) => {
    if (sortBy === 'etape') return etapeIndex(a.etape) - etapeIndex(b.etape);
    if (sortBy === 'date') return new Date(b.dateEtape).getTime() - new Date(a.dateEtape).getTime();
    return (a.soulNom || '').localeCompare(b.soulNom || '');
  });

  // Funnel stats for summary
  const funnelStats = useMemo(() => {
    const total = tracks.length;
    const actifs = tracks.filter(t => !['LEADER'].includes(t.etape)).length;
    const convertis = tracks.filter(t => ['BAPTEME', 'DEPARTEMENT', 'FAMILLE', 'DISCIPOLAT', 'LEADER'].includes(t.etape)).length;
    const leaders = tracks.filter(t => t.etape === 'LEADER').length;
    return { total, actifs, convertis, leaders, tauxConversion: total > 0 ? Math.round((convertis / total) * 100) : 0 };
  }, [tracks]);

  const move = (track: EvangelismTrack, delta: 1 | -1) => {
    const next = etapeIndex(track.etape) + delta;
    if (next < 0 || next >= ETAPES.length) return;
    updateMutation.mutate({ soulId: track.soulId, payload: { etape: ETAPES[next].etape } });
  };

  const progressionPct = (etape: EvangelismEtape) =>
    Math.round((etapeIndex(etape) / (ETAPES.length - 1)) * 100);

  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Suivi d'évangélisation</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Pipeline de croissance : de la nouvelle âme jusqu'au leadership
          </p>
        </div>
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher une âme…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="input w-full pl-9"
          />
        </div>
        <div className="flex gap-2">
          <button onClick={exportCsv} className="btn-secondary btn-sm">
            <Download className="w-4 h-4" /> Export
          </button>
          <select
            value={sortBy}
            onChange={e => setSortBy(e.target.value as typeof sortBy)}
            className="input w-auto text-xs"
          >
            <option value="etape">Trier par étape</option>
            <option value="date">Trier par date</option>
            <option value="name">Trier par nom</option>
          </select>
        </div>
      </div>

      {/* Funnel stats */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="glass-card p-4">
          <div className="flex items-center gap-2 text-emerald-500">
            <Target className="w-4 h-4" />
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Âmes suivies</span>
          </div>
          <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 mt-1">{stats?.totalAmes ?? '—'}</p>
        </div>
        {ETAPES.slice(0, 5).map(e => (
          <div key={e.etape} className="glass-card p-4">
            <div className={`w-2 h-2 rounded-full ${ETAPE_COLORS[e.etape]} mb-2`} />
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400 truncate">{e.label}</p>
            <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{stats?.parEtape?.[e.etape] ?? 0}</p>
          </div>
        ))}
      </div>

      {/* Summary KPIs */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: 'Total âmes', value: funnelStats.total, icon: Target, color: 'from-primary-500 to-primary-600' },
          { label: 'Taux conversion', value: `${funnelStats.tauxConversion}%`, icon: Trophy, color: 'from-emerald-500 to-green-500' },
          { label: 'Convertis', value: funnelStats.convertis, icon: CheckCircle2, color: 'from-violet-500 to-purple-500' },
          { label: 'Leaders', value: funnelStats.leaders, icon: Sprout, color: 'from-amber-500 to-orange-500' },
        ].map((s, i) => (
          <div key={s.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 50}ms` }}>
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{s.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.color} text-white shadow-sm`}>
                <s.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{s.value}</p>
          </div>
        ))}
      </div>

      {/* Pipeline horizontal */}
      <div className="glass-card p-4 overflow-x-auto">
        <div className="flex items-center gap-1 min-w-max">
          {ETAPES.map((e, idx) => {
            const count = stats?.parEtape?.[e.etape] ?? 0;
            const active = selectedEtape === e.etape;
            return (
              <div key={e.etape} className="flex items-center">
                <button
                  onClick={() => setSelectedEtape(active ? '' : e.etape)}
                  className={`flex flex-col items-center gap-1 px-3 py-2 rounded-xl transition-all hover:bg-gray-50 dark:hover:bg-gray-800/60 ${
                    active ? 'ring-2 ring-primary-500 bg-primary-50 dark:bg-primary-500/10' : ''
                  }`}
                  title={`${e.label} — ${count} âme(s)`}
                >
                  <span className={`w-9 h-9 rounded-full flex items-center justify-center text-white shadow-sm ${ETAPE_COLORS[e.etape]}`}>
                    {etapeIcon(e.etape)}
                  </span>
                  <span className="text-[10px] font-medium text-gray-600 dark:text-gray-300">{e.label}</span>
                  <span className="text-[10px] font-bold text-gray-400">{count}</span>
                </button>
                {idx < ETAPES.length - 1 && (
                  <ArrowRight className="w-4 h-4 text-gray-300 dark:text-gray-600 shrink-0" />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* List of souls */}
      <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
        <div className="px-4 py-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <Users className="w-4 h-4 text-primary-500" />
            Âmes dans le pipeline {selectedEtape && <span className="text-gray-400">— {ETAPES.find(e => e.etape === selectedEtape)?.label}</span>}
          </h2>
          <span className="text-xs text-gray-400">{tracks.length} résultat(s)</span>
        </div>

        {tracksQuery.isLoading ? (
          <div className="p-8 text-center text-sm text-gray-400">Chargement…</div>
        ) : tracks.length === 0 ? (
          <div className="p-12 text-center">
            <Sprout className="w-10 h-10 text-gray-300 mx-auto mb-3" />
            <p className="text-sm text-gray-500 dark:text-gray-400">Aucune âme dans cette étape du pipeline.</p>
            <p className="text-xs text-gray-400 mt-1">Créez une âme puis ouvrez sa fiche pour la faire progresser.</p>
          </div>
        ) : (
          sortedTracks.map(track => {
            const idx = etapeIndex(track.etape);
            const isExpanded = expanded === track.soulId;
            const showHistory = historyOpen === track.soulId;
            return (
              <div key={track.id} className="px-4 py-3 hover:bg-gray-50/60 dark:hover:bg-gray-800/30 transition-colors">
                <div className="flex items-center gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <Link
                        to={`/souls/${track.soulId}`}
                        className="text-sm font-semibold text-gray-900 dark:text-gray-100 hover:text-primary-600 truncate"
                      >
                        {track.soulNom ?? 'Âme'}
                      </Link>
                      {track.note && (
                        <button
                          onClick={() => setExpanded(isExpanded ? null : track.soulId)}
                          className="text-[10px] font-medium text-primary-600 hover:text-primary-700 bg-primary-50 dark:bg-primary-500/10 px-2 py-0.5 rounded-full transition-colors"
                          title="Afficher la note"
                        >
                          {isExpanded ? 'Masquer la note' : 'Voir la note'}
                        </button>
                      )}
                    </div>
                    <div className="flex items-center gap-2 mt-1">
                      <span className={`inline-flex items-center gap-1 text-[10px] font-medium text-white px-2 py-0.5 rounded-full ${ETAPE_COLORS[track.etape]}`}>
                        {etapeIcon(track.etape, 'w-3 h-3')}
                        {ETAPES[idx].label}
                      </span>
                      <span className="text-[10px] text-gray-400">
                        depuis le {new Date(track.dateEtape).toLocaleDateString('fr-FR')}
                      </span>
                    </div>
                    {/* Progression bar */}
                    <div className="mt-2 h-1.5 w-full max-w-xs bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all ${ETAPE_COLORS[track.etape]}`}
                        style={{ width: `${progressionPct(track.etape)}%` }}
                      />
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => setHistoryOpen(showHistory ? null : track.soulId)}
                      className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-400 hover:text-primary-600 transition-colors"
                      title="Historique des étapes"
                    >
                      <History className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => move(track, -1)}
                      disabled={idx === 0}
                      className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-400 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                      title="Étape précédente"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => move(track, 1)}
                      disabled={idx === ETAPES.length - 1}
                      className="p-2 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-500/10 text-gray-400 hover:text-primary-600 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                      title="Étape suivante"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {isExpanded && track.note && (
                  <p className="mt-2 text-xs text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800/50 rounded-lg p-2">
                    {track.note}
                  </p>
                )}

                {showHistory && (
                  <div className="mt-3 pl-4 border-l-2 border-gray-100 dark:border-gray-800 space-y-2">
                    <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wide">Historique du pipeline</p>
                    {historyQuery.isLoading ? (
                      <p className="text-xs text-gray-400">Chargement…</p>
                    ) : (historyQuery.data ?? []).length === 0 ? (
                      <p className="text-xs text-gray-400">Aucun franchissement enregistré.</p>
                    ) : (
                      (historyQuery.data ?? []).map((h, i) => (
                        <div key={i} className="flex items-center gap-2 text-xs">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                          <span className="text-gray-700 dark:text-gray-300">{ETAPES.find(e => e.etape === h.etape)?.label ?? h.etape}</span>
                          <span className="text-gray-400">· {new Date(h.creeLe).toLocaleDateString('fr-FR')}</span>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
