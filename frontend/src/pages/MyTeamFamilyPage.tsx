import { useI18n } from '@/i18n';
import { Users, Heart, MessageCircle, Star } from 'lucide-react';

interface TeamMember { id: string; name: string; role: string; avatar: string; lastContact: string; }

export default function MyTeamFamilyPage() {
  const { t } = useI18n();
  const MOCK: TeamMember[] = [
    { id: '1', name: 'Pasteur Pierre', role: 'Pasteur', avatar: '👨‍🦳', lastContact: '2026-08-22' },
    { id: '2', name: 'Sarah Mbarga', role: 'Faiseur', avatar: '👩', lastContact: '2026-08-21' },
    { id: '3', name: 'David Ngo', role: 'Chef de famille', avatar: '👨', lastContact: '2026-08-20' },
    { id: '4', name: 'Marie Kotto', role: 'Responsable Louange', avatar: '👩‍🦱', lastContact: '2026-08-19' },
    { id: '5', name: 'Grace Fouda', role: 'Membre', avatar: '👩‍🦰', lastContact: '2026-08-18' },
    { id: '6', name: 'Paul Essomba', role: 'Membre', avatar: '👨‍🦲', lastContact: '2026-08-15' },
  ];

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Users className="text-pink-400" /> {t('myTeam.title') || 'Mon équipe / Ma famille'}</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {MOCK.map(m => (
          <div key={m.id} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10 hover:border-pink-500/30 transition text-center">
            <div className="text-4xl mb-3">{m.avatar}</div>
            <h3 className="text-white font-semibold">{m.name}</h3>
            <p className="text-xs text-pink-400 mb-2">{m.role}</p>
            <p className="text-xs text-gray-500 mb-3">Dernier contact: {m.lastContact}</p>
            <div className="flex justify-center gap-2">
              <button className="p-2 bg-blue-600/20 hover:bg-blue-600/40 rounded-lg text-blue-400 transition"><MessageCircle className="w-4 h-4" /></button>
              <button className="p-2 bg-pink-600/20 hover:bg-pink-600/40 rounded-lg text-pink-400 transition"><Heart className="w-4 h-4" /></button>
              <button className="p-2 bg-yellow-600/20 hover:bg-yellow-600/40 rounded-lg text-yellow-400 transition"><Star className="w-4 h-4" /></button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
