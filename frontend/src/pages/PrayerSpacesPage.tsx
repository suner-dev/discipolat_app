import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import DataTable from '@/components/shared/DataTable';
import type { Prayer, PageResponse, CategoriePriere, PrioritePriere, VisibilitePriere } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Eye, EyeOff, Heart, Clock, Flame, Star, Tag, MessageSquare,
  Loader2, CheckCircle2, Lock, Globe, Shield, Users, BookOpen,
} from 'lucide-react';

interface SpaceTab {
  key: VisibilitePriere | 'ALL';
  label: string;
  description: string;
  icon: typeof Globe;
  color: string;
  badgeColor: string;
}

const SPACES: SpaceTab[] = [
  {
    key: 'ALL',
    label: 'Tous',
    description: 'Tous les espaces',
    icon: Heart,
    color: 'text-gray-600 dark:text-gray-300',
    badgeColor: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
  },
  {
    key: 'GENERALE',
    label: 'Général',
    description: 'Visible par tous les utilisateurs',
    icon: Globe,
    color: 'text-sky-600',
    badgeColor: 'bg-sky-100 text-sky-800 dark:bg-sky-900/30 dark:text-sky-300',
  },
  {
    key: 'PASTEUR_RESPONSABLE',
    label: 'Pasteur + Resp.',
    description: 'Réservé au Pasteur et aux Responsables',
    icon: Shield,
    color: 'text-purple-600',
    badgeColor: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
  },
  {
    key: 'FAISEUR',
    label: 'Chefs + Faiseurs',
    description: 'Réservé aux Chefs de famille et Faiseurs',
    icon: Users,
    color: 'text-amber-600',
    badgeColor: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
  },
  {
    key: 'PARTAGEE',
    label: 'Famille',
    description: 'Partagé dans le cadre familial',
    icon: BookOpen,
    color: 'text-blue-600',
    badgeColor: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  },
  {
    key: 'PRIVEE',
    label: 'Privé',
    description: 'Prières personnelles',
    icon: Lock,
    color: 'text-gray-600',
    badgeColor: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
  },
];

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const CATEGORIE_FALLBACK: Record<CategoriePriere, string> = {
  SANTE: 'Santé', FAMILLE: 'Famille', TRAVAIL: 'Travail',
  SPIRITUEL: 'Spirituel', AUTRE: 'Autre',
};

const CATEGORIE_COLORS: Record<CategoriePriere, string> = {
  SANTE: 'bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-300',
  FAMILLE: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  TRAVAIL: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
  SPIRITUEL: 'bg-violet-100 text-violet-800 dark:bg-violet-900/30 dark:text-violet-300',
  AUTRE: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
};

const PRIORITE_ICONS: Record<PrioritePriere, typeof Flame> = {
  BASSE: Tag, MOYENNE: Star, HAUTE: Flame,
};

const PRIORITE_COLORS: Record<PrioritePriere, string> = {
  BASSE: 'text-gray-500', MOYENNE: 'text-amber-500', HAUTE: 'text-red-500',
};

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const PRIORITE_FALLBACK: Record<PrioritePriere, string> = {
  BASSE: 'Basse', MOYENNE: 'Moyenne', HAUTE: 'Haute',
};

const VIS_FALLBACK: Record<VisibilitePriere, string> = {
  GENERALE: 'Général',
  PASTEUR_RESPONSABLE: 'Pasteur + Resp.',
  FAISEUR: 'Chefs + Faiseurs',
  PARTAGEE: 'Famille',
  PRIVEE: 'Privé',
};

const VIS_COLORS: Record<VisibilitePriere, string> = {
  GENERALE: 'bg-sky-100 text-sky-800 dark:bg-sky-900/30 dark:text-sky-300',
  PASTEUR_RESPONSABLE: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
  FAISEUR: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
  PARTAGEE: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  PRIVEE: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
};

