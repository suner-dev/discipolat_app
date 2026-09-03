import { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import { Route, CheckCircle, ArrowRight, Sparkles, Loader2, RefreshCw } from 'lucide-react';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import EmptyState from '@/components/shared/EmptyState';

interface DiscipleshipPath {
  id: string;
  memberId: string;
  currentStage: 'DISCOVERY' | 'FOUNDATION' | 'GROWTH' | 'SERVICE' | 'LEADERSHIP' | 'MATURITY';
  recommendedNextStep: string;
  progressPercent: number;
  status: 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'SKIP';
  aiNotes: string;
  createdAt: string;
  lastActivityAt: string;
  completedAt: string;
}

const STAGE_ORDER = ['DISCOVERY', 'FOUNDATION', 'GROWTH', 'SERVICE', 'LEADERSHIP', 'MATURITY'];

const STAGE_CONFIG: Record<string, { label: string; icon: string; color: string; description: string }> = {
  DISCOVERY: { label: 'Découverte', icon: '🌱', color: 'from-green-500 to-emerald-500', description: 'Premiers pas dans la foi' },
  FOUNDATION: { label: 'Fondations', icon: '🏗️', color: 'from-blue-500 to-cyan-500', description: 'Bases de la foi chrétienne' },
  GROWTH: { label: 'Croissance', icon: '🌳', color: 'from-purple-500 to-violet-500', description: 'Approfondissement spirituel' },
  SERVICE: { label: 'Service', icon: '🤝', color: 'from-orange-500 to-amber-500', description: 'Mise en pratique du service' },
  LEADERSHIP: { label: 'Leadership', icon: '👑', color: 'from-yellow-500 to-orange-500', description: 'Former et guider les autres' },
  MATURITY: { label: 'Maturité', icon: '🌟', color: 'from-rose-500 to-pink-500', description: 'Plénitude spirituelle' },
};

export default function DiscipleshipPathPage() {
  const { t } = useI18n();
  const { user } = useAuth();
  const [path, setPath] = useState<DiscipleshipPath | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isAdvancing, setIsAdvancing] = useState(false);

  const loadPath = async () => {
    if (!user?.id) return;
    setIsLoading(true);
    setError(null);
    try {
      const res = await api.get(`/discipleship-paths/member/${user.id}`);
      setPath(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Erreur de chargement');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadPath();
  }, [user?.id]);

  const advanceStage = async () => {
    if (!path || isAdvancing) return;
    setIsAdvancing(true);
    try {
      const res = await api.post(`/discipleship-paths/${path.id}/advance`);
      setPath(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Erreur lors de l\'avancement');
    } finally {
      setIsAdvancing(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6 p-6">
        <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-6 text-center">
          <p className="text-red-400 mb-4">{error}</p>
          <button onClick={loadPath} className="btn-glow btn-sm inline-flex items-center gap-2">
            <RefreshCw className="w-4 h-4" /> Réessayer
          </button>
        </div>
      </div>
    );
  }

  if (!path) {
    return (
      <div className="space-y-6 p-6">
        <EmptyState
          icon={<Route className="w-8 h-8 text-blue-400" />}
          title="Aucun parcours de discipolat"
          message="Votre parcours n'a pas encore été créé. Contactez votre pasteur pour commencer."
        />
      </div>
    );
  }

  const currentStageIndex = STAGE_ORDER.indexOf(path.currentStage);
  const progress = path.progressPercent ?? Math.round((currentStageIndex / (STAGE_ORDER.length - 1)) * 100);

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Route className="text-blue-400" /> {t('discipleship.title') || 'Parcours de discipolat IA'}</h1>

      {/* Progress bar */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <div className="flex items-center justify-between mb-2">
          <span className="text-white font-medium">Progression globale</span>
          <span className="text-blue-400 font-bold">{progress}%</span>
        </div>
        <div className="w-full h-3 bg-gray-700 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-blue-500 to-purple-500 rounded-full" style={{ width: `${progress}%` }} />
        </div>
      </div>

      {/* Stages pipeline */}
      <div className="bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2"><Sparkles className="text-purple-400" /> Étapes du parcours</h2>
        <div className="space-y-3">
          {STAGE_ORDER.map((stageKey, i) => {
            const config = STAGE_CONFIG[stageKey];
            const isCompleted = i < currentStageIndex;
            const isCurrent = i === currentStageIndex;
            const isFuture = i > currentStageIndex;
            return (
              <div key={stageKey} className={`flex items-center gap-4 p-4 rounded-xl transition ${isCurrent ? 'bg-blue-600/20 border border-blue-500/30' : isCompleted ? 'bg-green-500/10 border border-green-500/20' : 'bg-white/5 border border-white/5'}`}>
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${config.color} flex items-center justify-center text-xl ${isFuture ? 'opacity-40' : ''}`}>{config.icon}</div>
                <div className="flex-1">
                  <h3 className={`font-semibold ${isFuture ? 'text-gray-500' : 'text-white'}`}>{config.label}</h3>
                  <p className="text-xs text-gray-400">{config.description}</p>
                </div>
                {isCompleted && <CheckCircle className="w-6 h-6 text-green-400" />}
                {isCurrent && <span className="px-3 py-1 bg-blue-600 rounded-full text-white text-xs font-medium">En cours</span>}
                {isFuture && <span className="text-gray-600 text-xs">Verrouillé</span>}
              </div>
            );
          })}
        </div>
      </div>

      {/* AI Recommendation */}
      {path.recommendedNextStep && (
        <div className="bg-gradient-to-br from-purple-500/10 to-blue-500/10 rounded-2xl p-6 border border-purple-500/20">
          <h3 className="text-white font-semibold mb-2 flex items-center gap-2"><Sparkles className="text-purple-400" /> Recommandation IA</h3>
          <p className="text-gray-300 text-sm mb-4">{path.recommendedNextStep}</p>
          {path.status === 'ACTIVE' && currentStageIndex < STAGE_ORDER.length - 1 && (
            <button
              onClick={advanceStage}
              disabled={isAdvancing}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-xl text-white text-sm font-medium transition flex items-center gap-2 disabled:opacity-50"
            >
              {isAdvancing ? <Loader2 className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
              {isAdvancing ? 'Progression...' : 'Passer à l\'étape suivante'}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
