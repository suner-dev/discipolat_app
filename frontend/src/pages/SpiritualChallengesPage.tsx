import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import toast from 'react-hot-toast';
import { Flame, Plus, Target, CheckCircle2, Clock, TrendingUp } from 'lucide-react';

interface Challenge {
  id: string;
  titre: string;
  description: string;
  type: string;
  statut: 'EN_COURS' | 'TERMINÉ' | 'ABANDONNÉ';
  objectifJours: number;
  joursComplétés: number;
  deadline?: string;
}

const TYPES = [
  { key: 'JEÛNE', label: 'Jeûne', icon: '🙏' },
  { key: 'LECTURE', label: 'Lecture', icon: '📖' },
  { key: 'PRIÈRE', label: 'Prière', icon: '🙌' },
  { key: 'SERVICE', label: 'Service', icon: '🤝' },
  { key: 'ÉVANGÉLISATION', label: 'Évangélisation', icon: '📢' },
  { key: 'AUTRE', label: 'Autre', icon: '⭐' },
];

export default function SpiritualChallengesPage() {
  const { t } = useI18n();
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [stats, setStats] = useState({ enCours: 0, terminés: 0, abandonnés: 0 });
  const [newChallenge, setNewChallenge] = useState({ titre: '', description: '', type: 'AUTRE', objectifJours: 7 });

  useEffect(() => { loadChallenges(); loadStats(); }, []);

  const loadChallenges = async () => {
    try {
      setLoading(true);
      const res = await api.get('/spiritual-challenges');
      setChallenges(res.data.content || res.data || []);
    } catch (e) { toast.error(getErrorMessage(e)); setChallenges([]); }
    finally { setLoading(false); }
  };

  const loadStats = async () => {
    try { const res = await api.get('/spiritual-challenges/stats'); setStats(res.data); } catch (e) { toast.error(getErrorMessage(e)); }
  };

  const createChallenge = async () => {
    if (!newChallenge.titre.trim()) { toast('Entrez un titre', { icon: '⚠️' }); return; }
    try {
      await api.post('/spiritual-challenges', newChallenge);
      toast.success('Défi créé !');
      setShowCreate(false);
      setNewChallenge({ titre: '', description: '', type: 'AUTRE', objectifJours: 7 });
      loadChallenges(); loadStats();
    } catch (e) { toast.error(getErrorMessage(e)); }
  };

  const progress = async (id: string) => {
    try {
      await api.patch(`/spiritual-challenges/${id}/progress`);
      toast.success('Jour marqué ! Continuez 💪');
      loadChallenges(); loadStats();
    } catch (e) { toast.error(getErrorMessage(e)); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Flame className="w-8 h-8 text-orange-500" />
            {t('spiritualChallenges.title')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Défis pour grandir dans la foi</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-orange-500 to-red-500 text-white text-sm font-medium hover:from-orange-600 hover:to-red-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> {t('spiritualChallenges.create')}
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'En cours', value: stats.enCours, color: 'text-orange-600', icon: Clock },
          { label: 'Terminés', value: stats.terminés, color: 'text-green-600', icon: CheckCircle2 },
          { label: 'Abandonnés', value: stats.abandonnés, color: 'text-gray-400', icon: TrendingUp },
        ].map(s => (
          <div key={s.label} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
            <s.icon className={`w-5 h-5 mx-auto mb-1 ${s.color}`} />
            <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
            <div className="text-xs text-gray-500">{s.label}</div>
          </div>
        ))}
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        challenges.length === 0 ? (
          <EmptyState icon={<Flame className="w-8 h-8 text-gray-400" />}
            title="Aucun défi spirituel"
            message="Créez un défi pour stimuler votre croissance spirituelle"
            action={{ label: 'Créer un défi', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {challenges.map(c => {
              const progressPct = c.objectifJours > 0 ? Math.min((c.joursComplétés / c.objectifJours) * 100, 100) : 0;
              const typeInfo = TYPES.find(t => t.key === c.type) || TYPES[5];
              return (
                <div key={c.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                  <div className="flex items-start justify-between mb-3">
                    <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{c.titre}</h3>
                    <span className="text-lg">{typeInfo.icon}</span>
                  </div>
                  {c.description && <p className="text-xs text-gray-500 dark:text-gray-400 mb-3 line-clamp-2">{c.description}</p>}
                  <div className="mb-3">
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-gray-500">{c.joursComplétés}/{c.objectifJours} jours</span>
                      <span className="text-orange-600 font-medium">{Math.round(progressPct)}%</span>
                    </div>
                    <div className="h-2 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
                      <div className="h-full bg-gradient-to-r from-orange-400 to-red-400 rounded-full transition-all duration-500" style={{ width: `${progressPct}%` }} />
                    </div>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${c.statut === 'TERMINÉ' ? 'bg-green-100 text-green-700' : c.statut === 'ABANDONNÉ' ? 'bg-gray-100 text-gray-500' : 'bg-orange-100 text-orange-700'}`}>
                      {c.statut === 'EN_COURS' ? 'En cours' : c.statut === 'TERMINÉ' ? 'Terminé' : 'Abandonné'}
                    </span>
                    {c.statut === 'EN_COURS' && (
                      <button onClick={() => progress(c.id)}
                        className="px-3 py-1 rounded-lg bg-orange-500 text-white text-xs font-medium hover:bg-orange-600 transition-all">
                        +1 jour
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau défi spirituel</h2>
            <div className="space-y-4">
              <input type="text" value={newChallenge.titre} onChange={e => setNewChallenge({ ...newChallenge, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                placeholder="Titre du défi" />
              <textarea value={newChallenge.description} onChange={e => setNewChallenge({ ...newChallenge, description: e.target.value })}
                rows={3} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500 resize-none"
                placeholder="Description (optionnel)" />
              <div className="grid grid-cols-2 gap-4">
                <select value={newChallenge.type} onChange={e => setNewChallenge({ ...newChallenge, type: e.target.value })}
                  className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm">
                  {TYPES.map(tp => <option key={tp.key} value={tp.key}>{tp.icon} {tp.label}</option>)}
                </select>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">Objectif (jours)</label>
                  <input type="number" value={newChallenge.objectifJours} onChange={e => setNewChallenge({ ...newChallenge, objectifJours: parseInt(e.target.value) || 7 })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm" min={1} />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={createChallenge} className="px-4 py-2 rounded-xl bg-orange-500 text-white text-sm font-medium hover:bg-orange-600">Créer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
