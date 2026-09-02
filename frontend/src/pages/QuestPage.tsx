import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import toast from 'react-hot-toast';
import { Loader2, Trophy, Zap, Target, Crown, BarChart3, Users, Shield, Send, Award } from 'lucide-react';

interface QuestProfile {
  totalXp: number;
  level: number;
  xpInLevel: number;
  xpForNextLevel: number;
  progressPercent: number;
  title: string;
  history: { action: string; points: number; description: string; date: string }[];
}

interface WeeklyQuest {
  code: string;
  label: string;
  target: number;
  done: number;
  completed: boolean;
  xpReward: number;
  progressPercent: number;
}

interface LeaderboardEntry {
  rank: number;
  userId: string;
  totalXp: number;
  level: number;
  title: string;
}

interface GroupEntry {
  rank: number;
  nom: string;
  type: string;
  membres: number;
  xpTotal: number;
  xpMoyen: number;
}

interface QuestStats {
  levelStep: number;
  participants: number;
  totalXpDistributed: number;
}

interface WeeklyChallengesResult {
  challenges: { id: string; label: string; target: number; xpReward: number; action: string; current: number; completed: boolean; progressPct: number }[];
  weekStart: string;
  weekEnd: string;
  totalXpAvailable: number;
}

interface ContextualBadge {
  code: string;
  name: string;
  description: string;
  xpValue: number;
  earned: boolean;
}

/** Gamification « Discipolat Quest » — XP, niveaux, quêtes hebdo, classement. */
export default function QuestPage() {
  const { user, hasRole } = useAuth();
  const qc = useQueryClient();
  const [tab, setTab] = useState<'quests' | 'leaderboard'>('quests');
  const [viewUserId, setViewUserId] = useState('');
  const [awardUserId, setAwardUserId] = useState('');
  const [awardAction, setAwardAction] = useState('PRESENCE_CULTE');
  const [awardPoints, setAwardPoints] = useState('');
  const [awardDesc, setAwardDesc] = useState('');
  const isAdmin = hasRole('ADMIN', 'PASTEUR');

  const profileQuery = useQuery({
    queryKey: ['quest-profile'],
    queryFn: async () => (await api.get<QuestProfile>('/quest/profile')).data,
  });
  const questsQuery = useQuery({
    queryKey: ['quest-quests'],
    queryFn: async () => (await api.get<WeeklyQuest[]>('/quest/quests')).data,
  });
  const boardQuery = useQuery({
    queryKey: ['quest-leaderboard'],
    queryFn: async () => (await api.get<LeaderboardEntry[]>('/quest/leaderboard')).data,
  });

  if (profileQuery.isLoading) {
    return <Loader2 className="w-8 h-8 animate-spin text-primary-500 mx-auto mt-20" />;
  }

  const profile = profileQuery.data!;
  const quests = questsQuery.data ?? [];
  const allCompleted = quests.length > 0 && quests.every((q) => q.completed);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-indigo-600 text-white shadow-lg">
          <Zap className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Discipolat Quest</h1>
          <p className="page-subtitle">Gagnez de l'XP en servant — niveaux, quêtes et classement</p>
        </div>
      </div>

      {/* Carte niveau */}
      <div className="glass-card p-6 mb-6 animate-slide-up">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-14 h-14 rounded-full bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center text-white font-bold text-xl shadow-lg">
              {profile.level}
            </div>
            <div>
              <p className="font-semibold text-gray-900 dark:text-gray-100">{profile.title}</p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {profile.totalXp.toLocaleString('fr-FR')} XP au total
              </p>
            </div>
          </div>
          {allCompleted && (
            <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400">
              <Crown className="w-4 h-4" />
              <span className="text-sm font-medium">Semaine parfaite !</span>
            </div>
          )}
        </div>
        <div className="w-full h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-purple-500 to-indigo-500 rounded-full transition-all duration-500"
            style={{ width: `${profile.progressPercent}%` }}
          />
        </div>
        <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">
          {profile.xpInLevel} / {profile.xpForNextLevel} XP vers le niveau {profile.level + 1}
        </p>
      </div>

      {/* Onglets */}
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => setTab('quests')}
          className={`btn-sm px-4 py-2 rounded-lg font-medium ${tab === 'quests' ? 'btn-primary' : 'glass-card'}`}
        >
          <Target className="inline w-4 h-4 mr-1" /> Quêtes de la semaine
        </button>
        <button
          onClick={() => setTab('leaderboard')}
          className={`btn-sm px-4 py-2 rounded-lg font-medium ${tab === 'leaderboard' ? 'btn-primary' : 'glass-card'}`}
        >
          <Trophy className="inline w-4 h-4 mr-1" /> Classement
        </button>
      </div>

      {tab === 'quests' && (
        <div className="grid gap-3 md:grid-cols-2">
          {quests.map((q) => (
            <div key={q.code} className="glass-card p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-2">
                <p className="font-medium text-gray-900 dark:text-gray-100">{q.label}</p>
                <span className={`badge ${q.completed ? 'badge-success' : 'badge-warning'}`}>
                  {q.done}/{q.target}
                </span>
              </div>
              <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    q.completed ? 'bg-green-500' : 'bg-amber-500'
                  }`}
                  style={{ width: `${q.progressPercent}%` }}
                />
              </div>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">Récompense : +{q.xpReward} XP</p>
            </div>
          ))}
        </div>
      )}

      {tab === 'leaderboard' && (
        <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
          {boardQuery.isLoading && (
            <Loader2 className="w-6 h-6 animate-spin text-primary-500 mx-auto my-8" />
          )}
          {(boardQuery.data ?? []).map((e) => (
            <div key={e.userId} className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                <span
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm ${
                    e.rank === 1
                      ? 'bg-yellow-400 text-yellow-900'
                      : e.rank === 2
                        ? 'bg-gray-300 text-gray-700'
                        : e.rank === 3
                          ? 'bg-amber-600 text-white'
                          : 'bg-gray-100 dark:bg-gray-800 text-gray-500'
                  }`}
                >
                  {e.rank}
                </span>
                <div>
                  <p className="font-medium text-gray-900 dark:text-gray-100">{e.title}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Niveau {e.level}</p>
                </div>
              </div>
              <span className="font-semibold text-purple-600 dark:text-purple-400">
                {e.totalXp.toLocaleString('fr-FR')} XP
              </span>
            </div>
          ))}
          {!boardQuery.isLoading && (boardQuery.data ?? []).length === 0 && (
            <p className="text-center text-sm text-gray-500 py-8">
              Aucun XP distribué pour le moment — lancez la première quête !
            </p>
          )}
        </div>
      )}
    </div>
  );
}
