import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import toast from 'react-hot-toast';
import { Target, Plus, TrendingUp, CheckCircle2, Clock } from 'lucide-react';

interface Objective {
  id: string;
  titre: string;
  description?: string;
  catégorie: string;
  statut: 'EN_COURS' | 'ATTEINT' | 'ABANDONNÉ';
  objectifCible: number;
  progressionActuelle: number;
  deadline?: string;
}

const CATÉGORIES = [
  { key: 'PRIÈRE', label: 'Prière', icon: '🙏' },
  { key: 'LECTURE', label: 'Lecture', icon: '📖' },
  { key: 'SERVICE', label: 'Service', icon: '🤝' },
  { key: 'ÉVANGÉLISATION', label: 'Évangélisation', icon: '📢' },
  { key: 'FORMATION', label: 'Formation', icon: '🎓' },
  { key: 'AUTRE', label: 'Autre', icon: '⭐' },
];

export default function PersonalObjectivesPage() {
  const { t } = useI18n();
  const [objectives, setObjectives] = useState<Objective[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [stats, setStats] = useState({ total: 0, enCours: 0, atteints: 0 });
  const [newObj, setNewObj] = useState({ titre: '', description: '', catégorie: 'PRIÈRE', objectifCible: 1 });

  useEffect(() => { loadObjectives(); loadStats(); }, []);

  const loadObjectives = async () => {
    try { setLoading(true); const res = await api.get('/personal-objectives'); setObjectives(res.data || []); }
    catch (e) { toast.error(getErrorMessage(e)); setObjectives([]); } finally { setLoading(false); }
  };

  const loadStats = async () => { try { const res = await api.get('/personal-objectives/stats'); setStats(res.data); } catch (e) { toast.error(getErrorMessage(e)); } };

  const createObjective = async () => {
    if (!newObj.titre.trim()) { toast('Titre requis', { icon: '⚠️' }); return; }
    try { await api.post('/personal-objectives', newObj); toast.success('Objectif créé !'); setShowCreate(false); setNewObj({ titre: '', description: '', catégorie: 'PRIÈRE', objectifCible: 1 }); loadObjectives(); loadStats(); }
    catch (e) { toast.error(getErrorMessage(e)); }
  };

  const progress = async (id: string) => {
    try { await api.patch(`/personal-objectives/${id}/progress`); toast.success('Progression ! 🎯'); loadObjectives(); loadStats(); }
    catch (e) { toast.error(getErrorMessage(e)); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Target className="w-8 h-8 text-emerald-500" /> Objectifs Spirituels
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Définissez et suivez vos objectifs personnels</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 text-white text-sm font-medium hover:from-emerald-600 hover:to-teal-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> Nouvel objectif
        </button>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total', value: stats.total, color: 'text-emerald-600' },
          { label: 'En cours', value: stats.enCours, color: 'text-amber-600', icon: Clock },
          { label: 'Atteints', value: stats.atteints, color: 'text-green-600', icon: CheckCircle2 },
        ].map(s => (
          <div key={s.label} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
            <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
            <div className="text-xs text-gray-500">{s.label}</div>
          </div>
        ))}
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        objectives.length === 0 ? (
          <EmptyState icon={<Target className="w-8 h-8 text-gray-400" />}
            title="Aucun objectif" message="Créez des objectifs pour grandir intentionnellement"
            action={{ label: 'Créer un objectif', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {objectives.map(obj => {
              const progressPct = obj.objectifCible > 0 ? Math.min((obj.progressionActuelle / obj.objectifCible) * 100, 100) : 0;
              const catInfo = CATÉGORIES.find(c => c.key === obj.catégorie) || CATÉGORIES[5];
              return (
                <div key={obj.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className="text-lg">{catInfo.icon}</span>
                      <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{obj.titre}</h3>
                    </div>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${obj.statut === 'ATTEINT' ? 'bg-green-100 text-green-700' : obj.statut === 'ABANDONNÉ' ? 'bg-gray-100 text-gray-500' : 'bg-amber-100 text-amber-700'}`}>
                      {obj.statut === 'EN_COURS' ? 'En cours' : obj.statut === 'ATTEINT' ? 'Atteint' : 'Abandonné'}
                    </span>
                  </div>
                  {obj.description && <p className="text-xs text-gray-500 mb-2">{obj.description}</p>}
                  <div className="mb-3">
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-gray-500">{obj.progressionActuelle}/{obj.objectifCible}</span>
                      <span className="text-emerald-600 font-medium">{Math.round(progressPct)}%</span>
                    </div>
                    <div className="h-2 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
                      <div className="h-full bg-gradient-to-r from-emerald-400 to-teal-400 rounded-full" style={{ width: `${progressPct}%` }} />
                    </div>
                  </div>
                  {obj.statut === 'EN_COURS' && (
                    <button onClick={() => progress(obj.id)}
                      className="w-full px-3 py-1.5 rounded-lg bg-emerald-500 text-white text-xs font-medium hover:bg-emerald-600 transition-all">
                      +1 progression
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
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvel objectif</h2>
            <div className="space-y-4">
              <input type="text" value={newObj.titre} onChange={e => setNewObj({ ...newObj, titre: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Titre de l'objectif" />
              <textarea value={newObj.description} onChange={e => setNewObj({ ...newObj, description: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" placeholder="Description" />
              <div className="grid grid-cols-2 gap-4">
                <select value={newObj.catégorie} onChange={e => setNewObj({ ...newObj, catégorie: e.target.value })}
                  className="px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                  {CATÉGORIES.map(c => <option key={c.key} value={c.key}>{c.icon} {c.label}</option>)}
                </select>
                <div>
                  <label className="block text-xs text-gray-500 mb-1">Cible</label>
                  <input type="number" value={newObj.objectifCible} onChange={e => setNewObj({ ...newObj, objectifCible: parseInt(e.target.value) || 1 })} min={1}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={createObjective} className="px-4 py-2 rounded-xl bg-emerald-500 text-white text-sm font-medium hover:bg-emerald-600">Créer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
