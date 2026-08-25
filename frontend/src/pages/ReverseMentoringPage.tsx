import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { formatEnum } from '@/lib/labels';
import { useI18n } from '@/i18n';
import { HelpCircle, Clock, CheckCircle, UserPlus, RefreshCw, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import apiRaw from '@/lib/apiRaw';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface ReverseRequest {
  id: string;
  requesterId?: string;
  assignedMentorId?: string;
  topic: string;
  description?: string;
  status: string;
  outcome?: string;
  urgencyLevel?: number;
  createdAt?: string;
  resolvedAt?: string;
}

const TOPIC_LABEL: Record<string, string> = {
  PASTORAL_CARE: 'Soin pastoral',
  LEADERSHIP: 'Leadership',
  FAMILY_ISSUE: 'Problème familial',
  SPIRITUAL_CRISIS: 'Crise spirituelle',
  DISCIPLINE: 'Discipline',
  CONFLICT: 'Conflit',
};

export default function ReverseMentoringPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState('all');

  const { data: requests = [], isLoading, error, refetch } = useQuery({
    queryKey: ['reverse-mentoring'],
    queryFn: async () => (await apiRaw.get('/reverse-mentoring')).data as ReverseRequest[],
    retry: false,
  });

  const currentId = localStorage.getItem('userId') || '';
  const hasCurrentId = /^[0-9a-fA-F-]{36}$/.test(currentId);

  const acceptMutation = useMutation({
    mutationFn: async (id: string) => {
      if (!hasCurrentId) throw new Error('Identifiant mentor manquant (userId).');
      return (await apiRaw.post(`/reverse-mentoring/${id}/accept`, { mentorId: currentId })).data;
    },
    onSuccess: () => { toast.success('Demande acceptée'); queryClient.invalidateQueries({ queryKey: ['reverse-mentoring'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const filtered = filter === 'all' ? requests : requests.filter(r => r.status === filter);

  const statusColor = (s: string) =>
    s === 'PENDING' ? 'bg-yellow-500/20 text-yellow-400'
    : s === 'ACCEPTED' || s === 'IN_PROGRESS' ? 'bg-blue-500/20 text-blue-400'
    : s === 'COMPLETED' ? 'bg-green-500/20 text-green-400'
    : 'bg-red-500/20 text-gray-400';
  const urgencyDots = (n: number) => Array.from({ length: 5 }, (_, i) => <span key={i} className={`w-2 h-2 rounded-full ${i < n ? 'bg-red-400' : 'bg-gray-700'}`} />);

  if (isLoading) return <SkeletonLoader lines={5} variant="card" />;
  if (error) return <div className="p-6 text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <HelpCircle className="text-pink-400" /> {t('reverseMentoring.title') || 'Mentorat inversé'}
        </h1>
        <button onClick={() => refetch()} className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/5 text-gray-400 text-sm hover:bg-white/10 transition">
          <RefreshCw className="w-4 h-4" /> Actualiser
        </button>
      </div>

      <div className="flex gap-2">
        {[['all', 'Toutes'], ['PENDING', 'En attente'], ['ACCEPTED', 'Acceptées'], ['IN_PROGRESS', 'En cours'], ['COMPLETED', 'Terminées']].map(([k, l]) => (
          <button key={k} onClick={() => setFilter(k)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${filter === k ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>
            {l}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<UserPlus className="w-6 h-6 text-pink-400" />}
          title="Aucune demande de mentorat"
          message="Les demandes de mentorat inversé apparaîtront ici."
        />
      ) : (
        <div className="space-y-3">
          {filtered.map(r => (
            <div key={r.id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-3">
                  <span className="text-white font-medium">{r.requesterId ? `Demandeur #${r.requesterId.slice(0, 6)}` : 'Demandeur'}</span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${statusColor(r.status)}`}>{formatEnum(r.status)}</span>
                </div>
                {r.urgencyLevel != null && <div className="flex items-center gap-1">{urgencyDots(r.urgencyLevel)}</div>}
              </div>
              {r.description && <p className="text-sm text-gray-300 mb-2">{r.description}</p>}
              <div className="flex items-center gap-3 text-xs text-gray-400">
                <span>Thème: {TOPIC_LABEL[r.topic] || r.topic}</span>
                {r.createdAt && <span>{new Date(r.createdAt).toLocaleDateString('fr-FR')}</span>}
                {r.assignedMentorId && <span className="text-blue-400">→ Mentor #{r.assignedMentorId.slice(0, 6)}</span>}
              </div>
              {r.status === 'PENDING' && (
                <button onClick={() => acceptMutation.mutate(r.id)}
                  disabled={acceptMutation.isPending}
                  className="mt-2 px-3 py-1 bg-blue-600 hover:bg-blue-700 rounded-lg text-white text-xs font-medium transition disabled:opacity-50 flex items-center gap-1">
                  {acceptMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <UserPlus className="w-3 h-3" />} Accepter
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
