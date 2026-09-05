import { useState, useCallback, useRef, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { useDictionaries } from '@/hooks/useDictionaries';
import { useI18n } from '@/i18n';
import type { PageResponse, Soul, MakerReport, SoulHistoryEntry, SoulNote, Prayer } from '@/types';
import {
  Search, Heart, X, Loader2, Sparkles, User, Mail, Phone, Calendar,
  MapPin, Briefcase, Activity, MessageSquare, BookOpen, TrendingUp,
  AlertTriangle, Clock, ChevronRight, Church, Users, Shield,
  CheckCircle2, FileText, Eye, EyeOff, Star, Flame, Award,
  Home, Building2, UserCheck, ArrowLeft, ExternalLink,
} from 'lucide-react';
import { Link } from 'react-router-dom';

// ======================== Types ========================

interface SearchResult {
  type: 'AME' | 'UTILISATEUR';
  id: string;
  nom: string;
  prenom?: string;
  nomComplet?: string;
  email?: string;
  telephone?: string;
  statut?: string;
  typeDisciple?: string;
  etatSpirituel?: string;
  faiseurNom?: string;
  familleNom?: string;
  role?: string;
  estChefDeFamille?: boolean;
  dateIntegration?: string;
  dateDernierContact?: string;
  anneesDansEglise?: number;
}

interface CompleteProfile {
  informationsPersonnelles: {
    id: string;
    nom: string;
    prenom?: string;
    nomComplet: string;
    email?: string;
    telephone?: string;
    adresse?: string;
    dateNaissance?: string;
    age?: number;
    profession?: string;
    situationFamiliale?: string;
  };
  informationsEcclesiales: {
    dateIntegration: string;
    dateConversion?: string;
    typeDisciple: string;
    statut: string;
    etatSpirituel: string;
    niveauCroissance: number;
    dateDernierContact?: string;
    anneesDansEglise: number;
  };
  assignations: {
    faiseurId: string;
    faiseurNom?: string;
    faiseurEmail?: string;
    familleId?: string;
    familleNom?: string;
    chefFamilleId?: string;
    chefFamilleNom?: string;
    departementId?: string;
    departementNom?: string;
    departementResponsableNom?: string;
    disciplesSuivis?: { id: string; nom: string; statut: string }[];
    nombreDisciplesSuivis?: number;
  };
  presences: {
    totalRapports: number;
    rapportsSoumis: number;
    tauxSoumission: number;
    totalPresences: number;
    totalAbsences: number;
    tauxPresence: number;
    absencesJustifiees: number;
    historique: {
      id: string;
      semaine: string;
      presencesParCulte: Record<string, boolean>;
      absenceRaison?: string;
      absenceCommentaire?: string;
      soumis: boolean;
      dateSoumission?: string;
    }[];
  };
  historiqueComplet: {
    id: string;
    typeEvenement: string;
    description?: string;
    ancienStatut?: string;
    nouveauStatut?: string;
    ancienFaiseurId?: string;
    nouveauFaiseurId?: string;
    utilisateurId?: string;
    date: string;
  }[];
  notes: { id: string; contenu: string; auteurId: string; date: string }[];
  demandesPriere: {
    id: string; titre: string; description?: string; categorie: string;
    priorite: string; statut: string; temoignage?: string;
    dateCreation: string; dateExaucee?: string;
  }[];
  suivisParalleles: {
    id: string; raison: string; raisonDetail?: string; dateDebut: string;
    dateFin?: string; statut: string; initiateurId: string;
  }[];
  alertes: {
    id: string; typeAlerte: string; message: string;
    dateDeclenchement: string; statut: string;
  }[];
  sorties: {
    id: string; motif: string; motifDetail?: string;
    dateSortie: string; peutReintegrer: boolean;
  }[];
  evaluations?: Record<string, Record<string, { moyenne: number | null; total: number }>>;
  evenementsDisciplinaires?: {
    id: string; categorie: string; typeEvenement: string;
    gravite?: string; titre: string; description?: string;
    dateEvenement: string; resolu: boolean;
    dateResolution?: string; auteurId: string; createdAt: string;
  }[];
  statistiquesDisciplinaires?: { total: number; nonResolus: number };
}

// ======================== Labels ========================

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const STATUT_FALLBACK: Record<string, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti', NOUVEL_ARRIVANT: 'Nouvel arrivant',
  EN_INTEGRATION: 'En intégration', ACTIF: 'Actif', EN_VEILLE: 'En veille',
  DECROCHE: 'Décroché',
};

