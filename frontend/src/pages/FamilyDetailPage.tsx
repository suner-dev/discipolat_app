import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import type { Family, Soul, FamilyReport, User } from '@/types';
import {
  ArrowLeft, Users, FileText, Heart, UserCog, Loader2, CheckCircle2, X,
  Calendar, Crown, Sparkles, ChevronRight, Clock,
} from 'lucide-react';
import toast from 'react-hot-toast';

export default function FamilyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
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
      const res = await api.get(`/families/${id}/chief/history`);
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
          <button onClick={() => setShowChiefModal(true)} className="btn-secondary btn-sm self-start">
            <UserCog className="w-4 h-4" /> Changer chef
          </button>
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
