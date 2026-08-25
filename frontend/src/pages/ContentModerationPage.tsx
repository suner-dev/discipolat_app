import { formatEnum } from '@/lib/labels';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import {
  Shield, AlertTriangle, CheckCircle, XCircle, Filter, RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface ModerationItem {
  id: string;
  source: string;
  content: string;
  status: string;
  riskLevel: string;
  aiConfidence?: number;
  createdAt: string;
}

const FILTERS: Array<{ key: string; label: string; active: string; idle: string }> = [
  { key: 'all', label: 'Tous', active: 'bg-blue-600 text-white', idle: 'bg-white/5 text-gray-400 hover:bg-white/10' },
  { key: 'PENDING', label: 'En attente', active: 'bg-yellow-500 text-white', idle: 'bg-white/5 text-gray-400 hover:bg-white/10' },
  { key: 'APPROVED', label: 'Approuvés', active: 'bg-green-600 text-white', idle: 'bg-white/5 text-gray-400 hover:bg-white/10' },
  { key: 'REJECTED', label: 'Rejetés', active: 'bg-red-600 text-white', idle: 'bg-white/5 text-gray-400 hover:bg-white/10' },
];

export default function ContentModerationPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState('all');

  const { data: items = [], isLoading, error, refetch } = useQuery({
    queryKey: ['content-moderation'],
    queryFn: async () => (await api.get('/moderation')).data as ModerationItem[],
    retry: false,
  });

  const reviewMutation = useMutation({
    mutationFn: async ({ id, decision }: { id: string; decision: 'APPROVED' | 'REJECTED' }) =>
      (await api.put(`/moderation/${id}/review`, { decision })).data,
    onSuccess: () => {
      toast.success('Décision enregistrée');
      queryClient.invalidateQueries({ queryKey: ['content-moderation'] });
    },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const filtered = filter === 'all' ? items : items.filter(i => i.status === filter);

  const riskColor = (r: string) =>
    r === 'CRITICAL' ? 'text-red-400 bg-red-500/20'
    : r === 'HIGH' ? 'text-orange-400 bg-orange-500/20'
    : r === 'MEDIUM' ? 'text-yellow-400 bg-yellow-500/20'
    : 'text-green-400 bg-green-500/20';
  const statusIcon = (s: string) =>
    s === 'APPROVED' ? <CheckCircle className="w-4 h-4 text-green-400" />
    : s === 'REJECTED' ? <XCircle className="w-4 h-4 text-red-400" />
    : <AlertTriangle className="w-4 h-4 text-yellow-400" />;

  if (isLoading) return <SkeletonLoader lines={5} variant="card" />;
  if (error) return <div className="p-6 text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Shield className="text-red-400" /> {t('moderation.title') || 'Filtre de modération IA'}
        </h1>
        <button onClick={() => refetch()} className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/5 text-gray-400 text-sm hover:bg-white/10 transition">
          <RefreshCw className="w-4 h-4" /> Actualiser
        </button>
      </div>

      <div className="grid grid-cols-4 gap-3">
        {FILTERS.map(({ key, label, active, idle }) => (
          <button key={key} onClick={() => setFilter(key)}
            className={`p-3 rounded-xl text-sm font-medium transition ${filter === key ? active : idle}`}>
            {label}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Filter className="w-6 h-6 text-gray-400" />}
          title="Aucun contenu à modérer"
          message="Les contenus signalés apparaîtront ici pour approbation ou rejet."
        />
      ) : (
        <div className="space-y-3">
          {filtered.map(item => (
            <div key={item.id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10 flex items-start gap-4">
              {statusIcon(item.status)}
              <div className="flex-1 min-w-0">
                <p className="text-white text-sm line-clamp-2">{item.content}</p>
                <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
                  <span>{item.source}</span>
                  <span className={`px-2 py-0.5 rounded-full ${riskColor(item.riskLevel)}`}>{formatEnum(item.riskLevel)}</span>
                  {item.aiConfidence != null && (
                    <span>Confiance: {(item.aiConfidence * 100).toFixed(0)}%</span>
                  )}
                  <span>{new Date(item.createdAt).toLocaleDateString('fr-FR')}</span>
                </div>
              </div>
              {item.status === 'PENDING' && (
                <div className="flex gap-2">
                  <button onClick={() => reviewMutation.mutate({ id: item.id, decision: 'APPROVED' })}
                    disabled={reviewMutation.isPending}
                    className="px-3 py-1 bg-green-600 hover:bg-green-700 rounded-lg text-white text-xs font-medium transition disabled:opacity-50">
                    Approuver
                  </button>
                  <button onClick={() => reviewMutation.mutate({ id: item.id, decision: 'REJECTED' })}
                    disabled={reviewMutation.isPending}
                    className="px-3 py-1 bg-red-600 hover:bg-red-700 rounded-lg text-white text-xs font-medium transition disabled:opacity-50">
                    Rejeter
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
