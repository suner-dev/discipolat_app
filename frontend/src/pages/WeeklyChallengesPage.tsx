import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Flame, Trophy, Star, Clock, CheckCircle, Zap } from 'lucide-react';

interface Challenge { id: string; title: string; description: string; category: string; difficulty: string; xpReward: number; progress: number; status: string; }

export default function WeeklyChallengesPage() {
  const { t } = useI18n();
  const MOCK: Challenge[] = [
    { id: '1', title: '5 jours de prière consécutive', description: 'Priez au moins 10 minutes chaque jour pendant 5 jours', category: 'PRAYER', difficulty: 'EASY', xpReward: 50, progress: 60, status: 'ACTIVE' },
    { id: '2', title: 'Lisez 3 chapitres de la Bible', description: 'Lisez au moins 3 chapitres cette semaine', category: 'READING', difficulty: 'MEDIUM', xpReward: 75, progress: 33, status: 'ACTIVE' },
    { id: '3', title: 'Rendez visite à un membre', description: 'Visitez un membre de votre famille', category: 'SERVICE', difficulty: 'MEDIUM', xpReward: 100, progress: 100, status: 'COMPLETED' },
    { id: '4', title: 'Jour de jeûne', description: 'Jeûnez un jour complet cette semaine', category: 'FASTING', difficulty: 'HARD', xpReward: 150, progress: 0, status: 'ACTIVE' },
    { id: '5', title: 'Gratitude quotidienne', description: 'Notez 3 choses dont vous êtes reconnaissant chaque jour', category: 'GRATITUDE', difficulty: 'EASY', xpReward: 40, progress: 80, status: 'ACTIVE' },
  ];
  const diffColor = (d: string) => d === 'HARD' ? 'text-red-400 bg-red-500/20' : d === 'MEDIUM' ? 'text-yellow-400 bg-yellow-500/20' : 'text-green-400 bg-green-500/20';
  const catEmoji: Record<string, string> = { PRAYER: '🙏', READING: '📖', SERVICE: '🤝', FASTING: '💧', EVANGELISM: '📢', GRATITUDE: '✨', FAMILY: '👨‍👩‍👧‍👦' };

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Flame className="text-orange-400" /> {t('challenges.title') || 'Défis hebdomadaires'}</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {MOCK.map(c => (
          <div key={c.id} className={`bg-white/5 backdrop-blur rounded-2xl p-5 border transition hover:scale-[1.02] ${c.status === 'COMPLETED' ? 'border-green-500/30' : 'border-white/10 hover:border-orange-500/30'}`}>
            <div className="flex items-center justify-between mb-3">
              <span className="text-2xl">{catEmoji[c.category] || '🎯'}</span>
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${diffColor(c.difficulty)}`}>{c.difficulty}</span>
            </div>
            <h3 className="text-white font-semibold mb-1">{c.title}</h3>
            <p className="text-xs text-gray-400 mb-3">{c.description}</p>
            <div className="w-full h-2 bg-gray-700 rounded-full overflow-hidden mb-2">
              <div className={`h-full rounded-full transition-all ${c.status === 'COMPLETED' ? 'bg-green-500' : 'bg-gradient-to-r from-orange-500 to-yellow-400'}`} style={{ width: `${c.progress}%` }} />
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-gray-400">{c.progress}%</span>
              <span className="text-orange-400 font-medium flex items-center gap-1"><Zap className="w-3 h-3" /> +{c.xpReward} XP</span>
            </div>
            {c.status === 'COMPLETED' && <div className="mt-2 text-green-400 text-xs flex items-center gap-1"><CheckCircle className="w-3 h-3" /> Terminé !</div>}
          </div>
        ))}
      </div>
    </div>
  );
}
