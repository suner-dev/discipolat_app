import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import { Sparkles, CheckCircle2, Circle, Lock, Star, BookOpen, Heart, Users, Award } from 'lucide-react';

interface JourneyStage {
  id: string;
  titre: string;
  description: string;
  categorie: 'INITIATION' | 'FORMATION' | 'ENGAGEMENT' | 'SERVICE' | 'LEADERSHIP';
  completed: boolean;
  completedAt?: string;
  icon: string;
  order: number;
}

const STAGE_ICONS: Record<string, any> = {
  INITIATION: Sparkles,
  FORMATION: BookOpen,
  ENGAGEMENT: Heart,
  SERVICE: Users,
  LEADERSHIP: Award,
};

export default function SpiritualJourneyPage() {
  const { t } = useI18n();
  const [stages, setStages] = useState<JourneyStage[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadJourney(); }, []);

  const loadJourney = async () => {
    try {
      setLoading(true);
      const res = await api.get('/spiritual-journey');
      setStages(res.data.content || res.data || []);
    } catch { setStages([]); } finally { setLoading(false); }
  };

  const completedCount = stages.filter(s => s.completed).length;
  const progress = stages.length > 0 ? Math.round((completedCount / stages.length) * 100) : 0;

  const categories = ['INITIATION', 'FORMATION', 'ENGAGEMENT', 'SERVICE', 'LEADERSHIP'];

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
          <Sparkles className="w-8 h-8 text-violet-500" />
          Parcours Spirituel
        </h1>
        <p className="text-gray-500 dark:text-gray-400 mt-1">Votre progression spirituelle可视化</p>
      </div>

      {stages.length > 0 && (
        <div className="p-6 rounded-2xl border border-gray-200 dark:border-white/10 bg-gradient-to-r from-violet-500/10 to-purple-500/10 mb-8">
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Progression globale</span>
            <span className="text-sm font-bold text-violet-600">{completedCount}/{stages.length} étapes</span>
          </div>
          <div className="h-3 bg-gray-200 dark:bg-white/10 rounded-full overflow-hidden">
            <div className="h-full bg-gradient-to-r from-violet-500 to-purple-500 rounded-full transition-all duration-500" style={{ width: `${progress}%` }} />
          </div>
          <span className="text-xs text-gray-500 mt-2 block">{progress}% complété</span>
        </div>
      )}

      {loading ? <SkeletonLoader lines={6} variant="card" /> : stages.length === 0 ? (
        <div className="text-center py-12">
          <Sparkles className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500">Votre parcours spirituel sera bientôt disponible</p>
        </div>
      ) : (
        <div className="space-y-8">
          {categories.map(cat => {
            const catStages = stages.filter(s => s.categorie === cat);
            if (catStages.length === 0) return null;
            const Icon = STAGE_ICONS[cat] || Sparkles;
            const catCompleted = catStages.filter(s => s.completed).length;
            return (
              <div key={cat}>
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-violet-100 dark:bg-violet-500/10 flex items-center justify-center">
                    <Icon className="w-5 h-5 text-violet-600" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-gray-900 dark:text-white">{cat}</h2>
                    <p className="text-xs text-gray-500">{catCompleted}/{catStages.length} complétées</p>
                  </div>
                </div>
                <div className="space-y-3 ml-5 border-l-2 border-violet-200 dark:border-violet-500/20 pl-6">
                  {catStages.map(stage => (
                    <div key={stage.id} className={`p-4 rounded-xl border transition-all ${stage.completed ? 'border-green-200 dark:border-green-500/20 bg-green-50/50 dark:bg-green-500/5' : 'border-gray-200 dark:border-white/10 bg-white dark:bg-white/5'}`}>
                      <div className="flex items-center gap-3">
                        {stage.completed ? (
                          <CheckCircle2 className="w-5 h-5 text-green-500 shrink-0" />
                        ) : (
                          <Circle className="w-5 h-5 text-gray-300 shrink-0" />
                        )}
                        <div>
                          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">{stage.titre}</h3>
                          <p className="text-xs text-gray-500 dark:text-gray-400">{stage.description}</p>
                          {stage.completedAt && (
                            <span className="text-xs text-green-600 dark:text-green-400 mt-1 block">
                              Complété le {new Date(stage.completedAt).toLocaleDateString('fr-FR')}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
