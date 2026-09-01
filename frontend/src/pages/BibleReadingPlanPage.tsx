import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { BookOpen, Plus, CheckCircle2, Clock, Calendar, Users } from 'lucide-react';

interface ReadingPlan {
  id: string;
  titre: string;
  description: string;
  livre: string;
  chapitreDepart: number;
  chapitreFin: number;
  dureeJours: number;
  progression: number;
  statut: 'EN_COURS' | 'TERMINE' | 'NON_DEMARRE';
  participants: number;
  dateDebut?: string;
}

export default function BibleReadingPlanPage() {
  const { t } = useI18n();
  const [plans, setPlans] = useState<ReadingPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newPlan, setNewPlan] = useState({ titre: '', description: '', livre: '', chapitreDepart: 1, chapitreFin: 50, dureeJours: 30 });

  useEffect(() => { loadPlans(); }, []);

  const loadPlans = async () => {
    try {
      setLoading(true);
      const res = await api.get('/bible-reading/plans');
      setPlans(res.data.content || res.data || []);
    } catch { setPlans([]); } finally { setLoading(false); }
  };

  const createPlan = async () => {
    if (!newPlan.titre.trim()) { Toast.warning('Titre requis'); return; }
    try { await api.post('/bible-reading/plans', newPlan); Toast.success('Plan créé !'); setShowCreate(false); loadPlans(); }
    catch { Toast.error('Erreur'); }
  };

  const joinPlan = async (id: string) => {
    try { await api.get(`/bible-reading/plans/${id}`); Toast.success('Plan rejoint !'); loadPlans(); }
    catch { Toast.error('Erreur'); }
  };

  const markDayComplete = async (id: string) => {
    try { await api.post(`/bible-reading/entries/${id}/mark-read`); Toast.success('Journée marquée !'); loadPlans(); }
    catch { Toast.error('Erreur'); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-6xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <BookOpen className="w-8 h-8 text-amber-600" />
            Plans de Lecture Biblique
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Créez et partagez des parcours de lecture avec votre famille</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-amber-600 to-orange-600 text-white text-sm font-medium hover:from-amber-700 hover:to-orange-700 transition-all shadow-lg shadow-amber-500/25 flex items-center gap-2">
          <Plus className="w-4 h-4" /> Nouveau plan
        </button>
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> : plans.length === 0 ? (
        <EmptyState icon={<BookOpen className="w-8 h-8 text-gray-400" />} title="Aucun plan de lecture"
          message="Créez votre premier plan pour guider la lecture de votre famille"
          action={{ label: 'Créer un plan', onClick: () => setShowCreate(true) }} />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {plans.map(plan => (
            <div key={plan.id} className="p-5 rounded-2xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5">
              <div className="flex items-center gap-2 mb-2">
                <BookOpen className="w-4 h-4 text-amber-600" />
                <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{plan.titre}</h3>
              </div>
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-3 line-clamp-2">{plan.description}</p>
              <div className="flex items-center gap-4 text-xs text-gray-400 mb-3">
                <span className="flex items-center gap-1"><Calendar className="w-3 h-3" />{plan.dureeJours} jours</span>
                <span className="flex items-center gap-1"><Users className="w-3 h-3" />{plan.participants} participants</span>
              </div>
              <div className="mb-3">
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-gray-500">Progression</span>
                  <span className="text-amber-600 font-medium">{plan.progression}%</span>
                </div>
                <div className="h-2 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
                  <div className="h-full bg-gradient-to-r from-amber-500 to-orange-500 rounded-full transition-all"
                    style={{ width: `${plan.progression}%` }} />
                </div>
              </div>
              <div className="flex gap-2">
                <button onClick={() => markDayComplete(plan.id)}
                  className="flex-1 px-3 py-1.5 rounded-lg bg-amber-100 dark:bg-amber-500/10 text-amber-700 dark:text-amber-400 text-xs font-medium hover:bg-amber-200 dark:hover:bg-amber-500/20 transition-all flex items-center justify-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> Marquer jour
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau plan de lecture</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Titre *</label>
                <input type="text" value={newPlan.titre} onChange={e => setNewPlan({ ...newPlan, titre: e.target.value })}
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
                  placeholder="Ex: Évangile de Jean en 30 jours" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
                <textarea value={newPlan.description} onChange={e => setNewPlan({ ...newPlan, description: e.target.value })}
                  rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Livre</label>
                  <input type="text" value={newPlan.livre} onChange={e => setNewPlan({ ...newPlan, livre: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
                    placeholder="Jean" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Durée (jours)</label>
                  <input type="number" value={newPlan.dureeJours} onChange={e => setNewPlan({ ...newPlan, dureeJours: +e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-amber-500" />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm">Annuler</button>
              <button onClick={createPlan} className="px-4 py-2 rounded-xl bg-amber-600 text-white text-sm font-medium hover:bg-amber-700 transition-all">Créer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
