import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { Family, Soul, FamilyReport, User, FamilyRiskAssessment } from '@/types';
import {
  ArrowLeft, Users, FileText, Heart, UserCog, Loader2, CheckCircle2, X,
  Calendar, Crown, Sparkles, ChevronRight, Clock, BarChart3, AlertTriangle, ShieldCheck,
} from 'lucide-react';
import toast from 'react-hot-toast';

export default function FamilyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const [showChiefModal, setShowChiefModal] = useState(false);
  const [newChiefId, setNewChiefId] = useState('');

  const { data: family, isLoading } = useQuery({
    queryKey: ['family', id],
    queryFn: async () => {
      const res = await api.get(`/families/${id}`);
      return res.data as Family;
    },
    enabled: !!id,
  });

  const { data: souls } = useQuery({
    queryKey: ['souls', 'family', id],
    queryFn: async () => {
      const res = await api.get(`/souls?familleId=${id}&size=50`);
      return res.data.content as Soul[];
    },
    enabled: !!id,
  });

  const { data: chefHistory } = useQuery({
    queryKey: ['family', id, 'chief-history'],
    queryFn: async () => {
      const res = await api.get(`/families/${id}/chief-history`);
      return res.data as { ancienChefId: string; nouveauChefId: string; dateChangement: string }[];
    },
    enabled: !!id,
  });

  const { data: potentielsChefs } = useQuery({
    queryKey: ['users', 'potentiels-chefs', id],
    queryFn: async () => {
      const res = await api.get('/users?role=FAISEUR&size=100');
      return res.data.content as User[];
    },
    enabled: showChiefModal,
  });

  const reassignMutation = useMutation({
    mutationFn: async (newChefId: string) => {
      await api.patch(`/families/${id}/chief`, { newChefId });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family', id] });
      queryClient.invalidateQueries({ queryKey: ['families'] });
      setShowChiefModal(false);
      setNewChiefId('');
      toast.success('Chef de famille mis à jour');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const { data: reports } = useQuery({
    queryKey: ['family-reports', id],
    queryFn: async () => {
      const res = await api.get(`/reports/family-weekly?familleId=${id}&size=5`);
      return res.data.content as FamilyReport[];
    },
    enabled: !!id,
  });

  const { data: risk, refetch: refetchRisk } = useQuery({
    queryKey: ['family', id, 'risk'],
    queryFn: async () => {
      const res = await api.get(`/families/${id}/risk`);
      return res.data as FamilyRiskAssessment;
    },
    enabled: !!id,
  });

  const [riskLevel, setRiskLevel] = useState<'NORMAL' | 'SOUS_SURVEILLANCE' | 'A_RISQUE'>('NORMAL');
  const [riskRaison, setRiskRaison] = useState('');
  const [showRiskModal, setShowRiskModal] = useState(false);

  const setRiskMutation = useMutation({
    mutationFn: async ({ niveauRisque, raison }: { niveauRisque: string; raison: string }) => {
      await api.put(`/families/${id}/risk-level`, { niveauRisque, raison });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['family', id] });
      queryClient.invalidateQueries({ queryKey: ['family', id, 'risk'] });
      queryClient.invalidateQueries({ queryKey: ['families'] });
      setShowRiskModal(false);
      setRiskRaison('');
      toast.success('Niveau de risque mis à jour');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return (
      <div className="page-container">
        <div className="animate-fade-in space-y-4">
          <div className="skeleton h-8 w-48 rounded-lg" />
          <div className="skeleton h-64 w-full rounded-2xl" />
        </div>
      </div>
    );
  }

  if (!family) {
    return (
      <div className="page-container">
        <div className="glass-card p-12 text-center animate-scale-in">
          <Users className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500">Famille non trouvée</p>
          <Link to="/families" className="btn-primary btn-sm mt-4 inline-flex">Retour aux familles</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      {/* Back button */}
      <Link to="/families" className="btn-ghost btn-sm mb-4 inline-flex animate-fade-in">
        <ArrowLeft className="w-4 h-4" /> Retour aux familles
      </Link>

      {/* Header */}
      <div className="glass-card p-5 sm:p-6 mb-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center shadow-glow">
              <Users className="w-7 h-7 text-white" />
            </div>
            <div>
              <h1 className="page-title">{family.nom}</h1>
              <div className="flex flex-wrap items-center gap-2 mt-1.5">
                <span className={`badge text-xs ${family.statut === 'ACTIVE' ? 'badge-success' : 'badge-gray'}`}>
                  {family.statut === 'ACTIVE' ? 'Active' : 'Inactive'}
                </span>
                {family.niveauRisque && family.niveauRisque !== 'NORMAL' && (
                  <span className={`badge text-xs ${
                    family.niveauRisque === 'A_RISQUE' ? 'badge-danger' : 'badge-warning'
                  }`}>
                    <AlertTriangle className="w-3 h-3" />
                    {family.niveauRisque === 'A_RISQUE' ? 'Famille à risque' : 'Sous surveillance'}
                  </span>
                )}
                <span className="text-xs text-gray-400 flex items-center gap-1">
                  <Crown className="w-3 h-3 text-gold-500" />
                  Chef: {family.chefFamilleId?.slice(0, 12)}...
                </span>
                <span className="text-xs text-gray-400 flex items-center gap-1">
                  <Calendar className="w-3 h-3" />
                  Créée le {new Date(family.dateCreation).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
                </span>
              </div>
            </div>
          </div>
          <div className="flex gap-2 self-start">
            <Link to={`/families/${id}/faiseur-performance`} className="btn-secondary btn-sm">
              <BarChart3 className="w-4 h-4" /> Performance faiseurs
            </Link>
            <button onClick={() => setShowChiefModal(true)} className="btn-secondary btn-sm">
              <UserCog className="w-4 h-4" /> Changer chef
            </button>
          </div>
        </div>
      </div>

      {/* Change chief modal */}
      {showChiefModal && (
        <div className="modal-overlay" onClick={() => setShowChiefModal(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gold-100 dark:bg-gold-900/30">
                  <Crown className="w-5 h-5 text-gold-600 dark:text-gold-400" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Changer le chef de famille</h3>
                  <p className="text-xs text-gray-500">Sélectionnez un nouveau chef (US-07)</p>
                </div>
              </div>
              <button onClick={() => setShowChiefModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body">
              <select value={newChiefId} onChange={(e) => setNewChiefId(e.target.value)} className="input">
                <option value="">Sélectionner un faiseur...</option>
                {potentielsChefs
                  ?.filter((u) => u.id !== family.chefFamilleId)
                  .map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.firstName} {u.lastName} ({u.email})
                      {u.estChefDeFamille ? ' - Déjà chef' : ''}
                    </option>
                  ))}
              </select>
            </div>
            <div className="modal-footer">
              <button onClick={() => setShowChiefModal(false)} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={() => reassignMutation.mutate(newChiefId)} disabled={!newChiefId || reassignMutation.isPending} className="btn-primary btn-sm">
                {reassignMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                Confirmer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Risk level modal */}
      {showRiskModal && (
        <div className="modal-overlay" onClick={() => setShowRiskModal(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-amber-100 dark:bg-amber-900/30">
                  <AlertTriangle className="w-5 h-5 text-amber-600 dark:text-amber-400" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Niveau de risque</h3>
                  <p className="text-xs text-gray-500">Définir le niveau de la famille {family.nom}</p>
                </div>
              </div>
              <button onClick={() => setShowRiskModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <p className="text-xs font-medium text-gray-500 mb-1.5">Niveau</p>
                <div className="grid grid-cols-3 gap-2">
                  {(['NORMAL', 'SOUS_SURVEILLANCE', 'A_RISQUE'] as const).map((niveau) => (
                    <button
                      key={niveau}
                      onClick={() => setRiskLevel(niveau)}
                      className={`p-3 rounded-xl border-2 text-xs font-semibold transition-all ${
                        riskLevel === niveau
                          ? niveau === 'A_RISQUE'
                            ? 'border-red-500 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400'
                            : niveau === 'SOUS_SURVEILLANCE'
                              ? 'border-amber-500 bg-amber-50 dark:bg-amber-900/20 text-amber-600 dark:text-amber-400'
                              : 'border-green-500 bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400'
                          : 'border-gray-200 dark:border-gray-700 text-gray-500 hover:border-gray-300 dark:hover:border-gray-600'
                      }`}
                    >
                      {niveau === 'A_RISQUE' ? 'À risque' : niveau === 'SOUS_SURVEILLANCE' ? 'Sous surveillance' : 'Normal'}
                    </button>
                  ))}
                </div>
              </div>
              {risk && (
                <div className="p-3 rounded-xl bg-white/30 dark:bg-gray-800/30 text-xs text-gray-500">
                  Score calculé : <span className="font-semibold">{risk.scoreRisque}/100</span> · Niveau suggéré :{' '}
                  <span className="font-semibold">{risk.niveauSuggere === 'A_RISQUE' ? 'À risque' : risk.niveauSuggere === 'SOUS_SURVEILLANCE' ? 'Sous surveillance' : 'Normal'}</span>
                </div>
              )}
              <div>
                <p className="text-xs font-medium text-gray-500 mb-1.5">Raison (optionnel)</p>
                <textarea
                  value={riskRaison}
                  onChange={(e) => setRiskRaison(e.target.value)}
                  className="input"
                  rows={2}
                  placeholder="Ex : 3 âmes perdues ce trimestre, taux de présence en chute"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => setShowRiskModal(false)} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => setRiskMutation.mutate({ niveauRisque: riskLevel, raison: riskRaison })}
                disabled={setRiskMutation.isPending}
                className="btn-primary btn-sm"
              >
                {setRiskMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Content grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Stats card */}
        <div className="glass-card p-5 animate-slide-up">
          <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-primary-500" /> Statistiques
          </h3>
          <div className="space-y-5">
            <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
              <p className="stat-label">Membres suivis</p>
              <p className="stat-value text-2xl">{souls?.length || 0}</p>
              <div className="flex items-center gap-1 mt-1">
                <span className="w-1.5 h-1.5 rounded-full bg-primary-500" />
                <span className="text-[10px] text-gray-400">Disciples dans la famille</span>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
              <p className="stat-label">Chef actuel</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{family.chefFamilleId}</p>
            </div>

            {chefHistory && chefHistory.length > 0 && (
              <div className="p-4 rounded-xl bg-white/30 dark:bg-gray-800/30">
                <p className="stat-label">Dernier changement</p>
                <p className="text-xs text-gray-600 dark:text-gray-400 flex items-center gap-1 mt-1">
                  <Clock className="w-3 h-3" />
                  {new Date(chefHistory[0].dateChangement).toLocaleDateString('fr-FR', {
                    day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit',
                  })}
                </p>
              </div>
            )}

            {/* Chef history timeline */}
            {chefHistory && chefHistory.length > 1 && (
              <div>
                <p className="stat-label mb-2">Anciens chefs</p>
                <div className="space-y-2">
                  {chefHistory.map((entry, i) => (
                    <div key={i} className="flex items-center gap-2 text-xs text-gray-500">
                      <span className="w-1.5 h-1.5 rounded-full bg-gray-300 dark:bg-gray-600 flex-shrink-0" />
                      <span className="truncate">{entry.ancienChefId.slice(0, 8)}...</span>
                      <ChevronRight className="w-3 h-3 flex-shrink-0" />
                      <span className="truncate">{entry.nouveauChefId.slice(0, 8)}...</span>
                      <span className="text-gray-400 ml-auto">{new Date(entry.dateChangement).toLocaleDateString('fr-FR')}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Risk assessment */}
          {risk && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '160ms' }}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-primary-500" /> Évaluation de risque
                </h3>
                {hasRole('PASTEUR') && (
                  <button
                    onClick={() => { setRiskLevel(risk.niveauActuel); setShowRiskModal(true); }}
                    className="btn-secondary btn-sm"
                  >
                    <AlertTriangle className="w-3.5 h-3.5" />
                    Définir le niveau
                  </button>
                )}
              </div>

              {/* Score gauge */}
              <div className="flex items-center gap-4 mb-4">
                <div className="relative w-16 h-16 flex-shrink-0">
                  <svg viewBox="0 0 36 36" className="w-16 h-16 -rotate-90">
                    <circle cx="18" cy="18" r="15.9" fill="none" stroke="currentColor" strokeWidth="3" className="text-gray-100 dark:text-gray-700" />
                    <circle cx="18" cy="18" r="15.9" fill="none" stroke={risk.scoreRisque >= 60 ? '#ef4444' : risk.scoreRisque >= 30 ? '#f59e0b' : '#22c55e'} strokeWidth="3" strokeDasharray={`${risk.scoreRisque} 100`} strokeLinecap="round" />
                  </svg>
                  <span className="absolute inset-0 flex items-center justify-center text-sm font-bold text-gray-900 dark:text-gray-100">
                    {risk.scoreRisque}
                  </span>
                </div>
                <div>
                  <p className="stat-label">Indice de risque</p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">0 (sain) à 100 (critique)</p>
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    <span className={`badge text-[10px] ${risk.niveauActuel === 'NORMAL' ? 'badge-success' : risk.niveauActuel === 'SOUS_SURVEILLANCE' ? 'badge-warning' : 'badge-danger'}`}>
                      Actuel : {risk.niveauActuel === 'A_RISQUE' ? 'À risque' : risk.niveauActuel === 'SOUS_SURVEILLANCE' ? 'Sous surveillance' : 'Normal'}
                    </span>
                    {risk.niveauSuggere !== risk.niveauActuel && (
                      <span className={`badge text-[10px] ${risk.niveauSuggere === 'A_RISQUE' ? 'badge-danger' : risk.niveauSuggere === 'SOUS_SURVEILLANCE' ? 'badge-warning' : 'badge-success'}`}>
                        Suggéré : {risk.niveauSuggere === 'A_RISQUE' ? 'À risque' : risk.niveauSuggere === 'SOUS_SURVEILLANCE' ? 'Sous surveillance' : 'Normal'}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Indicators grid */}
              <div className="grid grid-cols-2 gap-2">
                <div className="p-2.5 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400">Taux de présence</p>
                  <p className={`text-sm font-semibold ${risk.tauxPresence < 65 ? 'text-red-500' : risk.tauxPresence < 80 ? 'text-amber-500' : 'text-green-600'}`}>{risk.tauxPresence}%</p>
                </div>
                <div className="p-2.5 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400">Âmes perdues</p>
                  <p className={`text-sm font-semibold ${risk.amesPerdues > 0 ? 'text-red-500' : 'text-green-600'}`}>{risk.amesPerdues}</p>
                </div>
                <div className="p-2.5 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400">Nouveaux (30 j)</p>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{risk.nouveaux30j}</p>
                </div>
                <div className="p-2.5 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400">En veille</p>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{risk.enVeille}</p>
                </div>
                <div className="p-2.5 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400">Absences (4 sem.)</p>
                  <p className={`text-sm font-semibold ${risk.absences4sem >= 4 ? 'text-red-500' : 'text-gray-900 dark:text-gray-100'}`}>{risk.absences4sem}</p>
                </div>
                <div className="p-2.5 rounded-xl bg-white/30 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400">Litiges / retards</p>
                  <p className={`text-sm font-semibold ${risk.litiges + risk.retards > 0 ? 'text-amber-500' : 'text-gray-900 dark:text-gray-100'}`}>{risk.litiges} / {risk.retards}</p>
                </div>
              </div>
              <p className="text-[10px] text-gray-400 mt-3">Évalué le {new Date(risk.evaluationDate).toLocaleDateString('fr-FR')} · {risk.totalSouls} âmes suivies</p>
            </div>
          )}
        </div>

        {/* Members & Reports */}
        <div className="lg:col-span-2 space-y-6">
          {/* Members */}
          <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '80ms' }}>
            <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <Heart className="w-4 h-4 text-rose-500" /> Membres ({souls?.length || 0})
            </h3>
            {souls && souls.length > 0 ? (
              <div className="space-y-1">
                {souls.map((soul, i) => (
                  <Link key={soul.id} to={`/souls/${soul.id}`}
                    className="flex items-center justify-between p-3 rounded-xl hover:bg-white/50 dark:hover:bg-gray-800/30 transition-all duration-200 animate-fade-in group"
                    style={{ animationDelay: `${i * 30}ms` }}>
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-rose-400 to-rose-600 flex items-center justify-center text-white text-xs font-bold shadow-sm group-hover:shadow transition-shadow">
                        {soul.prenom?.[0] || soul.nom[0]}
                      </div>
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {soul.prenom ? `${soul.prenom} ${soul.nom}` : soul.nom}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'badge-success text-[10px]' : 'badge-info text-[10px]'}>
                        {soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Nouveau converti' : 'Nouvel arrivant'}
                      </span>
                      <ChevronRight className="w-4 h-4 text-gray-300 dark:text-gray-600 group-hover:text-primary-500 transition-colors" />
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <Heart className="w-8 h-8 text-gray-300 dark:text-gray-600 mx-auto mb-2" />
                <p className="text-sm text-gray-500">Aucun membre dans cette famille</p>
              </div>
            )}
          </div>

          {/* Reports */}
          {reports && reports.length > 0 && (
            <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '120ms' }}>
              <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <FileText className="w-4 h-4 text-primary-500" /> Rapports récents
              </h3>
              <div className="space-y-2">
                {reports.map((report, i) => (
                  <div key={report.id}
                    className="flex items-center justify-between p-3 rounded-xl bg-white/30 dark:bg-gray-800/30 animate-fade-in"
                    style={{ animationDelay: `${i * 50}ms` }}>
                    <div className="flex items-center gap-3">
                      <div className={`p-1.5 rounded-lg ${
                        report.statutValidation === 'SOUMIS' ? 'bg-green-100 dark:bg-green-900/30' : 'bg-amber-100 dark:bg-amber-900/30'
                      }`}>
                        <FileText className={`w-4 h-4 ${
                          report.statutValidation === 'SOUMIS' ? 'text-green-600' : 'text-amber-600'
                        }`} />
                      </div>
                      <span className="text-sm text-gray-900 dark:text-gray-100">
                        Semaine du {new Date(report.semaine).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
                      </span>
                    </div>
                    <span className={`badge text-[10px] ${
                      report.statutValidation === 'SOUMIS' ? 'badge-success' :
                      report.statutValidation === 'VU_PAR_PASTEUR' ? 'badge-info' : 'badge-warning'
                    }`}>
                      {report.statutValidation === 'BROUILLON' ? 'Brouillon' :
                       report.statutValidation === 'SOUMIS' ? 'Soumis' :
                       report.statutValidation === 'VU_PAR_RESPONSABLE' ? 'Vu responsable' :
                       'Vu pasteur'}
                    </span>
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
