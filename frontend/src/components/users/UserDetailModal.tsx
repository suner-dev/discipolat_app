import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import {
  X, Loader2, Star, Send, UserRound, Heart, Building2, Users,
  Phone, Mail, Briefcase, Shield, Calendar, CheckCircle, UserX,
  ChevronRight, RefreshCw, Home, Sparkles, Target, FileText, Paperclip,
  StickyNote, Clock, ClipboardList,
} from 'lucide-react';
import toast from 'react-hot-toast';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrateur',
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable',
  CHEF_DE_FAMILLE: 'Chef de famille',
  FAISEUR: 'Faiseur de disciples',
  MEMBRE: 'Membre',
};

const ROLE_COLORS: Record<string, string> = {
  ADMIN: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  PASTEUR: 'bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-300',
  RESPONSABLE: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  CHEF_DE_FAMILLE: 'bg-gold-100 text-gold-700 dark:bg-gold-900/30 dark:text-gold-400',
  FAISEUR: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
  MEMBRE: 'bg-gray-100 text-gray-700 dark:bg-gray-700/50 dark:text-gray-300',
};

const STATUT_BADGES: Record<string, string> = {
  ACTIF: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300',
  EN_INTEGRATION: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300',
  EN_VEILLE: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300',
  DECROCHE: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
};

const CATEGORIE_LABELS: Record<string, string> = {
  RESPONSABLE: 'Responsable',
  CHEF_FAMILLE: 'Chef de famille',
  FAISEUR: 'Faiseur',
  MEMBRE: 'Membre',
};

