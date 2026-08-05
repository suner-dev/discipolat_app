import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Trophy,
  Medal,
  RefreshCw,
  Crown,
  Award,
  Gem,
  DoorOpen,
  CalendarCheck,
  Sprout,
  Heart,
  Handshake,
} from 'lucide-react';
import type { BadgeProfile, LeaderboardEntry, BadgeNiveau } from '@/types';

const NIVEAU_STYLE: Record<BadgeNiveau, { cls: string; icon: React.ReactNode }> = {
  BRONZE: { cls: 'text-amber-700 bg-amber-700/10', icon: <Medal className="w-5 h-5" /> },
  ARGENT: { cls: 'text-slate-500 bg-slate-500/10', icon: <Medal className="w-5 h-5" /> },
  OR: { cls: 'text-yellow-500 bg-yellow-500/10', icon: <Award className="w-5 h-5" /> },
  DIAMANT: { cls: 'text-cyan-500 bg-cyan-500/10', icon: <Gem className="w-5 h-5" /> },
};

const CRITERE_ICON: Record<string, React.ReactNode> = {
  VISITES: <DoorOpen className="w-4 h-4" />,
  PRESENCE: <CalendarCheck className="w-4 h-4" />,
  EVANGELISATION: <Sprout className="w-4 h-4" />,
  INTERACTIONS: <Handshake className="w-4 h-4" />,
  FIDELITE: <Heart className="w-4 h-4" />,
};

function getIcon(icone?: string, className = 'w-5 h-5') {
  switch (icone) {
    case 'Sprout': return <Sprout className={className} />;
    case 'Church': return <Medal className={className} />;
    case 'Heart': return <Heart className={className} />;
    case 'Crown': return <Crown className={className} />;
    case 'Handshake': return <Handshake className={className} />;
    case 'Footprints': return <DoorOpen className={className} />;
    case 'DoorOpen': return <DoorOpen className={className} />;
    default: return <Award className={className} />;
  }
}

