import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import Toast from '@/components/shared/Toast';
import { Brain, Sparkles, AlertTriangle, CheckCircle2, BookOpen, UserCheck, Eye, Archive } from 'lucide-react';

interface Suggestion {
  id: string;
  faiseurId: string;
  priorité: 'HAUTE' | 'MOYENNE' | 'BASSE';
  catégorie: string;
  titre: string;
  analyse: string;
  actionRecommandée: string;
  raisonnement: string;
  confiance: number;
  statut: string;
  createdAt: string;
}

const CATÉGORIES_ICONS: Record<string, React.ReactNode> = {
  ACCOMPAGNEMENT: <UserCheck className="w-4 h-4 text-blue-500" />,
  FORMATION: <BookOpen className="w-4 h-4 text-purple-500" />,
  DÉLÉGATION: <AlertTriangle className="w-4 h-4 text-orange-500" />,
  RECONNAISSANCE: <CheckCircle2 className="w-4 h-4 text-green-500" />,
  MISE_EN_GARDIEN: <AlertTriangle className="w-4 h-4 text-red-500" />,
};

export default function MentoratIAPage() {
  const { t } = useI18n();
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [stats, setStats] = useState({ actives: 0, lues: 0 });
  const [selectedSug, setSelectedSug] = useState<Suggestion | null>(null);

  useEffect(() => { loadSuggestions(); loadStats(); }, []);

  const loadSuggestions = async () => {
    try { setLoading(true); const res = await api.get('/mentoring/all'); setSuggestions(res.data || []); }
    catch { setSuggestions([]); } finally { setLoading(false); }
  };

  const loadStats = async () => { try { const res = await api.get('/mentoring/stats'); setStats(res.data); } catch {} };

    const generateSuggestions = async () => {
    try {
      setGenerating(true);
      // Load faiseurs dynamically (no mock payload) via workload endpoint
      const wlRes = await api.get('/users/faiseur-workload');
      const faiseurs = (wlRes.data as Array<Record<string, unknown>>).map((f) => ({
        id: String(f.id ?? f.idUtilisateur ?? ''),
        nom: String(f.nom ?? f.displayName ?? ''),
        disciples: Number(f.disciples ?? f.nombreDisciplesSuivis ?? 0),
        rapportsSoumis: Number(f.rapportsSoumis ?? 0),
        scoreMoyen: Number(f.scoreMoyen ?? f.score ?? 0),
        formationsSuivies: Number(f.formationsSuivies ?? 0),
        joursDepuisDernierContact: Number(f.joursDepuisDernierContact ?? 0),
      }));
      await api.post('/mentoring/generate', { faiseurs });
      Toast.success('Suggestions générées !');
      loadSuggestions(); loadStats();
    } catch { Toast.error('Erreur lors de la génération'); }
    finally { setGenerating(false); }
  };

  const markAsRead = async (id: string) => {
    try { await api.patch(`/mentoring/${id}/read`); loadSuggestions(); loadStats(); } catch {}
  };

  const getPrioritéColor = (p: string) => {
    switch (p) {
      case 'HAUTE': return 'bg-red-100 text-red-700 border-red-200';
      case 'MOYENNE': return 'bg-amber-100 text-amber-700 border-amber-200';
      default: return 'bg-gray-100 text-gray-600 border-gray-200';
    }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Brain className="w-8 h-8 text-violet-500" />
            Mentorat IA — Chefs de Famille
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Suggestions intelligentes d'accompagnement pour vos faiseurs</p>
        </div>
        <button onClick={generateSuggestions} disabled={generating}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-violet-500 to-purple-500 text-white text-sm font-medium hover:from-violet-600 hover:to-purple-600 transition-all shadow-lg flex items-center gap-2 disabled:opacity-50">
          <Sparkles className={`w-4 h-4 ${generating ? 'animate-spin' : ''}`} />
          {generating ? 'Génération...' : 'Générer des suggestions'}
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        <div className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
          <div className="text-2xl font-bold text-violet-600">{stats.actives}</div>
          <div className="text-xs text-gray-500">Suggestions actives</div>
        </div>
        <div className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
          <div className="text-2xl font-bold text-green-600">{stats.lues}</div>
          <div className="text-xs text-gray-500">Consultées</div>
        </div>
      </div>

      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        suggestions.length === 0 ? (
          <EmptyState icon={<Brain className="w-8 h-8 text-gray-400" />}
            title="Aucune suggestion"
            message="Générez des suggestions IA pour optimiser l'accompagnement de vos faiseurs"
            action={{ label: 'Générer', onClick: generateSuggestions }} />
        ) : (
          <div className="space-y-4">
            {suggestions.map(sug => (
              <div key={sug.id} onClick={() => { setSelectedSug(sug); markAsRead(sug.id); }}
                className={`bg-white dark:bg-white/5 rounded-xl p-5 border cursor-pointer transition-all hover:shadow-md ${
                  selectedSug?.id === sug.id ? 'border-violet-500 ring-2 ring-violet-500/20' : 'border-gray-200 dark:border-white/10'
                }`}>
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-center gap-2">
                    {CATÉGORIES_ICONS[sug.catégorie] || <Brain className="w-4 h-4 text-gray-400" />}
                    <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{sug.titre}</h3>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${getPrioritéColor(sug.priorité)}`}>
                    {sug.priorité}
                  </span>
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">{sug.analyse}</p>
                {selectedSug?.id === sug.id && (
                  <div className="mt-3 space-y-2 border-t border-gray-200 dark:border-white/10 pt-3">
                    <div className="bg-green-50 dark:bg-green-500/10 rounded-lg p-3">
                      <div className="text-xs font-medium text-green-700 dark:text-green-400 mb-1">🎯 Action recommandée</div>
                      <p className="text-sm text-green-800 dark:text-green-300">{sug.actionRecommandée}</p>
                    </div>
                    <div className="bg-blue-50 dark:bg-blue-500/10 rounded-lg p-3">
                      <div className="text-xs font-medium text-blue-700 dark:text-blue-400 mb-1">💡 Raisonnement</div>
                      <p className="text-sm text-blue-800 dark:text-blue-300">{sug.raisonnement}</p>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-gray-400">
                      <span>Confiance: {Math.round(sug.confiance * 100)}%</span>
                      <span>•</span>
                      <span>Faiseur: {sug.faiseurId.slice(0, 8)}...</span>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
    </div>
  );
}
