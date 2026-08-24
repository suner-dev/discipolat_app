import { useI18n } from '@/i18n';
import { Route, CheckCircle, ArrowRight, Sparkles, Users } from 'lucide-react';

const STAGES = [
  { key: 'DISCOVERY', label: 'Découverte', icon: '🌱', color: 'from-green-500 to-emerald-500', description: 'Premiers pas dans la foi' },
  { key: 'FOUNDATION', label: 'Fondations', icon: '🏗️', color: 'from-blue-500 to-cyan-500', description: 'Bases de la foi chrétienne' },
  { key: 'GROWTH', label: 'Croissance', icon: '🌳', color: 'from-purple-500 to-violet-500', description: 'Approfondissement spirituel' },
  { key: 'SERVICE', label: 'Service', icon: '🤝', color: 'from-orange-500 to-amber-500', description: 'Mise en pratique du service' },
  { key: 'LEADERSHIP', label: 'Leadership', icon: '👑', color: 'from-yellow-500 to-orange-500', description: 'Former et guider les autres' },
  { key: 'MATURITY', label: 'Maturité', icon: '🌟', color: 'from-rose-500 to-pink-500', description: 'Plénitude spirituelle' },
];

export default function DiscipleshipPathPage() {
  const { t } = useI18n();
  const currentStage = 2; // GROWTH (0-indexed)
  const progress = 35;

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
          {STAGES.map((stage, i) => {
            const isCompleted = i < currentStage;
            const isCurrent = i === currentStage;
            const isFuture = i > currentStage;
            return (
              <div key={stage.key} className={`flex items-center gap-4 p-4 rounded-xl transition ${isCurrent ? 'bg-blue-600/20 border border-blue-500/30' : isCompleted ? 'bg-green-500/10 border border-green-500/20' : 'bg-white/5 border border-white/5'}`}>
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${stage.color} flex items-center justify-center text-xl ${isFuture ? 'opacity-40' : ''}`}>{stage.icon}</div>
                <div className="flex-1">
                  <h3 className={`font-semibold ${isFuture ? 'text-gray-500' : 'text-white'}`}>{stage.label}</h3>
                  <p className="text-xs text-gray-400">{stage.description}</p>
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
      <div className="bg-gradient-to-br from-purple-500/10 to-blue-500/10 rounded-2xl p-6 border border-purple-500/20">
        <h3 className="text-white font-semibold mb-2 flex items-center gap-2"><Sparkles className="text-purple-400" /> Recommandation IA</h3>
        <p className="text-gray-300 text-sm">Rejoignez un petit groupe et commencez un plan de lecture biblique pour progresser vers l'étape de Croissance.</p>
        <button className="mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-xl text-white text-sm font-medium transition flex items-center gap-2">
          Voir les petits groupes <ArrowRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