export default function PrayerSpacesPage() {
  const dictionaries = useDictionaries();
  const [activeTab, setActiveTab] = useState<SpaceTab['key']>('ALL');
  const [page, setPage] = useState(0);

  const { data: allPrayers, isLoading } = useQuery({
    queryKey: ['prayers', 'spaces', page],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '50', page: String(page) });
      const res = await api.get(`/prayers?${params}`);
      return res.data as PageResponse<Prayer>;
    },
  });

  // Filter prayers by current tab
  const filteredPrayers = useMemo(() => {
    if (!allPrayers?.content) return [];
    if (activeTab === 'ALL') return allPrayers.content;
    return allPrayers.content.filter((p) => p.visibilite === activeTab);
  }, [allPrayers, activeTab]);

  // Compute counts per space from ALL prayers
  const spaceCounts = useMemo(() => {
    if (!allPrayers?.content) return {} as Record<string, number>;
    const counts: Record<string, number> = { ALL: allPrayers.content.length };
    for (const p of allPrayers.content) {
      counts[p.visibilite] = (counts[p.visibilite] || 0) + 1;
    }
    return counts;
  }, [allPrayers]);

  // Stats per space
  const totalPrayers = allPrayers?.totalElements || 0;
  const answeredCount = allPrayers?.content.filter((p) => p.statut === 'EXAUCEE').length || 0;
  const activeCount = totalPrayers - answeredCount;

  const columns: ColumnDef<Prayer>[] = [
    {
      header: 'Titre',
      cell: (prayer) => (
        <div>
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{prayer.titre}</p>
          <div className="flex flex-wrap gap-1 mt-1">
            {prayer.contenu && (
              <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">{prayer.contenu}</p>
            )}
            {prayer.statut === 'EXAUCEE' && prayer.temoignage && (
              <span className="inline-flex items-center gap-1 text-xs text-green-600 dark:text-green-400 italic">
                <MessageSquare className="w-3 h-3" />
                "{prayer.temoignage.slice(0, 60)}{prayer.temoignage.length > 60 ? '...' : ''}"
              </span>
            )}
          </div>
        </div>
      ),
    },
    {
      header: 'Catégorie',
      cell: (prayer) => (
        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${CATEGORIE_COLORS[prayer.categorie] || 'bg-gray-100 text-gray-700'}`}>
          {dictionaries.label('PRAYER_CATEGORIE', prayer.categorie) || CATEGORIE_FALLBACK[prayer.categorie] || prayer.categorie}
        </span>
      ),
    },
    {
      header: 'Priorité',
      cell: (prayer) => {
        const Icon = PRIORITE_ICONS[prayer.priorite];
        return (
          <span className={`inline-flex items-center gap-1 text-xs font-medium ${PRIORITE_COLORS[prayer.priorite]}`}>
            <Icon className="w-3 h-3" />
            {dictionaries.label('PRAYER_PRIORITE', prayer.priorite) || PRIORITE_FALLBACK[prayer.priorite] || prayer.priorite}
          </span>
        );
      },
    },
    {
      header: 'Visibilité',
      cell: (prayer) => (
        <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${VIS_COLORS[prayer.visibilite]}`}>
          {prayer.visibilite === 'PRIVEE' ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
          {dictionaries.label('PRAYER_VISIBILITE', prayer.visibilite) || VIS_FALLBACK[prayer.visibilite] || prayer.visibilite}
        </span>
      ),
    },
    {
      header: 'Statut',
      cell: (prayer) => (
        <span className={prayer.statut === 'EXAUCEE' ? 'badge-success' : 'badge-warning'}>
          {prayer.statut === 'EXAUCEE' ? 'Exaucé' : 'En cours'}
        </span>
      ),
    },
    {
      header: 'Date',
      cell: (prayer) => new Date(prayer.dateCreation).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }),
    },
  ];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-lg">
            <Heart className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Espaces de prière</h1>
            <p className="page-subtitle">Vue complète de toutes les prières organisées par niveau de visibilité</p>
          </div>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6 animate-slide-up">
        <div className="glass-card p-4 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
            <Heart className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{totalPrayers}</p>
            <p className="text-xs text-gray-400">Total prières</p>
          </div>
        </div>
        <div className="glass-card p-4 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
            <Clock className="w-5 h-5 text-amber-600 dark:text-amber-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{activeCount}</p>
            <p className="text-xs text-gray-400">En cours</p>
          </div>
        </div>
        <div className="glass-card p-4 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
            <CheckCircle2 className="w-5 h-5 text-green-600 dark:text-green-400" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{answeredCount}</p>
            <p className="text-xs text-gray-400">Exaucées</p>
          </div>
        </div>
      </div>

      {/* Space tabs */}
      <div className="glass-card p-1.5 mb-6 animate-slide-up overflow-x-auto">
        <div className="flex gap-1 min-w-max">
          {SPACES.map((space) => {
            const count = spaceCounts[space.key] || 0;
            const isActive = activeTab === space.key;
            const Icon = space.icon;
            return (
              <button
                key={space.key}
                onClick={() => { setActiveTab(space.key); setPage(0); }}
                className={`relative flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 whitespace-nowrap
                  ${isActive
                    ? 'bg-white dark:bg-gray-800 shadow-sm text-gray-900 dark:text-gray-100'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-white/40 dark:hover:bg-gray-800/30'
                  }`}
                title={space.description}
              >
                <Icon className={`w-4 h-4 ${isActive ? space.color : ''}`} />
                <span>{space.label}</span>
                <span className={`px-1.5 py-0.5 rounded-md text-[10px] font-semibold ${space.badgeColor}`}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : (
        <>
          <DataTable<Prayer>
            columns={columns}
            data={filteredPrayers}
            isLoading={false}
            emptyMessage={
              activeTab === 'ALL'
                ? 'Aucune prière dans la base'
                : `Aucune prière avec la visibilité "${SPACES.find(s => s.key === activeTab)?.label}"`
            }
            emptyIcon={<Heart className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />}
          />

          {allPrayers && allPrayers.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-gray-500">
                Page {allPrayers.number + 1} / {allPrayers.totalPages}
                {activeTab !== 'ALL' && (
                  <span className="ml-1 text-gray-400">
                    ({filteredPrayers.length} sur cette visibilité)
                  </span>
                )}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={allPrayers.first}
                  className="btn-secondary btn-sm"
                >
                  Précédent
                </button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={allPrayers.last}
                  className="btn-secondary btn-sm"
                >
                  Suivant
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
