import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Route, Plus, BarChart3, CheckCircle, Loader2, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface DiscipleshipPath {
  id: string;
  memberId: string;
  currentStage: string;
  recommendedNextStep: string;
  progressPercent: number;
  status: string;
  aiNotes: string;
  createdAt: string;
  lastActivityAt: string;
}

interface PathStats {
  total: number;
  byStage: Record<string, number>;
  byStatus: Record<string, number>;
  averageProgress: number;
}

const STAGES = ['DISCOVERY', 'FOUNDATION', 'GROWTH', 'SERVICE', 'LEADERSHIP', 'MATURITY'];

const STAGE_LABELS: Record<string, string> = {
  DISCOVERY: 'Découverte', FOUNDATION: 'Fondations', GROWTH: 'Croissance',
  SERVICE: 'Service', LEADERSHIP: 'Leadership', MATURITY: 'Maturité',
};

const STAGE_COLORS: Record<string, string> = {
  DISCOVERY: 'bg-green-100 text-green-700', FOUNDATION: 'bg-blue-100 text-blue-700',
  GROWTH: 'bg-purple-100 text-purple-700', SERVICE: 'bg-orange-100 text-orange-700',
  LEADERSHIP: 'bg-yellow-100 text-yellow-700', MATURITY: 'bg-rose-100 text-rose-700',
};

export default function DiscipleshipPathsPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [newPath, setNewPath] = useState({ memberId: '', currentStage: 'DISCOVERY' });

  const { data: paths = [], isLoading } = useQuery({
    queryKey: ['discipleship-paths'],
    queryFn: async () => (await api.get<DiscipleshipPath[]>('/discipleship-paths')).data,
  });

  const { data: stats } = useQuery({
    queryKey: ['discipleship-paths-stats'],
    queryFn: async () => (await api.get<PathStats>('/discipleship-paths/stats')).data,
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!newPath.memberId.trim()) { toast('Entrez l\'ID du membre', { icon: '⚠️' }); throw new Error('missing'); }
      return api.post('/discipleship-paths/member/' + newPath.memberId);
    },
    onSuccess: () => {
      toast.success('Parcours créé');
      setShowCreate(false);
      setNewPath({ memberId: '', currentStage: 'DISCOVERY' });
      queryClient.invalidateQueries({ queryKey: ['discipleship-paths'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'missing') toast.error(getErrorMessage(e)); },
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-lg">
          <Route className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Parcours de Discipolat</h1>
          <p className="page-subtitle">Gestion des parcours spirituels des membres</p>
        </div>
        <div className="ml-auto">
          <button onClick={() => setShowCreate(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-blue-500 to-indigo-500 text-white text-sm font-medium hover:from-blue-600 hover:to-indigo-600 transition-all shadow-lg flex items-center gap-2">
            <Plus className="w-4 h-4" /> Nouveau parcours
          </button>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.total}</div>
            <div className="text-xs text-gray-500">Total parcours</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.averageProgress?.toFixed(0)}%</div>
            <div className="text-xs text-gray-500">Progression moy.</div>
          </div>
          {Object.entries(stats.byStatus || {}).slice(0, 2).map(([key, val]) => (
            <div key={key} className="glass-card p-4 text-center">
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{val}</div>
              <div className="text-xs text-gray-500">{key}</div>
            </div>
          ))}
        </div>
      )}

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        paths.length === 0 ? (
          <EmptyState icon={<Route className="w-8 h-8 text-gray-400" />}
            title="Aucun parcours"
            message="Créez des parcours de discipolat pour vos membres"
            action={{ label: 'Créer un parcours', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {paths.map(p => (
              <div key={p.id} className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STAGE_COLORS[p.currentStage] || 'bg-gray-100 text-gray-700'}`}>
                    {STAGE_LABELS[p.currentStage] || p.currentStage}
                  </span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${p.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : p.status === 'COMPLETED' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-500'}`}>
                    {p.status}
                  </span>
                </div>
                <div className="text-xs text-gray-500 mb-2">Membre: {p.memberId.slice(0, 8)}...</div>
                <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden mb-2">
                  <div className="h-full bg-gradient-to-r from-blue-500 to-indigo-500 rounded-full"
                    style={{ width: `${p.progressPercent ?? 0}%` }} />
                </div>
                <div className="flex justify-between text-xs text-gray-400">
                  <span>{p.progressPercent ?? 0}% complété</span>
                  <span>{new Date(p.createdAt).toLocaleDateString('fr-FR')}</span>
                </div>
                {p.recommendedNextStep && (
                  <div className="mt-3 p-2 bg-indigo-50 dark:bg-indigo-500/10 rounded-lg text-xs text-indigo-700 dark:text-indigo-400 flex items-start gap-1">
                    <ArrowRight className="w-3 h-3 mt-0.5 flex-shrink-0" />
                    {p.recommendedNextStep}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouveau parcours</h2>
            <div className="space-y-4">
              <input type="text" value={newPath.memberId} onChange={e => setNewPath({ ...newPath, memberId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID du membre" />
              <select value={newPath.currentStage} onChange={e => setNewPath({ ...newPath, currentStage: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                {STAGES.map(s => <option key={s} value={s}>{STAGE_LABELS[s]}</option>)}
              </select>
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
                className="px-4 py-2 rounded-xl bg-blue-500 text-white text-sm font-medium hover:bg-blue-600 flex items-center gap-2">
                {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
