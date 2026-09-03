import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Trophy, Medal, Target, Flame, Award, TrendingUp, Crown,
  RefreshCw, Loader2, Star, CheckCircle, Users, Zap,
} from 'lucide-react';

interface LeaderboardEntry { userId: string; name: string; points: number; rank: number; badges: number; }
interface QuestStats { totalPoints: number; totalBadges: number; totalChallenges: number; activeUsers: number; }
interface UserProfile { userId: string; name: string; points: number; rank: number; badges: string[]; weeklyProgress: number; }
interface ContextualBadge { id: string; name: string; description: string; icon: string; unlockedAt?: string; }
interface WeeklyChallenge { id: string; title: string; description: string; progress: number; target: number; rewardPoints: number; endsAt: string; }

export default function QuestPage() {
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<'leaderboard' | 'stats' | 'challenges' | 'badges'>('leaderboard');
  const [awardUserId, setAwardUserId] = useState('');
  const [awardPoints, setAwardPoints] = useState(0);
  const [awardReason, setAwardReason] = useState('');

  const { data: leaderboard = [], isLoading: lbLoading } = useQuery({
    queryKey: ['quest', 'leaderboard'],
    queryFn: async () => {
      const res = await api.get('/quest/leaderboard/groups');
      return res.data as LeaderboardEntry[];
    },
  });

  const { data: stats } = useQuery({
    queryKey: ['quest', 'stats'],
    queryFn: async () => {
      const res = await api.get('/quest/stats');
      return res.data as QuestStats;
    },
  });

  const { data: badges = [] } = useQuery({
    queryKey: ['quest', 'badges'],
    queryFn: async () => {
      const res = await api.get('/quest/contextual-badges');
      return res.data as ContextualBadge[];
    },
  });

  const { data: challenges = [] } = useQuery({
    queryKey: ['quest', 'challenges'],
    queryFn: async () => {
      const res = await api.get('/quest/weekly-challenges');
      return res.data as WeeklyChallenge[];
    },
  });

  const awardMutation = useMutation({
    mutationFn: async () => {
      await api.post('/quest/award', { userId: awardUserId, points: awardPoints, reason: awardReason });
    },
    onSuccess: () => {
      toast.success('Points attribués');
      qc.invalidateQueries({ queryKey: ['quest'] });
      setAwardUserId(''); setAwardPoints(0); setAwardReason('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const getMedal = (rank: number) => {
    if (rank === 1) return <Crown className="w-5 h-5 text-yellow-400" />;
    if (rank === 2) return <Medal className="w-5 h-5 text-gray-300" />;
    if (rank === 3) return <Medal className="w-5 h-5 text-amber-600" />;
    return <span className="text-sm text-gray-400 w-5 text-center">#{rank}</span>;
  };

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Trophy className="w-5 h-5 text-yellow-500" />
            <h1 className="page-title">Quête & Gamification</h1>
          </div>
          <p className="page-subtitle">Classement, badges, défis hebdomadaires et récompenses</p>
        </div>
        <button onClick={() => qc.invalidateQueries({ queryKey: ['quest'] })} className="btn-ghost btn-sm">
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Stats cards */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Points totaux', value: stats.totalPoints, icon: Star, color: 'from-yellow-500 to-amber-500' },
            { label: 'Badges', value: stats.totalBadges, icon: Award, color: 'from-purple-500 to-violet-500' },
            { label: 'Défis', value: stats.totalChallenges, icon: Target, color: 'from-blue-500 to-indigo-500' },
            { label: 'Utilisateurs actifs', value: stats.activeUsers, icon: Users, color: 'from-green-500 to-emerald-500' },
          ].map((stat, i) => (
            <div key={stat.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
              <div className="flex items-start justify-between mb-2">
                <span className="stat-label text-[10px]">{stat.label}</span>
                <div className={`p-1.5 rounded-lg bg-gradient-to-br ${stat.color} text-white shadow-sm`}>
                  <stat.icon className="w-3.5 h-3.5" />
                </div>
              </div>
              <p className="stat-value text-xl">{stat.value}</p>
            </div>
          ))}
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: 'leaderboard' as const, label: 'Classement', icon: Trophy },
          { key: 'challenges' as const, label: 'Défis', icon: Flame },
          { key: 'badges' as const, label: 'Badges', icon: Award },
          { key: 'stats' as const, label: 'Attribuer', icon: Zap },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              activeTab === tab.key
                ? 'bg-yellow-100 dark:bg-yellow-500/20 text-yellow-700 dark:text-yellow-400'
                : 'text-gray-500 dark:text-gray-400 hover:bg-white/5'
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Leaderboard */}
      {activeTab === 'leaderboard' && (
        <div className="glass-card overflow-hidden animate-slide-up">
          {lbLoading ? (
            <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400 mx-auto" /></div>
          ) : leaderboard.length === 0 ? (
            <div className="p-10 text-center">
              <Trophy className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
              <p className="text-gray-500 font-medium">Aucun classement disponible.</p>
            </div>
          ) : (
            <div className="space-y-1">
              {leaderboard.map((entry, i) => (
                <div key={entry.userId} className={`flex items-center gap-4 px-5 py-3.5 transition-colors hover:bg-white/40 dark:hover:bg-white/[0.03] ${i === 0 ? 'bg-yellow-50/50 dark:bg-yellow-900/10' : ''}`}>
                  <div className="w-8 flex justify-center">{getMedal(entry.rank)}</div>
                  <div className="w-9 h-9 rounded-full bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-xs font-bold shrink-0">
                    {entry.name.split(' ').map((w) => w[0]).join('').slice(0, 2)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{entry.name}</p>
                    <p className="text-[10px] text-gray-400">{entry.badges} badges</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-bold text-yellow-500">{entry.points} pts</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Challenges */}
      {activeTab === 'challenges' && (
        <div className="space-y-3 animate-slide-up">
          {challenges.length === 0 ? (
            <div className="glass-card p-10 text-center">
              <Flame className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
              <p className="text-gray-500 font-medium">Aucun défi cette semaine.</p>
            </div>
          ) : (
            challenges.map((c) => (
              <div key={c.id} className="glass-card px-5 py-4">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{c.title}</h3>
                    <p className="text-xs text-gray-400">{c.description}</p>
                  </div>
                  <span className="badge text-[10px] bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400">
                    +{c.rewardPoints} pts
                  </span>
                </div>
                <div className="mt-3">
                  <div className="flex items-center justify-between text-[10px] text-gray-400 mb-1">
                    <span>Progression</span>
                    <span>{c.progress}/{c.target}</span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-yellow-500 to-amber-500 transition-all"
                      style={{ width: `${Math.min((c.progress / c.target) * 100, 100)}%` }}
                    />
                  </div>
                </div>
                <p className="text-[10px] text-gray-400 mt-2">
                  Se termine le {new Date(c.endsAt).toLocaleDateString('fr-FR')}
                </p>
              </div>
            ))
          )}
        </div>
      )}

      {/* Badges */}
      {activeTab === 'badges' && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 animate-slide-up">
          {badges.length === 0 ? (
            <div className="col-span-full glass-card p-10 text-center">
              <Award className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
              <p className="text-gray-500 font-medium">Aucun badge disponible.</p>
            </div>
          ) : (
            badges.map((b) => (
              <div key={b.id} className={`glass-card p-4 text-center transition-all ${b.unlockedAt ? 'border-yellow-500/30' : 'opacity-60'}`}>
                <div className="text-3xl mb-2">{b.icon}</div>
                <h4 className="text-xs font-bold text-gray-900 dark:text-gray-100 mb-1">{b.name}</h4>
                <p className="text-[10px] text-gray-400">{b.description}</p>
                {b.unlockedAt && (
                  <p className="text-[10px] text-yellow-500 mt-1">
                    Débloqué le {new Date(b.unlockedAt).toLocaleDateString('fr-FR')}
                  </p>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {/* Award points */}
      {activeTab === 'stats' && (
        <div className="glass-card p-6 max-w-lg animate-slide-up">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Zap className="w-4 h-4 text-yellow-500" />
            Attribuer des points
          </h3>
          <div className="space-y-4">
            <div>
              <label className="label">ID utilisateur</label>
              <input className="input" value={awardUserId} onChange={(e) => setAwardUserId(e.target.value)} placeholder="UUID de l'utilisateur" />
            </div>
            <div>
              <label className="label">Points</label>
              <input type="number" className="input" value={awardPoints || ''} onChange={(e) => setAwardPoints(Number(e.target.value))} placeholder="Nombre de points" min={1} />
            </div>
            <div>
              <label className="label">Raison</label>
              <input className="input" value={awardReason} onChange={(e) => setAwardReason(e.target.value)} placeholder="Ex: Participation à l'événement" />
            </div>
            <button
              onClick={() => awardMutation.mutate()}
              disabled={!awardUserId || awardPoints <= 0 || awardMutation.isPending}
              className="btn-primary btn-sm"
            >
              {awardMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
              Attribuer
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