const TYPE_FALLBACK: Record<string, string> = {
  NOUVEL_ARRIVANT: 'Nouvel arrivant', NOUVEAU_CONVERTI: 'Nouveau converti',
};

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const SPIRITUAL_FALLBACK: Record<string, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti', EN_CROISSANCE: 'En croissance',
  MATURE: 'Mature', EN_DIFFICULTE: 'En difficulté',
};

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const PRIORITE_FALLBACK: Record<string, { label: string; color: string }> = {
  BASSE: { label: 'Basse', color: 'text-gray-500' },
  MOYENNE: { label: 'Moyenne', color: 'text-amber-500' },
  HAUTE: { label: 'Haute', color: 'text-red-500' },
};

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const EVAL_FALLBACK: Record<string, string> = {
  RESPONSABLE: 'Responsable',
  CHEF_FAMILLE: 'Chef de famille',
  FAISEUR: 'Faiseur',
};

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const CATEGORIE_DISCIPLINE_FALLBACK: Record<string, string> = {
  COMPORTEMENT: 'Comportement', CONDUITE: 'Conduite', HABILLEMENT: 'Habillement',
  VIE_SPIRITUELLE: 'Vie spirituelle', PONCTUALITE: 'Ponctualité',
  PARTICIPATION: 'Participation', FIDELITE: 'Fidélité', ENGAGEMENT: 'Engagement',
  REPROCHE: 'Reproche', SANCTION: 'Sanction', LITIGE: 'Litige',
  CONFLIT: 'Conflit', SCANDALE: 'Scandale',
  RELATION_PROBLEMATIQUE: 'Relation problématique',
  FLIRT_INAPPROPRIE: 'Flirt inapproprié',
  DEGAT_MATERIEL: 'Dégât matériel', DEGAT_RELATIONNEL: 'Dégât relationnel',
  RESOLUTION: 'Résolution',
  TEMOIGNAGE_MEMBRE: 'Témoignage membre',
  TEMOIGNAGE_RESPONSABLE: 'Témoignage responsable',
  TEMOIGNAGE_CHEF: 'Témoignage chef',
  COMMENTAIRE_PASTORAL: 'Commentaire pastoral',
  AUTRE: 'Autre',
};

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const GRAVITE_FALLBACK: Record<string, string> = {
  FAIBLE: 'Faible', MOYENNE: 'Moyenne', GRAVE: 'Grave', CRITIQUE: 'Critique',
};

// ======================== Main Component ========================

