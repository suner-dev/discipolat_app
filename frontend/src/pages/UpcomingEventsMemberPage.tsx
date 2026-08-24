import { useI18n } from '@/i18n';
import { Calendar, Users, BookOpen, Star, Heart, TrendingUp, TrendingDown } from 'lucide-react';

interface MemberEvent { id: string; title: string; date: string; type: string; rsvp: string; }

export default function UpcomingEventsMemberPage() {
  const { t } = useI18n();
  const MOCK: MemberEvent[] = [
    { id: '1', title: 'Culte du dimanche', date: '2026-08-24', type: 'Culte', rsvp: 'going' },
    { id: '2', title: 'Réunion petits groupes', date: '2026-08-27', type: 'Groupe', rsvp: 'going' },
    { id: '3', title: 'Retraite jeunesse', date: '2026-08-30', type: 'Événement', rsvp: 'interested' },
    { id: '4', title: 'Soirée de témoignages', date: '2026-09-05', type: 'Témoignage', rsvp: 'none' },
  ];
  const rsvpColor = (r: string) => r === 'going' ? 'text-green-400 bg-green-500/20' : r === 'interested' ? 'text-blue-400 bg-blue-500/20' : 'text-gray-400 bg-gray-500/20';

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Calendar className="text-cyan-400" /> {t('upcomingEvents.title') || 'Mes événements à venir'}</h1>
      <div className="space-y-3">
        {MOCK.map(e => (
          <div key={e.id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10 flex items-center gap-4 hover:border-cyan-500/30 transition">
            <div className="w-14 h-14 bg-gradient-to-br from-cyan-500/20 to-blue-500/20 rounded-xl flex flex-col items-center justify-center border border-cyan-500/20">
              <span className="text-xs text-cyan-400 font-bold">{new Date(e.date).toLocaleDateString('fr-FR', { weekday: 'short' })}</span>
              <span className="text-lg font-bold text-white">{new Date(e.date).getDate()}</span>
            </div>
            <div className="flex-1">
              <h3 className="text-white font-medium">{e.title}</h3>
              <p className="text-xs text-gray-400">{e.type} • {e.date}</p>
            </div>
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${rsvpColor(e.rsvp)}`}>{e.rsvp === 'going' ? '✅ Y vais' : e.rsvp === 'interested' ? '⭐ Intéressé' : '— Non répondu'}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
