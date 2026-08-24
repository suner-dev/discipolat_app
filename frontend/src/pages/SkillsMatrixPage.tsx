import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { GraduationCap, Star, BarChart3 } from 'lucide-react';

interface SkillEval {
  id: string;
  membreId: string;
  compétence: string;
  niveau: 'DÉBUTANT' | 'INTERMÉDIAIRE' | 'AVANCÉ' | 'EXPERT';
  commentaire?: string;
  createdAt: string;
}

const NIVEAUX = [
  { key: 'DÉBUTANT', label: 'Débutant', color: 'bg-gray-100 text-gray-700', stars: 1 },
  { key: 'INTERMÉDIAIRE', label: 'Intermédiaire', color: 'bg-blue-100 text-blue-700', stars: 2 },
  { key: 'AVANCÉ', label: 'Avancé', color: 'bg-orange-100 text-orange-700', stars: 3 },
  { key: 'EXPERT', label: 'Expert', color: 'bg-green-100 text-green-700', stars: 4 },
];

const COMPÉTENCES = ['Animation', 'Musique', 'Accueil', 'Prédication', 'Pédagogie', 'Organisation', 'Communication', 'Écoute', 'Leadership', 'Technique'];

export default function SkillsMatrixPage() {
  const { t } = useI18n();
  const [evaluations, setEvaluations] = useState<SkillEval[]>([]);
  const [loading, setLoading] = useState(true);
  const [showEvaluate, setShowEvaluate] = useState(false);
  const [newEval, setNewEval] = useState({ membreId: '', compétence: 'Animation', niveau: 'DÉBUTANT', commentaire: '' });

  useEffect(() => { loadEvaluations(); }, []);

  const loadEvaluations = async () => {
    try {
      setLoading(true);
      const res = await api.get('/skills-matrix');
      setEvaluations(res.data || []);
    } catch { setEvaluations([]); }
    finally { setLoading(false); }
  };

  const submitEvaluation = async () => {
    if (!newEval.membreId.trim()) { Toast.warning('Entrez l\'ID du membre'); return; }
    try {
      await api.post('/skills-matrix', newEval);
      Toast.success('Évaluation enregistrée');
      setShowEvaluate(false);
      loadEvaluations();
    } catch { Toast.error('Erreur'); }
  };

  // Group by member
  const byMember = evaluations.reduce<Record<string, SkillEval[]>>((acc, e) => {
    (acc[e.membreId] = acc[e.membreId] || []).push(e);
    return acc;
  }, {});

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <GraduationCap className="w-8 h-8 text-teal-500" />
            {t('skills.title')}
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Évaluez les compétences de chaque membre</p>
        </div>
        <button onClick={() => setShowEvaluate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-teal-500 to-cyan-500 text-white text-sm font-medium hover:from-teal-600 hover:to-cyan-600 transition-all shadow-lg flex items-center gap-2">
          <Star className="w-4 h-4" /> Évaluer
        </button>
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        evaluations.length === 0 ? (
          <EmptyState icon={<GraduationCap className="w-8 h-8 text-gray-400" />}
            title="Aucune évaluation"
            message="Commencez à évaluer les compétences de vos membres"
            action={{ label: 'Évaluer un membre', onClick: () => setShowEvaluate(true) }} />
        ) : (
          <div className="space-y-6">
            {Object.entries(byMember).map(([memberId, evals]) => (
              <div key={memberId} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <h3 className="font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
                  <BarChartComp className="w-4 h-4 text-teal-500" />
                  Membre: {memberId.slice(0, 8)}...
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
                  {evals.map(e => {
                    const niveau = NIVEAUX.find(n => n.key === e.niveau) || NIVEAUX[0];
                    return (
                      <div key={e.id} className="bg-gray-50 dark:bg-white/5 rounded-lg p-3 text-center">
                        <div className="text-xs text-gray-500 mb-1">{e.compétence}</div>
                        <div className="flex justify-center mb-1">
                          {Array.from({ length: 4 }).map((_, i) => (
                            <Star key={i} className={`w-3 h-3 ${i < niveau.stars ? 'text-yellow-400 fill-yellow-400' : 'text-gray-300'}`} />
                          ))}
                        </div>
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${niveau.color}`}>{niveau.label}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}

      {showEvaluate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowEvaluate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Évaluer une compétence</h2>
            <div className="space-y-4">
              <input type="text" value={newEval.membreId} onChange={e => setNewEval({ ...newEval, membreId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID du membre" />
              <select value={newEval.compétence} onChange={e => setNewEval({ ...newEval, compétence: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                {COMPÉTENCES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
              <select value={newEval.niveau} onChange={e => setNewEval({ ...newEval, niveau: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                {NIVEAUX.map(n => <option key={n.key} value={n.key}>{n.stars}⭐ {n.label}</option>)}
              </select>
              <textarea value={newEval.commentaire} onChange={e => setNewEval({ ...newEval, commentaire: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none"
                placeholder="Commentaire (optionnel)" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowEvaluate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={submitEvaluation} className="px-4 py-2 rounded-xl bg-teal-500 text-white text-sm font-medium hover:bg-teal-600">Évaluer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Simple helper component
function BarChartComp({ className }: { className?: string }) {
  return <BarChart3 className={className} />;
}