export default function IntelligentSearchPage() {
  const dictionaries = useDictionaries();
  const { user } = useAuth();
  const [query, setQuery] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSoulId, setSelectedSoulId] = useState<string | null>(null);
  const [showProfile, setShowProfile] = useState(false);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Focus search on mount
  useEffect(() => {
    searchInputRef.current?.focus();
  }, []);

  const { data: searchResults, isLoading: searchLoading } = useQuery({
    queryKey: ['search', searchQuery],
    queryFn: async () => {
      if (!searchQuery.trim()) return { content: [] as SearchResult[] } as PageResponse<SearchResult>;
      const res = await api.get(`/search?q=${encodeURIComponent(searchQuery)}&size=20`);
      return res.data as PageResponse<SearchResult>;
    },
    enabled: searchQuery.length >= 2,
  });

  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['search', 'profile', selectedSoulId],
    queryFn: async () => {
      if (!selectedSoulId) return null;
      const res = await api.get(`/search/profile/${selectedSoulId}`);
      return res.data as CompleteProfile;
    },
    enabled: !!selectedSoulId && showProfile,
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchQuery(query);
    setShowProfile(false);
    setSelectedSoulId(null);
  };

  const handleSelectSoul = (soulId: string) => {
    setSelectedSoulId(soulId);
    setShowProfile(true);
  };

  const handleBack = () => {
    setShowProfile(false);
    setSelectedSoulId(null);
  };

  // RENDER
  return (
    <div className="page-container">
      {/* Page header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Sparkles className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Recherche intelligente</h1>
          </div>
          <p className="page-subtitle">
            Trouvez un membre et affichez sa fiche complète en un clic
          </p>
        </div>
      </div>

      {/* Search bar */}
      <form onSubmit={handleSearch} className="glass-card p-4 mb-6 animate-slide-up">
        <div className="relative group">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 group-focus-within:text-primary-500 transition-colors" />
          <input
            ref={searchInputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Rechercher par nom, prénom, email, téléphone, profession..."
            className="w-full pl-12 pr-12 py-3.5 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-white/20 dark:border-white/[0.06]
                       text-gray-900 dark:text-gray-100 placeholder-gray-400
                       focus:outline-none focus:border-primary-500/50 focus:ring-2 focus:ring-primary-500/20
                       transition-all duration-200 text-base"
          />
          {query && (
            <button
              type="button"
              onClick={() => { setQuery(''); setSearchQuery(''); setShowProfile(false); }}
              className="absolute right-4 top-1/2 -translate-y-1/2 p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              <X className="w-4 h-4 text-gray-400" />
            </button>
          )}
        </div>
        <div className="flex items-center justify-between mt-3">
          <p className="text-xs text-gray-400">
            {searchQuery && searchResults ? `${searchResults.totalElements} résultat(s) trouvé(s)` : 'Saisissez au moins 2 caractères'}
          </p>
          <button type="submit" className="btn-primary btn-sm">
            <Search className="w-4 h-4" /> Rechercher
          </button>
        </div>
      </form>

      {showProfile && profile ? (
        <CompleteMemberProfile
          profile={profile}
          onBack={handleBack}
          isLoading={profileLoading}
        />
      ) : showProfile && profileLoading ? (
        <div className="flex justify-center py-20">
          <div className="text-center">
            <Loader2 className="w-10 h-10 animate-spin text-primary-500 mx-auto mb-4" />
            <p className="text-gray-500">Chargement du profil complet...</p>
          </div>
        </div>
      ) : (
        /* Search results */
        <div className="space-y-3">
          {searchLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {[1,2,3,4,5,6].map(i => (
                <div key={i} className="glass-card p-5 animate-pulse">
                  <div className="skeleton h-5 w-40 mb-3" />
                  <div className="skeleton h-3 w-24 mb-2" />
                  <div className="skeleton h-3 w-32" />
                </div>
              ))}
            </div>
          ) : searchResults && searchResults.content.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {searchResults.content.map((result) => (
                <SearchResultCard
                  key={`${result.type}-${result.id}`}
                  result={result}
                  onSelect={() => {
                    if (result.type === 'AME') {
                      handleSelectSoul(result.id);
                    }
                  }}
                />
              ))}
            </div>
          ) : searchQuery ? (
            <div className="glass-card p-12 text-center animate-scale-in">
              <Search className="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Aucun résultat</h3>
              <p className="text-sm text-gray-500">Aucun membre ne correspond à votre recherche.</p>
            </div>
          ) : (
            <div className="glass-card p-12 text-center animate-scale-in">
              <Sparkles className="w-16 h-16 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
                Recherche intelligente
              </h3>
              <p className="text-sm text-gray-500 max-w-md mx-auto">
                Tapez le nom, l'email, le téléphone ou la profession d'un membre pour obtenir sa fiche complète avec tout son historique.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ======================== Search Result Card ========================

function SearchResultCard({ result, onSelect }: { result: SearchResult; onSelect: () => void }) {
  const dictionaries = useDictionaries();
  const initials = result.prenom
    ? (result.prenom[0] + result.nom[0]).toUpperCase()
    : result.nom.slice(0, 2).toUpperCase();

  return (
    <button
      onClick={onSelect}
      className="glass-card p-4 text-left w-full hover:border-primary-500/30 hover:shadow-glow transition-all duration-200 group"
    >
      <div className="flex items-start gap-3">
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-base font-bold shadow-sm flex-shrink-0
          ${result.type === 'AME'
            ? 'bg-gradient-to-br from-rose-400 to-pink-500 text-white'
            : 'bg-gradient-to-br from-primary-400 to-primary-600 text-white'
          }`}>
          {initials}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate group-hover:text-primary-600 transition-colors">
            {result.nomComplet || result.nom}
          </p>
          <div className="flex flex-wrap gap-1.5 mt-1">
            {result.type === 'AME' && result.statut && (
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400">
                {dictionaries.label('SOUL_STATUS', result.statut) || STATUT_FALLBACK[result.statut] || result.statut}
              </span>
            )}
            {result.type === 'UTILISATEUR' && result.role && (
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-400">
                {result.role}
              </span>
            )}
            {result.type === 'AME' && result.anneesDansEglise !== undefined && (
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400">
                {result.anneesDansEglise} an{result.anneesDansEglise > 1 ? 's' : ''}
              </span>
            )}
          </div>
          <div className="flex items-center gap-3 mt-1.5 text-xs text-gray-400">
            {result.email && (
              <span className="flex items-center gap-1 truncate">
                <Mail className="w-3 h-3 flex-shrink-0" /> {result.email}
              </span>
            )}
            {result.telephone && (
              <span className="flex items-center gap-1">
                <Phone className="w-3 h-3 flex-shrink-0" /> {result.telephone}
              </span>
            )}
          </div>
          {result.type === 'AME' && (result.faiseurNom || result.familleNom) && (
            <div className="flex items-center gap-3 mt-1.5 text-[10px] text-gray-400">
              {result.faiseurNom && <span>Faiseur: {result.faiseurNom}</span>}
              {result.familleNom && <span>Famille: {result.familleNom}</span>}
            </div>
          )}
        </div>
        <ChevronRight className="w-4 h-4 text-gray-300 dark:text-gray-600 mt-3 flex-shrink-0 group-hover:text-primary-500 transition-colors" />
      </div>
    </button>
  );
}

// ======================== Complete Member Profile ========================

function CompleteMemberProfile({
  profile,
  onBack,
  isLoading,
}: {
  profile: CompleteProfile;
  onBack: () => void;
  isLoading: boolean;
}) {
  const dictionaries = useDictionaries();
  const { locale } = useI18n();
  const p = profile.informationsPersonnelles;
  const e = profile.informationsEcclesiales;
  const a = profile.assignations;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Back button */}
      <button onClick={onBack} className="btn-ghost btn-sm inline-flex items-center gap-1">
        <ArrowLeft className="w-4 h-4" /> Retour aux résultats
      </button>

      {/* Header card */}
      <div className="glass-card p-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-primary-400 via-primary-500 to-primary-700
                          flex items-center justify-center shadow-glow text-white text-3xl font-bold">
              {p.prenom?.[0]}{p.nom[0]}
            </div>
            <div>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{p.nomComplet}</h2>
              <div className="flex flex-wrap gap-2 mt-1.5">
                <span className="badge-info text-xs">{dictionaries.label('SOUL_TYPE', e.typeDisciple) || TYPE_FALLBACK[e.typeDisciple] || e.typeDisciple}</span>
                <span className={`badge text-xs ${e.statut === 'ACTIF' ? 'badge-success' : 'badge-warning'}`}>
                  {dictionaries.label('SOUL_STATUS', e.statut) || STATUT_FALLBACK[e.statut] || e.statut}
                </span>
                {p.age !== undefined && (
                  <span className="badge-gray text-xs">{p.age} ans</span>
                )}
              </div>
            </div>
          </div>
          <div className="flex gap-2">
            <Link to={`/souls/${p.id}`} className="btn-secondary btn-sm">
              <ExternalLink className="w-4 h-4" /> Voir la fiche
            </Link>
            <Link to={`/souls/${p.id}/edit`} className="btn-primary btn-sm">
              <Heart className="w-4 h-4" /> Modifier
            </Link>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left column */}
        <div className="lg:col-span-2 space-y-6">
          {/* Personal Info & Church Info */}
          <div className="glass-card p-5 animate-slide-up">
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <User className="w-4 h-4 text-primary-500" /> Informations personnelles
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {[
                { icon: Mail, label: 'Email', value: p.email },
                { icon: Phone, label: 'Téléphone', value: p.telephone },
                { icon: MapPin, label: 'Adresse', value: p.adresse },
                { icon: Calendar, label: 'Date naissance', value: p.dateNaissance ? new Date(p.dateNaissance).toLocaleDateString(locale) : undefined },
                { icon: Briefcase, label: 'Profession', value: p.profession },
                { icon: Heart, label: 'Situation familiale', value: p.situationFamiliale },
              ].filter(f => f.value).map(field => (
                <div key={field.label} className="flex items-start gap-3 p-3 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <field.icon className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="text-xs text-gray-400">{field.label}</p>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{field.value}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '80ms' }}>
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <Church className="w-4 h-4 text-primary-500" /> Parcours ecclésial
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30 text-center">
                <p className="text-2xl font-bold text-primary-500">{e.anneesDansEglise}</p>
                <p className="text-xs text-gray-400 mt-1">Années dans l'église</p>
              </div>
              <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30 text-center">
                <p className="text-2xl font-bold text-amber-500">{e.niveauCroissance}</p>
                <p className="text-xs text-gray-400 mt-1">Niveau croissance</p>
              </div>
              <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30 text-center">
                <p className={`text-sm font-bold ${e.etatSpirituel === 'EN_DIFFICULTE' ? 'text-red-500' : 'text-green-500'}`}>
                  {dictionaries.label('SPIRITUAL_LEVEL', e.etatSpirituel) || SPIRITUAL_FALLBACK[e.etatSpirituel] || e.etatSpirituel}
                </p>
                <p className="text-xs text-gray-400 mt-1">État spirituel</p>
              </div>
              {e.dateConversion && (
                <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-xs text-gray-400">Date de conversion</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {new Date(e.dateConversion).toLocaleDateString(locale)}
                  </p>
                </div>
              )}
              <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                <p className="text-xs text-gray-400">Date d'intégration</p>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                  {new Date(e.dateIntegration).toLocaleDateString(locale)}
                </p>
              </div>
              {e.dateDernierContact && (
                <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-xs text-gray-400">Dernier contact</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {new Date(e.dateDernierContact).toLocaleDateString(locale)}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Assignment Chain */}
          <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '120ms' }}>
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <Users className="w-4 h-4 text-primary-500" /> Chaîne d'assignation
            </h3>
            <div className="space-y-3">
              <div className="flex items-center gap-3 p-3 rounded-xl bg-amber-50/50 dark:bg-amber-900/10">
                <div className="w-8 h-8 rounded-lg bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                  <Building2 className="w-4 h-4 text-amber-600" />
                </div>
                <div>
                  <p className="text-xs text-gray-400">Département</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {a.departementNom || 'Non assigné'}
                  </p>
                  {a.departementResponsableNom && (
                    <p className="text-[10px] text-gray-400">Responsable: {a.departementResponsableNom}</p>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-xl bg-primary-50/50 dark:bg-primary-900/10">
                <div className="w-8 h-8 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <Home className="w-4 h-4 text-primary-600" />
                </div>
                <div>
                  <p className="text-xs text-gray-400">Famille de disciples</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {a.familleNom || 'Non assignée'}
                  </p>
                  {a.chefFamilleNom && (
                    <p className="text-[10px] text-gray-400">Chef: {a.chefFamilleNom}</p>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 rounded-xl bg-green-50/50 dark:bg-green-900/10">
                <div className="w-8 h-8 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                  <UserCheck className="w-4 h-4 text-green-600" />
                </div>
                <div>
                  <p className="text-xs text-gray-400">Faiseur de disciples</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {a.faiseurNom || 'Non assigné'}
                  </p>
                  {a.faiseurEmail && <p className="text-[10px] text-gray-400">{a.faiseurEmail}</p>}
                </div>
              </div>
              {a.nombreDisciplesSuivis !== undefined && a.nombreDisciplesSuivis > 0 && (
                <div className="flex items-center gap-3 p-3 rounded-xl bg-violet-50/50 dark:bg-violet-900/10">
                  <div className="w-8 h-8 rounded-lg bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center">
                    <Users className="w-4 h-4 text-violet-600" />
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">Disciples suivis</p>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {a.nombreDisciplesSuivis} disciple{a.nombreDisciplesSuivis > 1 ? 's' : ''}
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Presence Stats */}
          <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '160ms' }}>
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-primary-500" /> Statistiques de présence
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
              <div className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30 text-center">
                <p className="text-lg font-bold text-green-500">{profile.presences.tauxPresence}%</p>
                <p className="text-[10px] text-gray-400">Présence</p>
              </div>
              <div className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30 text-center">
                <p className="text-lg font-bold text-blue-500">{profile.presences.totalRapports}</p>
                <p className="text-[10px] text-gray-400">Rapports</p>
              </div>
              <div className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30 text-center">
                <p className="text-lg font-bold text-amber-500">{profile.presences.absencesJustifiees}</p>
                <p className="text-[10px] text-gray-400">Abs. justifiées</p>
              </div>
              <div className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30 text-center">
                <p className="text-lg font-bold text-emerald-500">{profile.presences.rapportsSoumis}</p>
                <p className="text-[10px] text-gray-400">Soumis</p>
              </div>
            </div>
            {profile.presences.historique.length > 0 && (
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {profile.presences.historique.slice(0, 8).map((r) => {
                  const cultes = Object.entries(r.presencesParCulte || {});
                  const presents = cultes.filter(([, v]) => v).length;
                  const total = cultes.length;
                  return (
                    <div key={r.id} className="flex items-center justify-between p-2 rounded-lg bg-white/30 dark:bg-gray-800/30 text-xs">
                      <span className="text-gray-500">
                        {new Date(r.semaine).toLocaleDateString(locale, { day: 'numeric', month: 'short' })}
                      </span>
                      <span className={`font-medium ${presents === total ? 'text-green-600' : presents > 0 ? 'text-amber-600' : 'text-red-500'}`}>
                        {presents}/{total}
                      </span>
                      {r.absenceRaison && (
                        <span className="text-gray-400 italic">{r.absenceRaison}</span>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Prayer Requests */}
          {profile.demandesPriere.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '200ms' }}>
              <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-primary-500" /> Demandes de prière ({profile.demandesPriere.length})
              </h3>
              <div className="space-y-2">
                {profile.demandesPriere.map((prayer, i) => (
                  <div key={prayer.id} className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{prayer.titre}</p>
                      <span className={`text-xs font-medium ${PRIORITE_FALLBACK[prayer.priorite]?.color || 'text-gray-500'}`}>
                        {dictionaries.label('PRAYER_PRIORITE', prayer.priorite) || PRIORITE_FALLBACK[prayer.priorite]?.label || prayer.priorite}
                      </span>
                    </div>
                    {prayer.description && (
                      <p className="text-xs text-gray-500 mt-1">{prayer.description}</p>
                    )}
                    <div className="flex items-center gap-2 mt-1.5">
                      <span className={prayer.statut === 'EXAUCE' ? 'badge-success text-[10px]' : 'badge-warning text-[10px]'}>
                        {prayer.statut === 'EXAUCE' ? 'Exaucée' : 'En cours'}
                      </span>
                      {prayer.temoignage && (
                        <span className="text-[10px] text-green-600 italic">" {prayer.temoignage.slice(0, 50)} "</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Full History */}
          {profile.historiqueComplet.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '240ms' }}>
              <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <Activity className="w-4 h-4 text-primary-500" /> Historique complet ({profile.historiqueComplet.length})
              </h3>
              <div className="relative max-h-96 overflow-y-auto">
                <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary-500/30 via-primary-500/20 to-transparent" />
                <div className="space-y-3">
                  {profile.historiqueComplet.slice(0, 20).map((entry, i) => (
                    <div key={entry.id} className="relative pl-10">
                      <div className="absolute left-2.5 top-1.5 w-3 h-3 rounded-full bg-primary-500 border-2 border-white dark:border-gray-900 shadow-[0_0_6px_rgba(22,163,74,0.4)]" />
                      <div className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30">
                        <div className="flex items-center justify-between mb-0.5">
                          <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">{entry.typeEvenement}</span>
                          <span className="text-[10px] text-gray-400">
                            {new Date(entry.date).toLocaleDateString(locale, { day: 'numeric', month: 'short' })}
                          </span>
                        </div>
                        {entry.description && <p className="text-xs text-gray-600 dark:text-gray-400">{entry.description}</p>}
                        {(entry.ancienStatut || entry.nouveauStatut) && (
                          <div className="flex items-center gap-1 mt-1 text-[10px]">
                            {entry.ancienStatut && <span className="badge-gray">{entry.ancienStatut}</span>}
                            {entry.ancienStatut && entry.nouveauStatut && <ChevronRight className="w-3 h-3 text-gray-400" />}
                            {entry.nouveauStatut && <span className="badge-success text-[10px]">{entry.nouveauStatut}</span>}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Right sidebar */}
        <div className="space-y-6">
          {/* Quick Stats */}
          <div className="glass-card p-5 animate-slide-up">
            <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4">Synthèse rapide</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Fidélité</span>
                <div className="flex items-center gap-1">
                  <div className="w-16 h-2 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-green-500 to-emerald-400"
                      style={{ width: `${profile.presences.tauxPresence}%` }}
                    />
                  </div>
                  <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{profile.presences.tauxPresence}%</span>
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Engagement</span>
                <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{profile.presences.rapportsSoumis} rapports</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Suivis parallèles</span>
                <span className="text-xs font-medium">{profile.suivisParalleles.length}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Alertes actives</span>
                <span className={`text-xs font-medium ${profile.alertes.length > 0 ? 'text-red-500' : 'text-green-500'}`}>
                  {profile.alertes.length}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Notes</span>
                <span className="text-xs font-medium">{profile.notes.length}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Sorties</span>
                <span className={`text-xs font-medium ${profile.sorties.length > 0 ? 'text-orange-500' : 'text-gray-500'}`}>
                  {profile.sorties.length}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">Âme depuis</span>
                <span className="text-xs font-medium">{e.anneesDansEglise} an{e.anneesDansEglise > 1 ? 's' : ''}</span>
              </div>
            </div>
          </div>

          {/* Evaluation Scores */}
          {profile.evaluations && Object.keys(profile.evaluations).length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '140ms' }}>
              <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1.5">
                <Star className="w-3 h-3 text-amber-500" /> Évaluations de la chaîne
              </h3>
              <div className="space-y-2.5">
                {Object.entries(profile.evaluations).map(([role, scores]) => {
                  const allNotes = Object.values(scores);
                  const totalAvg = allNotes.reduce((acc, s) => acc + (s.moyenne || 0), 0) / Math.max(allNotes.length, 1);
                  const label = role === 'faiseur' ? 'Faiseur' : role === 'chefFamille' ? 'Chef de famille' : 'Responsable';
                  return (
                    <div key={role} className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30">
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{label}</span>
                        <div className="flex items-center gap-1">
                          <div className="flex">
                            {[1,2,3,4,5].map(i => (
                              <Star key={i} className={`w-2.5 h-2.5 ${i <= Math.round(totalAvg) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                            ))}
                          </div>
                          <span className="text-xs font-medium text-gray-500 ml-0.5">
                            {totalAvg > 0 ? totalAvg.toFixed(1) : '—'}
                          </span>
                        </div>
                      </div>
                      {/* Breakdown by category */}
                      <div className="space-y-0.5">
                        {Object.entries(scores).map(([cat, s]) => (
                          <div key={cat} className="flex items-center justify-between text-[10px]">
                            <span className="text-gray-400">{dictionaries.label('EVALUATION_CATEGORIE', cat) || EVAL_FALLBACK[cat] || cat}</span>
                            <span className="text-gray-500">
                              {s.moyenne !== null && s.moyenne > 0 ? `${s.moyenne.toFixed(1)}/5` : '—'}
                              <span className="text-gray-400 ml-0.5">({s.total})</span>
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Notes */}
          {profile.notes.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '100ms' }}>
              <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Notes ({profile.notes.length})</h3>
              <div className="space-y-2 max-h-60 overflow-y-auto">
                {profile.notes.slice(0, 5).map((note) => (
                  <div key={note.id} className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <p className="text-xs text-gray-700 dark:text-gray-300">{note.contenu}</p>
                    <p className="text-[10px] text-gray-400 mt-1">
                      {new Date(note.date).toLocaleDateString(locale, { day: 'numeric', month: 'short' })}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Alerts */}
          {profile.alertes.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '140ms' }}>
              <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1.5">
                <AlertTriangle className="w-3 h-3 text-red-500" /> Alertes actives
              </h3>
              <div className="space-y-2">
                {profile.alertes.map((alert) => (
                  <div key={alert.id} className="flex items-start gap-2 p-3 rounded-xl bg-red-50/50 dark:bg-red-900/10">
                    <AlertTriangle className="w-3.5 h-3.5 text-red-500 mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-xs font-medium text-red-800 dark:text-red-300">{alert.message}</p>
                      <p className="text-[10px] text-red-500/70">
                        {new Date(alert.dateDeclenchement).toLocaleDateString(locale)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Parallel Followups */}
          {profile.suivisParalleles.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '180ms' }}>
              <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
                Suivis parallèles ({profile.suivisParalleles.length})
              </h3>
              <div className="space-y-2">
                {profile.suivisParalleles.map((sp) => (
                  <div key={sp.id} className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-medium text-gray-900 dark:text-gray-100">{sp.raison}</span>
                      <span className={sp.statut === 'EN_COURS' ? 'badge-warning text-[10px]' : 'badge-success text-[10px]'}>
                        {sp.statut === 'EN_COURS' ? 'En cours' : 'Clôturé'}
                      </span>
                    </div>
                    {sp.raisonDetail && <p className="text-[10px] text-gray-500 mt-0.5">{sp.raisonDetail}</p>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Discipline Events */}
          {profile.evenementsDisciplinaires && profile.evenementsDisciplinaires.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '220ms' }}>
              <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1.5">
                <AlertTriangle className="w-3 h-3 text-amber-500" />
                Suivi disciplinaire ({profile.evenementsDisciplinaires.length})
              </h3>
              {profile.statistiquesDisciplinaires && (
                <div className="flex items-center gap-3 mb-3 text-xs">
                  <span className="text-gray-500">Total: <strong>{profile.statistiquesDisciplinaires.total}</strong></span>
                  {profile.statistiquesDisciplinaires.nonResolus > 0 && (
                    <span className="text-red-500">Non résolus: <strong>{profile.statistiquesDisciplinaires.nonResolus}</strong></span>
                  )}
                </div>
              )}
              <div className="space-y-2 max-h-60 overflow-y-auto">
                {profile.evenementsDisciplinaires.slice(0, 8).map((d) => (
                  <div key={d.id} className={`p-3 rounded-xl ${d.resolu ? 'bg-white/30 dark:bg-gray-800/30' : 'bg-red-50/50 dark:bg-red-900/10'}`}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-medium text-gray-900 dark:text-gray-100">{d.titre}</span>
                      {d.gravite && (
                        <span
                          className="text-[9px] px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300"
                          style={dictionaries.color('DISCIPLINE_GRAVITE', d.gravite)
                            ? { backgroundColor: `${dictionaries.color('DISCIPLINE_GRAVITE', d.gravite)}22`, color: dictionaries.color('DISCIPLINE_GRAVITE', d.gravite) }
                            : undefined}
                        >
                          {dictionaries.label('DISCIPLINE_GRAVITE', d.gravite) || GRAVITE_FALLBACK[d.gravite] || d.gravite}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 text-[10px] text-gray-400">
                      <span>{dictionaries.label('DISCIPLINE_CATEGORIE', d.categorie) || CATEGORIE_DISCIPLINE_FALLBACK[d.categorie] || d.categorie}</span>
                      <span>•</span>
                      <span>{new Date(d.dateEvenement).toLocaleDateString(locale)}</span>
                      {d.resolu && <span className="text-green-600">✓ Résolu</span>}
                    </div>
                    {d.description && (
                      <p className="text-[10px] text-gray-500 mt-1 line-clamp-2">{d.description}</p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Exits */}
          {profile.sorties.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '240ms' }}>
              <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
                Sorties du suivi ({profile.sorties.length})
              </h3>
              <div className="space-y-2">
                {profile.sorties.map((exit) => (
                  <div key={exit.id} className="p-3 rounded-xl bg-orange-50/50 dark:bg-orange-900/10">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-medium text-gray-900 dark:text-gray-100">{exit.motif}</span>
                      <span className="text-[10px] text-gray-400">
                        {new Date(exit.dateSortie).toLocaleDateString(locale)}
                      </span>
                    </div>
                    {exit.motifDetail && <p className="text-[10px] text-gray-500 mt-0.5">{exit.motifDetail}</p>}
                    {exit.peutReintegrer && (
                      <span className="text-[10px] text-green-600 mt-1 inline-flex items-center gap-1">
                        <CheckCircle2 className="w-3 h-3" /> Peut être réintégré
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
