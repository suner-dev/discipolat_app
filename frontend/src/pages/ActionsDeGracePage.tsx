import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Family } from '@/types';
import { Heart, Sparkles, Loader2, Star, Filter } from 'lucide-react';

interface AnsweredPrayer {
  id: string;
  titre: string;
  description?: string;
  categorie: string;
  temoignage?: string;
  dateExaucee?: string;
  auteurId: string;
  familleId?: string;
  createdAt: string;
}

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const CATEGORIE_FALLBACK: Record<string, string> = {
  SANTE: 'Santé',
  FAMILLE: 'Famille',
  TRAVAIL: 'Travail',
  SPIRITUEL: 'Spirituel',
  AUTRE: 'Autre',
};

const CATEGORIE_COLORS: Record<string, string> = {
  SANTE: 'text-rose-500 bg-rose-100 dark:bg-rose-900/20',
  FAMILLE: 'text-emerald-500 bg-emerald-100 dark:bg-emerald-900/20',
  TRAVAIL: 'text-blue-500 bg-blue-100 dark:bg-blue-900/20',
  SPIRITUEL: 'text-purple-500 bg-purple-100 dark:bg-purple-900/20',
  AUTRE: 'text-gray-500 bg-gray-100 dark:bg-gray-800',
};

export default function ActionsDeGracePage() {
  const dictionaries = useDictionaries();
  const [familleFilter, setFamilleFilter] = useState('');

  const { data: families } = useQuery({
    queryKey: ['families', 'all'],
    queryFn: async () => {
      const res = await api.get('/families?size=100');
      return res.data.content as Family[];
    },
  });

  const { data: answeredPrayers, isLoading } = useQuery({
    queryKey: ['prayers', 'actions-de-grace', familleFilter],
    queryFn: async () => {
      const params = familleFilter ? `?familleId=${familleFilter}` : '';
      const res = await api.get(`/prayers/actions-de-grace${params}`);
      return res.data as AnsweredPrayer[];
    },
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-yellow-500 text-white shadow-lg">
            <Heart className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Actions de grâce</h1>
            <p className="page-subtitle">Prières exaucées et témoignages</p>
          </div>
        </div>
      </div>

      <div className="glass-card p-4 mb-6 flex items-center gap-3">
        <Filter className="w-4 h-4 text-gray-400" />
        <select className="input flex-1" value={familleFilter} onChange={(e) => setFamilleFilter(e.target.value)}>
          <option value="">Toutes les familles</option>
          {(families || []).map((f) => (
            <option key={f.id} value={f.id}>{f.nom}</option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : !answeredPrayers || answeredPrayers.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Heart className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p className="text-gray-500">Aucune action de grâce pour le moment</p>
          <p className="text-xs text-gray-400 mt-1">Les prières exaucées apparaîtront ici</p>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {answeredPrayers.map((prayer) => (
            <div key={prayer.id} className="glass-card p-5 hover:shadow-lg transition-all hover:-translate-y-0.5">
              <div className="flex items-start gap-3 mb-3">
                <div className={`p-2 rounded-lg ${CATEGORIE_COLORS[prayer.categorie] || CATEGORIE_COLORS.AUTRE}`}>
                  <Star className="w-4 h-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{prayer.titre}</h3>
                  <span className="text-xs text-gray-400">
                    {dictionaries.label('GRATITUDE_CATEGORIE', prayer.categorie) || CATEGORIE_FALLBACK[prayer.categorie] || prayer.categorie}
                  </span>
                </div>
                {prayer.dateExaucee && (
                  <span className="text-xs text-gray-400 whitespace-nowrap">
                    {new Date(prayer.dateExaucee).toLocaleDateString('fr-FR')}
                  </span>
                )}
              </div>
              {prayer.description && (
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-3 line-clamp-3">{prayer.description}</p>
              )}
              {prayer.temoignage && (
                <div className="p-3 rounded-xl bg-amber-50 dark:bg-amber-900/10 border border-amber-100 dark:border-amber-800/30">
                  <div className="flex items-center gap-1 mb-1">
                    <Sparkles className="w-3 h-3 text-amber-500" />
                    <span className="text-[10px] font-semibold text-amber-600 dark:text-amber-400 uppercase tracking-wider">Témoignage</span>
                  </div>
                  <p className="text-sm text-amber-800 dark:text-amber-200 italic">"{prayer.temoignage}"</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
