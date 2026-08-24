import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Video, Radio, Play, Square, Users, Calendar, Plus, Clock, Eye, MessageCircle } from 'lucide-react';
import StreamingChat from '@/pages/StreamingChat';

interface Stream {
  id: string;
  title: string;
  description: string;
  status: 'scheduled' | 'live' | 'ended';
  viewerCount: number;
  scheduledAt?: string;
  startedAt?: string;
  thumbnailUrl?: string;
}

const MOCK_STREAMS: Stream[] = [
  { id: '1', title: 'Culte du dimanche', description: 'Culte dominical avec louange et prédication', status: 'scheduled', viewerCount: 0, scheduledAt: '2026-08-30T10:00:00' },
  { id: '2', title: 'Prières du matin', description: 'Moment de prière et adoration', status: 'live', viewerCount: 47, startedAt: '2026-08-23T06:00:00' },
  { id: '3', title: 'Étude biblique du mercredi', description: 'Approfondissement de la Parole', status: 'ended', viewerCount: 128, startedAt: '2026-08-20T19:00:00' },
];

export default function StreamingPage() {
  const { t } = useI18n();
  const [filter, setFilter] = useState<'all' | 'live' | 'scheduled' | 'ended'>('all');
  const [showChat, setShowChat] = useState(false);

  const filtered = filter === 'all' ? MOCK_STREAMS : MOCK_STREAMS.filter((s) => s.status === filter);

  const statusColor = (s: string) => {
    if (s === 'live') return 'bg-red-500 text-white animate-pulse';
    if (s === 'scheduled') return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300';
    return 'bg-gray-100 text-gray-500 dark:bg-white/5 dark:text-gray-400';
  };

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
        <button className="flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-500 text-white font-medium text-sm hover:bg-purple-600 transition">
          <Plus className="w-4 h-4" />
          Nouveau stream
        </button>
      </div>

      {/* Live stat card */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
              <Radio className="w-5 h-5 text-red-500" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">1</p>
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
              <p className="text-2xl font-bold text-gray-900 dark:text-white">47</p>
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
              <p className="text-2xl font-bold text-gray-900 dark:text-white">128</p>
              <p className="text-xs text-gray-500">Vues totales</p>
            </div>
          </div>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2">
        {(['all', 'live', 'scheduled', 'ended'] as const).map((f) => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === f ? 'bg-purple-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10 dark:bg-white/5 dark:text-gray-400'}`}>
            {f === 'all' ? 'Tous' : f === 'live' ? '🔴 En direct' : f === 'scheduled' ? 'Planifiés' : 'Terminés'}
          </button>
        ))}
      </div>

      {/* Chat toggle for live streams */}
      <div className="flex items-center gap-2">
        <button onClick={() => setShowChat(!showChat)}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition ${showChat ? 'bg-pink-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>
          <MessageCircle className="w-4 h-4" /> Chat en direct {showChat ? '✓' : ''}
        </button>
      </div>
      {showChat && <StreamingChat />}

      {/* Streams grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((stream) => (
          <div key={stream.id} className="glass rounded-2xl overflow-hidden border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
            <div className="h-40 bg-gradient-to-br from-purple-500/20 to-indigo-500/20 flex items-center justify-center relative">
              <Video className="w-12 h-12 text-purple-400/40" />
              <span className={`absolute top-3 right-3 px-2 py-1 rounded-lg text-xs font-bold ${statusColor(stream.status)}`}>
                {stream.status === 'live' ? '🔴 EN DIRECT' : stream.status === 'scheduled' ? '📅 Planifié' : '✓ Terminé'}
              </span>
            </div>
            <div className="p-4">
              <h3 className="font-semibold text-gray-900 dark:text-white">{stream.title}</h3>
              <p className="text-xs text-gray-500 mt-1 line-clamp-2">{stream.description}</p>
              <div className="flex items-center justify-between mt-3">
                {stream.status === 'live' ? (
                  <span className="flex items-center gap-1 text-xs text-red-500"><Users className="w-3 h-3" /> {stream.viewerCount} spectateurs</span>
                ) : stream.scheduledAt ? (
                  <span className="flex items-center gap-1 text-xs text-gray-400"><Calendar className="w-3 h-3" /> {new Date(stream.scheduledAt).toLocaleDateString('fr-FR')}</span>
                ) : (
                  <span className="flex items-center gap-1 text-xs text-gray-400"><Clock className="w-3 h-3" /> {stream.viewerCount} vues</span>
                )}
                <button className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-purple-500 text-white text-xs font-medium hover:bg-purple-600 transition">
                  {stream.status === 'live' ? <><Square className="w-3 h-3" /> Arrêter</> : stream.status === 'scheduled' ? <><Play className="w-3 h-3" /> Lancer</> : <><Play className="w-3 h-3" /> Replay</>}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
