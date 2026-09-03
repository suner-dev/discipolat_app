import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Gift, CheckCircle, Clock, Loader2, Plus, ExternalLink,
} from 'lucide-react';
import { useState } from 'react';

interface RewardClaim {
  id: string;
  rewardId: number;
  rewardName: string;
  status: string;
  claimedAt: string;
  processedAt?: string;
}

interface RewardItem {
  id: number;
  name: string;
  description?: string;
  pointsRequired: number;
  rewardType: string;
  isActive: boolean;
}

export default function RewardsClaimsPage() {
  const qc = useQueryClient();
  const [showClaimModal, setShowClaimModal] = useState(false);
  const [selectedRewardId, setSelectedRewardId] = useState<number | null>(null);

  const { data: claims = [], isLoading } = useQuery({
    queryKey: ['rewards', 'my-claims'],
    queryFn: async () => {
      const res = await api.get('/rewards/my-claims');
      return (res.data.content || res.data || []) as RewardClaim[];
    },
  });

  const { data: rewards = [] } = useQuery({
    queryKey: ['rewards'],
    queryFn: async () => {
      const res = await api.get('/rewards');
      return (res.data.content || res.data || []) as RewardItem[];
    },
  });

  const claimMutation = useMutation({
    mutationFn: async (rewardId: number) => {
      await api.post('/rewards/claim', { rewardId });
    },
    onSuccess: () => {
      toast.success('Réclamation envoyée');
      qc.invalidateQueries({ queryKey: ['rewards'] });
      setShowClaimModal(false);
      setSelectedRewardId(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const statusColor = (s: string) => {
    switch (s) {
      case 'APPROVED': return 'badge-success';
      case 'PENDING': return 'badge-warning';
      case 'REJECTED': return 'badge-error';
      default: return 'badge-info';
    }
  };

  const availableRewards = rewards.filter((r) => r.isActive);

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Gift className="w-5 h-5 text-yellow-500" />
            <h1 className="page-title">Mes réclamations</h1>
          </div>
          <p className="page-subtitle">Réclamez vos récompenses et suivez l'état de vos demandes</p>
        </div>
        <button onClick={() => setShowClaimModal(true)} className="btn-primary btn-sm">
          <Plus className="w-4 h-4" /> Réclamer une récompense
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total réclamations', value: claims.length, color: 'from-primary-500 to-primary-600' },
          { label: 'En attente', value: claims.filter((c) => c.status === 'PENDING').length, color: 'from-yellow-500 to-amber-500' },
          { label: 'Approuvées', value: claims.filter((c) => c.status === 'APPROVED').length, color: 'from-green-500 to-emerald-500' },
        ].map((stat, i) => (
          <div key={stat.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{stat.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${stat.color} text-white shadow-sm`}>
                <Gift className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{stat.value}</p>
          </div>
        ))}
      </div>

      {/* Claims list */}
      {isLoading ? (
        <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400 mx-auto" /></div>
      ) : claims.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Gift className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucune réclamation.</p>
          <button onClick={() => setShowClaimModal(true)} className="text-primary-500 hover:underline text-sm mt-2">
            Réclamer une récompense
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {claims.map((claim) => (
            <div key={claim.id} className="glass-card px-5 py-4 flex items-center gap-4">
              <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-yellow-500 to-amber-500 flex items-center justify-center text-white shadow-sm shrink-0">
                <Gift className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{claim.rewardName}</h3>
                <div className="flex items-center gap-3 text-[10px] text-gray-400 mt-0.5">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    Réclamé le {new Date(claim.claimedAt).toLocaleDateString('fr-FR')}
                  </span>
                  {claim.processedAt && (
                    <span>Traité le {new Date(claim.processedAt).toLocaleDateString('fr-FR')}</span>
                  )}
                </div>
              </div>
              <span className={`badge text-[10px] ${statusColor(claim.status)}`}>{claim.status}</span>
            </div>
          ))}
        </div>
      )}

      {/* Claim modal */}
      {showClaimModal && (
        <div className="modal-overlay" onClick={() => setShowClaimModal(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Réclamer une récompense</h3>
              <button className="btn-icon" onClick={() => setShowClaimModal(false)}>×</button>
            </div>
            <div className="modal-body space-y-3">
              {availableRewards.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-4">Aucune récompense disponible.</p>
              ) : (
                availableRewards.map((r) => (
                  <button
                    key={r.id}
                    onClick={() => setSelectedRewardId(r.id)}
                    className={`w-full text-left p-3 rounded-xl border transition-all ${
                      selectedRewardId === r.id
                        ? 'border-yellow-500 bg-yellow-50 dark:bg-yellow-900/10'
                        : 'border-gray-200 dark:border-white/10 hover:border-gray-300'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{r.name}</p>
                        {r.description && <p className="text-[10px] text-gray-400 mt-0.5">{r.description}</p>}
                      </div>
                      <span className="badge text-[10px] bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400">
                        {r.pointsRequired} pts
                      </span>
                    </div>
                  </button>
                ))
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowClaimModal(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                disabled={!selectedRewardId || claimMutation.isPending}
                onClick={() => selectedRewardId && claimMutation.mutate(selectedRewardId)}
              >
                {claimMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                Réclamer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
