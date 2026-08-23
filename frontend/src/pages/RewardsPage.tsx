import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Trophy, Star, Gift, Award, Zap, Check, Lock } from 'lucide-react';

interface RewardItem {
  id: string;
  name: string;
  description: string;
  pointsRequired: number;
  type: 'badge' | 'certificate' | 'privilege' | 'physical';
  icon: string;
  claimed?: boolean;
}

const MOCK_REWARDS: RewardItem[] = [
  { id: '1', name: 'Fidèle Inébranlable', description: 'Présent 50 cultes consécutifs', pointsRequired: 500, type: 'badge', icon: '⭐', claimed: true },
  { id: '2', name: 'Évangéliste', description: '10 personnes amenées au Seigneur', pointsRequired: 1000, type: 'badge', icon: '🔥', claimed: true },
  { id: '3', name: 'Mentor d\'excellence', description: 'Accompagné 5 disciples', pointsRequired: 2000, type: 'certificate', icon: '📜' },
  { id: '4', name: 'Siège VIP Culte', description: 'Place réservée pendant 1 mois', pointsRequired: 300, type: 'privilege', icon: '🪑' },
  { id: '5', name: 'T-shirt Discipolat', description: 'Merchandising exclusif', pointsRequired: 1500, type: 'physical', icon: '👕' },
  { id: '6', name: 'Parrain d\'or', description: '5 parrainages réussis', pointsRequired: 800, type: 'badge', icon: '🏆' },
];

export default function RewardsPage() {
  const { t } = useI18n();
  const [filter, setFilter] = useState<'all' | 'badge' | 'certificate' | 'privilege' | 'physical'>('all');
  const [userPoints] = useState(1250);

  const filtered = filter === 'all' ? MOCK_REWARDS : MOCK_REWARDS.filter((r) => r.type === filter);
  const typeLabel = (type: string) => {
    if (type === 'badge') return '🏅 Badge';
    if (type === 'certificate') return '📜 Certificat';
    if (type === 'privilege') return '✨ Privilège';
    return '🎁 Physique';
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
          <Trophy className="w-7 h-7 text-yellow-500" />
          {t('nav.rewards') ?? 'Récompenses & Gamification'}
        </h1>
        <p className="text-sm text-gray-500 mt-1">Gagnez des points et débloquez des récompenses</p>
      </div>

      {/* Points card */}
      <div className="glass rounded-2xl p-6 border border-white/20 dark:border-white/[0.06] bg-gradient-to-r from-yellow-500/10 to-amber-500/10">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-yellow-400 to-amber-500 flex items-center justify-center text-2xl">⭐</div>
            <div>
              <p className="text-xs text-gray-500 font-medium">Mes points</p>
              <p className="text-3xl font-bold text-gray-900 dark:text-white">{userPoints.toLocaleString()}</p>
            </div>
          </div>
          <div className="text-right">
            <p className="text-xs text-gray-500">Récompenses obtenues</p>
            <p className="text-xl font-bold text-yellow-500">{MOCK_REWARDS.filter(r => r.claimed).length}</p>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-2 flex-wrap">
        {(['all', 'badge', 'certificate', 'privilege', 'physical'] as const).map((f) => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === f ? 'bg-yellow-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10 dark:bg-white/5'}`}>
            {f === 'all' ? 'Toutes' : typeLabel(f).split(' ').slice(1).join(' ')}
          </button>
        ))}
      </div>

      {/* Rewards grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((reward) => {
          const canClaim = userPoints >= reward.pointsRequired && !reward.claimed;
          return (
            <div key={reward.id} className={`glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition ${reward.claimed ? 'opacity-75' : ''}`}>
              <div className="flex items-start justify-between">
                <span className="text-3xl">{reward.icon}</span>
                {reward.claimed ? (
                  <span className="flex items-center gap-1 px-2 py-1 rounded-lg text-xs bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300">
                    <Check className="w-3 h-3" /> Obtenu
                  </span>
                ) : canClaim ? (
                  <button className="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs bg-yellow-500 text-white font-medium hover:bg-yellow-600 transition">
                    <Zap className="w-3 h-3" /> Réclamer
                  </button>
                ) : (
                  <span className="flex items-center gap-1 px-2 py-1 rounded-lg text-xs bg-gray-100 text-gray-500 dark:bg-white/5">
                    <Lock className="w-3 h-3" /> {reward.pointsRequired - userPoints} pts manquants
                  </span>
                )}
              </div>
              <h3 className="font-semibold text-gray-900 dark:text-white mt-3">{reward.name}</h3>
              <p className="text-xs text-gray-500 mt-1">{reward.description}</p>
              <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100 dark:border-white/[0.06]">
                <span className="text-xs text-gray-400">{typeLabel(reward.type)}</span>
                <span className="text-xs font-bold text-yellow-500">⭐ {reward.pointsRequired} pts</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
