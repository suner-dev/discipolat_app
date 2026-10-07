import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Target, Search, Loader2, User, Star } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

import { tText } from '@/i18n';

// Contrat réel backend — SkillMatch (com.discipolat.modules.skillMatching.domain.SkillMatch)
interface SkillMatch {
  id: string;
  membreId: string;
  departementId: string;
  competence: string;
  scoreMatch: number; // 0..100
  statut: 'PROPOSE' | 'ACCEPTE' | 'REFUSE' | 'EN_COURS';
  justification?: string;
  creeLe: string;
}

// Contrat réel backend — SkillMatchService.getStats()
interface MatchStats {
  total: number;
  proposes: number;
  acceptes: number;
  refuses: number;
  scoreMoyen: number;
}

// Normalisation pour la recherche (insensible à la casse et aux accents)
const normalize = (s: string): string =>
  s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();

const statutBadge = (s: string): string => {
  switch (s) {
    case 'ACCEPTE': return 'badge-success';
    case 'REFUSE': return 'badge-error';
    case 'EN_COURS': return 'badge-info';
    default: return 'badge-warning'; // PROPOSE
  }
};

export default function SkillMatchingPage() {
  const queryClient = useQueryClient();
  const [showSearch, setShowSearch] = useState(false);
  const [requiredCompetences, setRequiredCompetences] = useState('');
  const [minScore, setMinScore] = useState('50');
  const [results, setResults] = useState<SkillMatch[] | null>(null);

  const { data: matches = [], isLoading } = useQuery({
    queryKey: ['skill-matching'],
    queryFn: async () => (await api.get<SkillMatch[]>('/skill-matching')).data,
  });

  const { data: stats } = useQuery({
    queryKey: ['skill-matching-stats'],
    queryFn: async () => (await api.get<MatchStats>('/skill-matching/stats')).data,
  });

  const runMatchingMutation = useMutation({
    // POST /skill-matching/run — aucun corps, retourne les matches PROPOSE
    mutationFn: async () => (await api.post<SkillMatch[]>('/skill-matching/run')).data,
    onSuccess: (data) => {
      toast.success(`${data.length} correspondances trouvées`);
      queryClient.invalidateQueries({ queryKey: ['skill-matching'] });
    },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const respondMutation = useMutation({
    // POST /skill-matching/{id}/respond?decision=ACCEPTE|REFUSE (enum SkillMatch.Statut)
    mutationFn: async ({ id, decision }: { id: string; decision: 'ACCEPTE' | 'REFUSE' }) =>
      api.post<SkillMatch>(`/skill-matching/${id}/respond`, null, { params: { decision } }),
    onSuccess: () => {
      toast.success(tText('Réponse enregistrée'));
      queryClient.invalidateQueries({ queryKey: ['skill-matching'] });
    },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  // Filtrage client sur les données réelles (aucune route /match côté serveur)
  const runSearch = () => {
    const competences = requiredCompetences.split(',').map(normalize).filter(Boolean);
    const seuil = Number(minScore) || 0;
    const filtered = matches.filter(m => {
      if (m.scoreMatch < seuil) return false;
      if (competences.length === 0) return true;
      return competences.some(c => normalize(m.competence).includes(c));
    });
    setResults(filtered);
    setShowSearch(false);
    toast.success(`${filtered.length} membres correspondants`);
  };

  const displayed = results ?? matches;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <Target className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">{tText('Matching de Compétences')}</h1>
          <p className="page-subtitle">{tText('Trouvez les meilleurs membres pour chaque besoin')}</p>
        </div>
        <div className="ml-auto flex gap-2">
          <button onClick={() => runMatchingMutation.mutate()} disabled={runMatchingMutation.isPending}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm font-medium hover:bg-white/10 flex items-center gap-2">
            {runMatchingMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Target className="w-4 h-4" />}
            Lancer le matching
          </button>
          <button onClick={() => setShowSearch(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-violet-500 to-purple-500 text-white text-sm font-medium hover:from-violet-600 hover:to-purple-600 transition-all shadow-lg flex items-center gap-2">
            <Search className="w-4 h-4" /> {tText('Recherche')}
          </button>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.total}</div>
            <div className="text-xs text-gray-500">Total</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.acceptes}</div>
            <div className="text-xs text-gray-500">{tText('Acceptés')}</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-amber-600">{stats.proposes}</div>
            <div className="text-xs text-gray-500">En attente</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-red-600">{stats.refuses}</div>
            <div className="text-xs text-gray-500">{tText('Refusés')}</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-purple-600">{Math.round(stats.scoreMoyen)}</div>
            <div className="text-xs text-gray-500">{tText('Score moyen')}</div>
          </div>
        </div>
      )}

      {results && (
        <div className="mb-4 flex items-center justify-between px-1">
          <p className="text-sm text-gray-500">
            {results.length} résultat(s) pour « {requiredCompetences || tText('toutes compétences')} » · score ≥ {minScore}
          </p>
          <button onClick={() => setResults(null)} className="text-xs text-violet-600 hover:underline">
            {tText('Réinitialiser')}
          </button>
        </div>
      )}

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        displayed.length === 0 ? (
          <EmptyState icon={<Target className="w-8 h-8 text-gray-400" />}
            title={tText('Aucune correspondance')}
            message={tText('Lancez le matching pour trouver les meilleurs profils')}
            action={{ label: 'Lancer le matching', onClick: () => runMatchingMutation.mutate() }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {displayed.map(m => (
              <div key={m.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-3">
                  <span className="px-2 py-0.5 rounded-full bg-purple-100 dark:bg-purple-500/20 text-purple-700 dark:text-purple-400 text-xs font-medium">
                    {m.competence}
                  </span>
                  <div className="flex items-center gap-1">
                    <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
                    <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{m.scoreMatch}%</span>
                  </div>
                </div>
                <div className="text-sm text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-1">
                  <User className="w-3 h-3" /> {m.membreId.slice(0, 8)}…
                </div>
                {m.justification && (
                  <div className="text-xs text-gray-400 mb-2 line-clamp-2">{m.justification}</div>
                )}
                <div className="flex items-center gap-2 mb-3">
                  <span className={`badge text-[10px] ${statutBadge(m.statut)}`}>{m.statut}</span>
                  <span className="text-[10px] text-gray-400">{new Date(m.creeLe).toLocaleDateString()}</span>
                </div>
                {m.statut === 'PROPOSE' && (
                  <div className="flex gap-2">
                    <button onClick={() => respondMutation.mutate({ id: m.id, decision: 'ACCEPTE' })}
                      className="px-3 py-1 rounded-lg bg-green-500 text-white text-xs font-medium hover:bg-green-600">{tText('Accepter')}</button>
                    <button onClick={() => respondMutation.mutate({ id: m.id, decision: 'REFUSE' })}
                      className="px-3 py-1 rounded-lg bg-red-500 text-white text-xs font-medium hover:bg-red-600">{tText('Refuser')}</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

      {showSearch && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowSearch(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">{tText('Recherche de compétences')}</h2>
            <div className="space-y-4">
              <input type="text" value={requiredCompetences} onChange={e => setRequiredCompetences(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder={tText('Compétences requises (séparées par virgule)')} />
              <div>
                <label className="block text-xs text-gray-500 mb-1">{tText('Score minimum (%)')}</label>
                <input type="number" min={0} max={100} value={minScore} onChange={e => setMinScore(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowSearch(false)} className="px-4 py-2 rounded-xl border text-sm">{tText('Annuler')}</button>
              <button onClick={runSearch}
                className="px-4 py-2 rounded-xl bg-violet-500 text-white text-sm font-medium hover:bg-violet-600 flex items-center gap-2">
                <Search className="w-4 h-4" /> {tText('Rechercher')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
