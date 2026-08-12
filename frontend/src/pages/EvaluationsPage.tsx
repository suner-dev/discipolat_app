import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import {
  Star, Loader2, MessageSquare, ThumbsUp, Users, Shield,
  UserCheck, BarChart3, Send, AlertCircle, Heart, Eye,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useDictionaries } from '@/hooks/useDictionaries';

interface PersonToEvaluate {
  id: string;
  nom: string;
  role: string;
  categorie: string;
}

interface EvalStats {
  moyenne: number | null;
  total: number;
  repartition: Record<string, number>;
  commentaires: { note: number; commentaire: string; date: string }[];
}

export default function EvaluationsPage() {
  const { user } = useAuth();
  const dictionaries = useDictionaries();
  const queryClient = useQueryClient();
  const isPasteurOrAdmin = !!user && (user.roles.includes('ADMIN') || user.roles.includes('PASTEUR'));
  const [activeTab, setActiveTab] = useState<'evaluate' | 'my-results' | 'all'>('evaluate');
  const [selectedPerson, setSelectedPerson] = useState<PersonToEvaluate | null>(null);
  const [note, setNote] = useState(0);
  const [hoverNote, setHoverNote] = useState(0);
  const [commentaire, setCommentaire] = useState('');
  const [selectedUserForPasteur, setSelectedUserForPasteur] = useState<string | null>(null);
  const [evalPage, setEvalPage] = useState(0);
  const [evalCategorieFilter, setEvalCategorieFilter] = useState<string>('');

  // People I can evaluate
  const { data: peopleToEvaluate, isLoading: peopleLoading } = useQuery({
    queryKey: ['evaluations', 'to-evaluate'],
    queryFn: async () => {
      const res = await api.get('/evaluations/to-evaluate');
      return res.data as PersonToEvaluate[];
    },
    enabled: !isPasteurOrAdmin,
  });

  // My evaluation results
  const { data: myResults, isLoading: resultsLoading } = useQuery({
    queryKey: ['evaluations', 'me'],
    queryFn: async () => {
      const res = await api.get('/evaluations/me');
      return res.data as {
        firstName: string; lastName: string; role: string;
        statistiques: Record<string, EvalStats>;
      };
    },
  });

  // My paginated evaluation list
  const { data: evalList, isLoading: evalListLoading } = useQuery({
    queryKey: ['evaluations', 'me', 'list', evalPage, evalCategorieFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(evalPage), size: '10' });
      if (evalCategorieFilter) params.set('categorie', evalCategorieFilter);
      const res = await api.get(`/evaluations/me/list?${params}`);
      return res.data as {
        content: { id: string; categorie: string; note: number; commentaire: string; date: string }[];
        totalElements: number; totalPages: number; number: number; first: boolean; last: boolean;
      };
    },
  });

  // All evaluations (Pasteur)
  const { data: allEvals, isLoading: allLoading } = useQuery({
    queryKey: ['evaluations', 'all'],
    queryFn: async () => {
      const res = await api.get('/evaluations/all');
      return res.data as Record<string, {
        moyenneGlobale: number; totalEvaluations: number;
        repartitionGlobale: Record<string, number>;
        parPersonne: { id: string; nom: string; moyenne: number; total: number }[];
      }>;
    },
    enabled: isPasteurOrAdmin,
  });

  // User detail (Pasteur)
  const { data: userDetail } = useQuery({
    queryKey: ['evaluations', 'user', selectedUserForPasteur],
    queryFn: async () => {
      const res = await api.get(`/evaluations/user/${selectedUserForPasteur}`);
      return res.data as {
        firstName: string; lastName: string; role: string;
        statistiques: Record<string, EvalStats>;
      };
    },
    enabled: !!selectedUserForPasteur,
  });

  const submitMutation = useMutation({
    mutationFn: async (data: { evalueId: string; categorie: string; note: number; commentaire?: string }) => {
      await api.post('/evaluations', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['evaluations'] });
      toast.success('Évaluation soumise (anonyme)');
      setSelectedPerson(null);
      setNote(0);
      setCommentaire('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const CATEGORIE_FALLBACK: Record<string, string> = {
    RESPONSABLE: 'Responsable', CHEF_FAMILLE: 'Chef de famille', FAISEUR: 'Faiseur',
  };
  const categorieLabel = (c: string) => dictionaries.label('EVALUATION_CATEGORIE', c) || CATEGORIE_FALLBACK[c] || c;

  const CATEGORIE_COLORS: Record<string, string> = {
    RESPONSABLE: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
    CHEF_FAMILLE: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
    FAISEUR: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  };

  const renderStars = (current: number, interactive = false) => (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map((i) => (
        <button
          key={i}
          type="button"
          disabled={!interactive}
          onClick={() => interactive && setNote(i)}
          onMouseEnter={() => interactive && setHoverNote(i)}
          onMouseLeave={() => interactive && setHoverNote(0)}
          className={`transition-all duration-150 ${interactive ? 'cursor-pointer hover:scale-110' : 'cursor-default'}`}
        >
          <Star
            className={`w-5 h-5 ${
              i <= (interactive ? hoverNote || note : current)
                ? 'fill-amber-400 text-amber-400'
                : 'text-gray-300 dark:text-gray-600'
            }`}
          />
        </button>
      ))}
    </div>
  );

  const renderStatsCard = (title: string, stats: EvalStats, color: string) => (
    <div className={`glass-card p-5 ${color}`}>
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{title}</h3>
        <span className="text-xs text-gray-400">{stats.total} évaluation{stats.total > 1 ? 's' : ''}</span>
      </div>
      {stats.moyenne !== null ? (
        <>
          <div className="flex items-center gap-2 mb-3">
            <span className="text-3xl font-bold text-gray-900 dark:text-gray-100">{stats.moyenne}</span>
            <span className="text-sm text-gray-400">/ 5</span>
          </div>
          {renderStars(Math.round(stats.moyenne))}
          {/* Repartition bars */}
          <div className="mt-3 space-y-1">
            {[5, 4, 3, 2, 1].map((i) => {
              const count = stats.repartition[i] || 0;
              const pct = stats.total > 0 ? (count / stats.total) * 100 : 0;
              return (
                <div key={i} className="flex items-center gap-2 text-xs">
                  <span className="w-3 text-gray-400">{i}</span>
                  <div className="flex-1 h-2 rounded-full bg-gray-100 dark:bg-gray-700 overflow-hidden">
                    <div
                      className="h-full rounded-full bg-amber-400 transition-all duration-500"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                  <span className="w-5 text-right text-gray-400">{count}</span>
                </div>
              );
            })}
          </div>
          {/* Comments */}
          {stats.commentaires.length > 0 && (
            <div className="mt-4 pt-3 border-t border-gray-100 dark:border-gray-700">
              <p className="text-[10px] font-semibold text-gray-400 uppercase mb-2">Commentaires anonymes</p>
              <div className="space-y-2 max-h-40 overflow-y-auto">
                {stats.commentaires.map((c, i) => (
                  <div key={i} className="p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                    <div className="flex items-center gap-1 mb-1">
                      {renderStars(c.note)}
                    </div>
                    <p className="text-xs text-gray-600 dark:text-gray-400 italic">"{c.commentaire}"</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      ) : (
        <p className="text-sm text-gray-400">Aucune évaluation reçue</p>
      )}
    </div>
  );

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-lg">
            <ThumbsUp className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Évaluations</h1>
            <p className="page-subtitle">Évaluations anonymes des responsables, chefs de famille et faiseurs</p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="glass-card p-1.5 mb-6 animate-slide-up">
        <div className="flex gap-1">
          {!isPasteurOrAdmin && (
            <button
              onClick={() => setActiveTab('evaluate')}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all ${
                activeTab === 'evaluate'
                  ? 'bg-white dark:bg-gray-800 shadow-sm text-gray-900 dark:text-gray-100'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              <Star className="w-4 h-4" /> Évaluer
            </button>
          )}
          <button
            onClick={() => setActiveTab('my-results')}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all ${
              activeTab === 'my-results'
                ? 'bg-white dark:bg-gray-800 shadow-sm text-gray-900 dark:text-gray-100'
                : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <BarChart3 className="w-4 h-4" /> Mes résultats
          </button>
          {isPasteurOrAdmin && (
            <button
              onClick={() => setActiveTab('all')}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all ${
                activeTab === 'all'
                  ? 'bg-white dark:bg-gray-800 shadow-sm text-gray-900 dark:text-gray-100'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              <Shield className="w-4 h-4" /> Vue Pasteur
            </button>
          )}
        </div>
      </div>

      {/* Tab: Evaluate */}
      {activeTab === 'evaluate' && (
        <div className="animate-slide-up">
          {selectedPerson ? (
            <div className="card p-6 max-w-lg mx-auto">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                    Évaluer {selectedPerson.nom}
                  </h3>
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium mt-1 ${CATEGORIE_COLORS[selectedPerson.categorie]}`}>
                    {categorieLabel(selectedPerson.categorie)}
                  </span>
                </div>
                <button
                  onClick={() => { setSelectedPerson(null); setNote(0); setCommentaire(''); }}
                  className="text-sm text-gray-400 hover:text-gray-600"
                >
                  Retour
                </button>
              </div>
              <p className="text-xs text-gray-400 mb-4 flex items-center gap-1">
                <Eye className="w-3 h-3" /> Votre évaluation est entièrement anonyme
              </p>
              <div className="space-y-4">
                <div>
                  <label className="label">Note</label>
                  <div className="mt-1">{renderStars(0, true)}</div>
                </div>
                <div>
                  <label className="label">Commentaire (optionnel)</label>
                  <textarea
                    className="input mt-1"
                    rows={3}
                    value={commentaire}
                    onChange={(e) => setCommentaire(e.target.value)}
                    placeholder="Partagez votre appréciation..."
                  />
                </div>
                <button
                  onClick={() => submitMutation.mutate({
                    evalueId: selectedPerson.id,
                    categorie: selectedPerson.categorie,
                    note,
                    commentaire: commentaire || undefined,
                  })}
                  disabled={note === 0 || submitMutation.isPending}
                  className="btn-primary w-full"
                >
                  {submitMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                  Soumettre l'évaluation (anonyme)
                </button>
              </div>
            </div>
          ) : (
            <>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <Star className="w-5 h-5 text-primary-500" />
                Personnes à évaluer
              </h2>
              {peopleLoading ? (
                <div className="flex justify-center py-8"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
              ) : peopleToEvaluate && peopleToEvaluate.length > 0 ? (
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {peopleToEvaluate.map((person) => (
                    <button
                      key={person.id + person.categorie}
                      onClick={() => setSelectedPerson(person)}
                      className="glass-card p-4 text-left hover:shadow-md transition-all hover:-translate-y-0.5 group"
                    >
                      <div className="flex items-center gap-3">
                        <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold
                          ${person.categorie === 'RESPONSABLE' ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300' :
                            person.categorie === 'CHEF_FAMILLE' ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' :
                            'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'}`}>
                          {person.nom.split(' ').map(n => n[0]).join('').slice(0, 2)}
                        </div>
                        <div className="flex-1">
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{person.nom}</p>
                          <span className={`inline-flex items-center text-xs font-medium ${CATEGORIE_COLORS[person.categorie]}`}>
                            {categorieLabel(person.categorie)}
                          </span>
                        </div>
                        <Star className="w-4 h-4 text-gray-300 group-hover:text-amber-400 transition-colors" />
                      </div>
                    </button>
                  ))}
                </div>
              ) : (
                <div className="glass-card p-10 text-center">
                  <Star className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                  <p className="text-gray-500">Aucune personne à évaluer pour le moment</p>
                  <p className="text-xs text-gray-400 mt-1">Les personnes que vous pouvez évaluer apparaîtront ici</p>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* Tab: My Results */}
      {activeTab === 'my-results' && (
        <div className="animate-slide-up">
          {resultsLoading ? (
            <div className="flex justify-center py-8"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
          ) : myResults && Object.keys(myResults.statistiques).length > 0 ? (
            <div className="space-y-6">
              {/* Stats cards */}
              <div className="glass-card p-4 flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <UserCheck className="w-5 h-5 text-primary-600" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                    {myResults.firstName} {myResults.lastName}
                  </p>
                  <p className="text-xs text-gray-400">{myResults.role}</p>
                </div>
              </div>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {Object.entries(myResults.statistiques).map(([cat, stats]) => (
                  <div key={cat}>{renderStatsCard(categorieLabel(cat), stats, '')}</div>
                ))}
              </div>

              {/* Paginated list */}
              <div className="glass-card p-4">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                    Détail des évaluations
                  </h3>
                  <select
                    className="input w-auto text-xs"
                    value={evalCategorieFilter}
                    onChange={(e) => { setEvalCategorieFilter(e.target.value); setEvalPage(0); }}
                  >
                    <option value="">Toutes les catégories</option>
                    {(dictionaries.options('EVALUATION_CATEGORIE').length > 0
                      ? dictionaries.options('EVALUATION_CATEGORIE')
                      : Object.entries(CATEGORIE_FALLBACK).map(([value, label]) => ({ code: value, label }))
                    ).map((o) => (
                      <option key={o.code} value={o.code}>{o.label}</option>
                    ))}
                  </select>
                </div>
                {evalListLoading ? (
                  <div className="flex justify-center py-4"><Loader2 className="w-5 h-5 animate-spin text-primary-500" /></div>
                ) : evalList && evalList.content.length > 0 ? (
                  <>
                    <div className="space-y-2">
                      {evalList.content.map((ev) => (
                        <div key={ev.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/30">
                          <div className="flex items-center gap-0.5">
                            {[1,2,3,4,5].map(i => (
                              <Star key={i} className={`w-3.5 h-3.5 ${i <= ev.note ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                            ))}
                          </div>
                          <span className={`px-2 py-0.5 rounded-full text-[10px] font-medium ${CATEGORIE_COLORS[ev.categorie] || ''}`}>
                            {categorieLabel(ev.categorie)}
                          </span>
                          {ev.commentaire && (
                            <p className="flex-1 text-xs text-gray-500 dark:text-gray-400 truncate">
                              "{ev.commentaire}"
                            </p>
                          )}
                          <span className="text-[10px] text-gray-400 whitespace-nowrap">
                            {new Date(ev.date).toLocaleDateString('fr-FR')}
                          </span>
                        </div>
                      ))}
                    </div>
                    {evalList.totalPages > 1 && (
                      <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100 dark:border-gray-700">
                        <p className="text-xs text-gray-400">
                          {evalList.totalElements} évaluation{evalList.totalElements > 1 ? 's' : ''}
                        </p>
                        <div className="flex gap-2">
                          <button
                            onClick={() => setEvalPage(p => Math.max(0, p - 1))}
                            disabled={evalList.first}
                            className="btn-ghost btn-xs text-xs"
                          >
                            Précédent
                          </button>
                          <button
                            onClick={() => setEvalPage(p => p + 1)}
                            disabled={evalList.last}
                            className="btn-ghost btn-xs text-xs"
                          >
                            Suivant
                          </button>
                        </div>
                      </div>
                    )}
                  </>
                ) : (
                  <p className="text-sm text-gray-400 text-center py-4">Aucune évaluation détaillée</p>
                )}
              </div>
            </div>
          ) : (
            <div className="glass-card p-10 text-center">
              <BarChart3 className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Aucune évaluation reçue pour le moment</p>
              <p className="text-xs text-gray-400 mt-1">Les résultats apparaîtront après que d'autres utilisateurs vous aient évalué</p>
            </div>
          )}
        </div>
      )}

      {/* Tab: All evaluations (Pasteur) */}
      {activeTab === 'all' && isPasteurOrAdmin && (
        <div className="animate-slide-up">
          {selectedUserForPasteur && userDetail ? (
            <div>
              <button
                onClick={() => setSelectedUserForPasteur(null)}
                className="btn-ghost btn-sm mb-4"
              >
                ← Retour à la vue globale
              </button>
              <h2 className="text-lg font-semibold mb-4 text-gray-900 dark:text-gray-100">
                {userDetail.firstName} {userDetail.lastName} — {userDetail.role}
              </h2>
              {Object.keys(userDetail.statistiques).length > 0 ? (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                  {Object.entries(userDetail.statistiques).map(([cat, stats]) => (
                    <div key={cat}>{renderStatsCard(categorieLabel(cat), stats, '')}</div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-gray-400">Aucune évaluation pour cet utilisateur</p>
              )}
            </div>
          ) : allLoading ? (
            <div className="flex justify-center py-8"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
          ) : allEvals && Object.keys(allEvals).length > 0 ? (
            <div className="space-y-6">
              {Object.entries(allEvals).map(([cat, data]) => (
                <div key={cat} className="glass-card p-5">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${CATEGORIE_COLORS[cat]}`}>
                        {categorieLabel(cat)}
                      </span>
                      <span className="text-sm text-gray-400">{data.totalEvaluations} évaluation{data.totalEvaluations > 1 ? 's' : ''}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <span className="text-2xl font-bold text-gray-900 dark:text-gray-100">{data.moyenneGlobale}</span>
                      <span className="text-sm text-gray-400">/ 5</span>
                    </div>
                  </div>
                  {/* Per-person ranking */}
                  <div className="space-y-2">
                    {data.parPersonne.map((person, idx) => (
                      <button
                        key={person.id}
                        onClick={() => setSelectedUserForPasteur(person.id)}
                        className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors text-left"
                      >
                        <span className="w-6 text-sm font-bold text-gray-300">{idx + 1}</span>
                        <div className="flex-1">
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{person.nom}</p>
                          <div className="flex items-center gap-2 mt-1">
                            <div className="flex-1 h-1.5 rounded-full bg-gray-100 dark:bg-gray-700 overflow-hidden">
                              <div
                                className="h-full rounded-full bg-gradient-to-r from-amber-400 to-amber-500"
                                style={{ width: `${(person.moyenne / 5) * 100}%` }}
                              />
                            </div>
                            <span className="text-xs font-semibold text-gray-500">{person.moyenne}</span>
                          </div>
                        </div>
                        <span className="text-xs text-gray-400">{person.total} éval.</span>
                        <Eye className="w-4 h-4 text-gray-300" />
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="glass-card p-10 text-center">
              <BarChart3 className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Aucune évaluation dans le système</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
