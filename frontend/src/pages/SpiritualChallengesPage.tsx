import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { Zap, Plus, Trophy, Clock, CheckCircle2, Flame } from 'lucide-react';

interface Challenge {
  id: string;
  titre: string;
  description: string;
  type: 'JEUNE' | 'LECTURE' | 'SERVICE' | 'PRIERE' | 'AUTRE';
  dureeJours: number;
  progression: number;
  statut: 'EN_COURS' | 'TERMINE' | 'NON_DEMARRE';
  creePar: { firstName: string; lastName: string };
  createdAt: string;
}

const CHALLENGE_TYPES = [
  { key: 'JEUNE', label: 'Jeûne', icon: '🌙', color: 'bg-purple-100 text-purple-700' },
  { key: 'LECTURE', label: 'Lecture', icon: '📖', color: 'bg-blue-100 text-blue-700' },
  { key: 'SERVICE', label: 'Service', icon: '🤝', color: 'bg-green-100 text-green-700' },
  { key: 'PRIERE', label: 'Prière', icon: '🙏', color: 'bg-amber-100 text-amber-700' },
  { key: 'AUTRE', label: 'Autre', icon: '⭐', color: 'bg-gray-100 text-gray-700' },
];

export default function SpiritualChallengesPage() {
  const { t } = useI18n();
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newChallenge, setNewChallenge] = useState({ titre: '', description: '', type: 'PRIERE', dureeJours: 7 });

  useEffect(() => { loadChallenges(); }, []);

  const loadChallenges = async () => {
    try {
      setLoading(true);
      const res = await api.get('/spiritual-challenges');
      setChallenges(res.data.content || res.data || []);
    } catch { setChallenges([]); } finally { setLoading(false); }
  };

  const createChallenge = async () => {
    if (!newChallenge.titre.trim()) { Toast.warning('Titre requis'); return; }
    try { await api.post('/spiritual-challenges', newChallenge); Toast.success('Défi créé !'); setShowCreate(false); loadChallenges(); }
    catch { Toast.error('Erreur'); }
  };

  const joinChallenge = async (id: string) => {
    try { await api.post(`/spiritual-challenges/${id}/join`); Toast.success('Défi accepté ! 💪'); loadChallenges(); }
    catch { Toast.error('Erreur'); }
  };

  const getTypeInfo = (key: string) => CHALLENGE_TYPES.find(c => c.key === key) || CHALLENGE_TYPES[4];

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-6xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Zap className="w-8 h-8 text-yellow-500" />
            Défis Spirituels
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Relevez des défis pour grandir spirituellement</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-yellow-500 to-orange-500 text-white text-sm font-medium hover:from-yellow-600 hover:to-orange-600 transition-all shadow-lg shadow-yellow-500/25 flex items-center gap-2">
          <Plus className="w-4 h-4" /> Créer un défi
        </button>
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> : challenges.length === 0 ? (
        <EmptyState icon={<Zap className="w-8 h-8 text-gray-400" />} title="Aucun défi"
          message="Créez votre premier défi spirituel pour motiver la communauté"
          action={{ label: 'Créer un défi', onClick: () => setShowCreate(true) }} />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {challenges.map(ch => {
            const typeInfo = getTypeInfo(ch.type);
            return (
              <div key={ch.id} className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 hover:shadow-lg transition-all">
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-xl">{typeInfo.icon}</span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${typeInfo.color}`}>{typeInfo.label}</span>
                  {ch.statut === 'TERMINE' && <Trophy className="w-4 h-4 text-yellow-500" />}
                  {ch.statut === 'EN_COURS' && <Flame className="w-4 h-4 text-orange-500" />}
                </div>
                <h3 className="font-semibold text-gray-900 dark:text-white mb-1">{ch.titre}</h3>
                <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-2 mb-3">{ch.description}</p>
                <div className="flex items-center gap-4 text-xs text-gray-400 mb-3">
                  <span className="flex items-center gap-1"><Clock className="w-3 h-3" />{ch.dureeJours} jours</span>
                  <span>Par {ch.creePar.firstName} {ch.creePar.lastName}</span>
                </div>
                <div className="mb-3">
                  <div className="h-2 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-yellow-500 to-orange-500 rounded-full transition-all"
                      style={{ width: `${ch.progression}%` }} />
                  </div>
                  <span className="text-xs text-gray-500 mt-1 block">{ch.progression}%</span>
                </div>
                {ch.statut === 'NON_DEMARRE' && (
                  <button onClick={() => joinChallenge(ch.id)}
                    className="w-full px-3 py-2 rounded-lg bg-yellow-100 dark:bg-yellow-500/10 text-yellow-700 dark:text-yellow-400 text-xs font-medium hover:bg-yellow-200 dark:hover:bg-yellow-500/20 transition-all">
                    Relever le défi 💪
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau défi</h2>
            <div className="space-y-4">
              <div className="grid grid-cols-5 gap-2">
                {CHALLENGE_TYPES.map(c => (
                  <button key={c.key} onClick={() => setNewChallenge({ ...newChallenge, type: c.key })}
                    className={`p-2 rounded-xl border text-center text-xs transition-all ${newChallenge.type === c.key ? 'border-yellow-500 bg-yellow-50 dark:bg-yellow-500/10' : 'border-gray-200 dark:border-white/10'}`}>
                    <span className="text-lg">{c.icon}</span>
                    <div className="mt-1">{c.label}</div>
                  </button>
                ))}
              </div>
              <input type="text" value={newChallenge.titre} onChange={e => setNewChallenge({ ...newChallenge, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500"
                placeholder="Titre du défi..." />
              <textarea value={newChallenge.description} onChange={e => setNewChallenge({ ...newChallenge, description: e.target.value })}
                rows={3} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500 resize-none" />
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Durée (jours)</label>
                <input type="number" value={newChallenge.dureeJours} onChange={e => setNewChallenge({ ...newChallenge, dureeJours: +e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500" />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-gray-700 dark:text-gray-300 text-sm">Annuler</button>
              <button onClick={createChallenge} className="px-4 py-2 rounded-xl bg-yellow-600 text-white text-sm font-medium hover:bg-yellow-700 transition-all">Créer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
