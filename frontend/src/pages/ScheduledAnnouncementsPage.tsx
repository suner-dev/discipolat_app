import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import toast from 'react-hot-toast';
import { Megaphone, Clock, CheckCircle, Calendar, Plus, Trash2 } from 'lucide-react';

interface Announcement { id: string; title: string; status: string; target: string; scheduledAt?: string; publishedAt?: string; expiresAt?: string; pinToTop: boolean; }

export default function ScheduledAnnouncementsPage() {
    const { t } = useI18n();
  const [filter, setFilter] = useState<string>('all');

  const { data: announcements = [], isLoading, error, refetch } = useQuery({
    queryKey: ['announcements'],
    queryFn: async () => {
      const res = await api.get('/announcements');
      return (res.data as Array<Record<string, unknown>>).map((a) => ({
        id: String(a.id), title: String(a.title ?? ''), status: String(a.status ?? 'DRAFT'),
        target: String(a.target ?? 'ALL'), scheduledAt: a.scheduledAt as string | undefined,
        publishedAt: a.publishedAt as string | undefined, expiresAt: a.expiresAt as string | undefined,
        pinToTop: Boolean(a.pinToTop),
      })) as Announcement[];
    },
    retry: false,
  });

  const publishMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/announcements/${id}/publish`); },
    onSuccess: () => { toast.success('Annonce publiée'); refetch(); },
    onError: () => toast.error('Erreur publication'),
  });
  const cancelMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/announcements/${id}/cancel`); },
    onSuccess: () => { toast.success('Annonce annulée'); refetch(); },
    onError: () => toast.error('Erreur'),
  });
  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/announcements/${id}`); },
    onSuccess: () => { toast.success('Annonce supprimée'); refetch(); },
    onError: () => toast.error('Erreur suppression'),
  });

  const filtered = filter === 'all' ? announcements : announcements.filter(a => a.status === filter);
  const statusColor = (s: string) => s === 'PUBLISHED' ? 'bg-green-500/20 text-green-400' : s === 'SCHEDULED' ? 'bg-blue-500/20 text-blue-400' : s === 'DRAFT' ? 'bg-gray-500/20 text-gray-400' : 'bg-red-500/20 text-red-400';

  if (isLoading) return <SkeletonLoader lines={4} variant="card" />;
  if (error) return <div className="text-red-500 p-6">{getErrorMessage(error)}</div>;
  if (announcements.length === 0) return <EmptyState title="Aucune annonce" message="Aucune annonce programmée pour le moment." />

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Megaphone className="text-orange-400" /> {t('announcements.title') || 'Annonces programmées'}</h1>
      <div className="flex gap-2">
        {['all','DRAFT','SCHEDULED','PUBLISHED','EXPIRED'].map(s => (
          <button key={s} onClick={() => setFilter(s)} className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${filter === s ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>{s === 'all' ? 'Toutes' : s}</button>
        ))}
      </div>
      <div className="space-y-3">
        {filtered.map(a => (
          <div key={a.id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10 flex items-center gap-4">
            {a.pinToTop && <span className="text-yellow-400 text-lg">📌</span>}
            <div className="flex-1">
              <h3 className="text-white font-medium">{a.title}</h3>
              <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                <span className={`px-2 py-0.5 rounded-full ${statusColor(a.status)}`}>{a.status}</span>
                <span>Cible: {a.target}</span>
                {a.scheduledAt && <span>Planifié: {a.scheduledAt}</span>}
                {a.publishedAt && <span>Publié: {a.publishedAt}</span>}
              </div>
            </div>
                        <div className="flex gap-2">
              {a.status === 'DRAFT' && <button onClick={() => publishMutation.mutate(a.id)} className="px-3 py-1 bg-green-600 hover:bg-green-700 rounded-lg text-white text-xs">Publier</button>}
              {a.status === 'SCHEDULED' && <button onClick={() => cancelMutation.mutate(a.id)} className="px-3 py-1 bg-yellow-600 hover:bg-yellow-700 rounded-lg text-white text-xs">Annuler</button>}
              <button onClick={() => deleteMutation.mutate(a.id)} className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
