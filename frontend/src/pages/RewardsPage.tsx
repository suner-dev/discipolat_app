import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Gift, Award, Lock } from 'lucide-react';

interface RewardItem { id: number; name: string; description?: string; pointsRequired?: number; rewardType?: string; isActive?: boolean; }

const TYPE_EMOJI: Record<string, string> = { BADGE: '🏅', CERTIFICATE: '📜', MENTION: '⭐', GIFT: '🎁' };

/**
 * P3 #108 — Système de récompenses avancé.
 * ⚠️ API legacy en IDs numériques (Long). La lecture est branchée si un tenantId
 * numérique est disponible ; sinon un état vide explicite est affiché.
 */
export default function RewardsPage() {
  const numericTenant = Number(localStorage.getItem('tenantId') || localStorage.getItem('orgId') || 0);
  const canList = Number.isFinite(numericTenant) && numericTenant > 0;

  const rewardsQ = useQuery({
    queryKey: ['rewards', numericTenant], enabled: canList,
    queryFn: async () => (await api.get('/rewards', { params: { tenantId: numericTenant } })).data as RewardItem[],
    retry: false,
  });

  const rewards = (Array.isArray(rewardsQ.data) ? rewardsQ.data : []).filter((r) => r.isActive !== false);

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Gift className="text-yellow-400" /> Récompenses</h1>
      {!canList && (
        <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-yellow-500/30 flex items-start gap-3">
          <Lock className="w-5 h-5 text-yellow-400 mt-0.5 shrink-0" />
          <p className="text-sm text-gray-300">Le catalogue de récompenses nécessite une configuration tenant numérique (ID legacy). Contactez l'administrateur plateforme pour activer ce module sur votre église.</p>
        </div>
      )}
      {canList && rewardsQ.isLoading && <p className="text-sm text-gray-400">Chargement du catalogue…</p>}
      {canList && !rewardsQ.isLoading && rewards.length === 0 && <p className="text-sm text-gray-500">Aucune récompense disponible pour le moment.</p>}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {rewards.map((r) => (
          <div key={r.id} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10 hover:border-yellow-500/30 transition">
            <div className="flex items-center justify-between mb-3">
              <span className="text-2xl">{TYPE_EMOJI[r.rewardType ?? ''] || '🏅'}</span>
              <span className="px-2 py-0.5 rounded-full text-xs text-yellow-400 bg-yellow-500/20">{r.rewardType}</span>
            </div>
            <h3 className="text-white font-semibold mb-1 flex items-center gap-2"><Award className="w-4 h-4 text-yellow-500" /> {r.name}</h3>
            {r.description && <p className="text-xs text-gray-400 mb-3">{r.description}</p>}
            {!!r.pointsRequired && <p className="text-xs text-yellow-400 font-medium">{r.pointsRequired} points requis</p>}
          </div>
        ))}
      </div>
    </div>
  );
}
