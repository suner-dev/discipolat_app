import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Flame, Trophy, CheckCircle, Zap, Loader2 } from 'lucide-react';

interface Challenge { id: string; title: string; description?: string; category?: string; difficulty?: string; xpReward?: number; progress?: number; status?: string; }

const diffColor = (d?: string) => d === 'HARD' ? 'text-red-400 bg-red-500/20' : d === 'MEDIUM' ? 'text-yellow-400 bg-yellow-500/20' : 'text-green-400 bg-green-500/20';
const catEmoji: Record<string, string> = { PRAYER: '🙏', READING: '📖', SERVICE: '🤝', FASTING: '💧', EVANGELISM: '📢', GRATITUDE: '✨', FAMILY: '👨‍👩‍👧‍👦' };

/** P3 #108 — Défis hebdomadaires générés automatiquement (API réelle). */
export default function WeeklyChallengesPage() {
  const qc = useQueryClient();
  const allQ = useQuery({ queryKey: ['weekly-challenges'], queryFn: async () => (await api.get('/weekly-challenges')).data as Challenge[] | { content: Challenge[] } });
  const myQ = useQuery({ queryKey: ['weekly-challenges-my'], queryFn: async () => (await api.get('/weekly-challenges/my')).data as Challenge[] | { content: Challenge[] } });

  const normalize = (d: Challenge[] | { content?: Challenge[] } | undefined): Challenge[] => Array.isArray(d) ? d : (d?.content ?? []);
  const challenges = [...normalize(allQ.data), ...normalize(myQ.data)];
  const unique = Array.from(new Map(challenges.map((c) => [c.id, c])).values());

  const setProgress = useMutation({
    mutationFn: async ({ id, progress }: { id: string; progress: number }) => api.put(`/weekly-challenges/${id}/progress`, { progress }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['weekly-challenges'] }); qc.invalidateQueries({ queryKey: ['weekly-challenges-my'] }); },
  });
  const generate = useMutation({
    mutationFn: async () => api.post('/weekly-challenges/generate'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['weekly-challenges'] }),
  });

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Flame className="text-orange-400" /> Défis hebdomadaires</h1>
        <button onClick={() => generate.mutate()} disabled={generate.isPending} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-orange-600 to-red-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
          {generate.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />} Générer les défis de la semaine
        </button>
      </div>
      {unique.length === 0 && <p className="text-sm text-gray-500">Aucun défi actif. Cliquez sur « Générer » pour créer les défis de la semaine.</p>}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {unique.map((c) => {
          const progress = c.progress ?? 0;
          const completed = progress >= 100 || c.status === 'COMPLETED';
          return (
            <div key={c.id} className={`bg-white/5 backdrop-blur rounded-2xl p-5 border transition hover:scale-[1.02] ${completed ? 'border-green-500/30' : 'border-white/10 hover:border-orange-500/30'}`}>
              <div className="flex items-center justify-between mb-3">
                <span className="text-2xl">{catEmoji[c.category ?? ''] || '🎯'}</span>
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${diffColor(c.difficulty)}`}>{c.difficulty}</span>
              </div>
              <h3 className="text-white font-semibold mb-1">{c.title}</h3>
              {c.description && <p className="text-xs text-gray-400 mb-3">{c.description}</p>}
              <div className="w-full h-2 bg-gray-700 rounded-full overflow-hidden mb-2">
                <div className={`h-full rounded-full transition-all ${completed ? 'bg-green-500' : 'bg-gradient-to-r from-orange-500 to-yellow-400'}`} style={{ width: `${progress}%` }} />
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-400">{progress}%</span>
                {!!c.xpReward && <span className="text-orange-400 font-medium flex items-center gap-1"><Trophy className="w-3 h-3" /> +{c.xpReward} XP</span>}
              </div>
              {!completed && (
                <button onClick={() => setProgress.mutate({ id: c.id, progress: Math.min(progress + 25, 100) })} disabled={setProgress.isPending}
                  className="mt-3 w-full py-1.5 rounded-lg text-xs font-medium text-white bg-white/10 hover:bg-orange-500/30 transition disabled:opacity-50">
                  +25 % de progression
                </button>
              )}
              {completed && <div className="mt-2 text-green-400 text-xs flex items-center gap-1"><CheckCircle className="w-3 h-3" /> Terminé !</div>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
