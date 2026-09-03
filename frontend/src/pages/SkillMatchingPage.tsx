import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Target, Plus, Trash2, Loader2, User, Star } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface SkillMatch {
  id: string;
  memberId: string;
  memberName?: string;
  competenceName: string;
  niveau: number;
  statut: string;
  score: number;
  matchedAt: string;
}

interface MatchStats {
  totalMatches: number;
  accepted: number;
  pending: number;
  rejected: number;
  topCompetences: string[];
}

export default function SkillMatchingPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [requiredCompetences, setRequiredCompetences] = useState('');
  const [minLevel, setMinLevel] = useState('2');

  const { data: matches = [], isLoading } = useQuery({
    queryKey: ['skill-matching'],
    queryFn: async () => (await api.get<SkillMatch[]>('/skill-matching')).data,
  });

  const { data: stats } = useQuery({
    queryKey: ['skill-matching-stats'],
    queryFn: async () => (await api.get<MatchStats>('/skill-matching/stats')).data,
  });

  const runMatchingMutation = useMutation({
    mutationFn: async () => (await api.post<SkillMatch[]>('/skill-matching/run')).data,
    onSuccess: (data) => {
      toast.success(`${data.length} correspondances trouvées`);
      queryClient.invalidateQueries({ queryKey: ['skill-matching'] });
    },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const respondMutation = useMutation({
    mutationFn: async ({ id, decision }: { id: string; decision: string }) =>
      api.post(`/skill-matching/${id}/respond`, null, { params: { decision } }),
    onSuccess: () => { toast.success('Réponse enregistrée'); queryClient.invalidateQueries({ queryKey: ['skill-matching'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const matchSearchMutation = useMutation({
    mutationFn: async () => {
      const competences = requiredCompetences.split(',').map(s => s.trim()).filter(Boolean);
      return (await api.post('/skill-matching/match', competences, { params: { minLevel } })).data;
    },
    onSuccess: (data: any[]) => {
      toast.success(`${data.length} membres correspondants`);
      setShowCreate(false);
    },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <Target className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Matching de Compétences</h1>
          <p className="page-subtitle">Trouvez les meilleurs membres pour chaque besoin</p>
        </div>
        <div className="ml-auto flex gap-2">
          <button onClick={() => runMatchingMutation.mutate()} disabled={runMatchingMutation.isPending}
            className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-sm font-medium hover:bg-white/10 flex items-center gap-2">
            {runMatchingMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Target className="w-4 h-4" />}
            Lancer le matching
          </button>
          <button onClick={() => setShowCreate(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-violet-500 to-purple-500 text-white text-sm font-medium hover:from-violet-600 hover:to-purple-600 transition-all shadow-lg flex items-center gap-2">
            <Plus className="w-4 h-4" /> Recherche
          </button>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.totalMatches}</div>
            <div className="text-xs text-gray-500">Total</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.accepted}</div>
            <div className="text-xs text-gray-500">Acceptés</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-amber-600">{stats.pending}</div>
            <div className="text-xs text-gray-500">En attente</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-red-600">{stats.rejected}</div>
            <div className="text-xs text-gray-500">Refusés</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-purple-600">{stats.topCompetences?.length || 0}</div>
            <div className="text-xs text-gray-500">Compétences</div>
          </div>
        </div>
      )}

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        matches.length === 0 ? (
          <EmptyState icon={<Target className="w-8 h-8 text-gray-400" />}
            title="Aucune correspondance"
            message="Lancez le matching pour trouver les meilleurs profils"
            action={{ label: 'Lancer le matching', onClick: () => runMatchingMutation.mutate() }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {matches.map(m => (
              <div key={m.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-3">
                  <span className="px-2 py-0.5 rounded-full bg-purple-100 dark:bg-purple-500/20 text-purple-700 dark:text-purple-400 text-xs font-medium">
                    {m.competenceName}
                  </span>
                  <div className="flex items-center gap-1">
                    <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
                    <span className="text-xs font-medium text-gray-600 dark:text-gray-300">{m.score?.toFixed(1)}</span>
                  </div>
                </div>
                <div className="text-sm text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-1">
                  <User className="w-3 h-3" /> {m.memberName || m.memberId.slice(0, 8)}...
                </div>
                <div className="text-xs text-gray-400 mb-3">Niveau: {m.niveau}/5 · {m.statut}</div>
                {m.statut === 'PENDING' && (
                  <div className="flex gap-2">
                    <button onClick={() => respondMutation.mutate({ id: m.id, decision: 'ACCEPTED' })}
                      className="px-3 py-1 rounded-lg bg-green-500 text-white text-xs font-medium hover:bg-green-600">Accepter</button>
                    <button onClick={() => respondMutation.mutate({ id: m.id, decision: 'REJECTED' })}
                      className="px-3 py-1 rounded-lg bg-red-500 text-white text-xs font-medium hover:bg-red-600">Refuser</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Recherche de compétences</h2>
            <div className="space-y-4">
              <input type="text" value={requiredCompetences} onChange={e => setRequiredCompetences(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="Compétences requises (séparées par virgule)" />
              <div>
                <label className="block text-xs text-gray-500 mb-1">Niveau minimum</label>
                <select value={minLevel} onChange={e => setMinLevel(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                  {[1,2,3,4,5].map(n => <option key={n} value={n}>{n}</option>)}
                </select>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => matchSearchMutation.mutate()} disabled={matchSearchMutation.isPending}
                className="px-4 py-2 rounded-xl bg-violet-500 text-white text-sm font-medium hover:bg-violet-600 flex items-center gap-2">
                {matchSearchMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Rechercher
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
