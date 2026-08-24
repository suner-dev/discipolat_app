import React, { useState, useEffect } from 'react';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { GitBranch, Plus, UserCheck, Clock, CheckCircle2 } from 'lucide-react';

interface Plan {
  id: string;
  candidatId: string;
  rôleCible: string;
  mentorId?: string;
  readiness: string;
  statut: string;
  planFormation?: string;
  createdAt: string;
}

export default function SuccessionPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newPlan, setNewPlan] = useState({ candidatId: '', rôleCible: '', planFormation: '' });

  useEffect(() => { loadPlans(); }, []);

  const loadPlans = async () => {
    try { setLoading(true); const res = await api.get('/succession'); setPlans(res.data || []); }
    catch { setPlans([]); } finally { setLoading(false); }
  };

  const createPlan = async () => {
    if (!newPlan.candidatId || !newPlan.rôleCible) { Toast.warning('Remplissez les champs'); return; }
    try { await api.post('/succession', newPlan); Toast.success('Plan créé'); setShowCreate(false); loadPlans(); }
    catch { Toast.error('Erreur'); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <GitBranch className="w-8 h-8 text-violet-500" /> Plan de Succession
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Préparez les futurs leaders de votre église</p>
        </div>
        <button onClick={() => setShowCreate(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-violet-500 to-purple-500 text-white text-sm font-medium hover:from-violet-600 hover:to-purple-600 transition-all shadow-lg flex items-center gap-2">
          <Plus className="w-4 h-4" /> Nouveau plan
        </button>
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        plans.length === 0 ? (
          <EmptyState icon={<GitBranch className="w-8 h-8 text-gray-400" />}
            title="Aucun plan de succession"
            message="Identifiez les futurs leaders et créez des plans de transition"
            action={{ label: 'Créer un plan', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {plans.map(plan => (
              <div key={plan.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-semibold text-gray-900 dark:text-white text-sm">Rôle: {plan.rôleCible}</h3>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${plan.statut === 'COMPLÉTÉ' ? 'bg-green-100 text-green-700' : plan.statut === 'PRÊT' ? 'bg-blue-100 text-blue-700' : 'bg-amber-100 text-amber-700'}`}>
                    {plan.statut}
                  </span>
                </div>
                <div className="text-xs text-gray-500 space-y-1">
                  <div className="flex items-center gap-1"><UserCheck className="w-3 h-3" /> Candidat: {plan.candidatId.slice(0, 8)}...</div>
                  {plan.mentorId && <div className="flex items-center gap-1"><Clock className="w-3 h-3" /> Mentor: {plan.mentorId.slice(0, 8)}...</div>}
                  <div>Readiness: {plan.readiness}</div>
                </div>
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau plan de succession</h2>
            <div className="space-y-4">
              <input type="text" value={newPlan.candidatId} onChange={e => setNewPlan({ ...newPlan, candidatId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="ID du candidat" />
              <input type="text" value={newPlan.rôleCible} onChange={e => setNewPlan({ ...newPlan, rôleCible: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" placeholder="Rôle cible (ex: RESPONSABLE)" />
              <textarea value={newPlan.planFormation} onChange={e => setNewPlan({ ...newPlan, planFormation: e.target.value })}
                rows={3} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none" placeholder="Plan de formation" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={createPlan} className="px-4 py-2 rounded-xl bg-violet-500 text-white text-sm font-medium hover:bg-violet-600">Créer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
