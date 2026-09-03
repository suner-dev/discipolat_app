import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Award, Star, Loader2, Filter, BarChart3 } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface SkillEvaluation {
  id: string;
  membreId: string;
  compétence: string;
  niveau: string;
  commentaire?: string;
  evaluatedBy?: string;
  createdAt: string;
}

interface MemberMatrix {
  membreId: string;
  competences: Record<string, string>;
  averageLevel: number;
}

interface DepartmentMatrix {
  departmentId: string;
  members: MemberMatrix[];
  topCompetences: string[];
}

const NIVEAUX = ['DÉBUTANT', 'INTERMÉDIAIRE', 'AVANCÉ', 'EXPERT'];
const NIVEAU_STARS: Record<string, number> = { DÉBUTANT: 1, INTERMÉDIAIRE: 2, AVANCÉ: 3, EXPERT: 4 };

export default function SkillsMatrixPage() {
  const queryClient = useQueryClient();
  const [view, setView] = useState<'list' | 'member' | 'department'>('list');
  const [memberId, setMemberId] = useState('');
  const [deptId, setDeptId] = useState('');
  const [showEvaluate, setShowEvaluate] = useState(false);
  const [newEval, setNewEval] = useState({ membreId: '', compétence: 'Animation', niveau: 'DÉBUTANT', commentaire: '' });

  const COMPÉTENCES = ['Animation', 'Musique', 'Accueil', 'Prédication', 'Pédagogie', 'Organisation', 'Communication', 'Écoute', 'Leadership', 'Technique'];

  const { data: evaluations = [], isLoading } = useQuery({
    queryKey: ['skills-matrix'],
    queryFn: async () => (await api.get<SkillEvaluation[]>('/skills-matrix')).data,
  });

  const { data: memberMatrix } = useQuery({
    queryKey: ['skills-matrix-member', memberId],
    queryFn: async () => (await api.get<MemberMatrix>(`/skills-matrix/member/${memberId}/matrix`)).data,
    enabled: !!memberId && view === 'member',
  });

  const { data: deptMatrix } = useQuery({
    queryKey: ['skills-matrix-dept', deptId],
    queryFn: async () => (await api.get<DepartmentMatrix>(`/skills-matrix/department/${deptId}/matrix`)).data,
    enabled: !!deptId && view === 'department',
  });

  const submitEvaluation = useMutation({
    mutationFn: async () => {
      if (!newEval.membreId.trim()) { toast('Entrez l\'ID du membre', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/skills-matrix', newEval);
    },
    onSuccess: () => {
      toast.success('Évaluation enregistrée');
      setShowEvaluate(false);
      setNewEval({ membreId: '', compétence: 'Animation', niveau: 'DÉBUTANT', commentaire: '' });
      queryClient.invalidateQueries({ queryKey: ['skills-matrix'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const byMember = evaluations.reduce<Record<string, SkillEvaluation[]>>((acc, e) => {
    (acc[e.membreId] = acc[e.membreId] || []).push(e);
    return acc;
  }, {});

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-teal-500 to-cyan-600 text-white shadow-lg">
          <Award className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Matrice de Compétences</h1>
          <p className="page-subtitle">Évaluez et visualisez les compétences de vos membres</p>
        </div>
        <div className="ml-auto flex items-center gap-3">
          <div className="flex rounded-xl bg-white/5 border border-white/10 p-1">
            {(['list', 'member', 'department'] as const).map(v => (
              <button key={v} onClick={() => setView(v)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${view === v ? 'bg-teal-500 text-white' : 'text-gray-400 hover:text-white'}`}>
                {v === 'list' ? 'Liste' : v === 'member' ? 'Membre' : 'Département'}
              </button>
            ))}
          </div>
          <button onClick={() => setShowEvaluate(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-teal-500 to-cyan-500 text-white text-sm font-medium hover:from-teal-600 hover:to-cyan-600 transition-all shadow-lg flex items-center gap-2">
            <Star className="w-4 h-4" /> Évaluer
          </button>
        </div>
      </div>

      {view === 'member' && (
        <div className="flex gap-3 mb-6">
          <input type="text" value={memberId} onChange={e => setMemberId(e.target.value)}
            className="px-3 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            placeholder="ID du membre" />
        </div>
      )}

      {view === 'department' && (
        <div className="flex gap-3 mb-6">
          <input type="text" value={deptId} onChange={e => setDeptId(e.target.value)}
            className="px-3 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
            placeholder="ID du département" />
        </div>
      )}

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        view === 'member' && memberMatrix ? (
          <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-4">Matrice du membre — Score moyen: {memberMatrix.averageLevel?.toFixed(1)}</h3>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
              {Object.entries(memberMatrix.competences || {}).map(([comp, niveau]) => (
                <div key={comp} className="bg-gray-50 dark:bg-white/5 rounded-lg p-3 text-center">
                  <div className="text-xs text-gray-500 mb-1">{comp}</div>
                  <div className="flex justify-center mb-1">
                    {Array.from({ length: 4 }).map((_, i) => (
                      <Star key={i} className={`w-3 h-3 ${i < (NIVEAU_STARS[niveau] || 0) ? 'text-yellow-400 fill-yellow-400' : 'text-gray-300'}`} />
                    ))}
                  </div>
                  <span className="text-xs text-gray-600 dark:text-gray-400">{niveau}</span>
                </div>
              ))}
            </div>
          </div>
        ) : view === 'department' && deptMatrix ? (
          <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-4">Matrice du département</h3>
            <div className="text-xs text-gray-500 mb-3">Compétences dominantes: {deptMatrix.topCompetences?.join(', ')}</div>
            <div className="space-y-3">
              {deptMatrix.members?.map(m => (
                <div key={m.membreId} className="p-3 bg-gray-50 dark:bg-white/5 rounded-lg">
                  <div className="text-sm font-medium text-gray-900 dark:text-white mb-1">Membre: {m.membreId.slice(0, 8)}... — Score: {m.averageLevel?.toFixed(1)}</div>
                  <div className="flex flex-wrap gap-1.5">
                    {Object.entries(m.competences || {}).map(([comp, niveau]) => (
                      <span key={comp} className="px-2 py-0.5 rounded-full bg-teal-100 dark:bg-teal-500/20 text-teal-700 dark:text-teal-400 text-xs">
                        {comp}: {niveau}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : evaluations.length === 0 ? (
          <EmptyState icon={<Award className="w-8 h-8 text-gray-400" />}
            title="Aucune évaluation"
            message="Commencez à évaluer les compétences de vos membres"
            action={{ label: 'Évaluer un membre', onClick: () => setShowEvaluate(true) }} />
        ) : (
          <div className="space-y-6">
            {Object.entries(byMember).map(([mId, evals]) => (
              <div key={mId} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Membre: {mId.slice(0, 8)}...</h3>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
                  {evals.map(e => (
                    <div key={e.id} className="bg-gray-50 dark:bg-white/5 rounded-lg p-3 text-center">
                      <div className="text-xs text-gray-500 mb-1">{e.compétence}</div>
                      <div className="flex justify-center mb-1">
                        {Array.from({ length: 4 }).map((_, i) => (
                          <Star key={i} className={`w-3 h-3 ${i < (NIVEAU_STARS[e.niveau] || 0) ? 'text-yellow-400 fill-yellow-400' : 'text-gray-300'}`} />
                        ))}
                      </div>
                      <span className="text-xs text-gray-600 dark:text-gray-400">{e.niveau}</span>
                    </div>
                  ))}
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
                {NIVEAUX.map(n => <option key={n} value={n}>{n}</option>)}
              </select>
              <textarea value={newEval.commentaire} onChange={e => setNewEval({ ...newEval, commentaire: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none"
                placeholder="Commentaire (optionnel)" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowEvaluate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => submitEvaluation.mutate()} disabled={submitEvaluation.isPending}
                className="px-4 py-2 rounded-xl bg-teal-500 text-white text-sm font-medium hover:bg-teal-600 flex items-center gap-2">
                {submitEvaluation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Évaluer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
