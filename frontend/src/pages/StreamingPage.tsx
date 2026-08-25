import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import {
  Video, Radio, Play, Square, Users, Calendar, Plus, Clock, Eye, MessageCircle, RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import StreamingChat from '@/pages/StreamingChat';

interface Stream {
  id: string;
  title: string;
  description?: string;
  status: string;
  viewerCount?: number;
  scheduledAt?: string;
  startedAt?: string;
  endedAt?: string;
  thumbnailUrl?: string;
  streamUrl?: string;
}

const STATUS_LABEL: Record<string, string> = {
  SCHEDULED: 'scheduled', LIVE: 'live', ENDED: 'ended', CANCELLED: 'ended',
};

export default function StreamingPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const tenantId = Number(localStorage.getItem('tenantId') || localStorage.getItem('orgId') || 0);
  const [filter, setFilter] = useState<'all' | 'live' | 'scheduled' | 'ended'>('all');
  const [showChat, setShowChat] = useState(false);

  const { data: streams = [], isLoading, error, refetch } = useQuery({
    queryKey: ['streams', tenantId],
    queryFn: async () => (await api.get('/streams', { params: { tenantId } })).data as Stream[],
    enabled: tenantId > 0,
    retry: false,
  });

  const goLiveMutation = useMutation({
    mutationFn: async (id: string) => (await api.post(`/streams/${id}/go-live`)).data,
    onSuccess: () => { toast.success('Stream lancé en direct'); queryClient.invalidateQueries({ queryKey: ['streams'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const endMutation = useMutation({
    mutationFn: async (id: string) => (await api.post(`/streams/${id}/end`)).data,
    onSuccess: () => { toast.success('Stream terminé'); queryClient.invalidateQueries({ queryKey: ['streams'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const normalized = streams.map(s => ({ ...s, status: STATUS_LABEL[s.status] || 'scheduled' }));
  const filtered = filter === 'all' ? normalized : normalized.filter((s) => s.status === filter);

  const liveStreams = normalized.filter(s => s.status === 'live');
  const currentViewers = liveStreams.reduce((a, s) => a + (s.viewerCount || 0), 0);
  const totalViews = normalized.reduce((a, s) => a + (s.viewerCount || 0), 0);

  const statusColor = (s: string) => {
    if (s === 'live') return 'bg-red-500 text-white animate-pulse';
    if (s === 'scheduled') return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300';
    return 'bg-gray-100 text-gray-500 dark:bg-white/5 dark:text-gray-400';
  };

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="p-6 text-red-500 dark:text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Video className="w-7 h-7 text-purple-500" />
            {t('nav.streaming') ?? 'Streaming & Diffusion'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">Gérez vos cultes en direct et replay</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => refetch()} className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/5 text-gray-500 text-sm hover:bg-white/10 transition">
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
          <button className="flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-500 text-white font-medium text-sm hover:bg-purple-600 transition">
            <Plus className="w-4 h-4" />
            Nouveau stream
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
              <Radio className="w-5 h-5 text-red-500" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">{liveStreams.length}</p>
              <p className="text-xs text-gray-500">En direct</p>
            </div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
              <Users className="w-5 h-5 text-blue-500" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">{currentViewers}</p>
              <p className="text-xs text-gray-500">Spectateurs actuels</p>
            </div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <Eye className="w-5 h-5 text-green-500" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">{totalViews}</p>
              <p className="text-xs text-gray-500">Vues totales</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex gap-2">
        {(['all', 'live', 'scheduled', 'ended'] as const).map((f) => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === f ? 'bg-purple-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10 dark:bg-white/5 dark:text-gray-400'}`}>
            {f === 'all' ? 'Tous' : f === 'live' ? '🔴 En direct' : f === 'scheduled' ? 'Planifiés' : 'Terminés'}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-2">
        <button onClick={() => setShowChat(!showChat)}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition ${showChat ? 'bg-pink-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>
          <MessageCircle className="w-4 h-4" /> Chat en direct {showChat ? '✓' : ''}
        </button>
      </div>
      {showChat && <StreamingChat />}

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Video className="w-6 h-6 text-purple-400" />}
          title="Aucun stream"
          message="Planifiez un stream pour diffuser vos cultes en direct."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((stream) => (
            <div key={stream.id} className="glass rounded-2xl overflow-hidden border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
              <div className="h-40 bg-gradient-to-br from-purple-500/20 to-indigo-500/20 flex items-center justify-center relative">
                {stream.thumbnailUrl ? (
                  <img src={stream.thumbnailUrl} alt={stream.title} className="w-full h-full object-cover" />
                ) : (
                  <Video className="w-12 h-12 text-purple-400/40" />
                )}
                <span className={`absolute top-3 right-3 px-2 py-1 rounded-lg text-xs font-bold ${statusColor(stream.status)}`}>
                  {stream.status === 'live' ? '🔴 EN DIRECT' : stream.status === 'scheduled' ? '📅 Planifié' : '✓ Terminé'}
                </span>
              </div>
              <div className="p-4">
                <h3 className="font-semibold text-gray-900 dark:text-white">{stream.title}</h3>
                <p className="text-xs text-gray-500 mt-1 line-clamp-2">{stream.description}</p>
                <div className="flex items-center justify-between mt-3">
                  {stream.status === 'live' ? (
                    <span className="flex items-center gap-1 text-xs text-red-500"><Users className="w-3 h-3" /> {stream.viewerCount || 0} spectateurs</span>
                  ) : stream.scheduledAt ? (
                    <span className="flex items-center gap-1 text-xs text-gray-400"><Calendar className="w-3 h-3" /> {new Date(stream.scheduledAt).toLocaleDateString('fr-FR')}</span>
                  ) : (
                    <span className="flex items-center gap-1 text-xs text-gray-400"><Clock className="w-3 h-3" /> {stream.viewerCount || 0} vues</span>
                  )}
                  {stream.status === 'live' ? (
                    <button onClick={() => endMutation.mutate(stream.id)}
                      disabled={endMutation.isPending}
                      className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-red-500 text-white text-xs font-medium hover:bg-red-600 transition disabled:opacity-50">
                      <Square className="w-3 h-3" /> Arrêter
                    </button>
                  ) : stream.status === 'scheduled' ? (
                    <button onClick={() => goLiveMutation.mutate(stream.id)}
                      disabled={goLiveMutation.isPending}
                      className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-purple-500 text-white text-xs font-medium hover:bg-purple-600 transition disabled:opacity-50">
                      <Play className="w-3 h-3" /> Lancer
                    </button>
                  ) : (
                    <button disabled className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-white/5 text-gray-400 text-xs font-medium">
                      <Play className="w-3 h-3" /> Replay
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