export default function BadgesPage() {
  const queryClient = useQueryClient();

  const profileQuery = useQuery({
    queryKey: ['badges', 'my'],
    queryFn: async () => {
      const res = await api.get('/badges/my');
      return res.data as BadgeProfile;
    },
  });

  const leaderboardQuery = useQuery({
    queryKey: ['badges', 'leaderboard'],
    queryFn: async () => {
      const res = await api.get('/badges/leaderboard');
      return res.data as LeaderboardEntry[];
    },
  });

  const evaluateMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/badges/evaluate');
      return res.data as unknown[];
    },
    onSuccess: (data) => {
      if (data.length > 0) {
        toast.success(`${data.length} nouveau(x) badge(s) débloqué(s) ! 🎉`);
      } else {
        toast('Aucun nouveau badge — continuez comme ça !');
      }
      queryClient.invalidateQueries({ queryKey: ['badges'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const profile = profileQuery.data;
  const leaderboard = leaderboardQuery.data ?? [];
  const earned = (profile?.badges ?? []).filter(b => b.gagne);
  const scoreTotal = profile
    ? Object.values(profile.scores).reduce((a, b) => a + Math.round(b * 10) / 10, 0)
    : 0;

  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Badges & Gamification</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Vos actions réelles récompensées : visites, suivis, évangélisation, fidélité
          </p>
        </div>
        <button
          onClick={() => evaluateMutation.mutate()}
          className="btn btn-secondary flex items-center gap-2 text-sm"
          title="Vérifier les nouveaux badges gagnés"
        >
          <RefreshCw className={`w-4 h-4 ${evaluateMutation.isPending ? 'animate-spin' : ''}`} />
          Vérifier mes badges
        </button>
      </div>

      {/* Header stats */}
      {profileQuery.isLoading ? (
        <div className="glass-card p-8 text-center text-sm text-gray-400">Chargement…</div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="glass-card p-5 flex items-center gap-4">
              <span className="p-3 rounded-xl bg-primary-500/10 text-primary-500"><Trophy className="w-6 h-6" /></span>
              <div>
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{profile?.totalBadges ?? 0}</p>
                <p className="text-xs text-gray-400">Badges débloqués</p>
              </div>
            </div>
            <div className="glass-card p-5 flex items-center gap-4">
              <span className="p-3 rounded-xl bg-amber-500/10 text-amber-500"><Crown className="w-6 h-6" /></span>
              <div>
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{scoreTotal}</p>
                <p className="text-xs text-gray-400">Score d'engagement</p>
              </div>
            </div>
            <div className="glass-card p-5 flex items-center gap-4">
              <span className="p-3 rounded-xl bg-emerald-500/10 text-emerald-500"><Award className="w-6 h-6" /></span>
              <div>
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {profile ? Math.round((profile.totalBadges / Math.max(1, profile.badges.length)) * 100) : 0}%
                </p>
                <p className="text-xs text-gray-400">Collection complétée</p>
              </div>
            </div>
          </div>

          {/* Badges earned */}
          {earned.length > 0 && (
            <div className="glass-card p-4">
              <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
                <Trophy className="w-4 h-4 text-primary-500" /> Badges gagnés ({earned.length})
              </h2>
              <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
                {earned.map(b => (
                  <div key={b.id} className="flex flex-col items-center gap-1.5 p-3 rounded-xl border border-gray-100 dark:border-gray-800 hover:border-primary-300 dark:hover:border-primary-700 transition-colors">
                    <span className={`p-2.5 rounded-full ${NIVEAU_STYLE[b.niveau].cls}`}>
                      {getIcon(b.icone)}
                    </span>
                    <p className="text-[11px] font-semibold text-gray-700 dark:text-gray-300 text-center leading-tight">{b.nom}</p>
                    <p className="text-[9px] uppercase font-bold text-gray-400">{b.niveau}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* All badges with progress */}
          <div className="glass-card p-4">
            <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">Tous les badges</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {(profile?.badges ?? []).map(b => {
                const nv = NIVEAU_STYLE[b.niveau];
                return (
                  <div key={b.id} className={`border rounded-xl p-3 transition-all ${b.gagne ? 'border-emerald-200 dark:border-emerald-800 bg-emerald-50/50 dark:bg-emerald-900/10' : 'border-gray-100 dark:border-gray-800'}`}>
                    <div className="flex items-start gap-3">
                      <span className={`p-2 rounded-full ${nv.cls} ${b.gagne ? '' : 'opacity-40 grayscale'}`}>
                        {getIcon(b.icone)}
                      </span>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{b.nom}</p>
                          <span className="text-[9px] uppercase font-bold text-gray-400">{b.niveau}</span>
                        </div>
                        <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-0.5">{b.description}</p>
                        <div className="flex items-center gap-2 mt-2">
                          {CRITERE_ICON[b.critere]}
                          <span className="text-[11px] text-gray-500 dark:text-gray-400">
                            {b.score} / {b.seuil}
                          </span>
                        </div>
                        <div className="mt-1.5 h-1.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all ${b.gagne ? 'bg-emerald-500' : 'bg-primary-500'}`}
                            style={{ width: `${Math.min(100, b.progression)}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}

      {/* Leaderboard */}
      <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
        <div className="px-4 py-3 flex items-center gap-2">
          <Trophy className="w-4 h-4 text-primary-500" />
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Classement des faiseurs</h2>
        </div>
        {leaderboardQuery.isLoading ? (
          <div className="p-6 text-center text-sm text-gray-400">Chargement…</div>
        ) : leaderboard.length === 0 ? (
          <div className="p-8 text-center text-sm text-gray-400">Aucun faiseur pour le moment.</div>
        ) : (
          leaderboard.map((entry, i) => (
            <div key={entry.userId} className="px-4 py-3 flex items-center gap-3">
              <span className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${i === 0 ? 'bg-amber-500 text-white' : i === 1 ? 'bg-slate-400 text-white' : i === 2 ? 'bg-orange-400 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'}`}>
                {i + 1}
              </span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{entry.nom}</p>
              </div>
              <span className="inline-flex items-center gap-1 text-xs font-semibold text-primary-600 dark:text-primary-400">
                <Medal className="w-3.5 h-3.5" /> {entry.badges}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
