import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  Shield, CheckCircle2, XCircle, AlertTriangle, Eye, Clock, RefreshCw,
  Loader2, Filter, Search, Ban, Check, ChevronDown, MessageSquare,
} from 'lucide-react';

interface ModerationItem {
  id: string;
  contentType: string;
  contentId: string;
  content: string;
  authorName: string;
  status: string;
  flagReason: string;
  flaggedAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
}

interface ModerationStats {
  totalPending: number;
  totalApproved: number;
  totalRejected: number;
  flaggedToday: number;
  avgReviewTime: string;
}

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400',
  APPROVED: 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400',
  REJECTED: 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400',
  ESCALATED: 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400',
};

export default function ModerationPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState('PENDING');
  const [search, setSearch] = useState('');
  const [selectedItem, setSelectedItem] = useState<ModerationItem | null>(null);

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['moderation', 'stats'],
    queryFn: async () => {
      const res = await api.get('/moderation/stats');
      return res.data as ModerationStats;
    },
  });

  const { data: items = [], isLoading, refetch } = useQuery({
    queryKey: ['moderation', filter],
    queryFn: async () => {
      if (filter === 'ALL') {
        const res = await api.get('/moderation');
        return res.data as ModerationItem[];
      }
      const res = await api.get('/moderation/pending');
      return res.data as ModerationItem[];
    },
  });

  const approveMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/moderation/${id}/approve`); },
    onSuccess: () => { toast.success('Contenu approuvé'); refetch(); setSelectedItem(null); },
    onError: () => toast.error('Erreur lors de l\'approbation'),
  });

  const rejectMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/moderation/${id}/reject`); },
    onSuccess: () => { toast.success('Contenu rejeté'); refetch(); setSelectedItem(null); },
    onError: () => toast.error('Erreur lors du rejet'),
  });

  const escalateMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/moderation/${id}/escalate`); },
    onSuccess: () => { toast.success('Escaladé au pasteur'); refetch(); setSelectedItem(null); },
    onError: () => toast.error('Erreur lors de l\'escalade'),
  });

  const filtered = items.filter(item =>
    (filter === 'ALL' || item.status === filter) &&
    (search === '' || item.content.toLowerCase().includes(search.toLowerCase()) ||
     item.authorName.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Shield className="w-5 h-5 text-purple-500" />
            Modération de contenu
          </h1>
          <p className="page-subtitle">Filtre IA • Approbation/rejet • Escalade au pasteur</p>
        </div>
        <button onClick={() => refetch()} className="btn-ghost btn-sm">
          <RefreshCw className="w-4 h-4" /> Actualiser
        </button>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
          {[
            { label: 'En attente', value: stats.totalPending, icon: Clock, color: 'amber' },
            { label: 'Approuvés', value: stats.totalApproved, icon: CheckCircle2, color: 'green' },
            { label: 'Rejetés', value: stats.totalRejected, icon: XCircle, color: 'red' },
            { label: 'Signalés aujourd\'hui', value: stats.flaggedToday, icon: AlertTriangle, color: 'purple' },
            { label: 'Temps moyen', value: stats.avgReviewTime, icon: Eye, color: 'blue' },
          ].map((stat, i) => (
            <div key={stat.label} className="stat-card" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="flex items-center justify-between mb-1">
                <span className="stat-label text-[10px]">{stat.label}</span>
                <stat.icon className={`w-3.5 h-3.5 text-${stat.color}-500`} />
              </div>
              <p className="stat-value text-xl">{stat.value}</p>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-4">
        <div className="flex gap-1 bg-gray-100 dark:bg-gray-800 rounded-xl p-1">
          {['PENDING', 'APPROVED', 'REJECTED', 'ALL'].map(s => (
            <button key={s} onClick={() => setFilter(s)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                filter === s ? 'bg-white dark:bg-gray-700 shadow-sm text-gray-900 dark:text-gray-100' : 'text-gray-500'
              }`}>
              {s === 'PENDING' ? 'En attente' : s === 'APPROVED' ? 'Approuvés' : s === 'REJECTED' ? 'Rejetés' : 'Tout'}
            </button>
          ))}
        </div>
        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input className="input pl-9 text-sm" placeholder="Rechercher..." value={search}
            onChange={e => setSearch(e.target.value)} />
        </div>
      </div>

      {/* Items */}
      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
      ) : filtered.length === 0 ? (
        <div className="glass-card p-12 text-center">
          <CheckCircle2 className="w-12 h-12 text-green-400 mx-auto mb-3" />
          <p className="text-gray-500 font-medium">Aucun contenu à modérer</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map(item => (
            <div key={item.id} className="glass-card px-5 py-4 flex items-start gap-4 cursor-pointer hover:shadow-md transition-all"
              onClick={() => setSelectedItem(item)}>
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center text-white flex-shrink-0">
                <MessageSquare className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="badge text-[10px]">{item.contentType}</span>
                  <span className={`badge text-[10px] ${STATUS_COLORS[item.status] || ''}`}>{item.status}</span>
                </div>
                <p className="text-sm text-gray-900 dark:text-gray-100 line-clamp-2">{item.content}</p>
                <div className="flex items-center gap-3 mt-1 text-[10px] text-gray-400">
                  <span>👤 {item.authorName}</span>
                  <span>🚩 {item.flagReason}</span>
                  <span>📅 {new Date(item.flaggedAt).toLocaleDateString('fr-FR')}</span>
                </div>
              </div>
              {item.status === 'PENDING' && (
                <div className="flex gap-1" onClick={e => e.stopPropagation()}>
                  <button onClick={() => approveMutation.mutate(item.id)}
                    className="btn-icon text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20" title="Approuver">
                    <Check className="w-4 h-4" />
                  </button>
                  <button onClick={() => rejectMutation.mutate(item.id)}
                    className="btn-icon text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20" title="Rejeter">
                    <XCircle className="w-4 h-4" />
                  </button>
                  <button onClick={() => escalateMutation.mutate(item.id)}
                    className="btn-icon text-purple-500 hover:bg-purple-50 dark:hover:bg-purple-900/20" title="Escalader">
                    <AlertTriangle className="w-4 h-4" />
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Detail Modal */}
      {selectedItem && (
        <div className="modal-overlay" onClick={() => setSelectedItem(null)}>
          <div className="modal-content max-w-lg" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Détail du contenu signalé</h3>
              <button className="btn-icon" onClick={() => setSelectedItem(null)}>
                <XCircle className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <span className={`badge ${STATUS_COLORS[selectedItem.status]}`}>{selectedItem.status}</span>
                <span className="badge ml-2">{selectedItem.contentType}</span>
              </div>
              <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                <p className="text-sm text-gray-900 dark:text-gray-100">{selectedItem.content}</p>
              </div>
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div><span className="text-gray-400">Auteur :</span> <span className="font-medium">{selectedItem.authorName}</span></div>
                <div><span className="text-gray-400">Raison :</span> <span className="font-medium">{selectedItem.flagReason}</span></div>
                <div><span className="text-gray-400">Signalé le :</span> <span className="font-medium">{new Date(selectedItem.flaggedAt).toLocaleString('fr-FR')}</span></div>
                {selectedItem.reviewedAt && <div><span className="text-gray-400">Revu le :</span> <span className="font-medium">{new Date(selectedItem.reviewedAt).toLocaleString('fr-FR')}</span></div>}
              </div>
            </div>
            {selectedItem.status === 'PENDING' && (
              <div className="modal-footer">
                <button onClick={() => rejectMutation.mutate(selectedItem.id)} className="btn-ghost btn-sm text-red-500">
                  <Ban className="w-4 h-4" /> Rejeter
                </button>
                <button onClick={() => escalateMutation.mutate(selectedItem.id)} className="btn-ghost btn-sm text-purple-500">
                  <AlertTriangle className="w-4 h-4" /> Escalader
                </button>
                <button onClick={() => approveMutation.mutate(selectedItem.id)} className="btn-primary btn-sm">
                  <Check className="w-4 h-4" /> Approuver
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
