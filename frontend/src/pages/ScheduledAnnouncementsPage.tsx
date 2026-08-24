import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Megaphone, Clock, CheckCircle, Calendar, Plus, Trash2 } from 'lucide-react';

interface Announcement { id: string; title: string; status: string; target: string; scheduledAt?: string; publishedAt?: string; expiresAt?: string; pinToTop: boolean; }

export default function ScheduledAnnouncementsPage() {
  const { t } = useI18n();
  const [filter, setFilter] = useState<string>('all');
  const MOCK: Announcement[] = [
    { id: '1', title: 'Culte spécial de prière - Samedi 30 Août', status: 'PUBLISHED', target: 'ALL', publishedAt: '2026-08-20', pinToTop: true },
    { id: '2', title: 'Réunion des responsables', status: 'SCHEDULED', target: 'DEPARTMENT', scheduledAt: '2026-08-25', pinToTop: false },
    { id: '3', title: 'Rappel: Don mensuel', status: 'DRAFT', target: 'ALL', pinToTop: false },
    { id: '4', title: 'Retraite spirituelle jeunesse', status: 'EXPIRED', target: 'FAMILY', expiresAt: '2026-08-15', pinToTop: false },
  ];
  const filtered = filter === 'all' ? MOCK : MOCK.filter(a => a.status === filter);
  const statusColor = (s: string) => s === 'PUBLISHED' ? 'bg-green-500/20 text-green-400' : s === 'SCHEDULED' ? 'bg-blue-500/20 text-blue-400' : s === 'DRAFT' ? 'bg-gray-500/20 text-gray-400' : 'bg-red-500/20 text-red-400';

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
              {a.status === 'DRAFT' && <button className="px-3 py-1 bg-green-600 hover:bg-green-700 rounded-lg text-white text-xs">Publier</button>}
              {a.status === 'SCHEDULED' && <button className="px-3 py-1 bg-yellow-600 hover:bg-yellow-700 rounded-lg text-white text-xs">Annuler</button>}
              <button className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
