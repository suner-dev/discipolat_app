import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  BookOpen, Plus, Loader2, RefreshCw, Play, Clock, BarChart3,
  Users, FileText, CheckCircle, Mic, Search, X,
} from 'lucide-react';

interface Sermon {
  id: string;
  title: string;
  speaker: string;
  date: string;
  duration?: string;
  category?: string;
  description?: string;
  transcriptionStatus?: 'NONE' | 'PENDING' | 'COMPLETED';
  status: string;
}

interface SermonStats {
  totalSermons: number;
  totalSpeakers: number;
  averageDuration: number;
  transcribed: number;
}

export default function SermonsPage() {
  const qc = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [search, setSearch] = useState('');
  const [newSermon, setNewSermon] = useState({ title: '', speaker: '', date: '', category: 'PREACHING', description: '' });

  const { data: sermons = [], isLoading } = useQuery({
    queryKey: ['sermons'],
    queryFn: async () => {
      const res = await api.get('/sermons');
      return (res.data.content || res.data || []) as Sermon[];
    },
  });

  const { data: stats } = useQuery({
    queryKey: ['sermons', 'stats'],
    queryFn: async () => {
      const res = await api.get('/sermons/stats');
      return res.data as SermonStats;
    },
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/sermons', newSermon);
    },
    onSuccess: () => {
      toast.success('Sermon créé');
      qc.invalidateQueries({ queryKey: ['sermons'] });
      setShowCreate(false);
      setNewSermon({ title: '', speaker: '', date: '', category: 'PREACHING', description: '' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const transcribeMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/sermons/${id}/transcribe`);
    },
    onSuccess: () => {
      toast.success('Transcription lancée');
      qc.invalidateQueries({ queryKey: ['sermons'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const filtered = sermons.filter((s) =>
    !search || s.title.toLowerCase().includes(search.toLowerCase()) || s.speaker.toLowerCase().includes(search.toLowerCase())
  );

  const statusColor = (s: string) => {
    switch (s) {
      case 'PUBLISHED': return 'badge-success';
      case 'DRAFT': return 'badge-warning';
      case 'ARCHIVED': return 'badge-gray';
      default: return 'badge-info';
    }
  };

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <BookOpen className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Sermons</h1>
          </div>
          <p className="page-subtitle">Gestion des sermons, transcription et bibliothèque</p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => qc.invalidateQueries({ queryKey: ['sermons'] })} className="btn-ghost btn-sm">
            <RefreshCw className="w-4 h-4" />
          </button>
          <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouveau sermon
          </button>
        </div>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Total sermons', value: stats.totalSermons, icon: BookOpen, color: 'from-primary-500 to-primary-600' },
            { label: 'Prédicateurs', value: stats.totalSpeakers, icon: Users, color: 'from-purple-500 to-violet-500' },
            { label: 'Durée moyenne', value: `${stats.averageDuration}min`, icon: Clock, color: 'from-blue-500 to-indigo-500' },
            { label: 'Transcrits', value: stats.transcribed, icon: Mic, color: 'from-green-500 to-emerald-500' },
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

      {/* Search */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex items-center gap-3">
          <Search className="w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Rechercher un sermon..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-transparent border-none outline-none text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400"
          />
        </div>
      </div>

      {/* Sermons list */}
      {isLoading ? (
        <div className="p-8 text-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400 mx-auto" /></div>
      ) : filtered.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <BookOpen className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucun sermon trouvé.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((sermon) => (
            <div key={sermon.id} className="glass-card px-5 py-4 flex items-center gap-4">
              <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-white shadow-sm shrink-0">
                <Play className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 truncate">{sermon.title}</h3>
                  <span className={`badge text-[10px] ${statusColor(sermon.status)}`}>{sermon.status}</span>
                </div>
                <div className="flex items-center gap-3 text-[10px] text-gray-400">
                  <span>{sermon.speaker}</span>
                  <span>{new Date(sermon.date).toLocaleDateString('fr-FR')}</span>
                  {sermon.duration && <span>{sermon.duration}</span>}
                  {sermon.category && <span className="badge text-[9px]">{sermon.category}</span>}
                </div>
              </div>
              <div className="flex items-center gap-1">
                {sermon.transcriptionStatus !== 'COMPLETED' && (
                  <button
                    onClick={() => transcribeMutation.mutate(sermon.id)}
                    disabled={transcribeMutation.isPending}
                    className="btn-icon text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                    title="Transcrire"
                  >
                    <Mic className="w-4 h-4" />
                  </button>
                )}
                {sermon.transcriptionStatus === 'COMPLETED' && (
                  <span className="text-green-500 p-1.5"><CheckCircle className="w-4 h-4" /></span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create modal */}
      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Nouveau sermon</h3>
              <button className="btn-icon" onClick={() => setShowCreate(false)}><X className="w-5 h-5" /></button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">Titre *</label>
                <input className="input" value={newSermon.title} onChange={(e) => setNewSermon({ ...newSermon, title: e.target.value })} placeholder="Titre du sermon" />
              </div>
              <div>
                <label className="label">Prédicateur</label>
                <input className="input" value={newSermon.speaker} onChange={(e) => setNewSermon({ ...newSermon, speaker: e.target.value })} placeholder="Nom du prédicateur" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">Date</label>
                  <input type="date" className="input" value={newSermon.date} onChange={(e) => setNewSermon({ ...newSermon, date: e.target.value })} />
                </div>
                <div>
                  <label className="label">Catégorie</label>
                  <select className="input" value={newSermon.category} onChange={(e) => setNewSermon({ ...newSermon, category: e.target.value })}>
                    <option value="PREACHING">Prédication</option>
                    <option value="TEACHING">Enseignement</option>
                    <option value="TESTIMONY">Témoignage</option>
                    <option value="OTHER">Autre</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="label">Description</label>
                <textarea className="input min-h-[60px]" value={newSermon.description} onChange={(e) => setNewSermon({ ...newSermon, description: e.target.value })} placeholder="Résumé du sermon..." />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowCreate(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => createMutation.mutate()} disabled={!newSermon.title || createMutation.isPending}>
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
