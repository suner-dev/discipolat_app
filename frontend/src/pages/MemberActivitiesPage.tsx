import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  CheckCircle, XCircle, Calendar, BookOpen, TrendingUp, StickyNote,
  Activity, Filter,
} from 'lucide-react';
import { useState } from 'react';

type ActivityItem = {
  id: string;
  type: 'presence' | 'event' | 'note' | 'progression';
  title: string;
  subtitle: string;
  date: string;
};

const TYPE_META: Record<string, { icon: any; color: string; bg: string }> = {
  presence: { icon: CheckCircle, color: 'text-emerald-500', bg: 'bg-emerald-500/10' },
  event: { icon: Calendar, color: 'text-blue-500', bg: 'bg-blue-500/10' },
  note: { icon: StickyNote, color: 'text-amber-500', bg: 'bg-amber-500/10' },
  progression: { icon: TrendingUp, color: 'text-purple-500', bg: 'bg-purple-500/10' },
};

export default function MemberActivitiesPage() {
  const [filter, setFilter] = useState<string>('all');

  const { data: presences = [] } = useQuery({
    queryKey: ['member', 'presences'],
    queryFn: async () => (await api.get('/members/me/presences')).data as any[],
  });

  const { data: events = [] } = useQuery({
    queryKey: ['member', 'events'],
    queryFn: async () => (await api.get('/members/me/events')).data as any[],
  });

  const { data: notes = [] } = useQuery({
    queryKey: ['member', 'notes'],
    queryFn: async () => (await api.get('/members/me/notes')).data as any[],
  });

  const { data: progression } = useQuery({
    queryKey: ['member', 'progression'],
    queryFn: async () => (await api.get('/members/me/progression')).data as any,
  });

  const activities: ActivityItem[] = [
    ...(presences || []).map((p: any) => ({
      id: `pres-${p.semaine}`,
      type: 'presence' as const,
      title: p.present ? 'Présence confirmée' : 'Absence enregistrée',
      subtitle: `Semaine du ${p.semaine ?? ''}${p.notes ? ` · ${p.notes}` : ''}`,
      date: p.semaine ?? '',
    })),
    ...(events || []).map((e: any) => ({
      id: `evt-${e.id}`,
      type: 'event' as const,
      title: e.titre ?? '',
      subtitle: `${e.dateDebut ?? ''}${e.lieu ? ` · ${e.lieu}` : ''}`,
      date: e.dateDebut ?? '',
    })),
    ...(notes || []).map((n: any) => ({
      id: `note-${n.id}`,
      type: 'note' as const,
      title: 'Note du faiseur',
      subtitle: n.contenu ?? '',
      date: n.createdAt ?? '',
    })),
    ...(progression ? [{
      id: 'prog',
      type: 'progression' as const,
      title: `Niveau spirituel : ${progression.niveauActuel ?? '—'}`,
      subtitle: 'Progression globale',
      date: '',
    }] : []),
  ].sort((a, b) => (b.date ?? '').localeCompare(a.date ?? ''));

  const filtered = filter === 'all' ? activities : activities.filter(a => a.type === filter);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Activity className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              Mon historique
            </span>
          </div>
          <h1 className="page-title">
            Mes <span className="text-gradient font-display">activités</span>
          </h1>
          <p className="page-subtitle">Timeline de votre engagement communautaire</p>
        </div>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-2 mb-6 flex-wrap">
        <Filter className="w-4 h-4 text-gray-400" />
        {[
          { key: 'all', label: 'Toutes' },
          { key: 'presence', label: 'Présences' },
          { key: 'event', label: 'Événements' },
          { key: 'note', label: 'Notes' },
          { key: 'progression', label: 'Progression' },
        ].map(f => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`px-3 py-1.5 rounded-xl text-xs font-medium transition-all ${
              filter === f.key
                ? 'bg-primary-500/20 text-primary-600 dark:text-primary-400 border border-primary-500/30'
                : 'bg-white/50 dark:bg-gray-800/50 text-gray-500 border border-transparent hover:bg-gray-100 dark:hover:bg-gray-800'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Timeline */}
      {filtered.length === 0 ? (
        <div className="glass-card p-12 text-center animate-fade-in">
          <Activity className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-600 dark:text-gray-400 mb-2">Aucune activité</h2>
          <p className="text-sm text-gray-400">Vos présences, événements et notes apparaîtront ici.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((item, i) => {
            const meta = TYPE_META[item.type] || TYPE_META.presence;
            const Icon = meta.icon;
            return (
              <div
                key={item.id}
                className="flex gap-4 animate-slide-up"
                style={{ animationDelay: `${i * 30}ms` }}
              >
                {/* Timeline dot */}
                <div className="flex flex-col items-center">
                  <div className={`w-10 h-10 rounded-full ${meta.bg} flex items-center justify-center shrink-0`}>
                    <Icon className={`w-5 h-5 ${meta.color}`} />
                  </div>
                  {i < filtered.length - 1 && (
                    <div className="w-0.5 flex-1 bg-gray-200 dark:bg-gray-700 mt-2" />
                  )}
                </div>
                {/* Content */}
                <div className="glass-card p-4 flex-1 mb-0">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{item.title}</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{item.subtitle}</p>
                    </div>
                    {item.date && (
                      <span className="text-[10px] text-gray-400 shrink-0 ml-2">
                        {item.date.length > 10 ? item.date.substring(0, 10) : item.date}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
