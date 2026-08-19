import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Family } from '@/types';
import { Heart, Sparkles, Loader2, Star, Filter, Search, X, TrendingUp, BookOpen, BarChart3 } from 'lucide-react';

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

const CATEGORIE_FALLBACK: Record<string, string> = {
  SANTE: 'Santé', FAMILLE: 'Famille', TRAVAIL: 'Travail', SPIRITUEL: 'Spirituel', AUTRE: 'Autre',
};

const CATEGORIE_COLORS: Record<string, { text: string; bg: string; gradient: string }> = {
  SANTE: { text: 'text-rose-500', bg: 'bg-rose-100 dark:bg-rose-900/20', gradient: 'from-rose-500 to-pink-500' },
  FAMILLE: { text: 'text-emerald-500', bg: 'bg-emerald-100 dark:bg-emerald-900/20', gradient: 'from-emerald-500 to-green-500' },
  TRAVAIL: { text: 'text-blue-500', bg: 'bg-blue-100 dark:bg-blue-900/20', gradient: 'from-blue-500 to-indigo-500' },
  SPIRITUEL: { text: 'text-purple-500', bg: 'bg-purple-100 dark:bg-purple-900/20', gradient: 'from-purple-500 to-violet-500' },
  AUTRE: { text: 'text-gray-500', bg: 'bg-gray-100 dark:bg-gray-800', gradient: 'from-gray-500 to-slate-500' },
};

export default function ActionsDeGracePage() {
  const dictionaries = useDictionaries();
  const [familleFilter, setFamilleFilter] = useState('');
  const [categorieFilter, setCategorieFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const { data: families } = useQuery({
    queryKey: ['families', 'all'],
    queryFn: async () => { const res = await api.get('/families?size=100'); return res.data.content as Family[]; },
  });

  const { data: answeredPrayers, isLoading } = useQuery({
    queryKey: ['prayers', 'actions-de-grace', familleFilter],
    queryFn: async () => {
      const params = familleFilter ? `?familleId=${familleFilter}` : '';
      const res = await api.get(`/prayers/actions-de-grace${params}`);
      return res.data as AnsweredPrayer[];
    },
  });

  const filtered = useMemo(() => {
    if (!answeredPrayers) return [];
    let items = answeredPrayers;
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      items = items.filter(p => p.titre?.toLowerCase().includes(q) || p.temoignage?.toLowerCase().includes(q) || p.description?.toLowerCase().includes(q));
    }
    if (categorieFilter) items = items.filter(p => p.categorie === categorieFilter);
    return items;
  }, [answeredPrayers, searchTerm, categorieFilter]);

  const stats = useMemo(() => {
    if (!answeredPrayers) return { total: 0, byCategorie: {} as Record<string, number>, withTemoignage: 0 };
    const byCategorie: Record<string, number> = {};
    answeredPrayers.forEach(p => { byCategorie[p.categorie] = (byCategorie[p.categorie] || 0) + 1; });
    return { total: answeredPrayers.length, byCategorie, withTemoignage: answeredPrayers.filter(p => p.temoignage).length };
  }, [answeredPrayers]);

  const catEntries = useMemo(() => Object.entries(stats.byCategorie).sort(([, a], [, b]) => b - a), [stats.byCategorie]);
  const hasActiveFilters = Boolean(familleFilter || categorieFilter || searchTerm);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-amber-500 to-yellow-500 text-white shadow-lg">
            <Heart className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Actions de grâce</h1>
            <p className="page-subtitle">Prières exaucées et témoignages — {stats.total} action(s) de grâce</p>
          </div>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total exaucées', value: stats.total, icon: Heart, color: 'from-amber-500 to-yellow-500' },
          { label: 'Avec témoignage', value: stats.withTemoignage, icon: Sparkles, color: 'from-purple-500 to-violet-500' },
          { label: 'Catégories', value: Object.keys(stats.byCategorie).length, icon: BookOpen, color: 'from-blue-500 to-indigo-500' },
          { label: 'Familles', value: families?.length || 0, icon: TrendingUp, color: 'from-emerald-500 to-green-500' },
        ].map((s, i) => (
          <div key={s.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 50}ms` }}>
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{s.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.color} text-white shadow-sm`}>
                <s.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{s.value}</p>
          </div>
        ))}
      </div>

      {/* Category distribution */}
      {catEntries.length > 0 && (
        <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '100ms' }}>
          <div className="flex items-center gap-2 mb-3">
            <BarChart3 className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Répartition par catégorie</h3>
          </div>
          <div className="flex flex-wrap gap-2">
            {catEntries.map(([cat, count]) => {
              const colors = CATEGORIE_COLORS[cat] || CATEGORIE_COLORS.AUTRE;
              return (
                <button key={cat}
                  onClick={() => setCategorieFilter(categorieFilter === cat ? '' : cat)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium border transition-all ${
                    categorieFilter === cat
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-1 ring-primary-500/30'
                      : `${colors.bg} border-transparent ${colors.text}`
                  }`}>
                  <Star className="w-3 h-3" />
                  {dictionaries.label('GRATITUDE_CATEGORIE', cat) || CATEGORIE_FALLBACK[cat] || cat} ({count})
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Search & filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '160ms' }}>
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher un titre, témoignage..."
              value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)}
              className="input pl-9" />
          </div>
          <div className="flex items-center gap-1.5">
            <Filter className="w-4 h-4 text-gray-400" />
            <select value={familleFilter} onChange={(e) => setFamilleFilter(e.target.value)} className="input !w-auto text-xs">
              <option value="">Toutes les familles</option>
              {(families || []).map((f) => (<option key={f.id} value={f.id}>{f.nom}</option>))}
            </select>
          </div>
          {hasActiveFilters && (
            <button onClick={() => { setSearchTerm(''); setCategorieFilter(''); setFamilleFilter(''); }}
              className="btn-ghost btn-sm">
              <X className="w-3.5 h-3.5" /> Réinitialiser
            </button>
          )}
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : filtered.length === 0 ? (
        <div className="glass-card p-10 text-center animate-scale-in">
          <Heart className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p className="text-gray-500 font-medium">
            {answeredPrayers?.length === 0 ? 'Aucune action de grâce pour le moment' : 'Aucun résultat'}
          </p>
          <p className="text-xs text-gray-400 mt-1">
            {answeredPrayers?.length === 0 ? 'Les prières exaucées apparaîtront ici' : 'Essayez de modifier les filtres'}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filtered.map((prayer, i) => {
            const colors = CATEGORIE_COLORS[prayer.categorie] || CATEGORIE_COLORS.AUTRE;
            return (
              <div key={prayer.id} className="glass-card p-5 hover:shadow-lg transition-all hover:-translate-y-0.5 animate-slide-up"
                style={{ animationDelay: `${i * 40}ms` }}>
                <div className="flex items-start gap-3 mb-3">
                  <div className={`p-2 rounded-lg ${colors.bg}`}>
                    <Star className={`w-4 h-4 ${colors.text}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{prayer.titre}</h3>
                    <span className={`text-xs ${colors.text}`}>
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
                    <p className="text-sm text-amber-800 dark:text-amber-200 italic line-clamp-4">"{prayer.temoignage}"</p>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