export function UserDetailModal({ userId, onClose }: { userId: string; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [note, setNote] = useState(0);
  const [hoverNote, setHoverNote] = useState(0);
  const [commentaire, setCommentaire] = useState('');

  const { data: detail, isLoading, refetch } = useQuery({
    queryKey: ['users', userId, 'detail'],
    queryFn: async () => (await api.get(`/users/${userId}/detail`)).data as any,
    enabled: !!userId,
  });

  // Pré-remplir le formulaire avec MA dernière évaluation de cet utilisateur
  const myEval = (detail?.monEvaluation ?? [])[0];
  const hasMyEval = !!myEval && !!detail;

  const saveMutation = useMutation({
    mutationFn: async (payload: { note: number; commentaire?: string }) =>
      (await api.put(`/evaluations/${userId}`, payload)).data,
    onSuccess: () => {
      toast.success(hasMyEval ? 'Évaluation modifiée ✅' : 'Évaluation enregistrée ✅');
      setNote(0);
      setCommentaire('');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['users', userId, 'detail'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const openEvaluationForm = () => {
    if (myEval && !note && !commentaire) {
      setNote(myEval.note ?? 0);
      setCommentaire(myEval.commentaire ?? '');
    }
  };

  const renderStars = (interactive = false) => (
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
            className={`w-6 h-6 ${
              i <= (interactive ? (hoverNote || note) : (myEval?.note ?? 0))
                ? 'fill-amber-400 text-amber-400'
                : 'text-gray-300 dark:text-gray-600'
            }`}
          />
        </button>
      ))}
    </div>
  );

  if (isLoading) {
    return (
      <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
        <div className="glass-card p-10 flex flex-col items-center gap-3" onClick={(e) => e.stopPropagation()}>
          <Loader2 className="w-6 h-6 animate-spin text-primary-500" />
          <p className="text-sm text-gray-400">Chargement de la fiche…</p>
        </div>
      </div>
    );
  }

  const user = detail ?? {};
  const ame = detail?.ame ?? null;
  const evaluations = detail?.evaluations ?? {};
  const scores = Object.entries(evaluations) as [string, { moyenne: number | null; total: number }][];
  const totalEvals = scores.reduce((acc, [, s]) => acc + (s.total ?? 0), 0);

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div className="glass-card p-0 w-full max-w-2xl max-h-[88vh] overflow-hidden flex flex-col" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="p-5 pb-4 border-b border-gray-100 dark:border-gray-700/60 flex items-start justify-between gap-3">
          <div className="flex items-center gap-4 min-w-0">
            <div className={`w-14 h-14 rounded-2xl flex items-center justify-center text-lg font-bold text-white shrink-0 shadow-lg ${
              user.role === 'RESPONSABLE' ? 'bg-gradient-to-br from-amber-500 to-orange-600'
                : user.role === 'FAISEUR' ? 'bg-gradient-to-br from-emerald-500 to-green-600'
                : user.role === 'CHEF_DE_FAMILLE' ? 'bg-gradient-to-br from-yellow-500 to-amber-600'
                : user.role === 'PASTEUR' ? 'bg-gradient-to-br from-violet-500 to-purple-600'
                : 'bg-gradient-to-br from-primary-500 to-primary-600'
            }`}>
              {((user.firstName?.[0] ?? '') + (user.lastName?.[0] ?? '')).toUpperCase() || '?'}
            </div>
            <div className="min-w-0">
              <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100 truncate">
                {user.firstName} {user.lastName}
              </h3>
              <div className="flex items-center gap-2 flex-wrap mt-1">
                <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium ${ROLE_COLORS[user.role] || 'badge-gray'}`}>
                  {ROLE_LABELS[user.role] || user.role}
                </span>
                <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium ${
                  user.statut === 'ACTIVE' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300' : 'badge-gray'
                }`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${user.statut === 'ACTIVE' ? 'bg-green-500' : 'bg-gray-400'}`} />
                  {user.statut === 'ACTIVE' ? 'Actif' : 'Inactif'}
                </span>
                {user.estChefDeFamille && (
                  <span className="badge text-[10px] bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400">Chef de famille</span>
                )}
              </div>
              <p className="text-xs text-gray-400 mt-1 flex items-center gap-1.5">
                <Mail className="w-3 h-3" /> {user.email || '—'}
                {user.dateCreation ? ` · membre depuis ${new Date(user.dateCreation).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}` : ''}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            <button onClick={() => refetch()} className="p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer" title="Rafraîchir">
              <RefreshCw className="w-4 h-4" />
            </button>
            <button onClick={onClose} className="p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer">
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Body */}
        <div className="p-5 overflow-y-auto space-y-5">
          {/* Âme liée */}
          {ame && (
            <div className="rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/50 p-4">
              <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-1.5">
                <UserRound className="w-3.5 h-3.5" /> Fiche âme liée au compte
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2">
                {[
                  { label: 'Nom', value: ame.nomComplet },
                  { label: 'Statut', value: ame.statut, badge: STATUT_BADGES[ame.statut] },
                  { label: 'Type', value: ame.typeDisciple?.replace(/_/g, ' ') },
                  { label: 'Famille', value: ame.familleNom || 'Sans famille' },
                  { label: 'Faiseur', value: ame.faiseurNom || '—' },
                ].map((f) => (
                  <div key={f.label} className="flex items-center gap-2 text-sm">
                    <span className="text-xs text-gray-400 w-16 shrink-0">{f.label}</span>
                    {f.badge ? (
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-medium ${f.badge}`}>
                        {f.value?.replace(/_/g, ' ') ?? '—'}
                      </span>
                    ) : (
                      <span className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{f.value ?? '—'}</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Évaluation */}
          <div className="rounded-2xl bg-gradient-to-br from-amber-50/60 to-orange-50/40 dark:from-amber-900/10 dark:to-orange-900/5 border border-amber-200/40 dark:border-amber-800/30 p-4">
            <div className="flex items-center justify-between flex-wrap gap-2 mb-3">
              <p className="text-[10px] font-semibold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                <Star className="w-3.5 h-3.5 text-amber-500" /> Évaluation
              </p>
              <div className="flex items-center gap-2">
                {hasMyEval ? (
                  <span className="badge text-[10px] badge-success">Vous avez évalué</span>
                ) : (
                  <span className="badge text-[10px] badge-gray">Pas encore évalué</span>
                )}
              </div>
            </div>

            {/* Formulaire donner / modifier */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 mb-3">
              {renderStars(true)}
              <span className="text-xs text-gray-400">
                {note > 0 ? `${note}/5` : 'Choisissez une note'}
              </span>
            </div>
            <textarea
              className="input w-full text-sm mb-3"
              rows={2}
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              placeholder="Appréciation (optionnel) — visible uniquement dans les statistiques globales"
            />
            <div className="flex items-center justify-between flex-wrap gap-2">
              <button
                onClick={() => { openEvaluationForm(); }}
                disabled={hasMyEval || note > 0}
                className="btn-ghost btn-xs"
                title="Reprendre votre dernière évaluation pour la modifier"
              >
                {hasMyEval && 'Modifier ma dernière évaluation'}
              </button>
              <button
                onClick={() => saveMutation.mutate({ note, commentaire: commentaire.trim() || undefined })}
                disabled={note === 0 || saveMutation.isPending}
                className="btn-primary btn-sm cursor-pointer"
              >
                {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                {hasMyEval ? 'Modifier l' + "'" + 'évaluation' : "Donner l'évaluation"}
              </button>
            </div>

            {/* Statistiques reçues */}
            {scores.length > 0 && (
              <div className="mt-4 pt-3 border-t border-amber-200/30 dark:border-amber-800/30">
                <p className="text-[10px] font-medium text-gray-400 mb-2">
                  Évaluations reçues ({totalEvals}) — anonymes
                </p>
                <div className="flex flex-wrap gap-2">
                  {scores.map(([cat, s]) => (
                    <div key={cat} className="px-3 py-1.5 rounded-xl bg-white/60 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-700/50">
                      <span className="text-[10px] text-gray-400 mr-2">{CATEGORIE_LABELS[cat] || cat}</span>
                      <span className="text-xs font-semibold text-amber-600">
                        {s.moyenne != null ? `${s.moyenne.toFixed(1)}/5` : '—'}
                      </span>
                      <span className="text-[10px] text-gray-400 ml-1">({s.total})</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Faiseur : âmes suivies */}
          {Array.isArray(detail?.amesSuivies) && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Heart className="w-3.5 h-3.5 text-emerald-500" /> Âmes suivies
                </p>
                <span className="badge text-[10px] badge-success">{detail.nombreAmesSuivies ?? detail.amesSuivies.length}</span>
              </div>
              {detail.amesSuivies.length === 0 ? (
                <p className="text-xs text-gray-400 text-center py-4 rounded-xl bg-gray-50 dark:bg-gray-800/40">Aucune âme suivie</p>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 max-h-56 overflow-y-auto pr-1">
                  {detail.amesSuivies.map((s: any) => (
                    <div key={s.id} className="flex items-center gap-2.5 p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[10px] font-bold text-white shrink-0 ${
                        s.statut === 'ACTIF' ? 'bg-emerald-500' : s.statut === 'EN_INTEGRATION' ? 'bg-amber-500' : s.statut === 'EN_VEILLE' ? 'bg-blue-500' : 'bg-red-500'
                      }`}>
                        {(s.nom || '?').split(' ').map((p: string) => p?.[0]).join('').slice(0, 2)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{s.nom}</p>
                        <p className="text-[10px] text-gray-400 truncate">
                          {s.familleNom ? `Famille ${s.familleNom}` : 'Sans famille'}
                          {s.hasCompte ? ' · compte lié' : ''}
                        </p>
                      </div>
                      <span className={`text-[9px] font-medium px-1.5 py-0.5 rounded-full shrink-0 ${STATUT_BADGES[s.statut] || 'badge-gray'}`}>
                        {s.statut === 'ACTIF' ? 'Actif' : s.statut === 'EN_INTEGRATION' ? 'Intégr.' : s.statut === 'EN_VEILLE' ? 'Veille' : 'Décroché'}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Faiseur : sorties */}
          {Array.isArray(detail?.sorties) && detail.sorties.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <UserX className="w-3.5 h-3.5 text-red-500" /> Sorties de suivi ({detail.sorties.length})
              </p>
              <div className="space-y-1.5">
                {detail.sorties.map((ex: any, i: number) => (
                  <div key={i} className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-red-50/50 dark:bg-red-900/10 border border-red-200/30 dark:border-red-800/20">
                    <p className="text-sm text-gray-700 dark:text-gray-300 truncate">{ex.motif || 'Sortie du suivi'}</p>
                    <span className="text-[10px] text-gray-400 whitespace-nowrap">
                      {ex.dateSortie ? new Date(ex.dateSortie).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) : '—'}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Responsable : départements + membres */}
          {Array.isArray(detail?.departements) && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Building2 className="w-3.5 h-3.5 text-amber-500" /> Départements dirigés
                </p>
                <span className="badge text-[10px] badge-warning">{detail.departements.length}</span>
              </div>
              <div className="space-y-3">
                {detail.departements.map((dept: any) => (
                  <div key={dept.id} className="rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/50 p-3.5">
                    <div className="flex items-center justify-between mb-2.5">
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
                        <Building2 className="w-4 h-4 text-amber-500" /> {dept.nom}
                      </p>
                      <span className="text-[10px] text-gray-400 flex items-center gap-1">
                        <Users className="w-3 h-3" /> {dept.membres.length} membre{dept.membres.length > 1 ? 's' : ''}
                      </span>
                    </div>
                    {dept.membres.length === 0 ? (
                      <p className="text-xs text-gray-400 text-center py-3">Aucun membre</p>
                    ) : (
                      <div className="space-y-1.5 max-h-52 overflow-y-auto pr-1">
                        {dept.membres.map((m: any) => (
                          <div key={m.id} className="flex items-center gap-2.5 p-2 rounded-xl bg-white/60 dark:bg-gray-900/30 border border-gray-100 dark:border-gray-700/40">
                            <div className={`w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-bold text-white shrink-0 ${
                              m.statut === 'ACTIF' ? 'bg-emerald-500' : m.statut === 'EN_INTEGRATION' ? 'bg-amber-500' : m.statut === 'EN_VEILLE' ? 'bg-blue-500' : 'bg-red-500'
                            }`}>
                              {(m.nomComplet || '?').split(' ').map((p: string) => p?.[0]).join('').slice(0, 2)}
                            </div>
                            <div className="min-w-0 flex-1">
                              <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{m.nomComplet}</p>
                              <p className="text-[10px] text-gray-400 truncate">
                                {m.familleNom ? `Famille ${m.familleNom}` : 'Sans famille'}
                                {m.faiseurNom ? ` · ${m.faiseurNom}` : ''}
                              </p>
                            </div>
                            <div className="flex items-center gap-1 shrink-0">
                              {m.telephone && (
                                <span className="text-[9px] text-gray-400 hidden sm:flex items-center gap-0.5">
                                  <Phone className="w-2.5 h-2.5" /> {m.telephone}
                                </span>
                              )}
                              <span className={`text-[9px] font-medium px-1.5 py-0.5 rounded-full ${STATUT_BADGES[m.statut] || 'badge-gray'}`}>
                                {m.statut === 'ACTIF' ? 'Actif' : m.statut === 'EN_INTEGRATION' ? 'Intégr.' : m.statut === 'EN_VEILLE' ? 'Veille' : 'Décroché'}
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Chef de famille : famille gérée */}
          {detail?.familleGeree && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider flex items-center gap-1.5">
                  <Home className="w-3.5 h-3.5 text-gold-500" /> Famille gérée
                </p>
                <span className="badge text-[10px] bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400">{detail.familleGeree.nom}</span>
              </div>
              <div className="rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/50 p-3.5">
                {detail.familleGeree.membres.length === 0 ? (
                  <p className="text-xs text-gray-400 text-center py-3">Aucun membre dans cette famille</p>
                ) : (
                  <div className="space-y-1.5 max-h-52 overflow-y-auto pr-1">
                    {detail.familleGeree.membres.map((m: any) => (
                      <div key={m.id} className="flex items-center gap-2.5 p-2 rounded-xl bg-white/60 dark:bg-gray-900/30 border border-gray-100 dark:border-gray-700/40">
                        <div className={`w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-bold text-white shrink-0 ${
                          m.statut === 'ACTIF' ? 'bg-emerald-500' : m.statut === 'EN_INTEGRATION' ? 'bg-amber-500' : m.statut === 'EN_VEILLE' ? 'bg-blue-500' : 'bg-red-500'
                        }`}>
                          {(m.nomComplet || '?').split(' ').map((p: string) => p?.[0]).join('').slice(0, 2)}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{m.nomComplet}</p>
                          {m.faiseurNom && <p className="text-[10px] text-gray-400 truncate">Suivi par {m.faiseurNom}</p>}
                        </div>
                        <span className={`text-[9px] font-medium px-1.5 py-0.5 rounded-full shrink-0 ${STATUT_BADGES[m.statut] || 'badge-gray'}`}>
                          {m.statut === 'ACTIF' ? 'Actif' : m.statut === 'EN_INTEGRATION' ? 'Intégr.' : m.statut === 'EN_VEILLE' ? 'Veille' : 'Décroché'}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Dossier du membre : objectifs + rapports du responsable + notes + documents */}
          {(Array.isArray(detail?.dossier) && detail.dossier.length > 0) || (Array.isArray(detail?.dossierDocuments) && detail.dossierDocuments.length > 0) ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider flex items-center gap-1.5">
                  <FileText className="w-3.5 h-3.5 text-primary-500" /> Dossier du membre
                </p>
                {detail?.dossier?.length > 0 && (
                  <span className="badge text-[10px] badge-primary">{detail.dossier.length} département{detail.dossier.length > 1 ? 's' : ''}</span>
                )}
              </div>

              {/* Objectifs & rapports & notes par département */}
              {(detail?.dossier ?? []).map((dept: any) => (
                <div key={dept.departmentId} className="rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/50 p-4 space-y-4">
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
                    <Building2 className="w-4 h-4 text-amber-500" /> {dept.departmentNom || 'Département'}
                  </p>

                  {/* Objectifs */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                        <Target className="w-3 h-3 text-emerald-500" /> Objectifs
                      </p>
                      <span className="badge text-[10px] badge-success">{dept.objectifs?.length ?? 0}</span>
                    </div>
                    {(dept.objectifs ?? []).length === 0 ? (
                      <p className="text-xs text-gray-400 py-2">Aucun objectif défini</p>
                    ) : (
                      <div className="space-y-1.5">
                        {dept.objectifs.map((o: any) => (
                          <div key={o.id} className="p-2.5 rounded-xl bg-white/60 dark:bg-gray-900/30 border border-gray-100 dark:border-gray-700/40">
                            <div className="flex items-center justify-between gap-2 flex-wrap">
                              <p className="text-sm font-medium text-gray-800 dark:text-gray-200 flex items-center gap-1.5">
                                {o.titre}
                                {o.enRetard && (
                                  <span className="inline-flex items-center gap-0.5 text-[9px] font-medium text-red-600 dark:text-red-400">
                                    <Clock className="w-2.5 h-2.5" /> en retard
                                  </span>
                                )}
                              </p>
                              <span className={`text-[9px] font-medium px-1.5 py-0.5 rounded-full ${
                                o.statut === 'ATTEINT' ? 'badge-success' : o.statut === 'EN_COURS' ? 'badge-info' : o.statut === 'ANNULE' ? 'badge-gray' : 'badge-warning'
                              }`}>
                                {o.statut?.replace(/_/g, ' ')}
                              </span>
                            </div>
                            {o.description && <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{o.description}</p>}
                            <div className="flex items-center gap-2 mt-1.5">
                              <div className="flex-1 h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                                <div className="h-full rounded-full bg-emerald-500" style={{ width: `${Math.min(o.avancement ?? 0, 100)}%` }} />
                              </div>
                              <span className="text-[10px] text-gray-400">{o.avancement ?? 0}%</span>
                              {o.echeance && (
                                <span className="text-[10px] text-gray-400 flex items-center gap-0.5">
                                  <Calendar className="w-2.5 h-2.5" />
                                  {new Date(o.echeance).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
                                </span>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Rapports du responsable */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                        <ClipboardList className="w-3 h-3 text-blue-500" /> Rapports du responsable
                      </p>
                      <span className="badge text-[10px] badge-info">{dept.rapportsResponsable?.length ?? 0}</span>
                    </div>
                    {(dept.rapportsResponsable ?? []).length === 0 ? (
                      <p className="text-xs text-gray-400 py-2">Aucun rapport du responsable</p>
                    ) : (
                      <div className="space-y-1.5">
                        {dept.rapportsResponsable.map((r: any) => (
                          <div key={r.id} className="p-2.5 rounded-xl bg-white/60 dark:bg-gray-900/30 border border-gray-100 dark:border-gray-700/40">
                            <div className="flex items-center justify-between gap-2">
                              <span className="text-[9px] font-medium px-1.5 py-0.5 rounded-full badge-info">{r.type?.replace(/_/g, ' ')}</span>
                              <span className="text-[10px] text-gray-400">{r.auteurNom || '—'} · {r.createdAt ? new Date(r.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) : '—'}</span>
                            </div>
                            <p className="text-xs text-gray-700 dark:text-gray-300 mt-1.5 leading-relaxed">{r.contenu}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Notes */}
                  {(dept.notes ?? []).length > 0 && (
                    <div>
                      <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                        <StickyNote className="w-3 h-3 text-amber-500" /> Notes ({dept.notes.length})
                      </p>
                      <div className="space-y-1.5">
                        {dept.notes.map((n: any) => (
                          <div key={n.id} className="p-2.5 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/30 dark:border-amber-800/20">
                            <p className="text-xs text-gray-700 dark:text-gray-300 leading-relaxed">{n.contenu}</p>
                            <p className="text-[10px] text-gray-400 mt-1">{n.auteurNom || '—'} · {n.createdAt ? new Date(n.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) : '—'}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ))}

              {/* Documents du dossier */}
              {(detail?.dossierDocuments ?? []).length > 0 && (
                <div>
                  <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <Paperclip className="w-3 h-3 text-violet-500" /> Documents du dossier ({detail.dossierDocuments.length})
                  </p>
                  <div className="space-y-1.5">
                    {detail.dossierDocuments.map((doc: any) => (
                      <a
                        key={doc.id}
                        href={doc.url || '#'}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center gap-2.5 p-2.5 rounded-xl bg-white/60 dark:bg-gray-900/30 border border-gray-100 dark:border-gray-700/40 hover:border-primary-300 dark:hover:border-primary-700 transition-colors"
                      >
                        <Paperclip className="w-3.5 h-3.5 text-violet-500 shrink-0" />
                        <span className="text-sm text-gray-800 dark:text-gray-200 truncate">{doc.nom || 'Document'}</span>
                        <ChevronRight className="w-3.5 h-3.5 text-gray-400 shrink-0 ml-auto" />
                      </a>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : null}

          {!ame && !Array.isArray(detail?.amesSuivies) && !Array.isArray(detail?.departements) && !detail?.familleGeree && !(Array.isArray(detail?.dossier) && detail.dossier.length > 0) && (
            <div className="flex items-center gap-3 p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 text-sm text-gray-500">
              <Sparkles className="w-4 h-4 text-primary-500 shrink-0" />
              Aucune âme liée à ce compte — l'utilisateur n'est pas encore rattaché à une fiche disciple.
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-gray-100 dark:border-gray-700/60 flex items-center justify-between gap-2">
          <p className="text-[10px] text-gray-400 flex items-center gap-1">
            <Shield className="w-3 h-3" /> Évaluations anonymes pour l'évalué — vos notes restent privées
          </p>
          <button onClick={onClose} className="btn-secondary btn-sm">Fermer</button>
        </div>
      </div>
    </div>
  );
}
